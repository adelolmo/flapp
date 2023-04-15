package flands;


import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.swing.text.AttributeSet;

import org.xml.sax.Attributes;

/**
 * A catch-all for something extra attached to an item, curse, blessing, or god.
 * The most common is AbilityEffect, which modifies an ability; the other main type
 * is a UseEffect, which is generally attached to an Item and activated when the user
 * double-clicks it.
 * 
 * @see AbilityEffect
 * @see UseEffect
 * @see EffectSet
 * 
 * @author Jonathan Mann
 */
public class Effect implements Comparable<Effect>, XMLOutput {
	public static final int TYPE_AURA = 0;
	public static final int TYPE_WIELDED = 1;
	public static final int TYPE_USE = 2;
	public static final int TYPE_TOOL = 3;
	// order is relevant - used to sort effects when applying in EffectSet
	// TODO: Make sure tool effects are applied in right order when negative effects are in place

	int type = TYPE_AURA; // by default
	protected String description = null;
	protected StyledTextList styledDescription = null;
	protected Effect next = null;

	public int getType() { return type; }
	public String getSimpleDescription() { return description; }
	public void setStyledDescription(StyledTextList text) {
		styledDescription = text;
	}

	public static Effect createEffect(Attributes atts) {
		String type = atts.getValue("type");
		String abilityStr = atts.getValue("ability");
		int ability = -1;
		if (abilityStr != null)
			ability = Adventurer.getAbilityType(abilityStr);
		String bonusStr = atts.getValue("bonus");
		int bonus = 0;
		if (bonusStr != null) {
			if (bonusStr.startsWith("+"))
				bonusStr = bonusStr.substring(1);
			try {
				bonus = Integer.parseInt(bonusStr);
			}
			catch (NumberFormatException nfe) {
				System.err.println("Bad bonus value: " + nfe);
			}
		}
		String divideStr = atts.getValue("divide");
		String targetStr = atts.getValue("target");

		Effect e = null;
		if (abilityStr != null && bonusStr != null) {
			e = AbilityEffect.createAbilityBonus(ability, bonus);
		}
		else if (abilityStr != null && divideStr != null) {
			try {
				int divide = Integer.parseInt(divideStr);
				e = AbilityEffect.createAbilityDivider(ability, divide);
			}
			catch (NumberFormatException nfe) {
				System.err.println("Bad divide value: " + nfe);
			}
		}
		else if (abilityStr != null && targetStr != null) {
			try {
				int target = Integer.parseInt(targetStr);
				e = AbilityEffect.createAbilityTarget(ability, target);
			}
			catch (NumberFormatException nfe) {
				System.err.println("Bad target value: " + nfe);
			}
		}
		else if (type != null && type.equalsIgnoreCase("use")) {
			e = new UseEffect(atts);
		}

		if (e != null && type != null) {
			type = type.toLowerCase();
			if (type.equals("aura"))
				e.type = TYPE_AURA;
			else if (type.startsWith("wiel"))
				e.type = TYPE_WIELDED;
			else if (type.startsWith("use"))
				e.type = TYPE_USE;
			else if (type.startsWith("tool"))
				e.type = TYPE_TOOL;
		}

		if (e != null)
			e.init(atts);

		return e;
	}

	protected void init(Attributes atts) {
		description = atts.getValue("text");
	}
	
	/**
	 * Does this Effect affect the given ability?
	 */
	public boolean affectsAbility(int ability) { return false; }

	/**
	 * Append a description of the effect. The list can then be used
	 * to add to either SectionDocument or a DefaultStyledDocument.
	 * @param atts the default attributes to use
	 * @return <code>true</code> if anything was added to the list
	 */
	public boolean addTo(StyledTextList textList, AttributeSet atts) { return false; }

	/**
	 * Get the next effect 
	 */
	public Effect nextEffect() { return next; }

	/**
	 * Get the first effect of the given type in the effect chain.
	 * Returns <code>this</code>, if the type matches.
	 */
	public Effect firstEffect(int type) {
		Effect e = this;
		while (e != null) {
			if (e.getType() == type)
				break;
			e = e.nextEffect();
		}
		return e;
	}

	public void addEffect(Effect e) {
		addEffect(e, false);
	}

	public void addEffect(Effect e, boolean cumulative) {
		if (next == null)
			next = e;
		else
			next.addEffect(e, cumulative);
	}

	public int compareTo(Effect e) {
		if (this == e) return 0;
		if (description != null && e.description != null)
			return description.compareTo(e.description);
		if (type != e.type)
			return type - e.type;
		return -1;
	}
	
	protected void saveProperties(Properties atts) {
		String typeString = null;
		switch (type) {
		case TYPE_AURA:
			typeString = "aura";
			break;
		case TYPE_WIELDED:
			typeString = "wielded";
			break;
		case TYPE_USE:
			typeString = "use";
			break;
		case TYPE_TOOL:
			typeString = "tool";
			break;
		}
		if (typeString != null)
			atts.setProperty("type", typeString);
		
		if (description != null)
			atts.setProperty("text", description);
	}
	
	public String getXMLTag() { return "effect"; }
	public void storeAttributes(Properties atts, int flags) {
		saveProperties(atts);
	}
	
	protected void addOutputChildren(List<XMLOutput> l) {
		if (styledDescription != null)
			l.add(styledDescription);
	}
	
	public Iterator<XMLOutput> getOutputChildren() {
		List<XMLOutput> l = new LinkedList<XMLOutput>();
		addOutputChildren(l);
		return l.iterator();
	}
	
	public void outputTo(PrintStream out, String indent, int flags) throws IOException {
		Node.output(this, out, indent, flags);
	}
	
	public Effect copy() {
		Effect e = createCopy();
		copyFieldsTo(e);
		return e;
	}
	
	protected Effect createCopy() {
		return new Effect();
	}
	
	protected void copyFieldsTo(Effect e) {
		e.type = type;
		e.description = description;
		e.styledDescription = styledDescription;
		if (next != null)
			e.next = next.copy();
	}
}
