package flands;


import java.awt.event.ActionEvent;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;

import org.xml.sax.Attributes;

/**
 * Action node that is a variant of DifficultyNode: compare a dice roll against the
 * character's Rank. The result can be handled using DifficultyResultNodes.
 * 
 * @see DifficultyNode
 * @see DifficultyResultNode
 * @author Jonathan Mann
 */
public class RankCheckNode extends ActionNode implements Executable, Roller.Listener, UndoManager.Creator {
	private int add;
	private int result = -1;
	private int dice;
	private String var;
	private boolean force = true;
	private List<AdjustNode> adjustments = null;

	public static final String ElementName = "rankcheck";
	static final String AbilityTypeVar = DifficultyNode.AbilityTypeVar;
	public RankCheckNode(Node parent) {
		super(ElementName, parent);
		setEnabled(false);
	}

	public void init(Attributes atts) {
		add = getIntValue(atts, "add", 0);
		dice = getIntValue(atts, "dice", 1);
		var = atts.getValue("var");
		force = getBooleanValue(atts, "force", true);
		setVariableValue(AbilityTypeVar, Adventurer.ABILITY_RANK);
	}

	protected void outit(Properties props) {
		super.outit(props);
		if (add != 0) saveProperty(props, "add", add);
		if (dice != 1) saveProperty(props, "dice", dice);
		if (var != null) props.setProperty("var", var);
		if (!force) saveProperty(props, "force", false);
	}
	
	private boolean hadContent = false;
	public void handleContent(String text) {
		if (text.trim().length() == 0)
			return;

		MutableAttributeSet atts = null;
		if (!hadContent)
			atts = createStandardAttributes();

		Element[] leaves = getDocument().addLeavesTo(getElement(), new StyledText[] { new StyledText(text, atts) });
		addEnableElements(leaves);
		if (!hadContent) {
			setHighlightElements(leaves);
			hadContent = true;
		}
	}

	private static final String[] numberStrings = {"zero", "one", "two", "three", "four", "five", "six"};
	public boolean handleEndTag() {
		if (!hadContent) {
			String text;
			if (getDocument().isNewSentence(getDocument().getLength()))
				text = "Roll ";
			else
				text = "roll ";
			text += numberStrings[dice];
			if (dice == 1)
				text += " die";
			else
				text += " dice";
			if (add > 0)
				text += " and add " + numberStrings[add];
			else if (add < 0)
				text += " and subtract " + numberStrings[-add];

			Element[] leaves = getDocument().addLeavesTo(getElement(), new StyledText[] { new StyledText(text, createStandardAttributes()) });
			addEnableElements(leaves);
			setHighlightElements(leaves);
			hadContent = true;
		}
		findExecutableGrouper().addExecutable(this);
		
		return super.handleEndTag();
	}

	protected Node createChild(String name) {
		Node n = null;
		if (name.equals(AdjustNode.ElementName)) {
			AdjustNode an = new AdjustNode(this);
			if (adjustments == null)
				adjustments = new LinkedList<AdjustNode>();
			adjustments.add(an);
			n = an;
		}

		if (n == null)
			return super.createChild(name);
		else {
			addChild(n);
			return n;
		}
	}

	private boolean callContinue = false;
	public boolean execute(ExecutableGrouper grouper) {
		if (result < 0) {
			// Set up for user to roll
			System.out.println("RankCheckNode: ready to roll!");
			setEnabled(true);
			callContinue = true;
			if (force)
				return false;
			else
				return true;
		}
		else {
			// Already rolled
			System.out.println("RankCheckNode: execute called after already rolled!");
			return true;
		}
	}

	public void resetExecute() {
		removeVariable(var);
		result = -1;
		setEnabled(false);
	}

	private Roller roller = null;
	public void actionPerformed(ActionEvent evt) {
		if (roller != null) return;

		setEnabled(false);

		int delta = getAdjustment();
		
		roller = new Roller(dice, delta);
		roller.addListener(this);
		roller.startRolling();
	}

	protected int getAdjustment() {
		int delta = add;
		if (adjustments != null) {
			for (Iterator<AdjustNode> i = adjustments.iterator(); i.hasNext(); )
				delta += i.next().getAdjustment();
			System.out.println("Adjustment for rankcheck=" + delta);
		}
		return delta;
	}
	
	public void rollerFinished(Roller r) {
		if (roller == r) {
			// Remember, it's good to score <= to your rank
			int result = 1 + getAdventurer().getRank().affected - r.getResult(); // > 0 is success, <= 0 is failure
			setVariableValue(var, result);
			roller.appendTooltipText(result > 0 ? " - Success" : " - Failure");
			roller = null;
			UndoManager.createNew(this).add(this);
			if (callContinue)
				findExecutableGrouper().continueExecution(this, true);
		}
	}

	public void undoOccurred(UndoManager undo) {
		// Pretend like we've just been called by the cached grouper...
		execute(findExecutableGrouper());
	}
	
	protected void loadProperties(Attributes atts) {
		super.loadProperties(atts);
		callContinue = getBooleanValue(atts, "continue", false);
	}
	
	protected void saveProperties(Properties props) {
		super.saveProperties(props);
		saveProperty(props, "continue", callContinue);
	}
	
	protected String getTipText() {
		String text = "Roll " + getDiceText(dice);
		int delta = getAdjustment();
		if (delta > 0)
			text += ", add " + delta;
		else if (delta < 0)
			text += ", subtract " + (-delta);
		text += " and compare the result with your Rank (" + getAdventurer().getRank().affected + ")";
		return text;
	}
}
