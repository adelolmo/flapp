package flands;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.swing.text.AttributeSet;

import org.xml.sax.Attributes;

/**
 * A UseEffect is attached to an Item; when the item is used, the effect is activated.
 * Commonly, an ActionNode is attached to the effect, which is activated.
 * A more specific case is for potions (eg. potion of strength (COMBAT +1)), which temporarily
 * add one to an ability.
 * @author Jonathan Mann
 */
public class UseEffect extends Effect implements ExecutableGrouper {
	/**
	 * If the number of 'charges' remaining is to be shown in the effect description,
	 * this pattern should be used. This pattern will then be replaced by the
	 * number of charges left.
	 */
	public static final String UsesPattern = "{uses}";
	
	private int uses = -1;
	private int ability = -1;
	private boolean disposable = true;
	private Blessing blessing = null;
	private String verb = null;
	private List<ActionNode> actions = null;

	private UseEffect() { super(); }
	
	public UseEffect(Attributes atts) {
		init(atts);
	}
	
	protected void init(Attributes atts) {
		super.init(atts);
		String val = atts.getValue("ability");
		if (val != null) {
			ability = Adventurer.getAbilityType(val);
			if (ability >= 0)
				uses = 1;
		}
		if (atts.getValue("blessing") != null)
			blessing = Blessing.getBlessing(atts);
		uses = Node.getIntValue(atts, "uses", uses);
		if (ability < 0)
			verb = atts.getValue("verb");
		else
			verb = "Drink";
		disposable = Node.getBooleanValue(atts, "disposable", disposable);
	}

	protected void saveProperties(Properties atts) {
		super.saveProperties(atts);
		if (ability >= 0)
			atts.setProperty("ability", Adventurer.getAbilityName(ability));
		if (blessing != null)
			blessing.saveTo(atts);
		Node.saveProperty(atts, "uses", uses);
		if (verb != null)
			atts.setProperty("verb", verb);
		Node.saveProperty(atts, "disposable", disposable);
	}
	
	public int getType() { return TYPE_USE; }

	public String getVerb() { return (verb == null ? "Use" : verb); }

	public void addActionNode(ActionNode n) {
		if (actions == null)
			actions = new LinkedList<ActionNode>();
		actions.add(n);
	}
	
	public boolean canUse() {
		if (blessing != null) {
			if (!BlessingList.canUseBlessing(blessing))
				return false;
		}
		return true;
	}
	
	public boolean use() {
		if (actions != null) {
			System.out.println("Trying to use effect " + this);
			for (Iterator<ActionNode> i = actions.iterator(); i.hasNext(); ) {
				ActionNode action = i.next();
				if (action instanceof Executable) {
					System.out.println("Executing action first");
					((Executable)action).execute(this);
				}
				System.out.println("Performing action now");
				action.actionPerformed(null);
			}
		}
		else if (ability >= 0) {
			FLApp.getSingle().getAdventurer().getEffects().addAbilityPotionBonus(ability);
		}
		else if (blessing != null) {
			if (!BlessingList.useBlessing(blessing))
				return true;
		}
		else
			return true;

		if (uses > 0) {
			if (--uses == 0 && disposable)
				return false; // to signal that this effect has been used up
		}
		return true;
	}

	private String replaceUses(String text) {
		int index = text.indexOf(UsesPattern);
		if (index < 0)
			return text;
		else
			return text.substring(0, index)
			+ uses
			+ text.substring(index+UsesPattern.length());
	}
	
	public boolean addTo(StyledTextList textList, AttributeSet atts) {
		if (styledDescription != null) {
			if (uses > 0)
				for (Iterator<StyledText> i = styledDescription.iterator(); i.hasNext(); ) {
					StyledText st = i.next();
					String newText = replaceUses(st.text);
					AttributeSet newAtts = StyledTextList.combine(st.atts, atts);
					if (st.text == newText)
						textList.add(new StyledText(st.text, newAtts));
					else
						textList.add(new StyledText(newText, newAtts));
				}
			else
				textList.add(styledDescription, atts);
		}
		else if (description != null) {
			textList.add(replaceUses(description), atts);
		}
		else if (ability >= 0) {
			textList.addAbilityName(Adventurer.getAbilityName(ability), atts);
			textList.add(" +1", atts);
		}
		else
			return false;

		return true;
	}
	
	protected void addOutputChildren(List<XMLOutput> l) {
		super.addOutputChildren(l);
		if (actions != null) {
			for (Iterator<ActionNode> i = actions.iterator(); i.hasNext(); )
				l.add(i.next());
		}
	}
	
	/* ExecutableGrouper methods */
	public boolean isSeparateThread() { return false; }
	public void addExecutable(Executable e) {}
	public void addIntermediateNode(Node n) {}
	public void continueExecution(Executable e, boolean separateThread) {}
	
	protected Effect createCopy() {
		return new UseEffect();
	}
	
	protected void copyFieldsTo(Effect e) {
		UseEffect ue = (UseEffect)e;
		ue.uses = uses;
		ue.ability = ability;
		ue.verb = verb;
		ue.actions = actions;
		super.copyFieldsTo(ue);
	}
}
