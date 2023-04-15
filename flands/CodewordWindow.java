package flands;

import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.ComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Window to display the known (named) codewords that have been set for a character.
 * @see Codewords
 * @author Jonathan Mann
 */
public class CodewordWindow extends JDialog implements ActionListener, ChangeListener {
	private class BookChoiceModel extends Books.BookListModel implements ComboBoxModel {
		private int selectedIndex;
		private int getSelectedIndex() { return selectedIndex; }

		private int getBookIndex(String key) {
			for (int i = 0; i < getSize(); i++) {
				Books.BookDetails book = getBook(i);
				if (book.getKey().equals(key))
					return i;
			}
			return -1;
		}
		
		public void setSelectedItem(Object anItem) {
			for (int i = 0; i < getSize(); i++) {
				Object item = getElementAt(i);
				if (item.equals(anItem)) {
					selectedIndex = i;
					return;
				}
			}
		}

		public Object getSelectedItem() {
			return getElementAt(selectedIndex);
		}
	}
	
	private BookChoiceModel bookChoice;
	private JComboBox bookComboBox;
	private JScrollPane codewordPane;
	private JTextArea notesArea;
	private JTabbedPane tabbedPane;
	private JPanel codewordPanel, notesPanel;
	
	public CodewordWindow(Frame parent) {
		super(parent, "Quest Notes", false);
		
		bookChoice = new BookChoiceModel();
		bookComboBox = new JComboBox(bookChoice);
		bookComboBox.setEditable(false);
		bookComboBox.setSelectedIndex(bookChoice.getBookIndex(Address.getCurrentBookKey()));
		bookComboBox.addActionListener(this);
		
		codewordPane = new JScrollPane();
		actionPerformed(null);
		
		notesArea = new JTextArea();
		resetNotes();
		
		GridBagLayout gbl = new GridBagLayout();
		codewordPanel = new JPanel();
		codewordPanel.setLayout(gbl);
		
		new GBC(0, 0)
			.setWeight(0, 0)
			.setInsets(12, 12, 0, 5)
			.addComp(codewordPanel, new JLabel("Book:"), gbl);
		new GBC(1, 0)
			.setWeight(1, 0)
			.setAnchor(GBC.WEST)
			.setInsets(12, 0, 0, 11)
			.addComp(codewordPanel, bookComboBox, gbl);
		new GBC(0, 1)
			.setSpan(2, 1)
			.setWeight(1, 1)
			.setBothFill()
			.setInsets(5, 12, 11, 11)
			.addComp(codewordPanel, codewordPane, gbl);
		
		notesPanel = new JPanel();
		notesPanel.setLayout(gbl);
		
		new GBC(0, 0)
			.setWeight(1, 1)
			.setBothFill()
			.setInsets(12, 12, 11, 11)
			.addComp(notesPanel, new JScrollPane(notesArea), gbl);
		
		tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Codewords", codewordPanel);
		tabbedPane.addTab("Notes", notesPanel);
		getContentPane().add(tabbedPane);
		
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		pack();
		setLocationRelativeTo(parent);
		getCodewords().addChangeListener(this);
	}
	
	private Codewords getCodewords() {
		return FLApp.getSingle().getAdventurer().getCodewords();
	}
	
	private String[] codewords;
	private JCheckBox[] boxes;
	
	private JPanel createCodewordPanel(String[] codewords) {
		this.codewords = codewords;
		this.boxes = new JCheckBox[codewords.length];
		Codewords selected = getCodewords();
		JPanel panel = new JPanel();
		GridBagLayout gbl = new GridBagLayout();
		panel.setLayout(gbl);
		GBC cgbc = new GBC().setAnchor(GBC.WEST).setInsets(2, 3, 1, 0);
		GBC lgbc = new GBC().setWeight(0.5, 0).setAnchor(GBC.WEST).setInsets(2, 0, 1, 3);
		int row = 0, col = 0;
		for (int c = 0; c < codewords.length; c++) {
			if (c == (codewords.length+1)/2) { // halfway
				row = 0;
				col = 2;
			}
			
			// We actually display a checkbox, plus a label.
			// That way the checkbox can be disabled while the label
			// colour the normal enabled colour.
			JCheckBox codewordBox = new JCheckBox();
			codewordBox.setSelected(selected.hasCodeword(codewords[c]));
			codewordBox.setEnabled(false);
			JLabel label = new JLabel(codewords[c]);
			//label.setFont(SectionDocument.getPreferredFont());
			cgbc.addComp(panel, codewordBox, gbl, col, row);
			lgbc.addComp(panel, label, gbl, col+1, row++);
			boxes[c] = codewordBox;
		}
		return panel;
	}

	public void actionPerformed(ActionEvent e) {
		int bookIndex = bookChoice.getSelectedIndex();
		Books.BookDetails book = bookChoice.getBook(bookIndex);
		String[] codewords = book.getOfficialCodewords();
		if (codewords != null) {
			JPanel codewordPanel = createCodewordPanel(codewords);
			codewordPane.setViewportView(codewordPanel);
			if (e != null)
				validate();
		}
	}
	
	public void refresh() {
		getCodewords().removeChangeListener(this);
		getCodewords().addChangeListener(this);
		if (isVisible())
			// Reload for the same book that is already showing
			actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "something"));
		else
			// Switch to the current book
			bookComboBox.setSelectedIndex(bookChoice.getBookIndex(Address.getCurrentBookKey()));
	}

	public void stateChanged(ChangeEvent e) {
		String codeword = e.getSource().toString();
		int index = Arrays.binarySearch(codewords, codeword);
		if (index >= 0) {
			boxes[index].setSelected(getCodewords().hasCodeword(codeword));
		}
	}
	
	public void showCodewords() {
		tabbedPane.setSelectedComponent(codewordPanel);
	}
	public void showNotes() {
		tabbedPane.setSelectedComponent(notesPanel);
	}
	
	public void applyNotes() {
		System.out.println("Ready to apply notes: " + notesArea.getText());
		getCodewords().setNotes(notesArea.getText());
	}
	
	public void resetNotes() {
		System.out.println("Notes are: " + getCodewords().getNotes());
		notesArea.setText(getCodewords().getNotes());
	}
}
