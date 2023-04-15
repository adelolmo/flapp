package flands;


import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.xml.sax.Attributes;

/**
 * Or, a possession; sometihng the character has in their inventory or leaves
 * in a cache. Weapons can be wielded to boost Combat; Armour can be worn to
 * boost Defence; Tools are automatically used to boost an ability.
 * Items may be cursed, have unusual prices, affect other abilities,
 * or have special actions connected with them.
 * ItemNode and MarketNode both rely heavily on this class doing its job.
 * 
 * @author Jonathan Mann
 */
public class Item implements XMLOutput {
	public static final int PLAIN_TYPE = 0;
	public static final int TOOL_TYPE = 1;
	public static final int WEAPON_TYPE = 2;
	public static final int ARMOUR_TYPE = 3;
	private static final String[] ItemTypeNames = {"item", "tool", "weapon", "armour"};

	static Object AbilityPotionSource = "SinglePotionSource";

	public int getType() { return PLAIN_TYPE; }
	public String getTypeName() { return getTypeName(getType()); }
	public static String getTypeName(int type) { return ItemTypeNames[type]; }

	public static final Item createItem(String elementName) {
		return createItem(elementName, null);
	}

	public static Item createItem(String elementName, String itemName) {
		if (itemName != null && itemName.indexOf("|") >= 0) {
			// Create a chain of items
			System.out.println("Creating item chain from attributes");
			String[] itemNames = itemName.split("\\|");
			Item firstItem = null;
			Item prevItem = null;
			for (int i = 0; i < itemNames.length; i++) {
				Item currItem = createItem(elementName, itemNames[i]);
				System.out.println("Item " + i + ": " + currItem.toDebugString());
				if (prevItem == null)
					firstItem = currItem;
				else
					prevItem.nextItem = currItem;
				prevItem = currItem;
			}
			return firstItem;
		}

		if (elementName.equals(ItemTypeNames[PLAIN_TYPE]))
			return new Item(itemName);
		else if (elementName.equals(ItemTypeNames[TOOL_TYPE]))
			return new Tool(itemName);
		else if (elementName.equals(ItemTypeNames[WEAPON_TYPE]))
			return new Weapon(itemName);
		else if (elementName.equals(ItemTypeNames[ARMOUR_TYPE]))
			return new Armour(itemName);
		
		if (itemName != null)
			System.out.println("Item element type unrecognised: " + elementName);
		return null;
	}

	/**
	 * Create an Item wholly from an element's attributes.
	 * The item type is used as an attribute key, with the value being the item's names.
	 * All other attributes are stored as normal.
	 */
	public static Item createItem(Attributes xmlAtts) {
		Item item = null;
		for (int type = 0; type < ItemTypeNames.length; type++) {
			String val = xmlAtts.getValue(ItemTypeNames[type]);
			if (val != null) {
				item = createItem(ItemTypeNames[type], val);
				break;
			}
		}

		if (item != null)
			item.init(xmlAtts);
		return item;
	}

	public Item(String name) {
		this.name = name;
	}

	public static Item createMoneyItem(int shards) {
		return createMoneyItem(shards, "Shard");
	}
	public static Item createMoneyItem(int money, String currency) {
		String name = money + " " + currency;
		if (money != 1)
			name += "s";
		Item i = new Item(name);
		i.money = money;
		i.currency = currency;
		return i;
	}
	
	public static final String NameAttribute = "name";
	public static final String ProfessionAttribute = "profession";
	public static final String AbilityAttribute = "ability";
	public static final String BuyAttribute = "buy";
	public static final String SellAttribute = "sell";
	public static final String BonusAttribute = "bonus";
	public static final String UsingAttribute = "using";

	private String name;
	private boolean checkedForWildcards = false;
	private Pattern namePattern = null;
	private Effect effect = null;
	private Curse curse = null;
	public String getName() { return name; }
	protected void setName(String name) { this.name = name; }

	private int profession = -1;
	public int getProfession() { return profession; }
	public boolean isProfessionSpecific() { return (profession >= 0); }

	private int money = -1;
	private String currency;
	public boolean isMoney() { return money > 0; }
	public int getMoney() { return money; }
	public void setMoney(int shards) {
		this.money = shards;
		name = shards + " " + currency + (shards == 1 ? "" : "s");
		updateDocument();
	}
	public void adjustMoney(int delta) {
		setMoney(money + delta);
	}
	public String getCurrency() {
		return currency;
	}

	public Effect getEffects() { return effect; }
	public void addEffect(Effect e) {
		if (effect == null)
			effect = e;
		else
			effect.addEffect(e);
	}
	public Curse getCurse() { return curse; }
	public void setCurse(Curse c) {
		this.curse = c;
	}

	private int bonus;
	private String bonusStr;
	public int getBonus() { return bonus; }
	void setBonus(int b) { this.bonus = b; }
	public String getBonusString() {
		return (bonus > 0) ? "+" + bonus :
			((bonus < 0) ? "" + bonus : "");
	}
	/**
	 * Adjust the bonus of the item, or the bonus of the first ability effect found.
	 */
	public void adjustBonus(int delta) {
		adjustBonus(delta, Adventurer.ABILITY_SINGLE);
	}
	/**
	 * Adjust the bonus of the item, or the bonus of the first ability effect found.
	 * @param ability the ability of the effect to be increased.
	 */
	public boolean adjustBonus(int delta, int ability) {
		// Subclasses should check against their own ability first, to see if it matches.
		boolean adjusted = false;
		Effect e = getEffects();
		while (e != null) {
			if (e instanceof AbilityEffect) {
				AbilityEffect ae = (AbilityEffect)e;
				if (ae.getModifyType() == AbilityEffect.ADJUST_ABILITY) {
					if (ability == Adventurer.ABILITY_SINGLE ||
						ability == Adventurer.ABILITY_ALL ||
						ability == ae.getAbility()) {
						// TODO: Make sure modified value is appropriate
						ae.adjustValue(delta);
						adjusted = true;
						if (ability != Adventurer.ABILITY_ALL)
							break;
					}
				}
			}
			e = e.nextEffect();
		}
		
		if (adjusted)
			updateDocument();
		
		return adjusted;
	}
	
	/**
	 * Check if the bonus of an item matches our bonus.
	 * @param i the item being matched.
	 */
	protected boolean matchBonus(Item i) {
		if (bonusStr != null) { // ie. a bonus was specified
			if (bonus >= 0) {
				// An exact bonus value was specified
				if (i.getBonus() != bonus)
					return false;
			}
			else {
				// Look for a '+'
				int index = bonusStr.indexOf('+');
				if (index >= 0) {
					// Check if the item has a natural bonus >= to this
					int minBonus = Integer.parseInt(bonusStr.substring(0, index));
					if (i.getBonus() < minBonus) {
						// Failed - now check any effects for the same thing
						boolean foundBonus = false;
						Effect e = i.effect;
						while (e != null) {
							if (e instanceof AbilityEffect) {
								AbilityEffect ae = (AbilityEffect)e;
								if (ae.getModifyType() == AbilityEffect.ADJUST_ABILITY && ae.getValue() >= minBonus) {
									foundBonus = true;
									break;
								}
							}
							e = e.nextEffect();
						}
						if (!foundBonus)
							return false;
					}
				}
			}
		}
		
		return true;
	}

	private int buy = -1, sell = -1;
	private TradeNode.SellNode sellNode = null;
	public int getPrice() { return buy; }
	void setSellNode(TradeNode.SellNode sellNode) {
		this.sellNode = sellNode;
	}
	void removeSellNode(String sellSection) {
		if (sellNode != null && sellNode.getSectionName().equals(sellSection))
			sellNode = null;
	}
	public int getSalePrice() {
		if (sellNode != null) {
			if (sellNode.getSectionName().equals(FLApp.getSingle().getCurrentSection()))
				return sellNode.getPrice();
		}
		if (sell >= 0 && ((SectionNode)FLApp.getSingle().getRootNode()).isSellingMarket())
			return sell;
		return -1;
	}
	public void clearSalePrice() {
		// Clear the sell field for items bought in a market;
		// see TradeNode.BuyNode.actionPerformed() for an explanation.
		sell = -1;
	}
	public boolean isSaleable() { return (getSalePrice() >= 0); }
	void soldItem() {
		if (sellNode != null)
			sellNode.soldItem(this);
	}

	private Item nextItem = null;
	public Item getNextItem() { return nextItem; }

	protected String group = null;
	protected void init(Attributes xmlAtts) {
		if (name == null)
			name = xmlAtts.getValue(NameAttribute);

		if (name != null && Character.isDigit(name.charAt(0))) {
			// Looks like money - parse the amount
			int spaceIndex = name.indexOf(' ');
			if (spaceIndex >= 0) {
				try {
					money = Integer.parseInt(name.substring(0, spaceIndex));
					currency = name.substring(spaceIndex+1);
					if (money != 1 && currency.endsWith("s"))
						currency = currency.substring(0, currency.length()-1);
				}
				catch (NumberFormatException nfe) {
					System.err.println("Item.init(): couldn't parse leading number: " + nfe);
				}
			}
		}

		bonusStr = xmlAtts.getValue(BonusAttribute);
		if (bonusStr != null) {
			try {
				bonus = Integer.parseInt(bonusStr);
			}
			catch (NumberFormatException nfe) { bonus = -1; }
		}

		// "profession" indicates that the item is part of a specific profession's starting equipment
		String professionStr = xmlAtts.getValue(ProfessionAttribute);
		if (professionStr != null)
			profession = Adventurer.getProfessionType(professionStr);

		// Get buy and sell prices - for use in markets
		String valStr = xmlAtts.getValue(BuyAttribute);
		if (valStr != null)
			buy = Integer.parseInt(valStr);
		valStr = xmlAtts.getValue(SellAttribute);
		if (valStr != null)
			sell = Integer.parseInt(valStr);

		if (nextItem != null)
			nextItem.init(xmlAtts);
		else if (name != null && name.indexOf("|") >= 0) {
			System.out.println("Creating chained items from name " + name);
			String[] names = name.split("\\|");
			Item currItem = this;
			for (int i = 0; i < names.length; i++) {
				if (i > 0) {
					Item newItem = currItem.copy();
					currItem.nextItem = newItem;
					currItem = newItem;
				}
				currItem.name = names[i];
				System.out.println("Item " + i + ": " + currItem.toDebugString());
			}
		}

		group = xmlAtts.getValue("group");
		tags = xmlAtts.getValue("tags");
		if (tags != null) {
			// Easier to match with commas before and after every tag
			if (!tags.startsWith(","))
				tags = "," + tags;
			if (!tags.endsWith(","))
				tags = tags + ",";
			System.out.println("Item matches tags " + tags);
		}
	}

	public String getGroup() { return group; }

	private String tags;
	public static final String TagKeep = "keep";
	public boolean hasTag(String tag) {
		if (tags == null)
			return false;
		
		int index = -1;
		while ((index = tags.indexOf(tag, index+1)) >= 0) {
			if (tags.charAt(index-1) == ',' && tags.charAt(index+tag.length()) == ',')
				return true;
		}
		return false;
	}
	public boolean hasKeepTag() { return hasTag(TagKeep); }
	public boolean matchTags(Item item) {
		if (tags == null) return true;
		String itags = item.tags;
		if (itags == null) return false;
		
		int index = tags.indexOf(',') + 1;
		while (index < tags.length()) {
			int index2 = tags.indexOf(',', index);
			if (!item.hasTag(tags.substring(index, index2)))
				return false;
			index = index2+1;
		}
		return true;
	}
	public void addTag(String tag) {
		if (hasTag(tag)) return;
		if (tags == null)
			tags = "," + tag + ",";
		else
			tags = "," + tag + tags;
		System.out.println("Tag string now = " + tags);
	}
	public void addTags(String tagString) {
		String[] split = tagString.split(",");
		for (int s = 0; s < split.length; s++) {
			split[s] = split[s].trim();
			if (split[s].length() > 0)
				addTag(split[s]);
		}
	}
	
	public void removeTag(String tag) {
		if (tags == null) return;
		String match = "," + tag + ",";
		int index = tags.indexOf(match);
		if (index >= 0) {
			tags = tags.substring(0, index+1) + // up to and including leading comma
			tags.substring(index + match.length()); // remainder of string, after tailing comma
			System.out.println("Tag string now = " + tags);
		}
	}
	
	public String toString() { return getName(); }
	public String toDebugString() { return "Plain - " + toString() + (group == null ? "" : " [" + group + "]"); }
	protected boolean matchName(Item item) {
		if (namePattern != null)
			return matchNamePattern(item);
		
		String name = getName().toLowerCase();
		if (!checkedForWildcards) {
			checkedForWildcards = true;
			if (name.length() > 1 && (name.indexOf('?') >= 0 || name.indexOf('*') >= 0)) {
				// Do wildcard matching
				namePattern = FLApp.createNamePattern(name);
				return matchNamePattern(item);
			}
		}
		
		return name.equals("?") || name.equals("*") || name.equals(item.getName().toLowerCase());
	}
	protected boolean matchNamePattern(Item item) {
		return namePattern.matcher(item.getName().toLowerCase()).matches();
	}
	public boolean matches(Item i) {
		if (group != null && (i.group == null || !group.equals(i.group))) return false;
		if (!matchBonus(i)) return false;
		if (!matchTags(i)) return false;
		return matchName(i);
	}

	protected DefaultStyledDocument doc = null;
	public StyledDocument getDocument() {
		if (doc == null) {
			doc = new DefaultStyledDocument();
			updateContent(doc);
		}
		return doc;
	}

	private static AttributeSet standardAtts = null;
	public static AttributeSet getStandardAttributes() {
		if (standardAtts == null) {
			SimpleAttributeSet atts = new SimpleAttributeSet();
			StyleConstants.setBold(atts, true);
			standardAtts = atts;
		}
		return standardAtts;
	}
	private static AttributeSet smallerAtts = null;
	public static AttributeSet getSmallerAttributes() {
		if (smallerAtts == null)
			smallerAtts = SectionDocument.getSmallerAtts(getStandardAttributes());
		return smallerAtts;
	}

	protected void updateDocument() {
		if (doc != null)
			updateContent(doc);
	}

	protected void updateContent(DefaultStyledDocument doc) {
		StyledTextList itemText = createItemText(getStandardAttributes(), false);
		itemText.addTo(doc, true);
	}

	protected StyledTextList createItemText(AttributeSet atts, boolean caps) {
		AttributeSet itemAtts;
		if (atts == null)
			itemAtts = getStandardAttributes();
		else {
			itemAtts = new SimpleAttributeSet(atts);
			StyleConstants.setBold((SimpleAttributeSet)itemAtts, true);
		}

		StyledTextList nameText = new StyledTextList();
		Item item = this;
		while (item != null) {
			item.addTo(nameText, atts, itemAtts, caps);
			item = item.nextItem;

			if (item != null) {
				// List is like "I1, I2, I3 or I4"
				String joiningText = (item.nextItem == null) ? " or " : ", ";
				nameText.add(joiningText, atts);
				caps = false; // only the first item should be capitalised
			}
		}

		return nameText;
	}

	protected void addTo(StyledTextList stList, AttributeSet regularAtts, AttributeSet itemAtts, boolean caps) {
		stList.add(caps ? capitalise(name) : name, itemAtts);

		StyledTextList effectsText = new StyledTextList();
		initialiseEffectsList(effectsText, itemAtts);
		effectsText.addEffects(getEffects(), itemAtts, effectsText.getSize() > 0);
		if (effectsText.getSize() > 0) {
			stList.add(" (", itemAtts);
			stList.add(effectsText);
			stList.add(")", itemAtts);
		}
	}

	protected void initialiseEffectsList(StyledTextList effectsList, AttributeSet atts) {}

	public final Element[] addTo(SectionDocument doc, Element parent, AttributeSet atts, boolean caps) {
		StyledTextList stList = createItemText(atts, caps);
		return stList.addTo(doc, parent);
	}

	protected static final String capitalise(String text) {
		return text.substring(0, 1).toUpperCase() + text.substring(1);
	}

	/**
	 * Let the user choose from the item chain.
	 * The name of each item will be shown in a list, from which the user can pick the item they want.
	 */
	public Item makeSpecific() {
		if (nextItem != null) {
			int itemCount = 1;
			Item currItem = this.nextItem;
			while (currItem != null) {
				itemCount++;
				currItem = currItem.nextItem;
			}

			Item[] items = new Item[itemCount];
			StyledDocument[] docs = new StyledDocument[itemCount];
			currItem = this;
			for (int index = 0; currItem != null; index++) {
				items[index] = currItem.copy();
				docs[index] = items[index].getDocument();
				currItem = currItem.nextItem;
			}

			DocumentChooser itemChooser = new DocumentChooser(FLApp.getSingle(), "Which Item?", docs, false);
			itemChooser.setVisible(true);

			if (itemChooser.getSelectedIndices() == null)
				return null;
			else
				return items[itemChooser.getSelectedIndices()[0]];
		}

		return this;
	}

	public Item copy() {
		Item i = new Item(name);
		copyFieldsTo(i);
		return i;
	}

	protected void copyFieldsTo(Item i) {
		i.profession = profession;
		i.buy = buy;
		i.sell = sell;
		i.money = money;
		i.currency = currency;
		i.tags = tags;
		if (effect != null)
			i.effect = effect.copy();
	}

	public boolean canBeWielded() { return false; }
	
	public static class Weapon extends Item {
		private Weapon(String name) { super(name); }
		public Weapon(String name, int bonus) {
			this(name);
			setBonus(bonus);
		}

		public int getType() { return WEAPON_TYPE; }

		protected void init(Attributes atts) {
			super.init(atts);
			if (Node.getBooleanValue(atts, UsingAttribute, false))
				wielded = true;
		}
		
		protected void saveProperties(Properties props) {
			super.saveProperties(props);
			if (wielded)
				Node.saveProperty(props, UsingAttribute, wielded);
		}
		
		public int getAbility() { return Adventurer.ABILITY_COMBAT; }
		public boolean affectsAbility(int ability) { return ability == Adventurer.ABILITY_COMBAT; }
		public String getAbilityName() { return Adventurer.getAbilityName(getAbility()); }
		
		public boolean adjustBonus(int delta, int ability) {
			boolean adjusted = false;
			if (ability == getAbility() ||
				ability == Adventurer.ABILITY_SINGLE ||
				ability == Adventurer.ABILITY_ALL) {
				setBonus(getBonus() + delta);
				adjusted = true;
			}
			
			if (!adjusted || ability == Adventurer.ABILITY_ALL)
				adjusted = adjusted || super.adjustBonus(delta, ability);
			
			if (adjusted)
				updateDocument();
			
			return adjusted;
		}
		
		private boolean wielded = false;
		public boolean canBeWielded() { return true; }
		public boolean isWielded() { return wielded; }
		public void setWielded(boolean b) {
			if (wielded != b) {
				wielded = b;
				updateDocument();
			}
		}

		protected boolean matchName(Item i) {
			return getName() == null || i.getName() == null || super.matchName(i);
		}
		public boolean matches(Item i) {
			if (group != null && (i.group == null || !group.equals(i.group))) return false;
			if (i.getType() == getType()) {
				Weapon w = (Weapon)i;
				return (matchBonus(w) &&
						matchTags(w) &&
						getAbility() == w.getAbility() &&
						matchName(w) &&
						(!wielded || w.wielded));
			}
			return false;
		}

		public String getFullName() {
			if (getBonus() == 0)
				return getName();
			else
				return getName() + " (" + getAbilityName() + " " + getBonusString() + ")";
		}

		/**
		 * This implementation should also work for Tool.
		 */
		protected void initialiseEffectsList(StyledTextList effectsList, AttributeSet atts) {
			if (getBonus() != 0) {
				// Include ability bonus
				if (getAbility() != Adventurer.ABILITY_ALL)
					effectsList.addAbilityName(getAbilityName(), atts);
				else
					effectsList.add("all", atts);
				effectsList.add(" " + getBonusString(), atts);
			}
		}

		public String toDebugString() { return "Weapon - " + getFullName() + (group == null ? "" : " [" + group); }

		protected void addTo(StyledTextList stList, AttributeSet regularAtts, AttributeSet itemAtts, boolean caps) {
			if (getName() == null) {
				// This is a market description of a weapon - no specific type
				if (getBonus() == 0) {
					stList.add("Without ", regularAtts);
					stList.addAbilityName("COMBAT", regularAtts);
					stList.add(" bonus", regularAtts);
				}
				else {
					stList.addAbilityName("COMBAT", regularAtts);
					stList.add(" bonus " + getBonusString(), regularAtts);
				}
			}
			else
				super.addTo(stList, regularAtts, itemAtts, caps);
		}

		private static String[] WeaponTypes = null;
		/**
		 * Weapons tend to be listed by bonuses in market, without a specific name;
		 * if the name attribute is null, this method will generate a list of weapon types
		 * from which the user can pick one.
		 */
		public Item makeSpecific() {
			if (getName() == null) {
				if (WeaponTypes == null) {
					WeaponTypes = new String[] {"sword", "scimitar", "mace", "quarterstaff", "axe", "dagger", "spear", "battle-axe", "staff", "trident", "warhammer"};
					Arrays.sort(WeaponTypes);
				}
				DefaultStyledDocument[] weaponDocs = new DefaultStyledDocument[WeaponTypes.length];
				for (int w = 0; w < WeaponTypes.length; w++) {
					weaponDocs[w] = new DefaultStyledDocument();
					try {
						weaponDocs[w].insertString(0, capitalise(WeaponTypes[w]), getStandardAttributes());
					}
					catch (BadLocationException ble) {}
				}

				DocumentChooser weaponChooser = new DocumentChooser(FLApp.getSingle(), "Choose Weapon Type", weaponDocs, false);
				weaponChooser.setVisible(true);

				if (weaponChooser.getSelectedIndices() == null)
					return null;
				
				Item i = copy();
				i.setName(WeaponTypes[weaponChooser.getSelectedIndices()[0]]);
				return i;
			}
			return this;
		}

		public Item copy() {
			Weapon w = new Weapon(getName(), getBonus());
			copyFieldsTo(w);
			return w;
		}
	}
	

	public static class Tool extends Weapon {
		private Tool(String name) { super(name); }
		public Tool(String name, int ability, int bonus) {
			super(name, bonus);
			this.ability = ability;
		}

		public int getType() { return TOOL_TYPE; }

		private int ability;
		public int getAbility() { return ability; }
		public boolean affectsAbility(int a) {
			return (ability == a) || (ability == Adventurer.ABILITY_ALL);
		}

		// Shouldn't need to choose which tool to wield
		public boolean canBeWielded() {
			return (getAbility() == Adventurer.ABILITY_ALL);
		}
		public void setWielded(boolean b) {
			if (canBeWielded())
				super.setWielded(b);
		}

		protected void init(Attributes xmlAtts) {
			super.init(xmlAtts);

			String valStr = xmlAtts.getValue(AbilityAttribute);
			if (valStr != null)
				ability = Adventurer.getAbilityType(valStr);
		}

		protected void saveProperties(Properties props) {
			super.saveProperties(props);
			if (ability >= 0)
				props.setProperty(AbilityAttribute, Adventurer.getAbilityName(ability));
		}
		
		public String toDebugString() { return "Tool - " + getFullName() + (group == null ? "" : " [" + group); }

		public Item copy() {
			Tool t = new Tool(getName(), ability, getBonus());
			copyFieldsTo(t);
			return t;
		}
	}

	public static class Armour extends Item {
		private Armour(String name) { super(name); }
		public Armour(String name, int bonus) {
			this(name);
			setBonus(bonus);
		}

		protected void init(Attributes atts) {
			super.init(atts);
			if (Node.getBooleanValue(atts, UsingAttribute, false))
				worn = true;
		}
		
		protected void saveProperties(Properties props) {
			super.saveProperties(props);
			if (worn)
				Node.saveProperty(props, UsingAttribute, worn);
		}
		
		private boolean worn = false;
		public boolean isWorn() { return worn; }
		public void setWorn(boolean b) {
			if (worn != b) {
				worn = b;
				updateDocument();
			}
		}
		
		public boolean adjustBonus(int delta, int ability) {
			boolean adjusted = false;
			if (ability == Adventurer.ABILITY_DEFENCE ||
				ability == Adventurer.ABILITY_SINGLE ||
				ability == Adventurer.ABILITY_ALL) {
				setBonus(getBonus() + delta);
				adjusted = true;
			}
			
			if (!adjusted || ability == Adventurer.ABILITY_ALL)
				adjusted = adjusted || super.adjustBonus(delta, ability);
			
			if (adjusted)
				updateDocument();
			
			return adjusted;
		}

		public int getType() { return ARMOUR_TYPE; }
		public String getFullName() { return getName() + " (Defence " + getBonusString() + ")"; }

		protected void initialiseEffectsList(StyledTextList effectsList, AttributeSet atts) {
			// Include Defence bonus
			if (getBonus() != 0)
				effectsList.add("Defence " + getBonusString(), atts);
		}

		public String toDebugString() { return "Armour - " + getFullName() + (group == null ? "" : " [" + group + "]"); }

		public Item copy() {
			Armour a = new Armour(getName(), getBonus());
			copyFieldsTo(a);
			return a;
		}

		public boolean matches(Item i) {
			if (group != null && (i.group == null || !group.equals(i.group))) return false;
			if (i.getType() == ARMOUR_TYPE) {
				Armour a = (Armour)i;
				return (matchBonus(i) &&
						matchTags(i) &&
						(!worn || a.worn));

				// Don't check name - types of armour can vary (eg. 5.355, leather jerkin +1)
				// TODO: May need more checking if there are effects attached
			}
			return false;
		}
	}

	public static Item[] createTestItems() {
		Item[] items = new Item[4];
		items[0] = new Item("verdigris key");
		items[1] = new Weapon("spear", 1);
		items[2] = new Tool("amber wand", Adventurer.ABILITY_MAGIC, 1);
		items[3] = new Armour("chain mail", 3);
		return items;
	}
	
	protected void saveProperties(Properties props) {
		if (name != null)
			props.setProperty("name", name);
		if (profession >= 0)
			props.setProperty("profession", Adventurer.getProfessionName(profession));
		if (bonusStr != null)
			props.setProperty(BonusAttribute, bonusStr);
		if (buy >= 0)
			Node.saveProperty(props, "buy", buy);
		if (sell >= 0)
			Node.saveProperty(props, "sell", sell);
		if (group != null)
			props.setProperty("group", group);
		if (bonus != 0)
			Node.saveProperty(props, "bonus", bonus);
		if (tags != null)
			props.setProperty("tags", tags);
	}
	
	public void outputTo(PrintStream out, String indent, int flags) throws IOException {
		Node.output(this, out, indent, flags);
	}
	public void outputXML(PrintStream out, String indent) throws IOException {
		outputTo(out, indent, XMLOutput.OUTPUT_PROPS_STATIC | XMLOutput.OUTPUT_PROPS_DYNAMIC);
	}
	
	public String getXMLTag() { return getTypeName(); }
	public void storeAttributes(Properties atts, int flags) { saveProperties(atts); }
	public Iterator<XMLOutput> getOutputChildren() {
		if (getEffects() == null && getCurse() == null)
			return null;
		
		LinkedList<XMLOutput> l = new LinkedList<XMLOutput>();
		Effect eff = getEffects();
		while (eff != null) {
			l.add(eff);
			eff = eff.nextEffect();
		}
		
		Curse c = getCurse();
		if (c != null)
			l.add(c);
		
		return l.iterator();
	}
}
