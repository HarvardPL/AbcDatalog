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
package abcdatalog.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import abcdatalog.ast.PositiveAtom;
import abcdatalog.ast.validation.DatalogValidationException;
import abcdatalog.engine.DatalogEngine;
import abcdatalog.engine.bottomup.sequential.SemiNaiveEngine;
import abcdatalog.parser.DatalogParseException;
import abcdatalog.parser.DatalogParser;
import abcdatalog.parser.DatalogTokenizer;

/**
 * A GUI with a Datalog editor and interpreter.
 *
 */
@SuppressWarnings("serial")
public class DatalogGui extends JFrame {
	private final JTextArea program, results;
	private final JTextField query;
	private final Action queryAction;
	private final JTextPane warning;
	private volatile boolean changedSinceLastSave = false;
	private volatile boolean programLoaded = false;
	private final JFileChooser fileChooser;
	private volatile DatalogEngine engine;

	/**
	 * Constructs the GUI.
	 */
	public DatalogGui() {
		super("Datalog Editor and Interpreter");
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
		this.program = new JTextArea(20, 60);
		this.program.setLineWrap(true);
		this.program.setWrapStyleWord(true);
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
		this.warning.setBackground(this.getBackground());
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
		JMenuItem open = new JMenuItem("Open");
		open.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				open();
			}
		});
		JMenuItem save = new JMenuItem("Save");
		save.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				save();
			}
		});
		JMenuItem exit = new JMenuItem("Exit");
		exit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				exit();
			}
		});
		JMenu menu = new JMenu("File");
		menu.add(open);
		menu.add(save);
		menu.addSeparator();
		menu.add(exit);
		JMenuBar bar = new JMenuBar();
		bar.add(menu);
		this.setJMenuBar(bar);

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
			this.engine = new SemiNaiveEngine();
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
		DatalogTokenizer t = new DatalogTokenizer(new StringReader(
				this.query.getText()));
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
	 * Runs the GUI.
	 * 
	 * @param args
	 *            the command-line arguments (not used)
	 */
	public static void main(String[] args) {
		new DatalogGui();
	}

}
