package flands;


import java.awt.event.ActionEvent;
import java.util.Properties;

import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.Element;

import org.xml.sax.Attributes;

import flands.UndoManager.Creator;

/**
 * Action node allowing the character to regain Stamina for money.
 * Allows complete or free healing, if either the 'stamina' or 'shards' attribute is missing.
 * 
 * @author Jonathan Mann
 */
public class RestNode extends ActionNode implements Executable, ChangeListener, Roller.Listener, Creator {
	public static final String ElementName = "rest";

	public String staminaStr;
	public int shards;
	public boolean useOnce;
	private boolean used = false;

	public RestNode(Node parent) {
		super(ElementName, parent);
		setEnabled(false);
	}

	public void init(Attributes atts) {
		// Amount of stamina regained
		staminaStr = atts.getValue("stamina");
		// Cost for each healing
		shards = getIntValue(atts, "shards", 0);
		if (shards > 0)
			// Listen for money changes
			getAdventurer().addMoneyListener(this);
		// Can it only be used once, or multiple times?
		// If no price is attached, defaults to once only.
		useOnce = getBooleanValue(atts, "once", shards == 0);

		super.init(atts);
	}

	protected void outit(Properties props) {
		super.outit(props);
		saveVarProperty(props, "stamina", staminaStr);
		if (shards > 0)
			saveProperty(props, "shards", shards);
	}
	
	public void handleContent(String text) {
		if (text.length() == 0) {
			//System.err.println("Empty " + ElementName + " tag: no default text!");
			return;
		}

		Element[] leaves = getDocument().addLeavesTo(getElement(), new StyledText[] { new StyledText(text, createStandardAttributes()) });
		addHighlightElements(leaves);
		addEnableElements(leaves);
	}

	public boolean handleEndTag() {
		findExecutableGrouper().addExecutable(this);
		return super.handleEndTag();
	}

	public boolean execute(ExecutableGrouper grouper) {
		if (shards > 0)
			// Listen for money changes - do an initial check on finances/health now
			stateChanged(null);
		else
			// Allow actions
			setEnabled(true);

		if (hidden)
			actionPerformed(null);

		// but don't block
		return true;
	}

	public void stateChanged(ChangeEvent evt) {
		if (!used && getAdventurer().getMoney() >= shards) {
			Adventurer.StaminaStat stat = getAdventurer().getStamina();
			if (stat.current < stat.affected) {
				setEnabled(true);
				return;
			}
		}
		setEnabled(false);
	}

	public void actionPerformed(ActionEvent e) {
		if (staminaStr != null && Character.isDigit(staminaStr.charAt(0))) {
			DiceExpression staminaExp = new DiceExpression(staminaStr);
			if (staminaExp.dice > 0) {
				setEnabled(false);
				Roller r = staminaExp.createRoller();
				r.addListener(this);
				r.startRolling();
				return;
			}
		}

		Adventurer.StaminaStat stat = getAdventurer().getStamina();
		int uses = 1;
		int stamina = (staminaStr == null ? -1 : getAttributeValue(staminaStr));
		if (shards > 0) {
			// Make sure the user needs it
			if (stat.current >= stat.affected)
				return;

			// Make sure the user can afford it
			if (!useOnce) {
				// Ask how many times to use it
				int maxUses = Math.min(getAdventurer().getMoney() / shards,
					                   (stat.affected - stat.current) / stamina);
				if (maxUses > 1) {
					/*
					SpinnerNumberModel usesModel = new SpinnerNumberModel(1, 1, maxUses, 1);
					JSpinner usesSpinner = new JSpinner(usesModel);
					javax.swing.JComponent spinnerEditor = usesSpinner.getEditor();
					if (spinnerEditor instanceof JSpinner.DefaultEditor)
						((JSpinner.DefaultEditor)spinnerEditor).getTextField().setEditable(false);
						*/
					JSlider usesSlider = new JSlider(0, maxUses, 1);
					if (maxUses >= 10) {
						usesSlider.setMajorTickSpacing(5);
						usesSlider.setMinorTickSpacing(1);
					}
					else
						usesSlider.setMajorTickSpacing(1);
					usesSlider.setPaintTicks(true);
					usesSlider.setPaintLabels(true);
					usesSlider.setSnapToTicks(true);
					JOptionPane choicePane = new JOptionPane(new Object[] {"How many days do you want to rest here?", usesSlider},
						JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
					System.out.println("Option type=" + choicePane.getOptionType());
					choicePane.createDialog(FLApp.getSingle(), "Rest").setVisible(true);
					Number result = (Number)choicePane.getValue();
					System.out.println("Result=" + result);
					if (result == null || result.intValue() != JOptionPane.OK_OPTION)
						return;

					//uses = ((Integer)usesSpinner.getValue()).intValue();
					uses = usesSlider.getValue();
					if (uses == 0)
						return;
				}
			}
			if (getAdventurer().getMoney() >= uses*shards)
				getAdventurer().adjustMoney(-uses*shards);
			else
				return; // quietly fail - should I pop up a dialog or something?
		}
		else
			setEnabled(false); // can only use once if there is no cost attached

		stat.heal(uses*stamina);
		if (useOnce) {
			used = true;
			setEnabled(false);
		}
		else if (stat.current == stat.affected ||
				getAdventurer().getMoney() < shards)
			setEnabled(false);
		
		callsContinue = false;
	}

	private int healAmount = 0;
	public void rollerFinished(Roller r) {
		Adventurer.StaminaStat stamina = getAdventurer().getStamina();
		int prevStamina = stamina.current;
		healAmount = r.getResult();
		if (shards != 0) {
			getAdventurer().adjustMoney(-shards);
			if (getAdventurer().getMoney() >= shards)
				setEnabled(true);
		}
		stamina.heal(healAmount);
		healAmount = stamina.current - prevStamina;
		
		UndoManager.createNew(this).add(this);
		
		findExecutableGrouper().continueExecution(this, true);
	}

	public void resetExecute() {
		setEnabled(false);
	}

	public void undoOccurred(UndoManager undo) {
		if (healAmount != 0) {
			getAdventurer().getStamina().damage(healAmount);
			healAmount = 0;
		}
		setEnabled(true);
	}
	
	protected String getTipText() {
		int stamina = (staminaStr == null ? -1 : getAttributeValue(staminaStr));
		String text = "Restore ";
		if (stamina > 1)
			text += stamina + " Stamina points";
		else if (stamina == 1)
			text += "a Stamina point";
		else if (stamina < 0)
			text += "all your Stamina";
		if (shards > 0) {
			if (shards > 1)
				text += " for every " + shards + " Shards";
			else
				text += " for every Shard";
			text += " you spend";
		}
		return text;
	}
}
