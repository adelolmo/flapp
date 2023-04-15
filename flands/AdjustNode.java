package flands;


import java.util.Properties;

import javax.swing.text.Element;

import org.xml.sax.Attributes;

/**
 * Behaving something like an IfNode, an AdjustNode adjusts the 'value'
 * (whatever that may be) of the parent Node if its conditions are met.
 * @author Jonathan Mann
 */
public class AdjustNode extends Node {
	public static final String ElementName = "adjust";
	private int value;
	private boolean automatic = false;
	private String valueStr;
	private String god;
	private int profession = -1;
	private Item item;
	private String codeword;
	private String name;
	private int ship = -1;
	private int crew = -1;
	private String titleKey;
	private int ability = -1;
	private int abilityModifier;
	private String greaterThan, lessThan;
	private int titleDefault = 0;
	
	public AdjustNode(Node parent) {
		super(ElementName, parent);
	}

	public void init(Attributes atts) {
		valueStr = atts.getValue("value");
		if (valueStr == null)
			valueStr = atts.getValue("amount");
		if (atts.getLength() == 1) {
			System.out.println("AdjustNode: 1 attribute (assume it's value)");
			automatic = true;
		}
		god = atts.getValue("god");
		String val = atts.getValue("profession");
		if (val != null)
			profession = Adventurer.getProfessionType(val);
		titleKey = atts.getValue("titleVal");
		if (titleKey != null)
			titleDefault = getIntValue(atts, "default", titleDefault);
		item = Item.createItem(atts);
		codeword = atts.getValue("codeword");
		name = atts.getValue("name");
		val = atts.getValue("ship");
		if (val != null)
			ship = Ship.getType(val);
		val = atts.getValue("crew");
		if (val != null)
			crew = Ship.getCrew(val);
		val = atts.getValue("ability");
		if (val != null) {
			ability = Adventurer.getAbilityType(val);
			if (ability >= 0)
				abilityModifier = Adventurer.getAbilityModifier(atts.getValue("modifier"));
		}

		if (ability >= 0 || name != null) { // or any other values that can be compared against
			greaterThan = atts.getValue("greaterthan");
			lessThan = atts.getValue("lessthan");
		}

		super.init(atts);
	}

	protected void outit(Properties props) {
		super.outit(props);
		
		saveVarProperty(props, "value", valueStr);
		if (god != null) props.setProperty("god", god);
		if (profession >= 0) props.setProperty("profession", Adventurer.getProfessionName(profession));
		if (titleKey != null) {
			props.setProperty("titleVal", titleKey);
			saveProperty(props, "default", titleDefault);
		}
		if (item != null) item.saveProperties(props);
		if (codeword != null) props.setProperty("codeword", codeword);
		if (name != null) props.setProperty("name", name);
		if (ship >= 0) props.setProperty("ship", Ship.getTypeName(ship));
		if (crew >= 0) props.setProperty("crew", Ship.getCrewName(crew));
		if (ability >= 0) {
			props.setProperty("ability", Adventurer.getAbilityName(ability));
			props.setProperty("modifier", Adventurer.getAbilityModifierName(abilityModifier));
		}
		if (greaterThan != null) props.setProperty("greaterThan", greaterThan);
		if (lessThan != null) props.setProperty("lessThan", lessThan);
	}
	
	protected Element createElement() { return null; }

	public int getAdjustment() {
		if (meetsConditions()) {
			if (valueStr != null)
				return getAttributeValue(valueStr);
			else
				return value;
		}
		return 0;
	}

	private boolean checkComparisons(int val) {
		boolean result = false;
		if (greaterThan != null) {
			try {
				int i = Integer.parseInt(greaterThan);
				if (val > i)
					return true;
			}
			catch (NumberFormatException nfe) { System.out.println("greaterthan attribute should have an integer value: " + nfe); }
		}
		if (lessThan != null) {
			try {
				int i = Integer.parseInt(lessThan);
				if (val < i)
					return true;
			}
			catch (NumberFormatException nfe) { System.out.println("lessthan attribute should have an integer value: " + nfe); }
		}
		return result;
	}

	protected boolean meetsConditions() {
		if (god != null && getAdventurer().hasGod(god))
			return true;
		if (profession >= 0 && getAdventurer().getProfession() == profession)
			return true;
		if (item != null && getItems().findMatches(item).length > 0)
			return true;
		if (codeword != null && getCodewords().hasCodeword(codeword))
			return true;
		if (titleKey != null) {
			value = getAdventurer().getTitleValue(titleKey, titleDefault);
			return true;
		}
		if (ship >= 0 || crew >= 0) {
			int shipIndex = getShips().getSingleShip();
			if (shipIndex < 0)
				System.out.println("AdjustNode: couldn't find single ship to adjust by!");
			else {
				Ship s = getShips().getShip(shipIndex);
				if (ship >= 0 && s.getType() == ship)
					return true;
				if (crew >= 0 && s.getCrew() == crew)
					return true;
			}
		}
		if (ability >= 0) {
			if (lessThan == null && greaterThan == null) {
				int score = getAdventurer().getAbilityValue(ability, abilityModifier, Adventurer.PURPOSE_VALUE);
				System.out.println("Ability score=" + score);
				value = score;
				return true;
			}
			else {
				int score = getAdventurer().getAbilityValue(ability, abilityModifier, Adventurer.PURPOSE_TESTING);
				if (checkComparisons(score))
					return true;
			}
		}
		if (name != null) {
			int nameValue = getCodewords().getValue(name);
			if (lessThan == null && greaterThan == null) {
				System.out.println("FieldNode value=" + nameValue);
				value = nameValue;
				return true;
			}
			else if (checkComparisons(nameValue))
				return true;
		}
		if (automatic)
			return true;

		return false;
	}
}
