package flands;


import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.AbstractDocument.BranchElement;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.text.Position;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Handles the character being played: abilities, gods, titles, money and resurrections,
 * as well as the visible Documents connected with each of these.
 * Loading and saving of this data is also centralised here.
 * 
 * @author Jonathan Mann
 * 
 * @see Codewords
 * @see BlessingList
 * @see CurseList
 * @see ShipList
 * @see ExtraChoice
 */
public class Adventurer implements Loadable {
	private static final String adventurerFileName = "chardata.ini";

	public static final int PROF_PRIEST = 0;
	public static final int PROF_MAGE = 1;
	public static final int PROF_ROGUE = 2;
	public static final int PROF_TROUBADOUR = 3;
	public static final int PROF_WARRIOR = 4;
	public static final int PROF_WAYFARER = 5;
	public static final int PROF_COUNT = 6;
	private static final String[] ProfessionNames = new String[] {"Priest", "Mage", "Rogue", "Troubadour", "Warrior", "Wayfarer"};

	public static final int ABILITY_CHARISMA = 0;
	public static final int ABILITY_COMBAT = 1;
	public static final int ABILITY_MAGIC = 2;
	public static final int ABILITY_SANCTITY = 3;
	public static final int ABILITY_SCOUTING = 4;
	public static final int ABILITY_THIEVERY = 5;
	/** The number of basic abilities (not including Rank, Defence & Stamina). */
	public static final int ABILITY_COUNT = 6;

	public static final int ABILITY_RANK = 6;
	public static final int ABILITY_DEFENCE = 7;
	public static final int ABILITY_STAMINA = 8;
	public static final int ABILITY_ALL = 9; // all of the 6 abilities, that is
	public static final int ABILITY_SINGLE = 10; // match one of the 6 abilities

	private static final String[] AbilityNames = new String[] {"Charisma", "Combat", "Magic", "Sanctity", "Scouting", "Thievery", "Rank", "Defence", "Stamina", "*"};

	private static final String[][] RankNames = new String[][]
		{
			{"Outcast"},
			{"Commoner"},
			{"Guildmember"},
			{"Master", "Mistress"},
			{"Gentleman", "Lady"},
			{"Baron", "Baroness"},
			{"Count", "Countess"},
			{"Earl", "Viscountess"},
			{"Marquis", "Marchioness"},
			{"Duke", "Duchess"},
			{"Hero", "Heroine"}
		};
	private String fullName;
	private int profession;
	private AbilityStat[] abilities;
	private StaminaStat stamina;
	private RankStat rank;
	private SingleStat defence;
	private boolean male;
	private StyledDocument history;
	private List<God> gods = new LinkedList<God>();
	private boolean godless = false;
	private int shards = 0;
	private PlainDocument godDoc = null;
	private PlainDocument shardsDoc = null;
	private List<Title> titles = new LinkedList<Title>();
	private ItemList items = new ItemList(this);
	private EffectSet effects = new EffectSet(this);

	private Codewords codewords = new Codewords(this);

	private BlessingList blessings = new BlessingList();

	private CurseList curses = new CurseList(this);

	private ShipList ships = new ShipList();

	private ExtraChoice.List choices = new ExtraChoice.List();
	
	private Flag.Set flags = new Flag.Set();
	
	private ActiveRuleset rules = new ActiveRuleset();
	
	private boolean hardcore = false;
	private long hardcoreTime = 0;
	
	Adventurer() {
		abilities = new AbilityStat[ABILITY_COUNT];
		for (int a = 0; a < ABILITY_COUNT; a++)
			abilities[a] = new AbilityStat(a);
		stamina = new StaminaStat();
		rank = new RankStat();
		defence = new SingleStat();
		ships.setOnLand();
	}

	public String getName() { return fullName; }
	public void setName(String name) {
		// TODO: Shift stuff that was saved under the old name
		this.fullName = name;
	}
	public String getFolderName() {
		// Remove everything but letters and digits from name
		if (fullName == null) return null;
		StringBuffer sb = new StringBuffer(fullName);
		for (int i = sb.length() - 1; i >= 0; i--)
			if (!Character.isLetterOrDigit(sb.charAt(i))) {
				if (sb.charAt(i) == ' ')
					sb.setCharAt(i, '_');
				else
					sb.deleteCharAt(i);
			}
		return sb.toString();
	}

	public boolean isMale() { return male; }
	public void setMale(boolean b) {
		if (male != b) {
			male = b;
			rank.updateDocument();
		}
	}
	public Stat getRank() { return rank; }
	public String getRankDescription() {
		int index = Math.min(rank.affected - 1, RankNames.length - 1);
		return RankNames[index][(male || RankNames[index].length == 1) ? 0 : 1];
	}

	public boolean isDead() {
		if (stamina.current <= 0) return true;
		for (int a = 0; a < ABILITY_COUNT; a++)
			if (abilities[a].natural <= 0)
				return true;
		return false;
	}

	public AbilityStat getCharisma() { return abilities[ABILITY_CHARISMA]; }
	public AbilityStat getCombat() { return abilities[ABILITY_COMBAT]; }
	public AbilityStat getMagic() { return abilities[ABILITY_MAGIC]; }
	public AbilityStat getSanctity() { return abilities[ABILITY_SANCTITY]; }
	public AbilityStat getScouting() { return abilities[ABILITY_SCOUTING]; }
	public AbilityStat getThievery() { return abilities[ABILITY_THIEVERY]; }
	public Stat getAbility(int abilityType) {
		if (abilityType >= 0 && abilityType < ABILITY_COUNT)
			return abilities[abilityType];
		switch (abilityType) {
			case ABILITY_DEFENCE:
				return defence;
			case ABILITY_STAMINA:
				return stamina;
			case ABILITY_RANK:
				return rank;
		}

		System.out.println("getAbility(" + abilityType + "): error!");
		return null;
	}

	public boolean isAbilityMaxed(int abilityType) {
		return (abilityType < ABILITY_COUNT && abilities[abilityType].natural == 12);
	}

	/**
	 * Adjust an ability by the given amount, without killing the player.
	 * @return the amount the ability was actually adjusted by; an ability can only be
	 * lowered to 1, and one of the major six can only go up to 12.
	 */
	public int adjustAbility(int abilityType, int delta) {
		return adjustAbility(abilityType, delta, false);
	}
	
    /**
     * Adjust an ability by the given amount.
     * @param abilityType one of the ability types; if <code>ABILITY_ALL</code>, the
     * result will be the value closest to 0.
     * @param fatal if <code>true</code>, and an ability would be decreased to 0 or below,
     * the ability is only decreased to 1 and the current stamina is decreased to 0.
	 * @return the amount the ability was actually adjusted by.
	 */
	public int adjustAbility(int abilityType, int delta, boolean fatal) {
		boolean death = false;
		int result;
		if (abilityType < ABILITY_COUNT || abilityType == ABILITY_ALL) {
			result = delta;
			int firstAbility = abilityType, lastAbility = abilityType;
			if (abilityType == ABILITY_ALL) {
				firstAbility = 0;
				lastAbility = ABILITY_COUNT - 1;
			}
			
			for (int a = firstAbility; a <= lastAbility; a++) {
				int d = delta;
				// Edited 16/2/08: this first if() worked on the affected stat, rather
				// than the natural one, which can lead to the natural stat being
				// mistakenly reduced below 1. This comment is here in case that
				// wasn't just a bug...
				if (abilities[a].natural + delta < 1) {
					if (fatal)
						death = true;
					d = -abilities[a].natural + 1;
					if (d > result)
						result = d;
				}
				else if (abilities[a].natural + delta > 12) {
					d = 12 - abilities[a].natural;
					if (d < result)
						result = d;
				}

				abilities[a].natural += d;
				abilities[a].affected += d;
				abilities[a].updateDocument();
				if (a == ABILITY_COMBAT) {
					defence.natural += d;
					defence.affected += d;
					defence.updateDocument();
				}
			}
		}
		else if (abilityType == ABILITY_RANK) {
			if (rank.affected + delta <= 0) {
				// Treat reduction to rank 0 as death.
				// Q: If we have an item that increases rank, and we die,
				// then surely a resurrection deal will remove that item and
				// the natural rank will be below 0 again!?
				if (fatal)
					death = true;
				delta = -rank.natural + 1; // assume we'll lose any rank-increasing item, then
			}
			rank.natural += delta;
			rank.affected += delta;
			rank.updateDocument();
			defence.natural += delta;
			defence.affected += delta;
			defence.updateDocument();
			result = delta;
		}
		else if (abilityType == ABILITY_STAMINA) {
			if (stamina.affected + delta < 1) {
				if (fatal)
					death = true;
				delta = -stamina.affected+1;
			}
			stamina.natural += delta;
			stamina.affected += delta;
			stamina.current += delta;
			if (stamina.current <= 0) {
				if (fatal)
					stamina.current = 0;
				else
					stamina.current = 1;
			}
			stamina.updateDocument();
			result = delta;
		}
		else if (abilityType == ABILITY_DEFENCE) {
			// huh? this stat is a sum of other stats - can't be adjusted itself
			System.err.println("Adventurer.adjustAbility(DEFENCE) - this shouldn't happen!");
			result = 0;
		}
		else {
			System.err.println("Adventurer.adjustAbility(" + abilityType + "): unknown type");
			result = 0;
		}

		if (death) {
			stamina.damage(stamina.current);
			getExtraChoices().checkMenu();
		}
		return result;
	}

	public void raiseAbility(int abilityType) {
		System.out.println("Raising ability " + abilityType);
		adjustAbility(abilityType, 1);
	}

	public StyledDocument getAbilityDocument() {
		return AbilityStat.createAbilityDocument(abilities);
	}

	/**
	 * Get the ability that has been selected, given a position in the ability document.
	 */
	public int posToAbility(int docPos) {
		for (int i = 0; i < ABILITY_COUNT; i++)
			if (abilities[i].containsPosition(docPos))
				return i;
		return -1;
	}
	
	public String getAbilityTooltip(int ability) {
		return effects.getAbilityAdjustments(ability, abilities[ability].natural);
	}
	
	/**
	 * Generate a set of Documents, each showing an ability name and an accompanying stat.
	 * This is intended for use by {@link DocumentChooser}.
	 */
	public static StyledDocument[] getAbilityDocuments(int[] abilityTypes, int[] stats) {
		DefaultStyledDocument[] docs = new DefaultStyledDocument[abilityTypes.length];
		SimpleAttributeSet bigFontAtts = new SimpleAttributeSet();
		SimpleAttributeSet smallFontAtts = SectionDocument.getSmallerAtts(bigFontAtts);
		for (int a = 0; a < abilityTypes.length; a++) {
			String str = getAbilityName(abilityTypes[a]).toUpperCase();
			docs[a] = new DefaultStyledDocument();
			try {
				docs[a].insertString(0, str.substring(0, 1), bigFontAtts);
				docs[a].insertString(1, str.substring(1), smallFontAtts);
				docs[a].insertString(docs[a].getLength(), " " + stats[a], bigFontAtts);
			}
			catch (BadLocationException ble) {}
		}

		return docs;
	}

	public Stat getDefence() { return defence; }

	public static String getAbilityName(int type) { return AbilityNames[type]; }
	public static int getAbilityType(String name) {
		String match = name.toLowerCase();
		for (int a = 0; a < ABILITY_COUNT; a++)
			if (match.equals(AbilityNames[a].toLowerCase()))
				return a;
		if (match.equals("rank"))
			return ABILITY_RANK;
		else if (match.startsWith("defen"))
			return ABILITY_DEFENCE;
		else if (match.startsWith("stam"))
			return ABILITY_STAMINA;
		else if (match.equals("*"))
			return ABILITY_ALL;
		else if (match.equals("?"))
			return ABILITY_SINGLE;

		return -1;
	}

	public static int[] getAbilityTypes(String attrVal) {
		int[] result;
		if (attrVal == null || attrVal.equals("?") || attrVal.equals("*")) {
			result = new int[ABILITY_COUNT];
			for (int a = 0; a < result.length; a++)
				result[a] = a;
		}
		else if (attrVal.indexOf("|") < 0) {
			result = new int[] { getAbilityType(attrVal) };
			if (result[0] < 0)
				System.out.println("Adventurer: unrecognised ability name: " + attrVal);
		}
		else {
			String[] vals = attrVal.split("\\|");
			result = new int[vals.length];
			for (int a = 0; a < vals.length; a++) {
				result[a] = Adventurer.getAbilityType(vals[a]);
				if (result[a] < 0)
					System.out.println("RatingExperience: unrecognised ability name: " + vals[a]);
			}
		}
		return result;
	}

	public StaminaStat getStamina() { return stamina; }

	private int difficultyRollDice = 2;
	/**
	 * Get the number of dice to be used in a Difficulty roll.
	 * See 3.91 for the case where this gets changed to 1.
	 */
	public int getDifficultyDice() { return difficultyRollDice; }
	public void setDifficultyDice(int dice) {
		if (dice > 0)
			difficultyRollDice = dice;
	}
	
	public int getProfession() { return profession; }
	public String getProfessionName() { return getProfessionName(getProfession()); }
	void setProfession(int p) { profession = p; FLApp.getSingle().refreshAdventureSheet(); }

	public static String getProfessionName(int type) { return ProfessionNames[type]; }
	public static int getProfessionType(String name) {
		if (name.length() >= 2) {
			String match = name.toLowerCase();
			for (int p = 0; p < PROF_COUNT; p++)
				if (ProfessionNames[p].toLowerCase().startsWith(match))
					return p;
		}
		return -1;
	}

	public static class God {
		private String name;
		private AbilityEffect effect = null;
		private List<String> compatible = null;
		
		public God(String name) { this.name = name; }
		public void addCompatibleGod(String god) {
			if (compatible == null) compatible = new LinkedList<String>();
			compatible.add(god.toLowerCase());
		}
		
		public String getName() { return name; }
		public AbilityEffect getEffect() { return effect; }
		public void setEffect(AbilityEffect effect) { this.effect = effect; }
		
		public boolean compatibleWith(String god) {
			if (compatible == null) return false;
			if (god.equals("")) return false;
			god = god.toLowerCase();
			for (Iterator<String> i = compatible.iterator(); i.hasNext(); ) {
				String compat = i.next();
				if (compat.indexOf('?') >= 0 || compat.indexOf('*') >= 0) {
					Pattern godPattern = FLApp.createNamePattern(compat);
					if (godPattern.matcher(god).matches())
						return true;
				}
				else if (compat.equals(god))
					return true;
			}
			return false;
		}
		
		private String getCompatibleString() {
			if (compatible == null) return null;
			StringBuffer sb = new StringBuffer();
			for (Iterator<String> i = compatible.iterator(); i.hasNext(); ) {
				if (sb.length() > 0)
					sb.append(",");
				sb.append(i.next());
			}
			return sb.toString();
		}
	}
	
	public static class GodEffectSrc {
		private String god;
		public GodEffectSrc(String god) { this.god = god; }
		public String getGod() { return god; }
		public boolean equals(Object o) {
			if (o instanceof GodEffectSrc)
				return ((GodEffectSrc)o).getGod().equalsIgnoreCase(god);
			return false;
		}
		public String toString() { return "[" + god + "]"; }
	}
	
	public boolean hasGod(String god) {
		if (god.equals("*"))
			return !gods.isEmpty();
		else if (god.equals(""))
			return gods.isEmpty();
		for (Iterator<God> i = gods.iterator(); i.hasNext(); )
			if (i.next().getName().equalsIgnoreCase(god))
				return true;
		return false;
	}
	
	/**
	 * Checks whether the given god can be 'added' without affecting any gods in place.
	 * @return <code>false</code> if the character already worships this god, or a non-compatible one;
	 * <code>true</code> otherwise.
	 */
	public boolean safeToAddGod(String god) {
		if (god.equals("")) return true;
		if (gods.isEmpty()) return true;
		if (hasGod(god)) return false;
		for (Iterator<God> i = gods.iterator(); i.hasNext(); )
			if (!i.next().compatibleWith(god))
				return false;
		return true;
	}
	
	/**
	 * Convenience method to handle the removal of a specific god.
	 * Removes any resurrections or ability effects tied to the god in question.
	 */
	private void removeAGod(God g, Iterator<God> i) {
		for (int r = resurrections.size() - 1; r >= 0; r--)
			if (resurrections.get(r).isGod(g.getName()))							
				// Resurrection was connected to following a particular god
				removeResurrection(resurrections.get(r));
		
		if (g.getEffect() != null) {
			// Remove it now
			effects.removeStatRelated(g.getEffect().getAbility(), new GodEffectSrc(g.getName()), g.getEffect());
			effects.notifyOwner();
		}
		i.remove();		
	}
	
	public void setGod(String god) { setGod(god, null); }
	public void setGod(String god, String compatible) {
		if (gods.size() == 1 && gods.get(0).getName().equalsIgnoreCase(god))
			// No change
			return;
		
		if (godless && god != null && !god.equals(""))
			return; // can't have a god
		
		God newGod = new God(god);
		if (compatible != null) {
			StringTokenizer st = new StringTokenizer(compatible, ",;");
			while (st.hasMoreTokens())
				newGod.addCompatibleGod(st.nextToken().trim());
		}
		
		// Remove incompatible gods
		for (Iterator<God> i = gods.iterator(); i.hasNext(); ) {
			God g = i.next();
			// Either old god should be compatible with new god,
			// or new god compatible with old god
			if (!g.compatibleWith(god) && !newGod.compatibleWith(g.getName())) {
				removeAGod(g, i);
			}
		}
		
		// Add new god
		if (god != null && !god.equals(""))
			gods.add(newGod);
		
		updateGodDocument();
	}
	public void setGodEffect(String god, AbilityEffect ae) {
		for (Iterator<God> i = gods.iterator(); i.hasNext(); ) {
			God g = i.next();
			if (g.getName().equalsIgnoreCase(god)) {
				if (g.getEffect() != null)
					effects.removeStatRelated(g.getEffect().getAbility(), new GodEffectSrc(g.getName()), g.getEffect());
			
				g.setEffect(ae);
				effects.addStatRelated(ae.getAbility(), new GodEffectSrc(g.getName()), ae);
				effects.notifyOwner();
			}
		}
	}
	public void removeGod(String god) {
		if (god != null) {
			for (Iterator<God> i = gods.iterator(); i.hasNext(); ) {
				God g = i.next();
				if (god.equals("*") || god.equalsIgnoreCase(g.getName())) {
					removeAGod(g, i);
				}
					
			}
			updateGodDocument();
		}
	}
	public boolean isGodless() { return godless; }
	public void setGodless(boolean b) {
		if (b != godless) {
			godless = b;
			if (b && !gods.isEmpty())
				setGod("", null);
			FLApp.getSingle().refreshAdventureSheet();
		}
	}
	
	private String getGodsString() {
		if (gods.isEmpty()) return "None";
		StringBuffer godStr = new StringBuffer();
		for (Iterator<God> i = gods.iterator(); i.hasNext(); ) {
			if (godStr.length() > 0)
				godStr.append(", ");
			godStr.append(i.next().getName());
		}
		return godStr.toString();		
	}
	private void updateGodDocument() {
		if (godDoc != null) {
			try {
				System.out.println("Updating God document");
				SimpleAttributeSet atts = null;
				/* Appears not to work - no Attributes in text-fields?
				if (godless) {
					atts = new SimpleAttributeSet();
					StyleConstants.setItalic(atts, true);
				}
				*/
				godDoc.replace(0, godDoc.getLength(), getGodsString(), atts);
			}
			catch (BadLocationException ble) {}
		}
	}
	
	public Document getGodDocument() {
		if (godDoc == null) {
			godDoc = new PlainDocument();
			try {
				godDoc.insertString(0, getGodsString(), null);
			}
			catch (BadLocationException ble) {}
		}
		return godDoc;
	}

	private PlainDocument titleDocument = null;
	public boolean hasTitle(String title) {
		String lowerTitle = title.toLowerCase();
		for (Iterator<Title> i = titles.iterator(); i.hasNext(); ) {
			if (i.next().matches(lowerTitle))
				return true;
		}
		return false;
	}
	public void addTitle(String title) {
		if (!hasTitle(title)) {
			Title t = new Title(title);
			titles.add(t);
			updateTitleDocument();
		}
	}
	public void addTitle(String title, String pattern, int initialValue, int adjustBy) {
		if (pattern == null) {
			addTitle(title);
			return;
		}

		String lowerTitle = title.toLowerCase();
		boolean existingTitle = false;
		for (Iterator<Title> i = titles.iterator(); i.hasNext(); ) {
			Title t = i.next();
			if (t.matches(lowerTitle)) {
				existingTitle = true;
				t.adjustValue(adjustBy);
				break;
			}
		}

		if (!existingTitle) {
			Title t = new Title(title, pattern, initialValue);
			titles.add(t);
		}

		updateTitleDocument();
	}
	public int getTitleValue(String titleKey, int defaultValue) {
		String lowerTitle = titleKey.toLowerCase();
		for (Iterator<Title> i = titles.iterator(); i.hasNext(); ) {
			Title t = i.next();
			if (t.matches(lowerTitle))
				return t.getValue();
		}
		return defaultValue;
	}

	public boolean removeTitle(String title) {
		String lowerTitle = title.toLowerCase();
		for (ListIterator<Title> i = titles.listIterator(); i.hasNext(); )
			if (i.next().matches(lowerTitle)) {
				i.remove();
				updateTitleDocument();
				return true;
			}
		return false;
	}
	public Document getTitleDocument() {
		if (titleDocument == null) {
			titleDocument = new PlainDocument();
			updateTitleDocument();
		}
		return titleDocument;
	}
	private void updateTitleDocument() {
		if (titleDocument == null) return;
		StringBuffer titleBuffer = new StringBuffer();
		for (Iterator<Title> i = titles.iterator(); i.hasNext(); )
			titleBuffer.append(i.next()).append('\n');
		if (titleBuffer.length() > 0)
			titleBuffer.setLength(titleBuffer.length() - 1); // removes the last newline

		try {
			titleDocument.replace(0, titleDocument.getLength(), titleBuffer.toString(), null);
		}
		catch (BadLocationException ble) {}
	}

	private List<ChangeListener> moneyListeners = null;
	public int getMoney() { return shards; }
	public void setMoney(int shards) {
		if (shards != this.shards) {
			this.shards = shards;
			updateMoneyDocument();
			notifyListeners(moneyListeners, this);
		}
	}
	public void adjustMoney(int delta) {
		if (delta != 0) {
			shards += delta;
			if (shards < 0)
				shards = 0;
			updateMoneyDocument();
			notifyListeners(moneyListeners, this);
		}
	}
	public Document getMoneyDocument() {
		if (shardsDoc == null) {
			shardsDoc = new PlainDocument();
			String moneyStr;
			if (shards > 1)
				moneyStr = shards + " Shards";
			else
				moneyStr = (shards == 0 ? "None" : "1 Shard");
			try {
				shardsDoc.insertString(0, moneyStr, null);
			}
			catch (BadLocationException ble) {}
		}
		return shardsDoc;
	}
	private void updateMoneyDocument() {
		if (shardsDoc != null) {
			String moneyStr = (shards > 0 ? shards + " Shards" : "None");
			try {
				shardsDoc.replace(0, shardsDoc.getLength(), moneyStr, null);
			}
			catch (BadLocationException ble) {}
		}
	}
	public void addMoneyListener(ChangeListener l) {
		if (moneyListeners == null)
			moneyListeners = new LinkedList<ChangeListener>();
				moneyListeners.add(l);
	}
	public void removeMoneyListener(ChangeListener l) {
		if (moneyListeners != null)
			moneyListeners.remove(l);
	}

	private static void notifyListeners(List<ChangeListener> listeners, Object src) {
		if (listeners != null) {
			ChangeEvent evt = new ChangeEvent(src);
			for (Iterator<ChangeListener> i = listeners.iterator(); i.hasNext(); )
				i.next().stateChanged(evt);
		}
	}

	public boolean isHardcore() { return hardcore; }
	void setHardcore(boolean b) { hardcore = b; }
	
	/**
	 * Check that the hardcore state is valid. This checks that the timestamp stored with
	 * the character is close enough to that on the file.
	 * @return <code>false</code> if the timestamps fail to match (meaning we won't continue
	 * as hardcore); <code>true</code> otherwise.
	 */
	public boolean validateHardcore(long time) {
		if (hardcore) {
			System.out.println("File timestamp: " + time);
			System.out.println("Hardcore stamp: " + hardcoreTime);
			long diff = Math.abs(time - hardcoreTime);
			if (diff > 100) {
				hardcore = false;
				return false;
			}
		}
		return true;
	}
	
	public ItemList getItems() { return items; }

	public EffectSet getEffects() { return effects; }

	public Codewords getCodewords() { return codewords; }

	public BlessingList getBlessings() { return blessings; }

	public CurseList getCurses() { return curses; }

	public ShipList getShips() { return ships; }

	public ExtraChoice.List getExtraChoices() { return choices; }
	
	public Flag.Set getFlags() { return flags; }
	
	private List<Resurrection> resurrections = new LinkedList<Resurrection>();
	private DefaultStyledDocument resurrectionDoc = null;
	//public Resurrection getResurrection() { return resurrection; }

	public boolean hasResurrection() { return !resurrections.isEmpty(); }
	public void addResurrection(Resurrection r) {
		if (!r.isSupplemental()) {
			// Remove existing, non-supplemental resurrections
			for (int i = resurrections.size() - 1; i >= 0; i--)
				if (!resurrections.get(i).isSupplemental())
					resurrections.remove(i);
		}
		resurrections.add(r);
		boolean worshipper = false;
		for (Iterator<God> i = gods.iterator(); i.hasNext(); ) {
			if (r.isGod(i.next().getName())) {
				worshipper = true;
				break;
			}
		}
		if (!worshipper)
			// ie. renouncing a god will only cancel a resurrection deal if that deal
			// was purchased while we worshipped that god.
			r.setGod(null);
		updateResurrectionDoc();
	}
	
	public void removeResurrection(Resurrection r) {
		for (int i = resurrections.size() - 1; i >= 0; i--)
			if (resurrections.get(i) == r)
				resurrections.remove(i);
		updateResurrectionDoc();
	}
	
	public Resurrection chooseResurrection(String title) {
		switch (resurrections.size()) {
			case 0:
				return null;
			case 1:
				return resurrections.get(0);
		}
		
		DefaultStyledDocument[] docs = new DefaultStyledDocument[resurrections.size()];
		for (int i = 0; i < resurrections.size(); i++) {
			DefaultStyledDocument doc = new DefaultStyledDocument();
			StyledTextList list = resurrections.get(i).getContent(null);
			list.addTo(doc, true);
			docs[i] = doc;
		}
		
		DocumentChooser chooser = new DocumentChooser(FLApp.getSingle(), title, docs, false);
		chooser.setVisible(true);
		
		if (chooser.getSelectedIndices() == null) return null;
		return resurrections.get(chooser.getSelectedIndices()[0]);
	}
	
	public StyledDocument getResurrectionDocument() {
		if (resurrectionDoc == null)
			resurrectionDoc = new DefaultStyledDocument();
		updateResurrectionDoc();
		return resurrectionDoc;
	}

	private void updateResurrectionDoc() {
		if (resurrectionDoc == null) return;
		try {
			if (resurrections.size() == 0)
				resurrectionDoc.remove(0, resurrectionDoc.getLength());
			else {
				boolean first = true;
				for (Iterator<Resurrection> i = resurrections.iterator(); i.hasNext(); ) {
					if (!first)
						resurrectionDoc.insertString(resurrectionDoc.getLength(), "\n", null);
					Resurrection r = i.next();
					StyledTextList list = r.getContent(null);
					list.addTo(resurrectionDoc, first);
					first = false;
				}
			}
		}
		catch (BadLocationException ble) {}
	}

	public static final int MODIFIER_AFFECTED = 0;
	public static final int MODIFIER_NATURAL = 1;
	public static final int MODIFIER_NOTOOL = 2;
	public static final int MODIFIER_NOARMOUR = 3;
	public static final int MODIFIER_CURRENT = 4;
	private static final String[] AbilityModifierNames =
		{"affected", "natural", "noweapon", "noarmour", "current"};
	
	public static int getAbilityModifier(String val) {
		if (val == null)
			return MODIFIER_AFFECTED;
		else if (val.startsWith("nat"))
			return MODIFIER_NATURAL;
		else if (val.startsWith("nowea") || val.startsWith("notoo"))
			return MODIFIER_NOTOOL;
		else if (val.startsWith("noarm"))
			return MODIFIER_NOARMOUR;
		else if (val.startsWith("curr"))
			return MODIFIER_CURRENT;
		else
			return MODIFIER_AFFECTED;
	}
	public static String getAbilityModifierName(int type) {
		try {
			return AbilityModifierNames[type];
		}
		catch (ArrayIndexOutOfBoundsException e) {
			return "";
		}
	}

	public static final int PURPOSE_TESTING = 0;
	public static final int PURPOSE_VALUE = 1;
	public int getAbilityValue(int ability, int modifier) {
		Stat stat = getAbility(ability);
		switch (modifier) {
			case MODIFIER_NATURAL:
				return stat.natural;
			case MODIFIER_CURRENT:
				if (ability == ABILITY_STAMINA) {
					System.out.println("Current stamina value=" + stamina.current);
					return ((StaminaStat)stat).current;
				}
				// Fall-through intentional
			case MODIFIER_AFFECTED:
				System.out.println("Affected ability value=" + stat.affected);
				return stat.affected;
		}
		return getEffects().adjustAbility(ability, stat.natural, modifier);
	}

	private static Item MaskItem = new Item("*mask");
	public int getAbilityValue(int ability, int modifier, int purpose) {
		int value = getAbilityValue(ability, modifier);
		Stat s = getAbility(ability);
		if (!s.isCursed() && !s.isFixed())
			return value;
		if (ability == ABILITY_CHARISMA && items.findMatches(MaskItem).length > 0)
			return value;
		
		if (s.isCursed())
			return (purpose == PURPOSE_TESTING ? -1000 : 0);
		else // s.isFixed()
			return 1;
	}

	
	void checkAbilityBonus(int ability) {
		System.out.println("Adventurer.checkAbilityBonus(" + ability + ")");
		Stat affectedStat = null;
		switch (ability) {
			case ABILITY_CHARISMA:
			case ABILITY_COMBAT:
			case ABILITY_MAGIC:
			case ABILITY_SANCTITY:
			case ABILITY_SCOUTING:
			case ABILITY_THIEVERY:
				affectedStat = abilities[ability];
				abilities[ability].affected = effects.adjustAbility(ability, abilities[ability].natural);
				break;
			case ABILITY_RANK:
				affectedStat = rank;
				rank.affected = effects.adjustAbility(ability, rank.natural);
				break;
			case ABILITY_DEFENCE:
				affectedStat = defence;
				defence.affected = abilities[ABILITY_COMBAT].natural + rank.affected;
				defence.affected = effects.adjustAbility(ability, defence.affected);
				break;
			case ABILITY_STAMINA:
				affectedStat = stamina;
				int oldValue = stamina.affected;
				int newValue = effects.adjustAbility(ability, stamina.natural);
				stamina.current += newValue - oldValue;
				stamina.affected = newValue;
				break;
		}
		affectedStat.updateDocument();
	}

	private void calcAffectedStats() {
		// Reset the affected stats
		for (int a = 0; a < ABILITY_COUNT; a++)
			abilities[a].affected = abilities[a].natural;
		stamina.affected = stamina.natural;
		rank.affected = rank.natural;
		defence.natural = abilities[ABILITY_COMBAT].natural + rank.natural;
		defence.affected = defence.natural;

		// Recalculate the bonuses
		for (int a = 0; a < ABILITY_COUNT; a++)
			abilities[a].affected = effects.adjustAbility(a, abilities[a].natural);
		rank.affected = effects.adjustAbility(ABILITY_RANK, rank.natural);
		stamina.affected = effects.adjustAbility(ABILITY_STAMINA, stamina.natural);

		// Look through all other items affecting abilities
		for (int a = 0; a < ABILITY_COUNT; a++)
			abilities[a].updateDocument();

		calcAffectedStamina();
		rank.updateDocument();
		calcDefence();
	}

	private void calcAffectedStamina() {
		// TODO: Look for items affecting stamina
		if (stamina.current > stamina.affected)
			stamina.current = stamina.affected;
		stamina.updateDocument();
	}

	private void calcDefence() {
		// Q: What is the 'natural' defence?  To quote the books:
		// Your Defence score is equal to:
		//   your COMBAT score, _not_ including any weapon bonus
		//   plus your Rank
		//   plus the bonus for the armour you're wearing (if any)
		// 5.689 has a fight where Defence=COMBAT + Rank only - a good definition of natural?
		// Except there are items that specifically raise Defence without being armour.
		// A compromise for now: defence.natural is the sum of relevant natural values
		// .affected is the sum of relevant affected values
		int naturalCombat = abilities[ABILITY_COMBAT].natural;
		int affectedRank = rank.affected;
		if (isRuleActive(ActiveRuleset.SARVEN)) {
			// Limit Combat and Rank bonuses to Defence to 12
			// naturalCombat = Math.min(12, naturalCombat); // not necessary for natural score
			affectedRank  = Math.min(12, affectedRank);
		}
		defence.affected = naturalCombat + affectedRank;
		defence.affected = effects.adjustAbility(ABILITY_DEFENCE, defence.affected);
		defence.updateDocument();
	}

	public boolean isRuleActive(String rule) {
		return FLApp.getSingle().isRuleActive(rule);
	}
	void updatedActiveRules() {
		calcDefence();
	}
	
	public String getFilename() { return adventurerFileName; }

	public static Adventurer[] loadAvailableAdventurers() {
		ArrayList<Adventurer> list = new ArrayList<Adventurer> ();
			File currentDir = new File(".");
		if (currentDir.isDirectory()) {
			String[] files = currentDir.list();
			for (int f = 0; f < files.length; f++) {
				if (new File(files[f]).isDirectory()) {
					System.out.println("Attempting to load from sub-directory: " + files[f]);
					try {
						FileInputStream in = new FileInputStream(files[f] + File.separator + adventurerFileName);
						Adventurer adv = load(in);
						if (adv != null)
							list.add(adv);
					}
					catch (FileNotFoundException fnfe) {}
				}
			}
		}

		Adventurer[] advArray = new Adventurer[list.size()];
		list.toArray(advArray);
		return advArray;
	}

	public boolean loadFrom(InputStream in) {
		try {
			Properties charProps = new Properties();
			charProps.load(in);

			boolean success = true;
			String val;
			fullName = charProps.getProperty("Name");
			if (fullName == null)
				success = false;

			val = charProps.getProperty("Rank");
			if (val == null)
				success = false;
			else
				rank.natural = Integer.parseInt(val);

			val = charProps.getProperty("Gender");
			if (val.toUpperCase().startsWith("M"))
				setMale(true);
			else if (val.toUpperCase().startsWith("F"))
				setMale(false);
			else
				success = false;

			val = charProps.getProperty("Stamina");
			if (val == null)
				success = false;
			else
				stamina.natural = Integer.parseInt(val);

			val = charProps.getProperty("CurrentStamina");
			if (val == null)
				success = false;
			else
				stamina.current = Integer.parseInt(val);

			for (int a = 0; a < ABILITY_COUNT; a++) {
				val = charProps.getProperty(AbilityNames[a]);
				if (val == null)
					success = false;
				else
					abilities[a].natural = Integer.parseInt(val);
				val = charProps.getProperty(AbilityNames[a]+".Flags");
				if (val != null)
					abilities[a].flags = Integer.parseInt(val);
			}

			String professionStr = charProps.getProperty("Profession");
			if (professionStr == null)
				success = false;
			else {
				profession = getProfessionType(professionStr);
				if (profession < 0)
					success = false;
			}

			godless = (charProps.getProperty("Godless") != null);
			for (int g = 0; true; g++) {
				String godKey = "God" + (g == 0 ? "" : "" + g);
				String god = charProps.getProperty(godKey);
				if (god == null)
					break;
				String godCompatible = charProps.getProperty(godKey + "Compatible");
				setGod(god, godCompatible);
				val = charProps.getProperty(godKey + "Effect");
				if (val != null) {
					AbilityEffect ge = AbilityEffect.createFrom(val);
					if (ge != null)
						setGodEffect(god, ge);
				}
			}

			val = charProps.getProperty("Shards");
			if (val != null)
				shards = Integer.parseInt(val);
			updateMoneyDocument();

			calcAffectedStats();
			codewords.refresh();

			resurrections.clear();
			for (int i = 0; true; i++) {
				Resurrection r = Resurrection.loadResurrection(charProps, i);
				if (r == null) break;
				resurrections.add(r);
			}
			
			val = charProps.getProperty("TitleCount");
			if (val != null) {
				int titleCount = Integer.parseInt(val);
				titles.clear();
				for (int i = 0; i < titleCount; i++) {
					String titleStr = charProps.getProperty("Title" + i);
					if (titleStr != null)
						titles.add(Title.createFromLoadableString(titleStr));
				}
			}
			
			val = charProps.getProperty("DifficultyDice");
			if (val != null)
				difficultyRollDice = Integer.parseInt(val);

			val = charProps.getProperty("Hardcore");
			System.out.println("Hardcore val=" + val);
			if (val == null)
				hardcore = false;
			else {
				hardcore = true;
				hardcoreTime = Long.parseLong(val);
			}
			
			flags.loadFrom(charProps);
			
			rules = new ActiveRuleset();
			rules.addFixedRules(charProps.getProperty("Rules"));
			
			return success;
		}
		catch (IOException ioe) {
			System.err.println("Corrupted file: " + ioe);
		}
		catch (NumberFormatException nfe) {
			System.err.println("Bad integer: " + nfe);
		}

		return false;
	}

	public static Adventurer load(InputStream in) {
		Adventurer ch = new Adventurer();
		return (ch.loadFrom(in) ? ch : null);
	}

	public boolean saveTo(OutputStream out) {
		ExtProperties charProps = new ExtProperties();
		charProps.setProperty("Name", fullName);
		charProps.setProperty("Gender", male ? "M" : "F");
		charProps.set("Rank", rank.natural);
		charProps.set("Stamina", stamina.natural);
		charProps.set("CurrentStamina",stamina.current);
		charProps.setProperty("Profession", getProfessionName());
		for (int a = 0; a < ABILITY_COUNT; a++) {
			charProps.set(AbilityNames[a], abilities[a].natural);
			if (abilities[a].flags != 0)
				charProps.set(AbilityNames[a]+".Flags", abilities[a].flags);
		}
		charProps.set("Shards", shards);
		
		if (godless)
			charProps.setProperty("Godless", "true");
		for (int i = 0; i < gods.size(); i++) {
			God g = gods.get(i);
			String godKey = "God" + (i == 0 ? "" : "" + i);
			charProps.setProperty(godKey, g.getName());
			String compatible = g.getCompatibleString();
			if (compatible != null)
				charProps.setProperty(godKey + "Compatible", compatible);
			if (g.getEffect() != null)
				charProps.setProperty(godKey + "Effect", g.getEffect().toLoadableString());
		}
		
		for (int i = 0; i < resurrections.size(); i++)
			resurrections.get(i).saveTo(charProps, i);
		charProps.set("TitleCount", titles.size());
		for (int t = 0; t < titles.size(); t++)
			charProps.setProperty("Title" + t, titles.get(t).toLoadableString());
		if (difficultyRollDice != 2)
			charProps.setProperty("DifficultyDice", Integer.toString(difficultyRollDice));

		if (hardcore)
			charProps.setProperty("Hardcore", Long.toString(System.currentTimeMillis()));
		
		flags.saveTo(charProps);
		charProps.setProperty("Rules", rules.getFixedRules());
		
		try {
			charProps.store(out, null);
			return true;
		}
		catch (IOException ioe) {
			System.err.println(ioe.toString());
		}

		return false;
	}
	
	public String toString() { return fullName; }
	public String toDebugString() {
		StringBuffer sb = new StringBuffer();
		sb.append("[Name=").append(fullName);
		sb.append("\n Profession=").append(getProfessionName());
		sb.append("\n Rank=").append(getRank().affected).append(", Description=").append(getRankDescription());
		sb.append("\n Stamina=").append(getStamina().current).append("/").append(getStamina().affected);
		for (int a = 0; a < ABILITY_COUNT; a++)
			sb.append("\n ").append(AbilityNames[a]).append('=').append(abilities[a].affected);
		sb.append('\n').append(items.toDebugString());
		sb.append("\n]");
		return sb.toString();
	}

	private static final String startingAdventurersFileName = "Adventurers.xml";
	public static Adventurer[] loadStarting(Books.BookDetails book) {
		if (book == null || !book.hasBook()) return null;
		
		try {
			InputStream in = book.getInputStream(startingAdventurersFileName);
			if (in == null) {
				System.out.println("Couldn't find starting adventurer's file: "
						+ startingAdventurersFileName);
				return null;
			}
			
			XMLReader reader = XMLReaderFactory.createXMLReader();
			Adventurer[] result = new Adventurer[PROF_COUNT];
			reader.setContentHandler(new Handler(result));
			reader.parse(new InputSource(in));
			return result;
		}
		catch (SAXException se) {
			System.out.println("Error creating reader: " + se);
		}
		catch (IOException ioe) {
			System.err.println("Parsing error: " + ioe);
		}
		return null;
	}

	private static class Handler implements ContentHandler {
		private Adventurer[] advs;

		private Handler(Adventurer[] advs) {
			this.advs = advs;
			for (int a = 0; a < advs.length; a++)
				if (advs[a] == null) {
					advs[a] = new Adventurer();
					advs[a].profession = a;
				}
		}

		private LinkedList<String> tagStack = new LinkedList<String>();
		private int[] abilityIndices = null;
		private int currentProf = -1;
		private int currentStyle = 0;
		private StyledDocument currentDocument = null;
		private String getCurrentTag() { return (tagStack.size() > 0 ? tagStack.getFirst() : ""); }
		public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
			handleContent();

			String tagName = qName.toLowerCase();
			try {
				if (tagName.equals("header")) {
					if (!getCurrentTag().equals("abilities"))
						System.out.println("Got <header> when parent is not <abilities>");
				}
				else if (tagName.equals("profession")) {
					if (getCurrentTag().equals("abilities"))
						currentProf = getProfessionType(atts.getValue("name"));
					else
						System.out.println("Got <profession> when parent is not <abilities>");
				}
				else if (tagName.equals("stamina")) {
					int amount = Integer.parseInt(atts.getValue("amount"));
					for (int a = 0; a < advs.length; a++) {
						advs[a].stamina.natural = amount;
						advs[a].stamina.current = amount;
					}
				}
				else if (tagName.equals("rank")) {
					int amount = Integer.parseInt(atts.getValue("amount"));
					for (int a = 0; a < advs.length; a++)
						advs[a].rank.natural = amount;
				}
				else if (tagName.equals("gold")) {
					int amount = Integer.parseInt(atts.getValue("amount"));
					for (int a = 0; a < advs.length; a++)
						advs[a].shards = amount;
				}
				else if (tagName.equals("items")) {
					System.out.println("Items element: throw to item parsing code");
				}
				else if (tagName.equals("starting")) {
				}
				else if (tagName.equals("adventurer")) {
					if (!getCurrentTag().equals("starting"))
						System.out.println("Hey, this tag should be inside the <starting> element!  grumble...");
					currentProf = getProfessionType(atts.getValue("profession"));
					if (currentProf >= 0) {
						advs[currentProf].fullName = atts.getValue("name");
						advs[currentProf].male = !atts.getValue("gender").toUpperCase().startsWith("F");
						currentDocument = new DefaultStyledDocument();
					}
					currentStyle = 0;
				}
				else if (tagName.equals("i"))
					currentStyle |= Font.ITALIC;
				else if (tagName.equals("b"))
					currentStyle |= Font.BOLD;
				else if (getCurrentTag().equals("items")) {
					Item currentItem = Item.createItem(tagName);
					if (currentItem != null) {
						currentItem.init(atts);
						if (currentItem.isProfessionSpecific())
							advs[currentItem.getProfession()].getItems().addItem(currentItem);
						else {
							// Owned by all professions
							for (int a = 0; a < advs.length; a++)
								advs[a].getItems().addItem(currentItem);
						}
						currentItem = null;
					}
				}
			}
			catch (NumberFormatException nfe) {
			}

			tagStack.addFirst(tagName);
		}

		public void endElement(String uri, String localName, String qName) throws SAXException {
			handleContent();
			tagStack.removeFirst();

			String tagName = qName.toLowerCase();
			if (tagName.equals("abilities")) {
				abilityIndices = null;
			}
			else if (tagName.equals("profession")) {
				currentProf = -1;
			}
			else if (tagName.equals("items")) {
				System.out.println("Items element: return from item parsing code");
			}
			else if (tagName.equals("adventurer")) {
				if (currentProf >= 0)
					advs[currentProf].history = currentDocument;
				currentProf = -1;
				currentDocument = null;
				currentStyle = 0;
			}
			else if (tagName.equals("i"))
				currentStyle &= ~Font.ITALIC;
			else if (tagName.equals("b"))
				currentStyle &= ~Font.BOLD;
		}
		private String content = "";
		public void characters(char[] ch, int start, int length) throws SAXException {
			if (length > 0)
				// Not sure why this would get called otherwise, but just in case
				content += new String(ch, start, length);
		}
		private void handleContent() {
			if (content.trim().length() == 0)
				return;

			if (getCurrentTag().equals("header")) {
				abilityIndices = new int[ABILITY_COUNT];
				String[] names = content.trim().split(" ");
				for (int a = 0; a < names.length; a++) {
					abilityIndices[a] = getAbilityType(names[a]);
					if (abilityIndices[a] < 0)
						System.out.println("Unrecognised ability name: " + names[a]);
				}
			}
			else if (getCurrentTag().equals("profession")) {
				if (abilityIndices == null)
					System.out.println("There's no ability header here!");
				else if (currentProf < 0)
					System.out.println("No profession set!");
				else {
					try {
						String[] values = content.trim().split(" ");
						for (int a = 0; a < values.length; a++)
							advs[currentProf].abilities[abilityIndices[a]].natural = Integer.parseInt(values[a]);
					}
					catch (NumberFormatException nfe) {
						System.out.println("Expected ability score, got: " + nfe);
					}
				}
			}
			else if (currentDocument != null) {
				if (currentDocument.getLength() == 0)
					// First bit of content - trim off leading whitespace
					while (Character.isWhitespace(content.charAt(0)))
						content = content.substring(1);

				// Add some styled content to the document
				SimpleAttributeSet atts = new SimpleAttributeSet();
				if ((currentStyle & Font.ITALIC) == Font.ITALIC)
					StyleConstants.setItalic(atts, true);
				if ((currentStyle & Font.BOLD) == Font.BOLD)
					StyleConstants.setBold(atts, true);
				try {
					currentDocument.insertString(currentDocument.getLength(), content, atts);
				}
				catch (BadLocationException ble) {
					System.err.println("Failed to insert string into styled document: " + ble);
				}
			}
			content = "";
		}

		public void setDocumentLocator(org.xml.sax.Locator l) {}
		public void startDocument() throws SAXException {}
		public void endDocument() throws SAXException {
			// Make sure affected stats are all refreshed to reflect items
			for (int a = 0; a < advs.length; a++)
				advs[a].calcAffectedStats();
		}
		public void startPrefixMapping(String prefix, String uri) throws SAXException {}
		public void endPrefixMapping(String prefix) throws SAXException {}
		public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {}
		public void processingInstruction(String target, String data) throws SAXException {}
		public void skippedEntity(String name) throws SAXException {}
	}


	public static abstract class Stat {
		public int natural, affected;
		protected int flags;
		public static final int FIXED = 1 << 0;
		public static final int CURSED = 1 << 1;
		
		public Stat() { this(1, 1); }
		public Stat(int natural, int affected) {
			this.natural = natural;
			this.affected = affected;
			flags = 0;
		}

		public Document getDocument() { return null; }
		protected abstract void updateDocument();
		
		public boolean isFixed() { return (flags & FIXED) != 0; }
		public boolean isCursed() { return (flags & CURSED) != 0; }
		public void setFixed(boolean b) {
			if (b)
				flags |= FIXED;
			else
				flags &= ~FIXED;
			updateDocument();
		}
		public void setCursed(boolean b) {
			if (b)
				flags |= CURSED;
			else
				flags &= ~CURSED;
			updateDocument();
		}
	}

	public static class SingleStat extends Stat {
		private class InnerDocument extends PlainDocument {
			public InnerDocument() {
				super();
				try {
					insertString(0, getDocString(), null);
				}
				catch (BadLocationException ble) {
					System.err.println(ble.toString());
				}
			}
			public void updatedVal() {
				try {
					replace(0, getLength(), getDocString(), null);
				}
				catch (BadLocationException ble) {
					System.err.println(ble.toString());
				}
			}
		}

		private InnerDocument doc = null;
		public SingleStat() { super(); }
		public SingleStat(int natural, int affected) { super(natural, affected); }

		protected String getDocString() { return Integer.toString(affected); }

		public Document getDocument() {
			if (doc == null)
				doc = new InnerDocument();
			return doc;
		}

		protected void updateDocument() {
			if (doc != null)
				doc.updatedVal();
		}
	}

	/*
	 * Note: non-static inner class.
	 * It needs a pointer to gender as well as to the rank.
	 */
	private class RankStat extends SingleStat {
		protected String getDocString() { return affected + " (" + getRankDescription() + ")"; }
	}

	public class StaminaStat extends SingleStat {
		public int current;

		public StaminaStat() {
			super();
			this.current = natural;
		}
		public StaminaStat(int natural, int affected, int current) {
			super(natural, affected);
			this.current = current;
		}

		/**
		 * Perform a heal.
		 * @param amount the amount to heal, up to the maximum; if less than <code>0</code>,
		 * restore to full.
		 */
		public void heal(int amount) {
			if (amount < 0 || current + amount > affected)
				// Full heal
				current = affected;
			else
				current += amount;
			updateDocument();
		}

		/**
		 * Take damage.
		 * @return <code>true</code> if the adventurer is now dead.
		 */
		public boolean damage(int amount) {
			current -= amount;
			if (current < 0)
				current = 0;
			updateDocument();
			if (current == 0) {
				getExtraChoices().checkMenu();
				return true;
			}
			else
				return false;
		}

		protected String getDocString() { return current + "/" + affected; }
	}

	public static class AbilityStat extends Stat {
		public final int abilityType;

		private StructuredDocument doc;
		private Position startPosition;
		private int valLength;
		private int startOffset, abilityNameOffset;

		public AbilityStat(int abilityType) {
			super();
			this.abilityType = abilityType;
		}

		public AbilityStat(int abilityType, int natural, int affected) {
			super(natural, affected);
			this.abilityType = abilityType;
		}

		public static StyledDocument createAbilityDocument(AbilityStat[] abilities) {
			StructuredDocument doc = new StructuredDocument();
			doc.grabWriteLock();
			Element root = doc.getDefaultRootElement();

			SimpleAttributeSet tableAtts = new SimpleAttributeSet();
			Node.setViewType(tableAtts, Node.TableViewType);
			Font rootFont = SectionDocument.getPreferredFont();
			StyleConstants.setFontFamily(tableAtts, rootFont.getFamily());
			StyleConstants.setFontSize(tableAtts, rootFont.getSize());
			TableView.setFillWidth(tableAtts, true);
			BranchElement table = doc.createBranch(root, tableAtts);

			for (int a = 0; a < abilities.length; a++)
				abilities[a].addContent(doc);
			Element[] rows = new Element[abilities.length];
			for (int a = 0; a < abilities.length; a++)
				rows[a] = abilities[a].addStructure(doc, table);

			table.replace(0, table.getElementCount(), rows);

			if (root instanceof BranchElement)
				((BranchElement)root).replace(0, root.getElementCount(), new Element[] { table });

			doc.releaseWriteLock();
			//doc.dump(System.out);
			return doc;
		}

		public static AttributeSet createColumnAtts(int align) {
			SimpleAttributeSet atts = new SimpleAttributeSet();
			Node.setViewType(atts, Node.ParagraphViewType);
			StyleConstants.setAlignment(atts, align);
			StyleConstants.setLeftIndent(atts, 5.0f);
			StyleConstants.setRightIndent(atts, 5.0f);
			return atts;
		}

		/** First pass: add only the textual content. */
		private void addContent(StructuredDocument doc) {
			this.doc = doc;
			abilityNameOffset = doc.getLength();
			doc.addContent(getAbilityName(abilityType).toUpperCase() + "\n");
			startOffset = doc.getLength();
			doc.addContent(getShownValue() + "\n");
			valLength = doc.getLength() - startOffset;
		}

		/** Second pass: structure into rows and cells. */
		private Element addStructure(StructuredDocument doc, Element tableElement) {
			BranchElement row = doc.createBranch(tableElement, null);

			BranchElement leftColumn = doc.createBranch(row, createColumnAtts(StyleConstants.ALIGN_LEFT));
			Element firstCharLeaf = doc.createLeafElement(leftColumn, null, abilityNameOffset, abilityNameOffset + 1);
			SimpleAttributeSet atts = SectionDocument.getSmallerAtts(null);
			Element otherCharsLeaf = doc.createLeafElement(leftColumn, atts, abilityNameOffset + 1, startOffset);
			leftColumn.replace(0, 0, new Element[] { firstCharLeaf, otherCharsLeaf });

			BranchElement rightColumn = doc.createBranch(row, createColumnAtts(StyleConstants.ALIGN_RIGHT));
			Element valLeaf = doc.createLeafElement(rightColumn, null, startOffset, startOffset + valLength);
			rightColumn.replace(0, 0, new Element[] { valLeaf });

			row.replace(0, 0, new Element[] { leftColumn, rightColumn });

			try {
				startPosition = doc.createPosition(startOffset);
			}
			catch (BadLocationException ble) {
				System.err.println(ble.toString());
			}
			valLength--; // don't replace the newline

			return row;
		}
		
		private boolean containsPosition(int pos) {
			if (doc != null) {
				int startOffset = startPosition.getOffset();
				if (pos > startOffset + valLength) return false;
				if (pos < startOffset - (getAbilityName(abilityType).length()+1)) return false;
				return true;
			}
			return false;
		}

		private String getShownValue() {
			String valText = Integer.toString(affected);
			if (isCursed() || isFixed())
				valText = "(" + valText + ")";
			return valText;
		}
		
		protected void updateDocument() {
			if (doc != null) {
				try {
					String valText = getShownValue();
					int startOffset = startPosition.getOffset();
					doc.replace(startOffset, valLength, valText, null);
					startPosition = doc.createPosition(startOffset); // because the position has moved forward by one
					valLength = valText.length();
				}
				catch (BadLocationException ble) {
					System.err.println("Error replacing ability value: " + ble);
				}
			}
		}
	}

	/**
	 * This document copies some of SectionDocument.
	 * Essentially we want a Document that we can structure as we like, while also being
	 * able to edit it like a normal StyledDocument.
	 */
	public static class StructuredDocument extends DefaultStyledDocument {
		void grabWriteLock() { writeLock(); }
		void releaseWriteLock() { writeUnlock(); }

		public void addContent(String text) {
			try {
				getContent().insertString(getLength(), text);
			}
			catch (BadLocationException ble) {}
		}

		public BranchElement createBranch(Element parent, AttributeSet a) {
			return (BranchElement)createBranchElement(parent, a);
		}

		public Element createLeafElement(Element parent, AttributeSet a, int p0, int p1) {
			return super.createLeafElement(parent, a, p0, p1);
		}

		public Element[] addLeavesTo(Element parentElement, StyledText[] contents) {
			if (!parentElement.isLeaf()) {
				BranchElement branch = (BranchElement)parentElement;
				Element[] children = new Element[contents.length];
				for (int i = 0; i < children.length; i++) {
					int startOffset = getLength();
					addContent(contents[i].text);
					children[i] = createLeafElement(parentElement, contents[i].atts, startOffset, getLength());
					System.out.println("Added content element, start=" + startOffset + ",end=" + getLength() + " to handle text=\"" + contents[i] + "\"");
				}
				branch.replace(branch.getElementCount(), 0, children);
				return children;
			}
			else {
				System.out.println("Can't add children to leaf: " + parentElement);
				return null;
			}
		}
	}
	
	public void fontChanged(Font f, int smallerFontSize) {
		// TODO Auto-generated method stub
		
	}

	public static void main(String args[]) {
		Adventurer[] advs = loadStarting(Books.getCanon().getBook(args.length > 0 ? args[0] : "5"));
		for (int a = 0; a < advs.length; a++) {
			System.out.println("Adventurer " + (a+1) + ":");
			System.out.println(advs[a].toDebugString());
			if (advs[a].history != null) {
				System.out.print("History:");
				//if (advs[a].history instanceof AbstractDocument)
				//	((AbstractDocument)advs[a].history).dump(System.out);
				//else
				System.out.println(advs[a].history.toString());
			}
			advs[a].codewords.addCodeword("Codeword" + a);
		}

		javax.swing.JFrame jf = new javax.swing.JFrame("Starting Adventurers");
		final javax.swing.JTextPane textPane = new javax.swing.JTextPane(advs[0].history);
		final javax.swing.JComboBox jcb = new javax.swing.JComboBox();
		for (int a = 0; a < advs.length; a++)
			jcb.addItem(advs[a]);
		jcb.setSelectedItem(advs[0]);
		jcb.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				Adventurer a = (Adventurer)jcb.getSelectedItem();
				if (a.history != null)
					textPane.setDocument(a.history);
			}
		});

		jf.getContentPane().add(new javax.swing.JScrollPane(textPane));
		jf.getContentPane().add(jcb, java.awt.BorderLayout.SOUTH);
		jf.setSize(200, 200);
		jf.setVisible(true);
		jf.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);

		int randomAdv = new java.util.Random().nextInt(advs.length);
		AdventurerFrame af = new AdventurerFrame();
		advs[randomAdv].addTitle("Masked Lady");
		af.init(advs[randomAdv]);
		af.pack();
		af.setVisible(true);
	}
}
