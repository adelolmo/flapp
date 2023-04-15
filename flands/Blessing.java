package flands;

import java.util.Properties;

import javax.swing.text.AttributeSet;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyledDocument;

import org.xml.sax.Attributes;

/**
 * One of the blessings on the character. Some blessings may be permanent (used
 * repeatedly); others may only activate when the player chooses
 * (eg. the Defence blessing).
 * @see BlessingList
 * 
 * @author Jonathan Mann
 */
public class Blessing {
	static final int MATCHALL_TYPE = -2;
	static final int MATCHANY_TYPE = -1;
	public static final int ABILITY_TYPE = 0;
	public static final int STORM_TYPE = 1;
	public static final int DEFENCE_TYPE = 2;
	public static final int INJURY_TYPE = 3;
	public static final int DISEASE_TYPE = 4;
	public static final int LUCK_TYPE = 5;
	public static final int TRAVEL_TYPE = 6;
	public static final int WRATH_TYPE = 7;

	private int type;
	private int ability = -1;
	private int bonus;
	private boolean permanent = false;
	private DefaultStyledDocument doc = null;

	public Blessing(int type) {
		this.type = type;
	}
	public Blessing(int type, int value) {
		this(type);
		if (type == ABILITY_TYPE)
			ability = value;
		else if (type == DEFENCE_TYPE)
			bonus = value;
	}

	private static final Blessing[] AbilityBlessings = new Blessing[Adventurer.ABILITY_COUNT];
	public static Blessing getAbilityBlessing(int ability) {
		if (AbilityBlessings[ability] == null)
			AbilityBlessings[ability] = new Blessing(ABILITY_TYPE, ability);
		return AbilityBlessings[ability];
	}

	public static final Blessing DEFENCE = new Blessing(DEFENCE_TYPE, 3); // 5.89 has this figure - is this a default?
	public static final Blessing DISEASE = new Blessing(DISEASE_TYPE);
	public static final Blessing INJURY = new Blessing(INJURY_TYPE);
	public static final Blessing LUCK = new Blessing(LUCK_TYPE);
	public static final Blessing STORM = new Blessing(STORM_TYPE);
	public static final Blessing TRAVEL = new Blessing(TRAVEL_TYPE);
	public static final Blessing WRATH = new Blessing(WRATH_TYPE);

	public static Blessing getBlessing(Attributes atts) {
		String type = atts.getValue("blessing");
		if (type == null)
			return null;
		type = type.toLowerCase();
		if (type.equals("?"))
			return new Blessing(MATCHANY_TYPE);
		else if (type.equals("*"))
			return new Blessing(MATCHALL_TYPE);

		type = type.toLowerCase();

		int ability = Adventurer.getAbilityType(type);
		if (type.indexOf("defen") >= 0) {
			int bonus = 3;
			String bonusStr = atts.getValue("bonus");
			if (bonusStr != null)
				try {
					bonus = Integer.parseInt(bonusStr);
				}
			catch (NumberFormatException nfe) {}
			return new Blessing(DEFENCE_TYPE, bonus);
		}
		else if (type.indexOf("disease") >= 0 || type.indexOf("poison") >= 0)
			return new Blessing(DISEASE_TYPE);
		else if (type.indexOf("injury") >= 0)
			return new Blessing(INJURY_TYPE);
		else if (type.indexOf("storm") >= 0)
			return new Blessing(STORM_TYPE);
		else if (type.indexOf("luck") >= 0)
			return new Blessing(LUCK_TYPE);
		else if (type.indexOf("travel") >= 0)
			return new Blessing(TRAVEL_TYPE);
		else if (type.indexOf("wrath") >= 0)
			return new Blessing(WRATH_TYPE);
		else if (ability >= 0 && ability < Adventurer.ABILITY_COUNT)
			return new Blessing(ABILITY_TYPE, ability);

		return null;
	}

	public void saveTo(Properties props) {
		String typeStr = null;
		switch (type) {
		case MATCHALL_TYPE:
			typeStr = "*";
			break;
		case MATCHANY_TYPE:
			typeStr = "?";
			break;
		case ABILITY_TYPE:
			typeStr = Adventurer.getAbilityName(ability);
			break;
		case STORM_TYPE:
			typeStr = "storm";
			break;
		case DEFENCE_TYPE:
			typeStr = "defence";
			break;
		case INJURY_TYPE:
			typeStr = "injury";
			break;
		case DISEASE_TYPE:
			typeStr = "disease";
			break;
		case LUCK_TYPE:
			typeStr = "luck";
			break;
		case TRAVEL_TYPE:
			typeStr = "travel";
			break;
		case WRATH_TYPE:
			typeStr = "wrath";
			break;
		}
		if (typeStr != null)
			props.setProperty("blessing", typeStr);
		
		if (type == DEFENCE_TYPE)
			Node.saveProperty(props, "bonus", bonus);
	}
	
	public int getType() { return type; }
	public boolean isAbilityBlessing() { return (type == ABILITY_TYPE); }
	public int getAbility() { return ability; }
	public int getBonus() { return bonus; }
	public boolean isPermanent() { return permanent; }
	public void setPermanent(boolean b) { permanent = b; }

	private static AbilityEffect lastDefenceEffect = null;
	private static Adventurer getAdventurer() { return FLApp.getSingle().getAdventurer(); }
	/**
	 * Add a Defence bonus to the player's abilities.
	 * @param bonus the bonus to the player's Defence stat.
	 */
	public static void addDefenceBlessing(int bonus) {
		AbilityEffect e = AbilityEffect.createAbilityBonus(Adventurer.ABILITY_DEFENCE, bonus);
		getAdventurer().getEffects().addStatRelated(Adventurer.ABILITY_DEFENCE, DEFENCE, e);
		lastDefenceEffect = e;
		getAdventurer().checkAbilityBonus(Adventurer.ABILITY_DEFENCE);
	}
	
	/**
	 * Add the same Defence bonus that was last used in the
	 * {@link #addDefenceBlessing(int)} method.
	 */
	public static void readdDefenceBlessing() {
		if (lastDefenceEffect != null) {
			getAdventurer().getEffects().addStatRelated(Adventurer.ABILITY_DEFENCE, DEFENCE, lastDefenceEffect);
			getAdventurer().checkAbilityBonus(Adventurer.ABILITY_DEFENCE);
		}
	}
	/**
	 * Remove the Defence blessing that's currently in place.
	 * The bonus will be remembered, in case it needs to be added again
	 * if a player undo-es the last roll of a fight.
	 */
	public static void removeDefenceBlessing() {
		if (lastDefenceEffect != null) {
			getAdventurer().getEffects().removeStatRelated(Adventurer.ABILITY_DEFENCE, DEFENCE, lastDefenceEffect);
			getAdventurer().checkAbilityBonus(Adventurer.ABILITY_DEFENCE);
		}
	}
	
	/**
	 * Looks for an active Defence blessing;
	 * @return the value of the Defence bonus; <code>0</code> if not present.
	 */
	public static int findActiveDefenceBlessing() {
		return getAdventurer().getEffects().getStatRelatedBonus(Adventurer.ABILITY_DEFENCE, DEFENCE);
	}
	
	public String getContentString() {
		switch (type) {
			case ABILITY_TYPE:
				return Adventurer.getAbilityName(ability).toUpperCase();
			case STORM_TYPE:
				return "Safety from Storms";
			case DEFENCE_TYPE:
				return "Defence through Faith";
			case DISEASE_TYPE:
				return "Immunity to Disease/Poison";
			case INJURY_TYPE:
				return "Immunity to Injury";
			case LUCK_TYPE:
				return "Luck";
			case TRAVEL_TYPE:
				return "Safe Travel";
			case WRATH_TYPE:
				return "Divine Wrath";
			case MATCHALL_TYPE:
				return "*MatchAll*";
			default:
				return "Unknown Blessing";
		}
	}

	public StyledDocument getDocument() {
		if (doc == null) {
			doc = new DefaultStyledDocument();
			StyledTextList text = new StyledTextList();
			addTo(text, null);
			text.addTo(doc, false);
		}
		return doc;
	}

	public void addTo(StyledTextList textList, AttributeSet atts) {
		if (type == ABILITY_TYPE)
			textList.addAbilityName(Adventurer.getAbilityName(ability), atts);
		else
			textList.add(getContentString(), atts);
		
		if (permanent)
			textList.add(" (permanent)", atts);
	}

	public boolean equals(Object o) {
		try {
			Blessing b = (Blessing)o;
			if (type != b.type) return false;
			if (type == ABILITY_TYPE && ability != b.ability) return false;
			return true;
		}
		catch (ClassCastException cce) {
			cce.printStackTrace();
		}
		return false;
	}
	
	String toLoadableString() {
		String str = "" + (char)type;
		if (type == ABILITY_TYPE)
			str += (char)ability;
		else if (type == DEFENCE_TYPE)
			str += (char)bonus;
		if (permanent)
			str += ",1";
		return str;
	}
	
	static Blessing createFromLoadableString(String str) {
		int type = str.charAt(0);
		Blessing b;
		if (type == ABILITY_TYPE)
			b = getAbilityBlessing(str.charAt(1));
		else if (str.length() > 1)
			b = new Blessing(type, str.charAt(1));
		else
			b = new Blessing(type);
		
		if (str.endsWith(",1"))
			b.setPermanent(true);
		
		return b;
	}
}
