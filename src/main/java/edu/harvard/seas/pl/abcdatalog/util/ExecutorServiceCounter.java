package edu.harvard.seas.pl.abcdatalog.util;

/*-
 * #%L
 * AbcDatalog
 * %%
 * Copyright (C) 2016 - 2021 President and Fellows of Harvard College
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the President and Fellows of Harvard College nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A wrapper for an executor service that tracks how many tasks are either pending or incomplete,
 * and can be used to block until all tasks have finished.
 */
public class ExecutorServiceCounter {
  /** Number of pending or incomplete tasks. */
  private final AtomicLong tasks = new AtomicLong(0);

  /** The executor service to submit tasks to. */
  private final ExecutorService exec;

  /**
   * Constructs an ExecutorServiceCounter backed by the given ExecutorService. The supplied
   * ExecutorService should only be accessed via the ExecutorServiceCounter.
   *
   * @param exec the ExecutorService to back the ExecutorServiceCounter
   */
  public ExecutorServiceCounter(ExecutorService exec) {
    this.exec = exec;
  }

  /** Reports that a task has been completed. */
  private void taskFinished() {
    if (this.tasks.decrementAndGet() == 0) {
      synchronized (this) {
        this.notifyAll();
      }
    }
  }

  /**
   * Adds a task to be tracked by this ExecutorServiceCounter. If this method is invoked from a
   * ForkJoinPool worker thread, the task is forked in that ForkJoinPool. Otherwise, it is submitted
   * to the ExecutorService backing this ExecutorServiceCounter.
   *
   * @param task the task
   */
  public void submitTask(Runnable task) {
    this.tasks.incrementAndGet();
    if (RecursiveAction.inForkJoinPool()) {
      new RecursiveAction() {

        @Override
        protected void compute() {
          task.run();
          taskFinished();
        }
      }.fork();
    } else {
      this.exec.execute(
          new Runnable() {

            @Override
            public void run() {
              task.run();
              taskFinished();
            }
          });
    }
  }

  /**
   * Returns whether this ExecutorServiceCounter is aware of any pending or incomplete tasks.
   *
   * @return whether there are any pending or incomplete tasks
   */
  private boolean hasUnfinishedTasks() {
    return this.tasks.get() > 0;
  }

  /**
   * Blocks the calling thread until this ExecutorServiceCounter has no pending or incomplete tasks.
   */
  public void blockUntilFinished() {
    synchronized (this) {
      while (this.hasUnfinishedTasks()) {
        try {
          this.wait();
        } catch (InterruptedException e) {
          // we've been interrupted
        }
      }
    }
  }

  /**
   * Shutdowns the ExecutorService backing this ExecutorServiceCounter (i.e., so it stops accepting
   * new tasks) and blocks until any outstanding tasks have been completed.
   */
  public void shutdownAndAwaitTermination() {
    this.exec.shutdown();
    boolean finished = false;
    // keep waiting until we successfully finish all the outstanding tasks
    do {
      try {
        finished = this.exec.awaitTermination(Long.MAX_VALUE, TimeUnit.HOURS);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    } while (!finished);
  }
}
