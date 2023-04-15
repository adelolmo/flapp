package flands;


import java.util.Properties;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.xml.sax.Attributes;

/**
 * A conditional node; if its conditions are met, execution will continue to
 * its child nodes.
 * @author Jonathan Mann
 */
public class IfNode extends Node implements Executable, ChangeListener {
	public static final int IF_TYPE = 0;
	public static final int ELSEIF_TYPE = 1;
	public static final int ELSE_TYPE = 2;
	public static final String IfElementName = "if";
	public static final String ElseIfElementName = "elseif";
	public static final String ElseElementName = "else";
	public static final String ElementName = IfElementName;
	private int type;

	private String ifElseVarName;
	private boolean not;
	private String[] codewords = null;
	private boolean andCodewords;
	private String god;
	private String safeAddGod;
	private String[] titles = null;
	private boolean andTitles;
	private int ticks;
	private String shards;
	private String cache;
	private String book;
	private String wordName;
	private String var;
	private String greaterThan;
	private String lessThan;
	private String equals;
	private Item item = null;
	private Blessing blessing = null;
	private int ability = -1;
	private int abilityModifier;
	private String gender = null;
	private int profession = -1;
	private boolean resurrection;
	private Boolean dead = null;
	private int ship = -1;
	private int crew = -1;
	private int cargo = 0;
	private String docked;
	private ExecutableRunner runner = null;
	private Curse curse = null;

	public IfNode(Node parent) {
		this(IfElementName, parent);
	}

	public IfNode(String name, Node parent) {
		super(name, parent);
		setEnabled(false);
		type = IF_TYPE;
		if (name.equals(ElseIfElementName))
			type = ELSEIF_TYPE;
		else if (name.equals(ElseElementName))
			type = ELSE_TYPE;
	}

	public ExecutableGrouper getExecutableGrouper() {
		if (runner == null)
			runner = new ExecutableRunner("if", this);
		return runner;
	}

	private static final String CodewordAttribute = "codeword";
	private static final String GodAttribute = "god";
	private static final String TitleAttribute = "title";
	private static final String TicksAttribute = "ticks";
	private static final String ShardsAttribute = "shards";
	public void init(Attributes xmlAtts) {
		ifElseVarName = getRoot().getIfElseVarName(type == IF_TYPE);
		not = getBooleanValue(xmlAtts, "not", false);
		codewords = split(xmlAtts.getValue(CodewordAttribute));
		andCodewords = andSplitter;
		god = xmlAtts.getValue(GodAttribute);
		safeAddGod = xmlAtts.getValue("safeAddGod");
		titles = split(xmlAtts.getValue(TitleAttribute));
		andTitles = andSplitter;
		ticks = getIntValue(xmlAtts, TicksAttribute, -1);
		shards = xmlAtts.getValue(ShardsAttribute);
		cache = xmlAtts.getValue("cache");
		book = xmlAtts.getValue("book");
		wordName = xmlAtts.getValue("name");
		var = xmlAtts.getValue("var");
		greaterThan = xmlAtts.getValue("greaterthan");
		lessThan = xmlAtts.getValue("lessthan");
		equals = xmlAtts.getValue("equals");
		String val = xmlAtts.getValue("ability");
		if (val != null) {
			ability = Adventurer.getAbilityType(val);
			abilityModifier = Adventurer.getAbilityModifier(xmlAtts.getValue("modifier"));
		}
		gender = xmlAtts.getValue("gender");
		item = Item.createItem(xmlAtts);
		blessing = Blessing.getBlessing(xmlAtts);
		curse = Curse.createCurse(xmlAtts);
		resurrection = getBooleanValue(xmlAtts, "resurrection", false);
		if (xmlAtts.getValue("dead") != null)
			dead = Boolean.valueOf(getBooleanValue(xmlAtts, "dead", false));
		val = xmlAtts.getValue("ship");
		if (val != null)
			ship = Ship.getType(val);
		val = xmlAtts.getValue("crew");
		if (val != null)
			crew = Ship.getCrew(val);
		val = xmlAtts.getValue("cargo");
		if (val != null)
			cargo = Ship.getCargo(val);
		docked = xmlAtts.getValue("docked");
		val = xmlAtts.getValue("profession");
		if (val != null)
			profession = Adventurer.getProfessionType(val);

		super.init(xmlAtts);
	}

	protected void outit(Properties props) {
		super.outit(props);
		if (not) saveProperty(props, "not", true);
		if (codewords != null) props.setProperty("codeword", concatenate(codewords, andCodewords));
		if (god != null) props.setProperty(GodAttribute, god);
		if (safeAddGod != null) props.setProperty("safeAddGod", safeAddGod);
		if (titles != null) props.setProperty(TitleAttribute, concatenate(titles, andTitles));
		if (ticks != -1) saveProperty(props, TicksAttribute, ticks);
		if (shards != null) saveVarProperty(props, ShardsAttribute, shards);
		if (cache != null) props.setProperty("cache", cache);
		if (book != null) props.setProperty("book", book);
		if (wordName != null) props.setProperty("name", wordName);
		if (var != null) props.setProperty("var", var);
		if (greaterThan != null) saveVarProperty(props, "greaterthan", greaterThan);
		if (lessThan != null) saveVarProperty(props, "lessthan", lessThan);
		if (equals != null) saveVarProperty(props, "equals", equals);
		if (ability >= 0) {
			props.setProperty("ability", Adventurer.getAbilityName(ability));
			if (abilityModifier >= 0)
				props.setProperty("modifier", Adventurer.getAbilityModifierName(abilityModifier));
		}
		if (gender != null) props.setProperty("gender", gender);
		if (item != null) item.saveProperties(props);
		if (blessing != null) blessing.saveTo(props);
		if (curse != null) curse.storeAttributes(props, XMLOutput.OUTPUT_PROPS_STATIC);
		if (resurrection) saveProperty(props, "resurrection", true);
		if (dead != null) saveProperty(props, "dead", dead.booleanValue());
		if (ship >= 0) props.setProperty("ship", Ship.getTypeName(ship));
		if (crew >= 0) props.setProperty("crew", Ship.getCrewName(crew));
		if (cargo >= 0) props.setProperty("cargo", Ship.getCargoName(cargo));
		if (docked != null) props.setProperty("docked", docked);
		if (profession >= 0) props.setProperty("profession", Adventurer.getProfessionName(profession));
	}
	
	public void handleContent(String text) {
		if (codewords != null) {
			// Look for one or more codewords in this content
			SimpleAttributeSet codewordAtts = new SimpleAttributeSet();
			StyleConstants.setItalic(codewordAtts, true);

			int[] indices = new int[codewords.length];
			for (int i = 0; i < indices.length; i++)
				indices[i] = text.indexOf(codewords[i]);

			int textOffset = 0;
			for (int i = 0; i < indices.length; i++) {
				int firstIndex = Integer.MAX_VALUE;
				int firstCodeword = -1;
				for (int c = 0; c < indices.length; c++) {
					if (indices[c] >= 0 && indices[c] < firstIndex) {
						firstCodeword = c;
						firstIndex = indices[c];
					}
				}
				if (firstCodeword >= 0) {
					// Got the first codeword to appear in the content
					int endOffset = firstIndex + codewords[firstCodeword].length();
					addEnableElements(getDocument().addStyledText(getElement(), text.substring(textOffset, endOffset), codewords[firstCodeword], null, codewordAtts));
					textOffset = endOffset;
					indices[firstCodeword] = -1; // we won't look for it again
				}
				else
					// No more codewords in the content
					break;
			}
			text = text.substring(textOffset);
			// fall-through so that remaining text gets added below
		}

		addEnableElements(getDocument().addLeavesTo(getElement(), new StyledText[] { new StyledText(text, StyleNode.createActiveAttributes()) }));
	}

	public boolean handleEndTag() {
		System.out.println("IfNode adding itself as Executable child");
		findExecutableGrouper().addExecutable(this);
		if (type == IF_TYPE || type == ELSEIF_TYPE)
			// Pass on our var name so any following elseifs or elses can use it.
			getRoot().setElseVarName(ifElseVarName);
		return super.handleEndTag();
	}

	public boolean execute(ExecutableGrouper grouper) {
		boolean executeBlock = true;
		if (type == IF_TYPE)
			// Reset - there may have been an earlier if/else in this section
			removeVariable(ifElseVarName);
		
		if (type == ELSE_TYPE || type == ELSEIF_TYPE)
			// Only execute if the last IF or ELSEIF didn't
			executeBlock = (getVariableValue(ifElseVarName) != 1);

		if (executeBlock && (type == IF_TYPE || type == ELSEIF_TYPE)) {
			// Still need to meet conditions
			executeBlock = meetsConditions();
			if (not) executeBlock = !executeBlock;
			if (executeBlock)
				setVariableValue(ifElseVarName, 1);
		}

		if (!executeBlock)
			// Doesn't meet the entry conditions, so skip this 'block'
			// We're done.
			return true;

		// Enable this block
		setEnabled(true);
		if (runner == null)
			return true;

		// We have children that we can start executing
		if (runner.execute(grouper))
			// All children finished
			return true;
		else
			// Temporary halt
			return false;
	}

	public void resetExecute() {
		if (isEnabled()) {
			if (runner != null)
				runner.resetExecute();
			setEnabled(false);
		}
	}

	public boolean meetsConditions() {
		Adventurer adv = getAdventurer();

		if (codewords != null) {
			//System.out.println("Testing for codewords, and=" + andCodewords);
			Codewords words = adv.getCodewords();
			for (int c = 0; c < codewords.length; c++) {
				boolean hasWord = words.hasCodeword(codewords[c]);
				//System.out.println("Do you have codeword " + codewords[c] + (hasWord ? "? Yes!" : "? No."));
				if (andCodewords && !hasWord) {
					return false;
				}
				else if (!andCodewords && hasWord) {
					return true;
				}
			}
			if (andCodewords)
				return true;
		}

		if (god != null) {
			if (adv.hasGod(god))
				return true;
		}
		
		if (safeAddGod != null)
			return adv.safeToAddGod(safeAddGod);

		if (titles != null) {
			//System.out.println("Testing for titles, and=" + andTitles);
			for (int t = 0; t < titles.length; t++) {
				boolean hasTitle = adv.hasTitle(titles[t]);
				//System.out.println("Do you have title " + titles[t] + (hasTitle ? "? Yes!" : "? No."));
				if (andTitles && !hasTitle) {
					return false;
				}
				else if (!andTitles && hasTitle) {
					return true;
				}
			}
			if (andTitles)
				return true;
		}

		if (ticks >= 0) {
			if (getCodewords().getTickCount(getSectionName()) == ticks)
				return true;
		}

		if (shards != null) {
			int money = (cache == null ? adv.getMoney() : CacheNode.getMoneyCache(cache));
			if (money >= getAttributeValue(shards)) {
				System.out.println("Have enough money");
				return true;
			}
		}

		if (book != null) {
			if (Books.getCanon().getBook(book).hasBook()) {
				System.out.println("Have book '" + book + "'");
				return true;
			}
		}

		if (wordName != null) {
			int val = adv.getCodewords().getValue(wordName);
			if (checkComparisons(val))
				return true;
		}

		if (var != null) {
			int val = getVariableValue(var);
			if (checkComparisons(val))
				return true;
		}

		if (ability >= 0) {
			int val = adv.getAbilityValue(ability, abilityModifier, Adventurer.PURPOSE_TESTING);
			if (checkComparisons(val))
				return true;
		}

		if (gender != null) {
			if (gender.toLowerCase().startsWith("m") && adv.isMale())
				return true;
			else if (gender.toLowerCase().startsWith("f") && !adv.isMale())
				return true;
			else
				System.out.println("IfNode: can't understand gender attribute: " + gender);
		}

		if (item != null) {
			ItemList items = (cache == null ? getItems() : CacheNode.getItemCache(cache));
			int[] matches = items.findMatches(item);
			System.out.println("Found " + matches.length + " matches for if");
			if (greaterThan == null && lessThan == null && equals == null) {
				if (matches.length > 0)
					return true;
			}
			else {
				if (checkComparisons(matches.length))
					return true;
			}
		}

		if (blessing != null) {
			if (getBlessings().hasBlessing(blessing))
				return true;
		}
		
		if (curse != null) {
			if (getCurses().findMatches(curse).length > 0)
				return true;
		}

		if (resurrection && adv.hasResurrection())
			return true;

		if (docked != null) {
			for (int i = 0; i < getShips().getShipCount(); i++) {
				Ship s = getShips().getShip(i);
				if (docked.equals(s.getDocked())) {
					// If there are other constraints, check for matches
					if (ship >= 0 && s.getType() != ship)
						continue;
					if (crew >= 0 && s.getCrew() != crew)
						continue;
					if (cargo != 0 && !s.hasCargo(cargo))
						continue;
					return true;
				}
			}
		}
		else if (ship >= 0 || crew >= 0 || cargo != 0) {
			int shipIndex = getShips().getSingleShip();
			if (shipIndex < 0)
				System.out.println("IfNode: couldn't find single ship to check conditions!");
			else {
				Ship s = getShips().getShip(shipIndex);
				if (ship >= 0 && s.getType() == ship)
					return true;
				if (crew >= 0 && s.getCrew() == crew)
					return true;
				if (cargo != 0 && s.hasCargo(cargo))
					return true;
			}
		}

		if (profession >= 0) {
			if (getAdventurer().getProfession() == profession)
				return true;
		}
		
		if (dead != null) {
			if (getAdventurer().isDead() == dead.booleanValue())
				return true;
		}
		
		// At this point we've failed to meet any conditions
		// But if conditions depend on shards or items, we want to stay aware of changes
		if (!not && ignoreChangeEvents) {
			System.out.println("Adding IfNode change listeners");
			ignoreChangeEvents = false;
			if (item != null)
				getItems().addChangeListener(this);
			if (shards != null && cache == null)
				adv.addMoneyListener(this);
		}
		return false;
	}

	private boolean ignoreChangeEvents = true;
	public void stateChanged(ChangeEvent evt) {
		if (ignoreChangeEvents) return;

		// See if conditions are met now
		if (meetsConditions() != not) {
			// Remove listeners so we don't get notified again
			// Hey - can't do that now, or we get a ConcurrentModificationException
			// This is because the object firing the event is iterating through the listeners right now,
			// and we can't remove ourself from that list at the same time
			System.out.println("Conditions belatedly met: " + (not ? "not is true" : "not is false"));
			ignoreChangeEvents = true;
			setEnabled(true);
			if (runner != null)
				runner.startExecution(true);
		}
	}

	private boolean checkComparisons(int val) {
		boolean result = false;
		if (equals != null && val == getAttributeValue(equals))
			return true;
		if (greaterThan != null && val > getAttributeValue(greaterThan))
			return true;
		if (lessThan != null && val < getAttributeValue(lessThan))
			return true;
		return result;
	}

	public boolean isEnabled() {
		return super.isEnabled();
		//if (!super.isEnabled()) return false;
		//return meetsConditions();
	}

	public void dispose() {
		// Might be present as listener at these two points, or might not
		System.out.println("Disposing of IfNode listeners");
		if (item != null)
			getItems().removeChangeListener(this);
		if (shards != null && cache == null)
			getAdventurer().removeMoneyListener(this);
	}

	protected Element createElement() {
		Node parent = getParent();
		while (parent != null & !(parent instanceof ParagraphNode))
			parent = parent.getParent();

		if (parent instanceof ParagraphNode)
			return null;
		else
			// We need a paragraph as our parent element
			return super.createElement();
	}
	
	public void saveProperties(Properties props) {
		super.saveProperties(props);
		if (runner != null && runner.willCallContinue())
			saveProperty(props, "continue", true);
	}
	
	public void loadProperties(Attributes atts) {
		super.loadProperties(atts);
		if (getBooleanValue(atts, "continue", false))
			((ExecutableRunner)getExecutableGrouper()).setCallback(findExecutableGrouper());
	}
}
