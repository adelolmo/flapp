package flands;


import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.xml.sax.Attributes;

/**
 * Displays a paragraph of justified text, as with HTML. May contain other nodes.
 * @author Jonathan Mann
 */
public class ParagraphNode extends Node {
	public static String ElementName = "p";

	private int alignment;

	public ParagraphNode(Node parent) {
		this(parent, StyleConstants.ALIGN_JUSTIFIED);
	}
	public ParagraphNode(Node parent, int alignment) {
		super(ElementName, parent);
		this.alignment = alignment;
		setEnabled(false);
		findExecutableGrouper().addIntermediateNode(this);
	}

	public void init(Attributes atts) {
		super.init(atts);
	}

	public void handleContent(String content) {
		// Add a child with the current style
		if (content.trim().length() == 0) {
			// Check whether this whitespace is necessary - see what the last character was
			try {
				if (Character.isWhitespace(getDocument().getText(getDocument().getLength() - 1, 1).charAt(0)))
					// previous character was whitespace - don't need any more!
					return;
				System.out.println("ParagraphNode: including whitespace");
			}
			catch (javax.swing.text.BadLocationException ble) {
				// No content before this? Don't lead with whitespace either.
				return;
			}
		}
		Element[] leaves = getDocument().addLeavesTo(getElement(), new String[] { content }, new AttributeSet[] { StyleNode.createActiveAttributes() });
		addEnableElements(leaves);
	}

	public boolean handleEndTag() {
		Element e = getElement();
		if (e.getElementCount() > 0) {
			Element lastChild = e.getElement(e.getElementCount() - 1);
			if (lastChild.isLeaf())
				((SectionDocument.ContentElement)lastChild).endWithNewline();
		}
		return super.handleEndTag();
	}

	protected String getElementViewType() { return ParagraphViewType; }

	protected MutableAttributeSet getElementStyle() {
		SimpleAttributeSet atts = new SimpleAttributeSet();
		StyleConstants.setAlignment(atts, alignment);
		if (alignment == StyleConstants.ALIGN_JUSTIFIED)
			StyleConstants.setFirstLineIndent(atts, 25.0f);
		return atts;
	}
}
