/*******************************************************************************
 * This file is part of the AbcDatalog project.
 *
 * Copyright (c) 2016, Harvard University
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under
 * the terms of the BSD License which accompanies this distribution.
 *
 * The development of the AbcDatalog project has been supported by the 
 * National Science Foundation under Grant Nos. 1237235 and 1054172.
 *
 * See README for contributors.
 ******************************************************************************/
package abcdatalog.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A wrapper for an executor service that tracks how many tasks are either
 * pending or incomplete, and can be used to block until all tasks have
 * finished.
 */
public class ExecutorServiceCounter {
	/**
	 * Number of pending or incomplete tasks.
	 */
	private final AtomicLong tasks = new AtomicLong(0);
	/**
	 * The executor service to submit tasks to.
	 */
	private final ExecutorService exec;

	/**
	 * Constructs an ExecutorServiceCounter backed by the given ExecutorService.
	 * The supplied ExecutorService should only be accessed via the
	 * ExecutorServiceCounter.
	 * 
	 * @param exec
	 *            the ExecutorService to back the ExecutorServiceCounter
	 */
	public ExecutorServiceCounter(ExecutorService exec) {
		this.exec = exec;
	}

	/**
	 * Reports that a task has been completed.
	 */
	private void taskFinished() {
		if (this.tasks.decrementAndGet() == 0) {
			synchronized (this) {
				this.notifyAll();
			}
		}
	}

	/**
	 * Adds a task to be tracked by this ExecutorServiceCounter. If this method
	 * is invoked from a ForkJoinPool worker thread, the task is forked in that
	 * ForkJoinPool. Otherwise, it is submitted to the ExecutorService backing
	 * this ExecutorServiceCounter.
	 * 
	 * @param task
	 *            the task
	 */
	@SuppressWarnings("serial")
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
			this.exec.execute(new Runnable() {
	
				@Override
				public void run() {
					task.run();
					taskFinished();
				}
				
			});
		}
	}

	/**
	 * Returns whether this ExecutorServiceCounter is aware of any pending or
	 * incomplete tasks.
	 * 
	 * @return whether there are any pending or incomplete tasks
	 */
	private boolean hasUnfinishedTasks() {
		return this.tasks.get() > 0;
	}

	/**
	 * Blocks the calling thread until this ExecutorServiceCounter has no
	 * pending or incomplete tasks.
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
	 * Shutdowns the ExecutorService backing this ExecutorServiceCounter (i.e.,
	 * so it stops accepting new tasks) and blocks until any outstanding tasks
	 * have been completed.
	 */
	public void shutdownAndAwaitTermination() {
		this.exec.shutdown();
		boolean finished = false;
		// keep waiting until we successfully finish all the outstanding tasks
		do {
			try {
				finished = this.exec
						.awaitTermination(Long.MAX_VALUE, TimeUnit.HOURS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} while (!finished);
	}
}
