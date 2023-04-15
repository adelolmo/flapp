package flands;

import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * First component the player sees on start-up, allowing them to start a new game or
 * continue an old one.
 * 
 * @see Books
 * @author Jonathan Mann
 */
public class StartPanel extends JPanel
implements ActionListener, MouseListener, SectionDocument.FontUser, ListSelectionListener {
	private FLApp parent;
	private Books.BookListModel bookModel;
	private JList bookList;
	private JButton newButton, hardcoreButton, loadButton, quitButton;
	
	public StartPanel(FLApp parent) {
		this.parent = parent;
		
		newButton = new JButton("New Game");
		newButton.setEnabled(false);
		newButton.addActionListener(this);
		hardcoreButton = new JButton("Hardcore Game");
		hardcoreButton.setEnabled(false);
		hardcoreButton.setToolTipText("In Hardcore mode, you can play without a safety net...");
		hardcoreButton.addActionListener(this);
		loadButton = new JButton("Load Game...");
		loadButton.addActionListener(this);
		quitButton = new JButton("Quit");
		quitButton.addActionListener(this);
		
		bookModel = new Books.BookListModel();
		bookList = new JList(bookModel);
		bookList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		bookList.addMouseListener(this);
		bookList.addListSelectionListener(this);
		Font f = SectionDocument.getPreferredFont();
		bookList.setFont(new Font(f.getFontName(), f.getStyle() | Font.ITALIC, f.getSize()));
		SectionDocument.addFontUser(this);
		
		GridBagLayout gbl = new GridBagLayout();
		setLayout(gbl);
		new GBC(0, 0)
			.setSpan(2, 1)
			.setWeight(1, 0)
			.setAnchor(GBC.WEST)
			.setInsets(12, 12, 5, 12)
			.addComp(this, new JLabel("Choose a book to start in:"), gbl);
		new GBC(0, 1)
			.setWeight(1, 3)
			.setSpan(1, 4)
			.setBothFill()
			.setInsets(0, 12, 11, 5)
			.addComp(this, new JScrollPane(bookList), gbl);
		new GBC(1, 1)
			.setWeight(0, 1)
			.setHorizFill()
			.setInsets(0, 0, 5, 11)
			.addComp(this, newButton, gbl);
		new GBC(1, 2)
			.setWeight(0, 1)
			.setHorizFill()
			.setInsets(0, 0, 5, 11)
			.addComp(this, hardcoreButton, gbl);
		new GBC(1, 3)
			.setWeight(0, 1)
			.setHorizFill()
			.setInsets(0, 0, 5, 11)
			.addComp(this, loadButton, gbl);
		new GBC(1, 4)
			.setWeight(0, 1)
			.setHorizFill()
			.setInsets(0, 0, 11, 11)
			.addComp(this, quitButton, gbl);
	}
	
	private void newGame(int listIndex, boolean hardcore) {
		Books.BookDetails book = bookModel.getBook(listIndex);
		parent.doBeginBook(book.getKey(), hardcore);
	}
	
	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		if (src == newButton || src == hardcoreButton) {
			int listIndex = bookList.getSelectedIndex();
			if (listIndex >= 0)
				newGame(listIndex, src == hardcoreButton);
		}
		else if (src == loadButton) {
			String file = parent.chooseSavedGame(true);
			if (file != null)
				parent.doLoadSave(true, file);
		}
		else if (src == quitButton) {
			parent.quitGame();
		}
	}

	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount() != 2) return;
		int listIndex = bookList.locationToIndex(e.getPoint());
		if (listIndex >= 0)
			newGame(listIndex, false);
	}

	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}

	public void fontChanged(Font f, int smallerFontSize) {
		bookList.setFont(new Font(f.getFontName(), f.getStyle() | Font.ITALIC, f.getSize()));
	}

	public void valueChanged(ListSelectionEvent e) {
		boolean bookSelected = bookList.getSelectedIndices().length == 1;
		newButton.setEnabled(bookSelected);
		hardcoreButton.setEnabled(bookSelected);
	}
}
