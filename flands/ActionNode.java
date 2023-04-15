package flands;


import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.swing.text.AbstractDocument.AbstractElement;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.xml.sax.Attributes;

/**
 * Common superclass for nodes that are actionable; that is, they can be clicked on.
 * 
 * @author Jonathan Mann
 */
public abstract class ActionNode extends Node implements ActionListener {
	private boolean highlighted = false;
	private Element[] highlightElements = null;
	private List<ActionListener> listeners = null;
	private String tooltip;

	public ActionNode(String name, Node parent) {
		super(name, parent);
	}

	public void init(Attributes atts) {
		tooltip = atts.getValue("tip");
		
		super.init(atts);
	}
	
	public void addActionListener(ActionListener l) {
		if (listeners == null)
			listeners = new LinkedList<ActionListener>();
		listeners.add(l);
	}

	public void removeActionListener(ActionListener l) {
		if (listeners != null)
			listeners.remove(l);
	}

	protected void enabledStateChange() {
		super.enabledStateChange();
		if (highlighted)
			doHighlight();
	}

	public boolean isHighlighted() { return highlighted; }
	public void setHighlighted(boolean b) {
		if (highlighted != b) {
			highlighted = b;
			if (isEnabled())
				doHighlight();
		}
	}
	
	public void enableAll() {
		super.enableAll();
		setHighlighted(false);
	}

	protected boolean gotHighlightElements() { return (highlightElements != null); }

	/** Set the element to be highlighted when the mouse is over the hyperlinked text. */
	public void setHighlightElement(Element e) {
		setHighlightElements(new Element[] { e });
	}

	/** Set the elements to be highlighted when the mouse is over the hyperlinked text. */
	public void setHighlightElements(Element[] es) {
		if (highlighted) {
			highlighted = false;
			doHighlight();
			highlighted = true;
		}
		highlightElements = es;
		if (highlighted)
			doHighlight();
	}

	public void addHighlightElement(Element e) {
		addHighlightElements(new Element[] { e });
	}

	public void addHighlightElements(Element[] es) {
		if (highlightElements == null)
			highlightElements = es;
		else {
			Element[] temp = highlightElements;
			highlightElements = new Element[temp.length + es.length];
			System.arraycopy(temp, 0, highlightElements, 0, temp.length);
			System.arraycopy(es, 0, highlightElements, temp.length, es.length);
		}

		if (highlighted && isEnabled())
			doHighlight();
	}

	protected Color getHighlightColor() { return Color.yellow; }

	protected void doHighlight() {
		if (highlightElements != null) {
			getDocument().grabWriteLock();

			Color highlightColor = getHighlightColor();
			for (int i = 0; i < highlightElements.length; i++) {
				if (highlightElements[i] instanceof AbstractElement) {
					if (highlighted && isEnabled())
						((AbstractElement)highlightElements[i]).addAttribute(StyleConstants.Background, highlightColor);
					else
						((AbstractElement)highlightElements[i]).removeAttribute(StyleConstants.Background);
				}
			}
			getDocument().releaseWriteLock();

			getDocument().fireChangeEvents(highlightElements);
		}
	}

	public boolean handleEndTag() {
		if (super.handleEndTag()) return true;
		if (highlightElements != null && highlightElements.length > 0) return true;
		return false;
	}
	
	private static final String ActionAttribute = "actionListener";
	protected void addListenerTo(MutableAttributeSet atts) {
		atts.addAttribute(ActionAttribute, this);
	}

	public static ActionNode getActionNode(Element e) {
		if (e == null) return null;
		Object val = e.getAttributes().getAttribute(ActionAttribute);
		return (val == null ? null : (ActionNode)val);
	}
	public void fireActionEvent(Element e) {
		FLApp.getSingle().actionTaken();
		UndoManager.createNull();
		actionPerformed(new ActionEvent(e, ActionEvent.ACTION_PERFORMED, "command?"));
	}

	/**
	 * Default implementation of actionPerformed passes the event to all listeners.
	 */
	public void actionPerformed(ActionEvent e) {
		if (listeners != null)
			for (Iterator<ActionListener> i = listeners.iterator(); i.hasNext(); )
				i.next().actionPerformed(e);
	}

	protected boolean callsContinue;
	/**
	 * Called by GroupNode to simulate a button press on each child node.
	 * This method lets the child notify GroupNode whether it will call
	 * (or already has called) continueExecution, by returning <code>true</code>
	 * by default. Other nodes that won't call continueExecution (like nodes with
	 * the price or flag attributes) should return <code>false</code> by setting
	 * callsContinue to false.
	 */
	public boolean doGroupAction(GroupNode group) {
		callsContinue = true;
		actionPerformed(new ActionEvent(group, ActionEvent.ACTION_PERFORMED, "groupAction"));
		return callsContinue;
	}
	
	/**
	 * Check whether this node is the child of an EffectNode.
	 * If so, the enabled state doesn't really matter when we execute it.
	 */
	protected boolean isEffectChild() {
		Node parent = getParent();
		while (parent != null) {
			if (parent instanceof EffectNode)
				return true;
			parent = parent.getParent();
		}
		return false;
	}
	
	protected Element createElement() {
		Node parent = getParent();
		if (parent.hideChildContent()) return null;
		while (parent != null && !(parent instanceof ParagraphNode))
			parent = parent.getParent();

		if (parent == null)
			// We need a paragraph as a parent element
			return super.createElement();
		else
			return null;
	}

	protected String getElementViewType() { return ParagraphViewType; }

	protected MutableAttributeSet createStandardAttributes() {
		SimpleAttributeSet atts = new SimpleAttributeSet();
		StyleNode.applyActiveStyles(atts);
		StyleConstants.setUnderline(atts, true);
		addListenerTo(atts);
		return atts;
	}

	protected void loadProperties(Attributes atts) {
		super.loadProperties(atts);
		setHighlighted(getBooleanValue(atts, "highlighted", false));
	}

	protected void saveProperties(Properties props) {
		super.saveProperties(props);
		saveProperty(props, "highlighted", highlighted);
	}
	
	/**
	 * Get the tooltip text explaining what this action does.
	 * If the result of {@link #getTipText()} is <code>null</code>, the
	 * value passed in the <code>tip</code> attribute will be returned.
	 */
	public String getToolTip() {
		String text = getTipText();
		if (text == null)
			text = tooltip;
		else if (text.indexOf("</") >= 0 && !text.startsWith("<html"))
			text = "<html>" + text + "</html>";
		return text;
	}

	/**
	 * Get the tooltip text explaining what this action does.
	 * Action subclasses should override this.
	 * @return <code>null</code> by default.
	 */
	protected String getTipText() {
		return null;
	}
	
	protected static String getDiceText(int dice) {
		switch (dice) {
		case 1:
			return "one die";
		case 2:
			return "two dice";
		case 3:
			return "three dice";
		default:
			return dice + " dice";
		}
	}
}
