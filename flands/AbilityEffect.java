package flands;

import java.util.Properties;

import javax.swing.text.AttributeSet;

import org.xml.sax.Attributes;

/**
 * An object to handle an effect on one of the character's abilities.
 * @author Jonathan Mann
 */
public class AbilityEffect extends Effect {
	public static final int ADJUST_ABILITY = 0;
	public static final int DIVIDE_ABILITY = 1;
	public static final int TARGET_ABILITY = 2;

	private int ability;
	private int modifyType;
	private int modifier;
	private int multiplier = 1;

	public static AbilityEffect createAbilityBonus(int ability, int bonus) {
		AbilityEffect e = new AbilityEffect();
		e.ability = ability;
		e.modifyType = ADJUST_ABILITY;
		e.modifier = bonus;
		System.out.println("AbilityEffect: ability=" + ability + ",bonus=" + bonus);
		return e;
	}
	public static AbilityEffect createAbilityDivider(int ability, int divideBy) {
		AbilityEffect e = new AbilityEffect();
		e.ability = ability;
		e.modifyType = DIVIDE_ABILITY;
		e.modifier = divideBy;
		System.out.println("AbilityEffect: ability=" + ability + ",divideBy=" + divideBy);
		return e;
	}
	public static AbilityEffect createAbilityTarget(int ability, int target) {
		AbilityEffect e = new AbilityEffect();
		e.ability = ability;
		e.modifyType = TARGET_ABILITY;
		e.modifier = target;
		return e;
	}

	public int getAbility() { return ability; }
	public int getModifyType() { return modifyType; }
	public int getValue() { return modifier; }
	/**
	 * Adjust the ability modifier. It is assumed that this will only be
	 * done on an adjust-ability effect.
	 */
	public void adjustValue(int delta) {
		modifier += delta;
	}

	public int adjustAbility(int value) {
		switch (modifyType) {
			case ADJUST_ABILITY:
				return value + modifier*multiplier;
			case DIVIDE_ABILITY:
				return (value + (modifier - 1)) / modifier;
			case TARGET_ABILITY:
				return modifier;
			default:
				System.err.println("AbilityEffect: what type is this?: " + modifyType);
				return value;
		}
	}

	public void addEffect(Effect e, boolean cumulative) {
		if (cumulative) {
			if (e == this) {
				multiplier++;
				return;
			}
			else if (e instanceof AbilityEffect) {
				AbilityEffect ae = (AbilityEffect)e;
				if (ae.ability == ability && modifyType == ADJUST_ABILITY && ae.modifyType == ADJUST_ABILITY) {
					modifier += ae.modifier;
					System.out.println("Modifier is now " + modifier + ",multiplier " + multiplier);
					return;
				}
			}
		}
		super.addEffect(e, cumulative);
	}

	protected String getModStr() {
		switch (modifyType) {
		case ADJUST_ABILITY:
			if (modifier < 0)
				return Integer.toString(modifier);
			else
				return "+" + modifier;
		case DIVIDE_ABILITY:
			return "/" + modifier;
		case TARGET_ABILITY:
			return "\u2192" + modifier; // right-arrow
		}
		return "";
	}

	public boolean addTo(StyledTextList textList, AttributeSet atts) {
		switch (modifyType) {
			case ADJUST_ABILITY:
				String abilityName = Adventurer.getAbilityName(ability);
				if (ability < Adventurer.ABILITY_COUNT)
					abilityName = abilityName.toUpperCase();
				if (styledDescription == null) {
					String text = (description == null) ? abilityName + " " + getModStr() : description;
					/*
					if (ability < Adventurer.ABILITY_COUNT) {
						int abilityIndex = text.toUpperCase().indexOf(abilityName);
						if (abilityIndex >= 0) {
							textList.add(text.substring(0, abilityIndex+1), atts);
							SimpleAttributeSet smallAtts = SectionDocument.getSmallerAtts(atts);
							textList.add(abilityName.substring(1), smallAtts);
							textList.add(text.substring(abilityIndex+abilityName.length()), atts);
							return true;
						}
					}
					*/
					textList.add(text, atts);
				}
				else
					textList.add(styledDescription, atts);
				return true;
			default:
				if (styledDescription != null) {
					textList.add(styledDescription, atts);
				}
				else if (description != null) {
					textList.add(description, atts);
					return true;
				}
				return false;
		}
	}

	public int compareTo(Effect e) {
		if (e instanceof AbilityEffect) {
			AbilityEffect ae = (AbilityEffect)e;
			if (this == ae) return 0;
			if (modifyType != ae.modifyType) return ae.modifyType - modifyType;
			if (modifier != ae.modifier) return ae.modifier - modifier;
			if (ability != ae.ability) return ability - ae.ability;
			return 0;
		}
		return super.compareTo(e);
	}

	public String toString() {
		switch (modifyType) {
			case ADJUST_ABILITY:
				return "AbilityBonus: " + Adventurer.getAbilityName(ability) + getModStr();
			case DIVIDE_ABILITY:
				return "AbilityDivider: " + Adventurer.getAbilityName(ability) + "/" + modifier;
			case TARGET_ABILITY:
				return "AbilityFall: " + Adventurer.getAbilityName(ability) + " -> " + modifier;
		}
		return "AbilityEffect: unknown type";
	}
	
	protected void init(Attributes atts) {
		super.init(atts);
		multiplier = Node.getIntValue(atts, "multiplier", 1);
	}
	
	protected void saveProperties(Properties atts) {
		super.saveProperties(atts);
		
		if (ability >= 0)
			atts.setProperty("ability", Adventurer.getAbilityName(ability));
		
		switch (modifyType) {
		case ADJUST_ABILITY:
			Node.saveProperty(atts, "bonus", modifier);
			break;
		case DIVIDE_ABILITY:
			Node.saveProperty(atts, "divide", modifier);
			break;
		case TARGET_ABILITY:
			Node.saveProperty(atts, "target", modifier);
			break;
		}
	}
	
	public String toLoadableString() {
		String str = "AE(" + Adventurer.getAbilityName(ability) + "," + modifyType + "," + modifier;
		if (description != null)
			str += "," + description;
		return str + ")";
	}
	
	public static AbilityEffect createFrom(String loadable) {
		if (loadable.startsWith("AE(") && loadable.endsWith(")")) {
			String[] params = loadable.substring(3, loadable.length() - 1).split(",");
			if (params.length >= 3) {
				try {
					int ability = Adventurer.getAbilityType(params[0]);
					int modifyType = Integer.parseInt(params[1]);
					int modifier = Integer.parseInt(params[2]);
					if (ability >= 0) {
						AbilityEffect ae = null;
						switch (modifyType) {
						case ADJUST_ABILITY:
							ae = createAbilityBonus(ability, modifier);
							break;
						case DIVIDE_ABILITY:
							ae = createAbilityDivider(ability, modifier);
							break;
						case TARGET_ABILITY:
							ae = createAbilityTarget(ability, modifier);
							break;
						}
						
						if (ae != null) {
							if (params.length == 4)
								ae.description = params[3];
							return ae;
						}
					}
				}
				catch (NumberFormatException nfe) {
					nfe.printStackTrace();
				}
			}
		}
		System.err.println("Couldn't parse AbilityEffect: " + loadable);
		return null;
	}
	
	protected Effect createCopy() {
		return new AbilityEffect();
	}
	protected void copyFieldsTo(Effect e) {
		AbilityEffect ae = (AbilityEffect)e;
		ae.ability = ability;
		ae.modifyType = modifyType;
		ae.modifier = modifier;
		ae.multiplier = multiplier;
		super.copyFieldsTo(ae);
	}
}
