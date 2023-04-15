package flands;

import javax.swing.BoxLayout;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;

/**
 * A special, internally-created layout node that will display its child elements
 * as a column or row (depending on orientation).
 * 
 * @see OutcomeNode
 * @author Jonathan Mann
 */
public class BoxNode extends Node {
	private static final boolean OverrideCreateElement = false;
	public static final int X_AXIS = BoxLayout.X_AXIS;
	public static final int Y_AXIS = BoxLayout.Y_AXIS;
	private int orientation;
	
	public BoxNode(Node parent, int orientation) {
		super("box", parent);
		this.orientation = orientation;
	}

	protected String getElementViewType() { return BoxYViewType; }

	protected MutableAttributeSet getElementStyle() {
		// TODO: Have a play around with these attributes
		return new SimpleAttributeSet();
	}
	
	protected Element createElement() {
		if (OverrideCreateElement) {
			Element rootElement = getParent().getElement();
			MutableAttributeSet atts = new SimpleAttributeSet();
			if (orientation == X_AXIS)
				setViewType(atts, Node.BoxXViewType);
			else
				setViewType(atts, Node.BoxYViewType);
			SectionDocument.Branch branch = getDocument().createBranchElement(rootElement, atts);
			if (branch != null && rootElement != null)
				((SectionDocument.Branch)rootElement).addChild(branch);
			return branch;
		}
		else
			return super.createElement();
	}
	
}
