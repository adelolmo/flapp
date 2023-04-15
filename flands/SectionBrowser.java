package flands;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextPane;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Pop-up window to display book sections without interaction.
 * Specially built for an encounter in book 5, section 114, but also useful
 * for browsing.
 * @see SectionViewNode
 * @author Jonathan Mann
 */
public class SectionBrowser extends JPanel implements ChangeListener, ActionListener {
	private JTextPane textPane;
	private SpinnerListModel bookSpinner;
	private SpinnerNumberModel sectionSpinner;
	private JButton randomButton, defaultButton = null;

	private String currentBook, currentSection;
	private Node currentRoot;
	private int random;

	private static final String browseCommand = "b",
		randomCommand = "?",
		closeCommand = "x";

	private SectionBrowser() {
		setLayout(new BorderLayout());

		textPane = new JTextPane();
		textPane.setForeground(Color.black);
		textPane.setFont(SectionDocument.getPreferredFont());
		textPane.setEditable(false);
		textPane.setEditorKit(new BookEditorKit());

		add(new JScrollPane(textPane));
	}

	public SectionBrowser(String xmlFile) {
		this();

		try {
			FileReader reader = new FileReader(xmlFile);
			showFile(reader);
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public SectionBrowser(String book, boolean sectionChoice, int random) {
		this();

		this.random = random;

		JPanel buttonPanel = new JPanel();
		if (book == null) {
			String[] books = Books.getCanon().getAvailableKeys();
			Arrays.sort(books);
			bookSpinner = new SpinnerListModel(books);
			bookSpinner.setValue(books[0]);
			bookSpinner.addChangeListener(this);
			buttonPanel.add(new JLabel("Book:"));
			buttonPanel.add(new JSpinner(bookSpinner));
			currentBook = bookSpinner.getValue().toString();
		}
		else
			currentBook = book;

		if (sectionChoice) {
			Books.BookDetails b = Books.getCanon().getBook(currentBook);
			sectionSpinner = new SpinnerNumberModel(b.getLowestSection(), b.getLowestSection(), b.getHighestSection(), 1);
			sectionSpinner.addChangeListener(this);
			buttonPanel.add(new JLabel("Section:"));
			buttonPanel.add(new JSpinner(sectionSpinner));

			JButton browseButton = new JButton("Browse");
			browseButton.setActionCommand(browseCommand);
			browseButton.addActionListener(this);
			buttonPanel.add(browseButton);
			defaultButton = browseButton;

			currentSection = sectionSpinner.getValue().toString();
		}

		if (random > 0) {
			randomButton = new JButton("Random");
			randomButton.setActionCommand(randomCommand);
			randomButton.addActionListener(this);
			buttonPanel.add(randomButton);
			if (defaultButton == null)
				defaultButton = randomButton;

			showRandomSection();
			if (random == 0)
				randomButton.setEnabled(false);
		}

		add(buttonPanel, BorderLayout.SOUTH);
	}

	private Window parent = null;
	private void configureWindow(Window w) {
		if (randomButton != null && random == 0) {
			randomButton.setText("Close");
			randomButton.setActionCommand(closeCommand);
			randomButton.setEnabled(true);
		}
		w.pack();
		Dimension screenSize = getToolkit().getScreenSize();
		if (w.getSize().width > screenSize.width / 2) // tends to occur
			w.setSize(screenSize.width / 2, screenSize.height / 3);
		w.setLocationRelativeTo(w);
		parent = w;
	}

	private String baseTitle;
	private JFrame outerFrame;
	private JDialog outerDialog;
	
	public JFrame createFrame(String title) {
		baseTitle = title;
		outerFrame = new JFrame(title);
		outerFrame.getContentPane().add(this);
		outerFrame.getRootPane().setDefaultButton(defaultButton);
		configureWindow(outerFrame);
		return outerFrame;
	}

	public JDialog createDialog(Window owner, String title) {
		baseTitle = title;
		if (owner instanceof Frame)
			outerDialog = new JDialog((Frame)owner, title, true);
		else
			outerDialog = new JDialog((Dialog)owner, title, true);
		outerDialog.getContentPane().add(this);
		outerDialog.getRootPane().setDefaultButton(defaultButton);
		configureWindow(outerDialog);
		return outerDialog;
	}

	public void stateChanged(ChangeEvent evt) {
		if (evt.getSource() == bookSpinner) {
			currentBook = bookSpinner.getValue().toString();
			Books.BookDetails b = Books.getCanon().getBook(currentBook);
			if (sectionSpinner != null) {
				sectionSpinner.setMinimum(Integer.valueOf(b.getLowestSection()));
				sectionSpinner.setMaximum(Integer.valueOf(b.getHighestSection()));
			}
		}
	}

	public void actionPerformed(ActionEvent evt) {
		if (evt.getActionCommand().equals(browseCommand)) {
			showSection(currentBook, sectionSpinner.getValue().toString());
		}
		else if (evt.getActionCommand().equals(randomCommand)) {
			showRandomSection();
			if (random == 0) {
				if (parent == null)
					randomButton.setEnabled(false);
				else {
					randomButton.setText("Close");
					randomButton.setActionCommand(closeCommand);
				}
			}
		}
		else if (evt.getActionCommand().equals(closeCommand)) {
			parent.setVisible(false);
			parent.dispose();
		}
	}

	private ParserHandler handler = null;
	private ParserHandler getHandler() {
		if (handler == null)
			handler = new ParserHandler();
		return handler;
	}

	private boolean showFile(Reader r) {
		try {
			XMLReader reader = XMLReaderFactory.createXMLReader();
			reader.setContentHandler(getHandler());
			reader.parse(new InputSource(r));

			Node oldRoot = currentRoot;
			currentRoot = getHandler().getRootNode();
			if (currentRoot == null) {
				currentRoot = oldRoot;
				System.err.println("Error loading section");
				return false;
			}
			currentRoot.enableAll();
			textPane.setDocument(getHandler().getDocument());
			if (oldRoot != null)
				oldRoot.dispose();
			return true;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean showSection(String book, String section) {
		Books.BookDetails b = Books.getCanon().getBook(book);
		if (b != null) {
			InputStream in = b.getInputStream(section + ".xml");
			getHandler().setBook(book);
			if (in != null && showFile(new InputStreamReader(in))) {
				currentBook = book;
				currentSection = section;
				//Address.setCurrentBookKey(book);
				return true;
			}
		}
		return false;
	}

	public void showRandomSection() {
		String[] availableKeys = Books.getCanon().getAvailableKeys();
		Books.BookDetails b = Books.getCanon().getBook(availableKeys[(int)(Math.random()*availableKeys.length)]);
		System.out.println("Random book=" + b.getKey());

		int min = b.getLowestSection(), max = b.getHighestSection();
		while (true) {
			int section = min + (int)(Math.random() * ((max+1) - min));
			if (showSection(b.getKey(), Integer.toString(section))) {
				if (bookSpinner != null)
					bookSpinner.setValue(b.getKey());
				if (sectionSpinner != null)
					sectionSpinner.setValue(Integer.valueOf(section));
				if (random > 0)
					random--;
				
				if (outerFrame != null)
					outerFrame.setTitle(baseTitle + " (" + b.getTitle() + ")");
				else if (outerDialog != null)
					outerDialog.setTitle(baseTitle + " (" + b.getTitle() + ")");
				return;
			}
		}
	}

	public static void main(String args[]) {
		FLApp.getSingle().init(null);
		FLApp.getSingle().showProfession((int)(Math.random() * Adventurer.PROF_COUNT));
		FLApp.getSingle().setVisible(false);

		SectionBrowser sb = null;
		if (args.length > 0)
			try {
				sb = new SectionBrowser(null, false, Integer.parseInt(args[0]));
			}
			catch (NumberFormatException nfe) {}
		if (sb == null)
			sb = new SectionBrowser(null, true, 10);
		JFrame jf = sb.createFrame("Section Browser");
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jf.setVisible(true);
	}
}
