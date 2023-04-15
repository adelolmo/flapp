package flands;

import java.util.LinkedList;
import java.util.List;

import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;

import org.xml.sax.Attributes;

/**
 * 'Small' node that should contain text, possibly with some formatting.
 * Used by larger nodes (generally Action ones) to delegate this text handling.
 * 
 * @author Jonathan Mann
 */
public class TextNode extends Node {
	/**
	 * A text node is a convenience for Nodes like GroupNode - the text within
	 * is written to the document.
	 */
	public static final String TextElementName = "text";
	/**
	 * A description node gathers the styled text within without adding it to the
	 * document. It can then be used as the description for an effect, as part of
	 * a curse or item on the Adventurer Sheet.
	 */
	public static final String DescElementName = "desc";
	
	private final boolean isTextNode;
	private AttributeSet defaultAtts = null;
	private StyledTextList styledTextList = new StyledTextList();
	private boolean hadAnyContent = false;
	
	public TextNode(Node parent, boolean isTextNode) {
		super(isTextNode ? TextElementName : DescElementName , parent);
		this.isTextNode = isTextNode;
	}

	public void setTextAttributes(AttributeSet atts) {
		defaultAtts = atts;
	}

	private List<Element> elementList;
	public void init(Attributes atts) {
		super.init(atts);
		elementList = new LinkedList<Element>();
	}

	protected Element createElement() {
		if (isTextNode) {
			Node parent = getParent();
			while (parent != null & !(parent instanceof ParagraphNode))
				parent = parent.getParent();
	
			if (parent instanceof ParagraphNode)
				return null;
			else
				// We need a paragraph as our parent element
				// TODO: Why?
				return super.createElement();
		}
		else
			return null;
	}

	public void handleContent(String text) {
		if (text.length() == 0)
			return;

		hadAnyContent = true;
		SimpleAttributeSet atts;
		StyledText textUnit;
		
		// Add the unit of styled text to the document
		if (isTextNode) {
			atts = (defaultAtts == null ? new SimpleAttributeSet() : new SimpleAttributeSet(defaultAtts));
			StyleNode.applyActiveStyles(atts);
			textUnit = new StyledText(text, atts);
			Element[] leaves = getDocument().addLeavesTo(getElement(), new StyledText[] { textUnit });
			for (int i = 0; i < leaves.length; i++)
				elementList.add(leaves[i]);
		}

		// Also add a copy to our cached text list, without any default attributes
		atts = new SimpleAttributeSet();
		StyleNode.applyActiveStyles(atts);
		textUnit = new StyledText(text, atts);
		styledTextList.add(textUnit);
	}

	public boolean handleEndTag() {
		if (!hadAnyContent) {
			// Empty text node = deliberate space
			this.handleContent(" ");
		}
		return super.handleEndTag();
	}

	public Element[] getLeaves() {
		return elementList.toArray(new Element[elementList.size()]);
	}
	
	public StyledTextList getText() { return styledTextList; }
}