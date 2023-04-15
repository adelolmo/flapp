package flands;

import java.awt.AWTEvent;
import java.awt.Container;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * Lets the user choose the book font, displaying a sample piece of text
 * for immediate feedback.
 * 
 * @author Jonathan Mann
 */
public class FontChooser extends JDialog
  implements ListSelectionListener, ChangeListener, ActionListener {
	private JList fontList;
	private SpinnerNumberModel sizeModel, smallerSizeModel;
	private Font currentFont;
	private JTextPane displayPane;

	public FontChooser(Frame parent, Font initialFont, int smallerFontSize) {
		super(parent, "Choose Font", true);

		this.currentFont = initialFont;
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		String[] fontNames = ge.getAvailableFontFamilyNames();
		fontList = new JList(fontNames);
		String initialFontName = initialFont.getFamily();
		int selectedFontIndex = -1;
		for (int i = 0; i < fontNames.length; i++)
			if (fontNames[i].equals(initialFontName)) {
				fontList.setSelectedIndex(i);
				selectedFontIndex = i;
				break;
			}
		fontList.addListSelectionListener(this);

		sizeModel = new SpinnerNumberModel(initialFont.getSize(), 5, 30, 1);
		JSpinner sizeSpinner = new JSpinner(sizeModel);
		sizeSpinner.addChangeListener(this);

		smallerSizeModel = new SpinnerNumberModel(smallerFontSize, 2, 30, 1);
		JSpinner smallerSizeSpinner = new JSpinner(smallerSizeModel);
		smallerSizeSpinner.addChangeListener(this);

		displayPane = new JTextPane();
		//displayPane.setFont(currentFont);
		displayPane.setStyledDocument(createDisplayDocument());

		CommandButtons buttons = CommandButtons.createRow(CommandButtons.OK_CANCEL, this);
		
		GridBagLayout gbl = new GridBagLayout();
		Container content = getContentPane();
		content.setLayout(gbl);
		
		new GBC(0, 0)
			.setSpan(2, 1)
			.setInsets(12, 12, 0, 0)
			.addComp(content, new JLabel("Font Family:"), gbl);
		new GBC(0, 1)
			.setSpan(2, 1)
			.setWeight(0, 1)
			.setBothFill()
			.setInsets(6, 12, 5, 0)
			.addComp(content, new JScrollPane(fontList), gbl);
		
		new GBC(0, 2)
			.setAnchor(GBC.WEST)
			.setInsets(0, 12, 0, 0)
			.addComp(content, new JLabel("Size:"), gbl);
		new GBC(1, 2)
			.setAnchor(GBC.EAST)
			.setInsets(0, 6, 0, 0)
			.addComp(content, sizeSpinner, gbl);
		
		new GBC(0, 3)
			.setAnchor(GBC.WEST)
			.setInsets(6, 12, 0, 0)
			.addComp(content, new JLabel("Smaller caps size:"), gbl);
		new GBC(1, 3)
			.setAnchor(GBC.EAST)
			.setInsets(6, 6, 0, 0)
			.addComp(content, smallerSizeSpinner, gbl);
		
		new GBC(2, 0)
			.setWeight(1, 0)
			.setInsets(12, 5, 0, 11)
			.addComp(content, new JLabel("Example text:"), gbl);
		new GBC(2, 1)
			.setWeight(1, 1)
			.setSpan(1, 3)
			.setBothFill()
			.setInsets(5, 5, 0, 11)
			.addComp(content, new JScrollPane(displayPane), gbl);

		new GBC(0, 4)
			.setWeight(1, 0)
			.setSpan(3, 1)
			.setBothFill()
			.addComp(content, buttons, gbl);
		
		pack();
		setLocationRelativeTo(parent);
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		fontList.ensureIndexIsVisible(selectedFontIndex);
	}

	protected void processWindowEvent(WindowEvent evt) {
		if (evt.getID() == WindowEvent.WINDOW_CLOSING)
			close();
		super.processWindowEvent(evt);
	}
	
	private void close() {
		setVisible(false);
		dispose();
	}
	
	private void changeFont() {
		currentFont = new Font(fontList.getSelectedValue().toString(), Font.PLAIN, 
			sizeModel.getNumber().intValue());
		displayPane.setDocument(createDisplayDocument());
	}

	private static void append(StyledDocument doc, String text, AttributeSet atts)
	throws BadLocationException {
		doc.insertString(doc.getLength(), text, atts);
	}
	
	private StyledDocument createDisplayDocument() {
		DefaultStyledDocument doc = new DefaultStyledDocument();
		MutableAttributeSet atts = new SimpleAttributeSet();
		StyleConstants.setFontFamily(atts, currentFont.getFamily());
		StyleConstants.setFontSize(atts, currentFont.getSize());
		MutableAttributeSet boldAtts = new SimpleAttributeSet(atts);
		StyleConstants.setBold(boldAtts, true);
		MutableAttributeSet italicAtts = new SimpleAttributeSet(atts);
		StyleConstants.setItalic(italicAtts, true);
		MutableAttributeSet smallAtts = new SimpleAttributeSet(atts);
		StyleConstants.setFontSize(smallAtts, smallerSizeModel.getNumber().intValue());
		MutableAttributeSet boldSmallAtts = new SimpleAttributeSet(smallAtts);
		StyleConstants.setBold(boldSmallAtts, true);
		try {
			append(doc, "Note ", atts);
			append(doc, "moonstone of teleportation", boldAtts);
			append(doc, " \u2013 ", atts);
			append(doc, "The War-Torn Kingdom", italicAtts);
			append(doc, ", ", atts);
			append(doc, "650", boldAtts);
			append(doc, " on your Adventure Sheet.\n", atts);
			append(doc, "Make a S", atts);
			append(doc, "ANCTITY", smallAtts);
			append(doc, " roll at Difficulty 10.\n", atts);
			append(doc, "Lifting the lid, you find a ", atts);
			append(doc, "golden katana (C", boldAtts);
			append(doc, "OMBAT", boldSmallAtts);
			append(doc, " +1)", boldAtts);
			append(doc, ".\n", atts);
		}
		catch (BadLocationException ble) {}
		return doc;
	}
	
	public void valueChanged(ListSelectionEvent e) {
		changeFont();
	}

	public void stateChanged(ChangeEvent e) {
		changeFont();
	}

	private Font chosenFont = null;
	private int smallerCapsSize = 0;
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals(CommandButtons.okCommand)) {
			chosenFont = currentFont;
			smallerCapsSize = smallerSizeModel.getNumber().intValue();
			close();
		}
		else if (e.getActionCommand().equals(CommandButtons.cancelCommand)) {
			close();
		}
	}
	
	public Font getChosenFont() { return chosenFont; }
	public int getSmallerCapsFontSize() { return smallerCapsSize; }
}
