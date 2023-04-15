package flands;


import java.awt.event.ActionEvent;
import java.util.Properties;

import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.xml.sax.Attributes;

/**
 * Convenience node for increasing an ability if a dice roll exceeds its current value.
 * @author Jonathan Mann
 */
public class TrainingNode extends ActionNode implements Executable, Roller.Listener, UndoManager.Creator {
	public static final String ElementName = "training";

	// There's either one ability, or a choice of several
	private int ability = -1;
	private int[] abilities = null;
	private int dice;
	private int add;
	private String var;

	public TrainingNode(Node parent) {
		super(ElementName, parent);
		setEnabled(false);
	}

	public void init(Attributes atts) {
		abilities = Adventurer.getAbilityTypes(atts.getValue("ability"));
		if (abilities.length == 1) {
			ability = abilities[0];
			abilities = null;
		}

		dice = getIntValue(atts, "dice", 2);
		add = getIntValue(atts, "add", 0);
		var = atts.getValue("var");

		super.init(atts);
	}

	protected void outit(Properties props) {
		super.outit(props);
		
		StringBuffer abilityString = new StringBuffer();
		if (ability >= 0)
			abilityString.append(Adventurer.getAbilityName(ability));
		else {
			for (int a = 0; a < abilities.length; a++) {
				if (a > 0) abilityString.append("|");
				abilityString.append(Adventurer.getAbilityName(abilities[a]));
			}
		}
		props.setProperty("ability", abilityString.toString());
		
		if (dice != 2) saveProperty(props, "dice", dice);
		if (add != 0) saveProperty(props, "add", add);
		if (var != null) props.setProperty("var", var);
	}
	
	public void handleContent(String text) {
		if (text.length() == 0) {
			// end tag without any content
			if (getParent().hideChildContent())
				return;
			text = (getDocument().isNewSentence() ? "Roll " : "roll ");
			text += getDiceText(dice);
		}

		// Look for one or more abilities in this content
		SimpleAttributeSet atts = new SimpleAttributeSet();
		StyleConstants.setUnderline(atts, true);
		addListenerTo(atts);

		Element[] leaves = getDocument().addLeavesTo(getElement(), new StyledText[] { new StyledText(text, atts) });
		addHighlightElements(leaves);
		addEnableElements(leaves);
	}

	public boolean handleEndTag() {
		findExecutableGrouper().addExecutable(this);
		return super.handleEndTag();
	}

	public boolean execute(ExecutableGrouper grouper) {
		setEnabled(true);
		return false;
	}

	public void resetExecute() { setEnabled(false); }

	public void actionPerformed(ActionEvent e) {
		if (abilities != null) {
			// More than one ability - do a popup dialog to choose one
			int[] stats = new int[abilities.length];
			for (int a = 0; a < stats.length; a++)
				stats[a] = getAdventurer().getAbility(abilities[a]).natural;
			StyledDocument[] docs = Adventurer.getAbilityDocuments(abilities, stats);

			DocumentChooser dc = new DocumentChooser(FLApp.getSingle(), "Choose", docs, false);
			dc.setVisible(true);

			if (dc.getSelectedIndices() == null)
				return; // failure
			System.out.println("Selected indices has length " + dc.getSelectedIndices());
			ability = abilities[dc.getSelectedIndices()[0]];
		}
		setEnabled(false);

		Roller roller = new Roller(dice, add);
		roller.addListener(this);
		roller.startRolling();
	}

	private boolean success;
	public void rollerFinished(Roller r) {
		Adventurer adv = getAdventurer();
		setVariableValue("exp", r.getResult() - adv.getAbility(ability).natural); // might need this result
		if (var != null)
			setVariableValue(var, r.getResult());
		if (r.getResult() > adv.getAbility(ability).natural) {
			success = true;
			r.appendTooltipText(" - Ability raised");
			adv.raiseAbility(ability);
		}
		else {
			success = false;
			r.appendTooltipText(" - No change");
		}

		UndoManager.createNew(this).add(this);
		findExecutableGrouper().continueExecution(this, true);
	}

	public void undoOccurred(UndoManager undo) {
		if (success)
			// Don't know exactly why you'd want to undo this, but anyway...
			getAdventurer().adjustAbility(ability, -1);
		execute(findExecutableGrouper());
	}
	
	protected String getTipText() {
		String text = "Roll " + getDiceText(dice);
		if (add > 0)
			text += " and add " + add;
		else if (add < 0)
			text += " and subtract " + (-add);
		text += "; if the result is higher than ";
		if (abilities.length == 1)
			text += "your " + Adventurer.getAbilityName(abilities[0]) + " score";
		else
			text += "your chosen ability";
		text += " it will be increased";
		return text;
	}
}
