package flands;


import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;

import org.xml.sax.Attributes;

/**
 * A curse on the character - either a curse (magical), disease, or poison, although
 * the latter two are usually treated the same. Each curse will generally have
 * one or more ability effects attached, generally nasty.
 * 
 * @author Jonathan Mann
 */
public class Curse implements XMLOutput {
	public static final int CURSE_TYPE = 0;
	public static final int DISEASE_TYPE = 1;
	public static final int POISON_TYPE = 2;
	private static final String[] TypeNames = {"curse", "disease", "poison"};
	public static String getTypeName(int type) { return TypeNames[type]; }

	private int type;
	private String name;
	private Effect effect;
	private boolean cumulative;
	private Item item = null;
	private String liftQuestion;

	public Curse(int type, String name) {
		this.type = type;
		this.name = name;
	}

	public static final Curse createCurse(String elementName) {
		for (int t = 0; t < TypeNames.length; t++)
			if (elementName.equals(TypeNames[t]))
				return new Curse(t, null);
		return null;
	}

	public static Curse createCurse(Attributes atts) {
		for (int t = 0; t < TypeNames.length; t++) {
			String name = atts.getValue(TypeNames[t]);
			if (name != null) {
				Curse c = new Curse(t, name);
				c.init(atts);
				return c;
			}
		}
		return null;
	}

	public void init(Attributes atts) {
		if (name == null)
			name = atts.getValue("name");
		cumulative = Node.getBooleanValue(atts, "cumulative", false);
		liftQuestion = atts.getValue("lift");
	}

	public int getType() { return type; }
	public boolean isCurse() { return type == CURSE_TYPE; }
	public boolean isDisease() { return type == DISEASE_TYPE; }
	public boolean isPoison() { return type == POISON_TYPE; }
	public boolean isCumulative() { return cumulative; }
	public String getLiftQuestion() { return liftQuestion; }

	public String getName() { return name; }
	public Effect getEffects() { return effect; }
	public void addEffect(Effect e) {
		if (effect == null)
			effect = e;
		else
			effect.addEffect(e);
	}
	public void addCurse(Curse c) {
		if (cumulative) {
			// Add any adjust effects together
			for (Effect e = c.effect; e != null; e = e.nextEffect()) {
				System.out.println("Adding effect " + e + " to " + effect);
				effect.addEffect(e, true);
			}
		}
		else
			System.out.println("Adding curses together doesn't make sense if they're not cumulative");
	}
	public Item getItem() { return item; }
	public void setItem(Item i) {
		this.item = i;
	}

	public boolean matches(Curse c) {
		switch (type) {
			case CURSE_TYPE:
				if (c.type != type)
					return false;
				break;
			case DISEASE_TYPE:
			case POISON_TYPE:
				// I think poisons and diseases are usually treated the same
				// Maybe I could make them one type, but until I'm sure, I'll leave them separated
				if (c.type != DISEASE_TYPE && c.type != POISON_TYPE)
					return false;
		}
		if (name != null && !name.equals("?") && !name.equals("*") && !name.equalsIgnoreCase(c.name)) return false;
		return true;
	}

	public boolean equals(Object o) {
		try {
			Curse c = (Curse)o;
			return name.equalsIgnoreCase(c.name);
		}
		catch (ClassCastException cce) { return false; }
	}

	public String toString() { return getName(); }

	public static void main(String args[]) {
		Curse c = new Curse(CURSE_TYPE, "Curse of Donkey's Ears");
		c.addEffect(AbilityEffect.createAbilityBonus(Adventurer.ABILITY_CHARISMA, -2));
	}

	/* *****************
	 * XMLOutput methods
	 ***************** */
	public String getXMLTag() { return getTypeName(type); }

	public void storeAttributes(Properties atts, int flags) {
		if (name != null)
			atts.setProperty("name", name);
		if (cumulative)
			Node.saveProperty(atts, "cumulative", true);
		if (liftQuestion != null)
			atts.setProperty("lift", liftQuestion);
	}

	public Iterator<XMLOutput> getOutputChildren() {
		LinkedList<XMLOutput> l = new LinkedList<XMLOutput>();
		Effect eff = getEffects();
		while (eff != null) {
			l.add(eff);
			eff = eff.nextEffect();
		}
		return l.iterator();
	}

	public void outputTo(PrintStream out, String indent, int flags) throws IOException {
		Node.output(this, out, indent, flags);
	}
}
