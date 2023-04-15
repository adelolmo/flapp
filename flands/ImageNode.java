package flands;

import java.awt.event.ActionEvent;
import java.util.Properties;

import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyleConstants;

import org.xml.sax.Attributes;

/**
 * Displays an image in the text.
 * @author Jonathan Mann
 */
public class ImageNode extends ActionNode {
	public static final String ElementName = "image";
	private String title;
	private String book;
	private String filename;
	
	public ImageNode(Node parent) {
		super(ElementName, parent);
		setEnabled(true);
	}

	public void init(Attributes atts) {
		filename = atts.getValue("file");
		book = atts.getValue("book");
		title = atts.getValue("title");
		
		super.init(atts);
	}
	
	public void outit(Properties props) {
		super.outit(props);
		if (filename != null)
			props.setProperty("file", filename);
		if (book != null)
			props.setProperty("book", book);
		if (title != null)
			props.setProperty("title", title);
	}
	
	private boolean hadContent = false;
	public void handleContent(String text) {
		if (!hadContent && text.trim().length() == 0) return;
		
		hadContent = true;
		Element[] leaves = getDocument().addLeavesTo(getElement(), new StyledText[] { new StyledText(text, createStandardAttributes()) });
		addEnableElements(leaves);
		addHighlightElements(leaves);
	}
	
	public boolean handleEndTag() {
		if (!hadContent && !getParent().hideChildContent()) {
			MutableAttributeSet atts = createStandardAttributes();
			StyleConstants.setItalic(atts, true);
			Element[] leaves = getDocument().addLeavesTo(getElement(), new StyledText[] { new StyledText("[illustration]", atts) });
			addEnableElements(leaves);
			addHighlightElements(leaves);
		}
		return super.handleEndTag();
	}
	
	private ImageWindow imageWindow = null;
	public void actionPerformed(ActionEvent evt) {
		if (filename == null) {
			System.err.println("No filename attribute for ImageNode");
			return;
		}
		
		if (imageWindow == null) {
			Books.BookDetails bookInfo;
			if (book == null)
				bookInfo = Address.getCurrentBook();
			else
				bookInfo = Books.getCanon().getBook(book);
			
			
			imageWindow = FLApp.getSingle()
				.createImageWindow(bookInfo.getInputStream(filename),
								   (title == null ? "Illustration" : title),
								   true);
		}
		imageWindow.setVisible(true);
	}
	
	protected String getTipText() {
		return "Shows an illustration in a separate window [" + (book == null ? "" : book + "/") + filename + "]";
	}
	
	public void dispose() {
		if (imageWindow != null) {
			imageWindow.setVisible(false);
			imageWindow.dispose();
		}
	}
}
