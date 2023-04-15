package flands;


import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/**
 * Common node to handle styled text - bold, italics, underlined, and mixed-size upper-case.
 * The currently active style is modified when one of these nodes is entered or exited; this
 * active style is applied to any added text.
 * 
 * @author Jonathan Mann
 */
public abstract class StyleNode extends Node {
	public static String BoldElementName = "b";
	public static String ItalicElementName = "i";
	public static String CapsElementName = "caps";
	public static String UnderlineElementName = "u";

	protected static List<StyleNode> activeStyles;
	/** Add a style to the stack of active styles. */
	protected static void addActiveStyle(StyleNode node) {
		if (activeStyles == null)
			activeStyles = new LinkedList<StyleNode>();
				activeStyles.add(0, node);
	}
	/** Remove the style from the stack of active styles. */
	protected static void removeActiveStyle(StyleNode node) {
		StyleNode removed = activeStyles.remove(0);
		if (!node.equals(removed))
			System.out.println("Removing style " + node + " that doesn't match " + removed);
	}
	/** Apply all active styles to an attribute set. */
	protected static void applyActiveStyles(MutableAttributeSet atts) {
		if (activeStyles != null)
			for (Iterator<StyleNode> i = activeStyles.iterator(); i.hasNext(); )
				i.next().setAttribute(atts);
	}
	protected static javax.swing.text.MutableAttributeSet createActiveAttributes() {
		if (activeStyles == null || activeStyles.size() == 0)
			return null;
		else {
			SimpleAttributeSet atts = new SimpleAttributeSet();
			applyActiveStyles(atts);
			return atts;
		}
	}

	/**
	 * Create a new StyleNode.
	 * Adds itself to the stack of active styles.
	 */
	public StyleNode(String name, Node parent) {
		super(name, parent);
		addActiveStyle(this);
	}

	public boolean isStyleNode() { return true; }

	/**
	 * Child nodes should be created as direct children of the parent node.
	 */
	protected Node createChild(String name) {
		return getParent().createChild(name);
	}
	
	/**
	 * Handle textual content by passing it to the parent node.
	 * If the parent has been keeping track of style nodes, as Node does,
	 * it can apply this style (and any others) to the content being handled.
	 */
	public void handleContent(String content) {
		getParent().handleContent(content);
	}
	/** Handles the close tag by removing itself from the stack of active styles. */
	public boolean handleEndTag() {
		removeActiveStyle(this);
		return true;
	}

	protected Element createElement() { return null; }

	protected abstract void setAttribute(MutableAttributeSet attributes);

	public static class Bold extends StyleNode {
		public Bold(Node parent) { super(BoldElementName, parent); }
		protected void setAttribute(MutableAttributeSet attributes) {
			StyleConstants.setBold(attributes, true);
		}
	}

	public static class Italic extends StyleNode {
		public Italic(Node parent) { super(ItalicElementName, parent); }
		protected void setAttribute(MutableAttributeSet attributes) {
			StyleConstants.setItalic(attributes, true);
		}
	}

	public static class Caps extends StyleNode {
		public Caps(Node parent) { super(CapsElementName, parent); }
		protected void setAttribute(MutableAttributeSet attributes) {
			attributes.addAttribute(CapsElementName, Boolean.TRUE);
		}
	}

	public static class Underline extends StyleNode {
		public Underline(Node parent) { super(UnderlineElementName, parent); }
		protected void setAttribute(MutableAttributeSet attributes) {
			StyleConstants.setUnderline(attributes, true);
		}
	}
}
