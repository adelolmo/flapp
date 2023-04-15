package flands;


import java.awt.Color;
import java.util.Properties;

import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;

import org.xml.sax.Attributes;

/**
 * Handles the result of a difficulty roll.
 * @author Jonathan Mann
 */
public class DifficultyResultNode extends ActionNode implements Executable {
	public static String SuccessElementName = "success";
	public static String FailureElementName = "failure";

	private final boolean success;
	private String section;
	private String var;
	private String ability;
	private int abilityType;
	private ParagraphNode gotoParagraph = null, textNode = null;
	private GotoNode gotoNode = null;

	public DifficultyResultNode(boolean success, Node parent) {
		super(success ? SuccessElementName : FailureElementName, parent);
		this.success = success;
		setEnabled(false);
	}

	protected Color getHighlightColor() { return (success ? new Color(127, 255, 127) : new Color(255, 127, 127)); }

	private ExecutableRunner runner = null;
	public ExecutableGrouper getExecutableGrouper() {
		if (runner == null)
			runner = new ExecutableRunner("DifficultyResult/" + success, this);
		return runner;
	}

	public void init(Attributes atts) {
		section = atts.getValue("section");
		var = atts.getValue("var");
		ability = atts.getValue("ability");
		if (ability == null)
			// See if the DifficultyNode stashed the ability type
			abilityType = getVariableValue(DifficultyNode.AbilityTypeVar);

		super.init(atts);

		if (section != null) {
			gotoParagraph = new ParagraphNode(this, StyleConstants.ALIGN_RIGHT);
			gotoNode = new GotoNode(gotoParagraph); // forced
			gotoNode.init(atts);
			gotoNode.setEnabled(false);
			gotoParagraph.addChild(gotoNode);
		}
	}

	public void handleContent(String content) {
		if (content.length() == 0) return;
		if (section != null) {
			// Add this content to the range description part 
			if (textNode == null) {
				textNode = new ParagraphNode(this, StyleConstants.ALIGN_LEFT);
				addChild(textNode);
				setHighlighted(true);
			}
			Element[] leaves = getDocument().addLeavesTo(textNode.getElement(), new String[] { content }, new AttributeSet[] { StyleNode.createActiveAttributes() });
			addHighlightElements(leaves);
		}
		else {
			Element[] leaves = getDocument().addLeavesTo(getElement(), new StyledText[] { new StyledText(content, StyleNode.createActiveAttributes()) });
			addEnableElements(leaves);
		}
	}

	public boolean handleEndTag() {
		if (section != null) {
			if (textNode == null) {
				// Add the 'Successful X roll' bit now
				textNode = new ParagraphNode(this, StyleConstants.ALIGN_LEFT);
				addChild(textNode);
				String text = success ? "Successful" : "Failed";
				String abilityStr = ability;
				if (ability == null && abilityType >= 0)
					abilityStr = Adventurer.getAbilityName(abilityType);
				if (abilityStr != null)
					text += " " + abilityStr.toUpperCase();
				text += " roll";
				Element[] leaves = getDocument().addLeavesTo(textNode.getElement(), new String[] { text }, null);
				setHighlightElements(leaves);
				setHighlighted(true); // will highlight as soon as it is enabled
			}
		
			// Add the next cell - goto
			addChild(gotoParagraph);
			gotoNode.handleEndTag();
			gotoParagraph.handleEndTag();
		}

		System.out.println("DifficultyResultNode adding itself as Executable child");
		findExecutableGrouper().addExecutable(this);
		
		return super.handleEndTag();
	}

	public boolean execute(ExecutableGrouper grouper) {
		if (!meetsConditions())
			// Doesn't meet the entry conditions, so skip this 'block'
			// We're done.
			return true;

		// Enable this block
		setEnabled(true);
		if (runner == null)
			return true;

		// We have children that we can start executing
		if (runner.execute(grouper))
			// All children finished
			return true;
		else
			// Temporary halt
			return false;
	}

	public boolean meetsConditions() {
		if (isVariableDefined(var)) {
			if (ability == null || Adventurer.getAbilityType(ability) == getVariableValue(DifficultyNode.AbilityTypeVar)) {
				if (success == getVariableValue(var) > 0)
					return true;
			}
		}
		return false;
	}

	public void setEnabled(boolean b) {
		super.setEnabled(b);
		if (gotoNode != null)
			gotoNode.setEnabled(b);
	}

	public void resetExecute() {
		if (runner != null)
			runner.resetExecute();
		setEnabled(false);
	}

	protected String getElementViewType() { return (section == null ? null : RowViewType); }
	
	public void saveProperties(Properties props) {
		super.saveProperties(props);
		if (runner != null && runner.willCallContinue())
			saveProperty(props, "continue", true);
	}
	
	public void loadProperties(Attributes atts) {
		super.loadProperties(atts);
		if (getBooleanValue(atts, "continue", false))
			((ExecutableRunner)getExecutableGrouper()).setCallback(findExecutableGrouper());
	}
}
