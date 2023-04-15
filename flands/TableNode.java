package flands;


import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;

/**
 * General node used to group nodes that will be represented as rows.
 * That is, ChoiceNodes or OutcomeNodes.
 * @author Jonathan Mann
 */
public class TableNode extends Node {
	public TableNode(String name, Node parent) {
		super(name, parent);
	}
	
	/** Overridden to add the table Element as a direct child of the root Element. */	
	protected Element createElement() {
		Element rootElement = getParent().getElement();
		// Unnecessary; and since fights can be located inside OutcomeNodes,
		// especially in the first two books, it screwed things up badly.
		//while (rootElement.getParentElement() != null) {
		//	System.out.println("TableNode jumped a level");
		//	rootElement = rootElement.getParentElement();
		//}
		MutableAttributeSet atts = getDocument().getTableStyle();
		setViewType(atts, TableViewType);
		SectionDocument.Branch branch = getDocument().createBranchElement(rootElement, atts);
		if (branch != null && rootElement != null)
			((SectionDocument.Branch)rootElement).addChild(branch);
		return branch;
	}
}
