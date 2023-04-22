package edu.harvard.seas.pl.abcdatalog.gui;

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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.Font;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import javax.swing.undo.UndoManager;

import com.formdev.flatlaf.FlatLightLaf;
import edu.harvard.seas.pl.abcdatalog.ast.PositiveAtom;
import edu.harvard.seas.pl.abcdatalog.ast.validation.DatalogValidationException;
import edu.harvard.seas.pl.abcdatalog.engine.DatalogEngine;
import edu.harvard.seas.pl.abcdatalog.engine.bottomup.sequential.SemiNaiveEngine;
import edu.harvard.seas.pl.abcdatalog.parser.DatalogParseException;
import edu.harvard.seas.pl.abcdatalog.parser.DatalogParser;
import edu.harvard.seas.pl.abcdatalog.parser.DatalogTokenizer;

/**
 * A GUI with a Datalog editor and interpreter.
 *
 */
@SuppressWarnings("serial")
public class DatalogGui extends JFrame {
	private final TextEditor program;
	private final JTextArea results;
	private final JTextField query;
	private final Action queryAction;
	private final JTextPane warning;
	private volatile boolean changedSinceLastSave = false;
	private volatile boolean programLoaded = false;
	private final JFileChooser fileChooser;
	private volatile DatalogEngine engine;
	private final int defaultFontSize = 12;
	private final UndoManager undoManager = new UndoManager();

	/**
	 * Constructs the GUI.
	 */
	public DatalogGui() {
		super("Datalog Editor and Interpreter");
		FlatLightLaf.setup();
		this.setSize(400, 400);
		// Make sure to warn the user when attempting to exit with unsaved work.
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (DatalogGui.this.changedSinceLastSave
						&& !DatalogGui.this.continueWithoutSaving()) {
					return;
				}
				DatalogGui.this.dispose();
			}
		});

		// The user enters source code into a text area.
		JPanel editorPanel = new JPanel(new BorderLayout());
		JLabel editorLabel = new JLabel("Program: ");
		editorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		editorPanel.add(editorLabel, BorderLayout.NORTH);
		this.program = new TextEditor(20, 60);
		this.program.setFont(new Font(Font.DIALOG, Font.PLAIN, this.defaultFontSize));
		this.program.setLineWrap(true);
		this.program.setWrapStyleWord(true);
		this.program.getDocument().addUndoableEditListener(this.undoManager);
		this.program.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				programChanged();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				programChanged();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				// Should not be called.
				throw new AssertionError();
			}
		});
		JScrollPane editorScroll = new JScrollPane(this.program,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		editorPanel.add(editorScroll, BorderLayout.CENTER);
		editorPanel.add(Box.createRigidArea(new Dimension(0, 10)), BorderLayout.SOUTH);

		// The user has to load the code in the text area into the interpreter.
		Box loadPane = Box.createVerticalBox();
		JButton load = new JButton("Load");
		load.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				load();
			}
		});
		load.setAlignmentX(Component.CENTER_ALIGNMENT);
		loadPane.add(Box.createRigidArea(new Dimension(0, 10)));
		loadPane.add(load);

		// A warning message appears if the code loaded in the interpreter is
		// not up to date with the code in the program text area.
		StyledDocument document = new DefaultStyledDocument();
		Style defaultStyle = document.getStyle(StyleContext.DEFAULT_STYLE);
		StyleConstants.setAlignment(defaultStyle, StyleConstants.ALIGN_CENTER);
		this.warning = new JTextPane(document);
		this.warning.setEditable(false);
		this.warning.setOpaque(false);
		loadPane.add(this.warning);

		// The user enters a query and requests the interpreter to run it.
		this.queryAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				query();
			}
		};
		this.queryAction.setEnabled(false);
		JPanel queryPane = new JPanel();
		queryPane.setLayout(new BoxLayout(queryPane, BoxLayout.LINE_AXIS));
		JLabel queryLabel = new JLabel("Query: ");
		queryPane.add(queryLabel);
		this.query = new JTextField(40);
		this.query.setAction(this.queryAction);
		queryPane.add(this.query);
		JButton run = new JButton();
		run.setAction(this.queryAction);
		run.setText("Run");
		queryPane.add(Box.createRigidArea(new Dimension(5, 0)));
		queryPane.add(run);
		loadPane.add(queryPane);
		loadPane.add(Box.createRigidArea(new Dimension(0, 20)));

		// The results are printed in a non-editable text area.
		JPanel resultsLabelPanel = new JPanel(new BorderLayout());
		JLabel resultsLabel = new JLabel("Results: ");
		resultsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		resultsLabelPanel.add(resultsLabel);
		loadPane.add(resultsLabelPanel);
		this.results = new JTextArea(10, 60);
		this.results.setEditable(false);
		this.results.setText("No program loaded.");
		JScrollPane resultsScroll = new JScrollPane(this.results,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		JPanel evaluatorPanel = new JPanel(new BorderLayout());
		evaluatorPanel.add(loadPane, BorderLayout.NORTH);
		evaluatorPanel.add(resultsScroll, BorderLayout.CENTER);

		JSplitPane mainPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorPanel, evaluatorPanel);
		mainPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		mainPane.setOneTouchExpandable(true);

		// Set up a basic menu bar.
		JMenuItem openMenuItem = createMenuItem(
				"Open",
				'o',
				KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK),
				e -> open()
		);
		JMenuItem saveMenuItem = createMenuItem(
				"Save",
				's',
				KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK),
				e -> save()
		);
		JMenuItem exitMenuItem = createMenuItem(
				"Exit",
				'e',
				KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK),
				e -> exit()
		);
		JMenuItem undoMenuItem = createMenuItem(
				"Undo",
				'u',
				KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK),
				e -> {
					if (undoManager.canUndo()) undoManager.undo();
				}
		);
		JMenuItem redoMenuItem = createMenuItem(
				"Redo",
				'r',
				KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK),
				e -> {
					if (undoManager.canRedo()) undoManager.redo();
				}
		);
		JMenuItem cutMenuItem = createMenuItem(
				"Cut",
				'c',
				KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK),
				e -> program.cut()
		);
		JMenuItem copyMenuItem = createMenuItem(
				"Copy",
				'o',
				KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK),
				e -> program.copy()
		);
		JMenuItem pasteMenuItem = createMenuItem(
				"Paste",
				'p',
				KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK),
				e -> program.paste()
		);
		JMenuItem selectAllMenuItem = createMenuItem(
				"Select all",
				's',
				KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK),
				e -> program.selectAll()
		);
		JMenuItem clearAllMenuItem = createMenuItem(
				"Clear all",
				'a',
				KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK),
				e -> program.setText("")
		);
		JMenuItem deleteLineMenuItem = createMenuItem(
				"Delete line",
				'd',
				KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK),
				e -> program.deleteCurrentLine()
		);
		JMenuItem duplicateLineMenuItem = createMenuItem(
				"Duplicate line",
				'l',
				KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK),
				e -> program.duplicateCurrentLine()
		);
		JMenuItem moveLineUpMenuItem = createMenuItem(
				"Move line up",
				'm',
				KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK),
				e -> program.moveLine(-1)
		);
		JMenuItem moveLineDownMenuItem = createMenuItem(
				"Move line down",
				'e',
				KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK),
				e -> program.moveLine(1)
		);
		JMenuItem toggleLineComment = createMenuItem(
				"Toggle comment",
				't',
				KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, KeyEvent.CTRL_DOWN_MASK),
				e -> program.toggleComment()
		);
		JMenuItem zoomInMenuItem = createMenuItem(
				"Zoom in",
				'i',
				KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, KeyEvent.CTRL_DOWN_MASK),
				e -> zoomText(2)
		);
		JMenuItem zoomOutMenuItem = createMenuItem(
				"Zoom out",
				'o',
				KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, KeyEvent.CTRL_DOWN_MASK),
				e -> zoomText(-2)
		);
		JMenuItem restoreZoomMenuItem = createMenuItem(
				"Default zoom",
				'd',
				KeyStroke.getKeyStroke(KeyEvent.VK_0, KeyEvent.CTRL_DOWN_MASK),
				e -> zoomText(0)
		);
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic('f');
		JMenu editMenu = new JMenu("Edit");
		editMenu.setMnemonic('e');
		JMenu viewMenu = new JMenu("View");
		viewMenu.setMnemonic('v');
		fileMenu.add(openMenuItem);
		fileMenu.add(saveMenuItem);
		fileMenu.addSeparator();
		fileMenu.add(exitMenuItem);
		editMenu.add(undoMenuItem);
		editMenu.add(redoMenuItem);
		editMenu.addSeparator();
		editMenu.add(cutMenuItem);
		editMenu.add(copyMenuItem);
		editMenu.add(pasteMenuItem);
		editMenu.addSeparator();
		editMenu.add(selectAllMenuItem);
		editMenu.add(clearAllMenuItem);
		editMenu.addSeparator();
		editMenu.add(deleteLineMenuItem);
		editMenu.add(duplicateLineMenuItem);
		editMenu.add(moveLineUpMenuItem);
		editMenu.add(moveLineDownMenuItem);
		editMenu.add(toggleLineComment);
		viewMenu.add(zoomInMenuItem);
		viewMenu.add(zoomOutMenuItem);
		viewMenu.add(restoreZoomMenuItem);
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(fileMenu);
		menuBar.add(editMenu);
		menuBar.add(viewMenu);
		this.setJMenuBar(menuBar);

		this.add(mainPane);
		this.pack();
		this.setVisible(true);

		this.fileChooser = new JFileChooser();
	}

	/**
	 * Is called when the load button is clicked.
	 */
	private void load() {
		this.warning.setText("");
		DatalogTokenizer t = new DatalogTokenizer(new StringReader(
				this.program.getText()));
		try {
			this.engine = SemiNaiveEngine.newEngine();
			this.engine.init(DatalogParser.parseProgram(t));
			this.results.setText("Program successfully loaded.");
			this.queryAction.setEnabled(true);
			this.programLoaded = true;
		} catch (DatalogParseException e) {
			this.results.setText("Error parsing program: " + e.getMessage());
			this.queryAction.setEnabled(false);
			this.programLoaded = false;
		} catch (DatalogValidationException e) {
			this.results.setText("Error validating program: " + e.getMessage());
			this.queryAction.setEnabled(false);
			this.programLoaded = false;
		}
	}

	/**
	 * Is called when the user tries to make a query.
	 */
	private void query() {
		String text = this.query.getText();
		if (!text.endsWith("?")) {
			text += "?";
		}
		DatalogTokenizer t = new DatalogTokenizer(new StringReader(text));
		try {
			Set<PositiveAtom> facts = this.engine.query(DatalogParser.parseQuery(t));
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(buffer);
			for (String fact : facts.stream().map(f -> f.toString()).sorted().collect(Collectors.toList())) {
				ps.println(fact);
			}
			this.results.setText(buffer.toString());
		} catch (DatalogParseException e) {
			this.results.setText("Error parsing query: " + e.getMessage());
		}
	}

	/**
	 * Is called when the user attempts to open a new file.
	 */
	private void open() {
		int returnVal = this.fileChooser.showOpenDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			if (this.changedSinceLastSave && !this.continueWithoutSaving()) {
				return;
			}
			File file = this.fileChooser.getSelectedFile();
			try {
				byte[] bytes = Files.readAllBytes(file.toPath());
				this.program.setText(new String(bytes));
				this.changedSinceLastSave = false;
			} catch (IOException e) {
				JOptionPane.showMessageDialog(this,
						"Could not read from file: " + e.getMessage(),
						"IO Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	/**
	 * Is called when the user tries to save the current code in the editor.
	 */
	private void save() {
		int returnVal = this.fileChooser.showSaveDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = this.fileChooser.getSelectedFile();
			try {
				Files.write(file.toPath(), this.program.getText().getBytes());
				this.changedSinceLastSave = false;
			} catch (IOException e) {
				JOptionPane.showMessageDialog(this, "Could not write to file: "
						+ e.getMessage(), "IO Error",
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	/**
	 * Is called when the user clicks the exit button in the menu.
	 */
	private void exit() {
		this.dispatchEvent(new WindowEvent(DatalogGui.this,
				WindowEvent.WINDOW_CLOSING));
	}

	/**
	 * Is called when the user tries to open a new file or exit the GUI without
	 * saving her work.
	 *
	 * @return whether to continue without saving
	 */
	private boolean continueWithoutSaving() {
		int response = JOptionPane.showConfirmDialog(this,
				"The current program has changed since the "
						+ "last save. Continue without saving?",
				"Unsaved Work", JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE);
		return response == JOptionPane.YES_OPTION;
	}

	/**
	 * Is called when the user edits the text in the code editor.
	 */
	private void programChanged() {
		this.changedSinceLastSave = true;
		if (this.programLoaded) {
			this.warning
					.setText("WARNING: Program has changed since last load.");
		}
	}

	/**
	 * Is called when the user zooms in, out, or restores default zoom.
	 *
	 * @param increment
	 * 				   the increment to add to the old font size
	 */
	private void zoomText(int increment) {
		Font oldFont = program.getFont();
		int newSize = increment == 0 ? defaultFontSize : oldFont.getSize() + increment;
		Font newFont = new Font(oldFont.getFontName(), oldFont.getStyle(), newSize);
		program.setFont(newFont);
	}

	/**
	 * Creates and returns a JMenuItem
	 *
	 * @param text
	 * 			  text to be displayed on the menu item
	 * @param mnemonic
	 * 			  mnemonic for quick access
	 * @param accelerator
	 * 			  keyboard accelerator to trigger the menu item
	 * @param listener
	 *            action listener for the menu item
	 * @return a JMenuItem
	 *
	 */
	private JMenuItem createMenuItem(String text, char mnemonic, KeyStroke accelerator, ActionListener listener) {
		JMenuItem menuItem = new JMenuItem(text);
		menuItem.setMnemonic(mnemonic);
		menuItem.setAccelerator(accelerator);
		menuItem.addActionListener(listener);
		return menuItem;
	}

	/**
	 * Runs the GUI.
	 *
	 * @param args
	 *            the command-line arguments (not used)
	 */
	public static void main(String[] args) {
		new DatalogGui();
	}

}
