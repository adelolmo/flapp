package flands;


import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/**
 * A node that displays a larger, bold font, using the HTML &lt;hx&gt;
 * system. Only used in the rules sections. Not to be confused with
 * (@link MarketNode.HeaderNode}.
 * @author Jonathan Mann
 */
public class HeadingNode extends Node {
	private static float sizeMultipliers[] = { 2, 1.5f, 1.25f, 1 };
	public static boolean isElementName(String elementName) {
		if (elementName.length() != 2 || !elementName.startsWith("h")) return false;
		char level = elementName.charAt(1);
		return (level > '0' && level < '5');
	}

	public static HeadingNode createHeadingNode(String elementName, Node parent) {
		if (isElementName(elementName)) {
			try {
				int level = Integer.parseInt(elementName.substring(1));
				if (level > 0 && level < 5)
					return new HeadingNode(elementName, parent, level);
			}
			catch (NumberFormatException nfe) {
				// shouldn't happen - we already know it's a digit!
			}
		}
		return null;
	}

	private int level;
	public HeadingNode(String name, Node parent, int level) {
		super(name, parent);
		this.level = level;
	}

	public void handleContent(String text) {
		MutableAttributeSet atts = StyleNode.createActiveAttributes();
		getDocument().addLeavesTo(getElement(), new StyledText[] { new StyledText(text, atts) });
	}

	public boolean handleEndTag() {
		Element e = getElement();
		if (e.getElementCount() > 0) {
			Element lastChild = e.getElement(e.getElementCount() - 1);
			if (lastChild.isLeaf())
				((SectionDocument.ContentElement)lastChild).endWithNewline();
		}
		return true;
	}

	protected MutableAttributeSet getElementStyle() {
		SimpleAttributeSet atts = new SimpleAttributeSet();
		StyleConstants.setBold(atts, true);
		float multiplier = sizeMultipliers[level-1];
		int size = SectionDocument.getPreferredFont().getSize();
		if (multiplier != 1) {
			size = (int)Math.round(size * multiplier);
			StyleConstants.setFontSize(atts, size);
		}
		if (level > 2)
			StyleConstants.setSpaceAbove(atts, size);
		return atts;
	}
}
