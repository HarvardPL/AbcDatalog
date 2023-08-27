package edu.harvard.seas.pl.abcdatalog.gui;

/*-
 * #%L
 * AbcDatalog
 * %%
 * Copyright (C) 2016 - 2023 President and Fellows of Harvard College
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

import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

@SuppressWarnings("serial")
public class TextEditor extends JTextArea {
    TextEditor(int rows, int columns) {
        this.setRows(rows);
        this.setColumns(columns);
        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (c == '(') {
                    autocompleteParentheses();
                    e.consume();
                }
                else if (c == ')') {
                    overtypeClosingParenthesis(e);
                }
            }
        });
    }

    /**
     * Is called when ( key is typed. Either inserts () with
     * caret in the middle or encloses text with parentheses
     * if there is any selected text.
     */
    private void autocompleteParentheses() {
        String selectedText = this.getSelectedText();
        if (selectedText != null && !selectedText.isEmpty()) {
            int start = this.getSelectionStart();
            int end = this.getSelectionEnd();
            String newText = '(' + selectedText + ')';
            this.replaceSelection(newText);
            this.setSelectionStart(start);
            this.setSelectionEnd(end + 2);
        } else {
            int pos = this.getCaretPosition();
            this.insert("()", pos);
            this.setCaretPosition(pos + 1);
        }
    }

    /**
     * Is called when ) key is typed. If no text is currently
     * selected and the next character is already an ), does not
     * insert an ) and just moves the cursor forward.
     * Otherwise, handles the keypress as normal.
     */
    private void overtypeClosingParenthesis(KeyEvent e) {
        int pos = this.getCaretPosition();
        try {
            String nextCharacter = this.getText(pos, 1);
            if (this.getSelectedText() == null && nextCharacter.equals(")")) {
                this.setCaretPosition(pos + 1);
                e.consume();
            }
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Deletes the current line and if there is no
     * text, sets the caret to the end of the previous line.
     */
    public void deleteCurrentLine() {
        int pos = this.getCaretPosition();

        try {
            int lineNumber = this.getLineOfOffset(pos);
            int start = this.getLineStartOffset(lineNumber);
            int end = this.getLineEndOffset(lineNumber);
            String currentLine = this.getText(start, end - start);
            if (currentLine.trim().isEmpty() && lineNumber > 0) {
                // Current line is empty, move caret to end of previous line
                int prevLineEnd = this.getLineEndOffset(lineNumber - 1);
                this.setCaretPosition(prevLineEnd - 1);
            } else {
                // Current line is not empty, delete it
                this.replaceRange("", start, end);
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Duplicates the current line, moving the caret
     * to the new line but keeping its position relative
     * to that line.
     */
    public void duplicateCurrentLine() {
        int pos = this.getCaretPosition();

        try {
            int lineNumber = this.getLineOfOffset(pos);
            int start = this.getLineStartOffset(lineNumber);
            int end = this.getLineEndOffset(lineNumber);
            String currentLineText = this.getText(start, end - start);

            boolean endsWithNewLine = currentLineText.endsWith(System.lineSeparator());

            // Calculate the position of the new line
            int newLineStart;
            if (endsWithNewLine) {
                newLineStart = end;
            } else {
                newLineStart = end + System.lineSeparator().length();
                this.insert(System.lineSeparator(), end);
            }

            // Insert duplicate line
            this.insert(currentLineText, newLineStart);

            // Set the caret position to the same place relative to the current line
            int relativeCaretPosition = pos - start;
            this.setCaretPosition(newLineStart + relativeCaretPosition);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Moves the current line up or down.
     * @param direction
     *                  Direction to move the line, where 1 = down and -1 = up.
     */
    public void moveLine(int direction) {
        int pos = this.getCaretPosition();

        try {
            int lineNumber = this.getLineOfOffset(pos);
            int lineCount = this.getLineCount();
            if ((direction < 0 && lineNumber == 0) || (direction > 0 && lineNumber == lineCount - 1)) {
                // Can't move first line up or last line down
                return;
            }

            int start = this.getLineStartOffset(lineNumber);
            int caretOffset = pos - start;

            // Regex \\R matches any newline separator
            String[] lines = this.getText().split("\\R");
            String temp = lines[lineNumber];
            lines[lineNumber] = lines[lineNumber + direction];
            lines[lineNumber + direction] = temp;
            this.setText(String.join(System.lineSeparator(), lines));

            int newLineStart = this.getLineStartOffset(lineNumber + direction);
            int newPos = newLineStart + caretOffset;
            this.setCaretPosition(newPos);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Inserts a comment and a space if there is none,
     * or removes an existing comment character if there
     * is one. Works best if comment character is
     * followed by a space.
     */
    public void toggleComment() {
        int pos = this.getCaretPosition();

        try {
            int lineNumber = this.getLineOfOffset(pos);
            int start = this.getLineStartOffset(lineNumber);
            int end = this.getLineEndOffset(lineNumber);
            String currentLine = this.getText(start, end - start);

            if (currentLine.trim().startsWith("% ")) {
                // Line is already commented, remove the %
                this.replaceRange(currentLine.replaceFirst("% ", ""), start, end);
                this.setCaretPosition(pos - 2);
            } else {
                // Line is not commented, insert the %
                this.insert("% ", start);
                this.setCaretPosition(pos + 2);
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
}
