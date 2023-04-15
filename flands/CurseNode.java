package flands;


import java.awt.event.ActionEvent;
import java.util.Properties;

import javax.swing.text.Element;

import org.xml.sax.Attributes;

/**
 * Action node that adds a curse to the player when clicked.
 * One of the simplest action nodes.
 * 
 * @author Jonathan Mann
 */
public class CurseNode extends ActionNode implements Executable {
	private Curse curse;

	public static CurseNode createCurseNode(String name, Node parent) {
		Curse curse = Curse.createCurse(name);
		return (curse == null ? null : new CurseNode(curse, parent));
	}

	public CurseNode(Curse c, Node parent) {
		super(Curse.getTypeName(c.getType()), parent);
		this.curse = c;
		setEnabled(false);
	}

	public Curse getCurse() { return curse; }

	public void init(Attributes atts) {
		curse.init(atts);
		super.init(atts);
	}

	protected Node createChild(String name) {
		Node n = null;
		if (name.equals(EffectNode.ElementName))
			n = new EffectNode(this, curse);

		if (n == null)
			n = super.createChild(name);
		else
			addChild(n);

		return n;
	}

	private boolean hadContent = false;
	private void addContent(String text) {
		Element[] leaves = getDocument().addLeavesTo(getElement(), new StyledText[] { new StyledText(text, createStandardAttributes()) });
		addEnableElements(leaves);
		setHighlightElements(leaves);
	}

	public void handleContent(String text) {
		text = text.trim();
		if (text.length() == 0) return;

		hadContent = true;
		addContent(text);
	}

	public boolean handleEndTag() {
		if (!(getParent() instanceof ItemNode)) {
			findExecutableGrouper().addExecutable(this);
			if (!hadContent)
				addContent(curse.getName());
			return hadContent;
		}
		return false;
	}

	private boolean callContinue = false;
	public boolean execute(ExecutableGrouper grouper) {
		if (!curse.isCumulative() && getCurses().findMatches(curse).length > 0)
			return true;

		setEnabled(true);
		callContinue = true;
		return false;
	}

	public void actionPerformed(ActionEvent e) {
		setEnabled(false);
		getCurses().addCurse(curse);
		if (callContinue) {
			callContinue = false;
			findExecutableGrouper().continueExecution(this, false);
		}
	}

	public void resetExecute() {
		setEnabled(false);
	}
	
	protected void loadProperties(Attributes atts) {
		super.loadProperties(atts);
		callContinue = getBooleanValue(atts, "continue", false);
	}
	
	protected void saveProperties(Properties props) {
		super.saveProperties(props);
		saveProperty(props, "continue", true);
	}
	
	protected String getTipText() {
		return "Add " + curse.getName() + "[" + Curse.getTypeName(curse.getType()) + "]";
	}
}
