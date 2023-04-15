package flands;


import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Iterator;
import java.util.Properties;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.xml.sax.Attributes;

/**
 * Action node that generally 'adds' something to the character (most commonly, a
 * codeword or one of the section's checkboxes). The functionality
 * varies depending on the type of thing being affected; the complementary function
 * is usually in LoseNode.
 * 
 * @author Jonathan Mann
 */
public class TickNode extends ActionNode implements Executable, ItemListener, Flag.Listener, Roller.Listener, UndoManager.Creator {
	public static final String GainElementName = "gain";
	public static final String TickElementName = "tick";

	private String codeword = null;
	private int ticks;
	private String god = null;
	private String compatibleGods = null;
	private String title = null, titlePattern;
	private int titleInitialValue, titleAdjustment;
	private String name;
	private String shards;
	private int ability = -1;
	private String amount;
	private String abilityEffect;
	private boolean forced;
	private Blessing blessing = null;
	private String special;
	private String bonus;
	private String cache;
	private String price, flag;
	private String crew;
	private int cargo = Ship.NO_CARGO;
	private String addtag, removetag;
	private String addbonus;
	private Item item = null;
	private String profession;

	public TickNode(String name, Node parent) {
		super(name, parent);
		setEnabled(false);
	}

	public void init(Attributes atts) {
		codeword = atts.getValue("codeword");
		god = atts.getValue("god");
		if (god != null)
			compatibleGods = atts.getValue("compatible");
		title = atts.getValue("title");
		if (title != null) {
			titlePattern = atts.getValue("titlePattern");
			titleInitialValue = getIntValue(atts, "titleValue", 1);
			titleAdjustment = getIntValue(atts, "titleAdjust", 1);
		}
		ticks = getIntValue(atts, "count", (atts.getLength() == 0 ? 1 : 0));
		name = atts.getValue("name");
		shards = atts.getValue("shards");
		String val = atts.getValue("ability");
		if (val != null) {
			ability = Adventurer.getAbilityType(val);
			abilityEffect = atts.getValue("effect");
		}
		amount = atts.getValue("amount");
		forced = getBooleanValue(atts, "force", true);
		blessing = Blessing.getBlessing(atts);
		if (blessing != null)
			blessing.setPermanent(getBooleanValue(atts, "permanent", false));
		special = atts.getValue("special");
		bonus = atts.getValue("bonus");
		cache = atts.getValue("cache");
		crew = atts.getValue("crew");
		if (crew != null) {
			int crewType = Ship.getCrew(crew);
			if (crewType >= 0)
				crew = Integer.toString(crewType);
		}
		val = atts.getValue("cargo");
		if (val != null)
			cargo = Ship.getCargo(val);
		addtag = atts.getValue("addtag");
		removetag = atts.getValue("removetag");
		addbonus = atts.getValue("addbonus");
		if (addtag != null || removetag != null || addbonus != null) {
			item = Item.createItem(atts);
			if (item == null)
				System.err.println("TickNode: has item modification attributes, but no item!");
		}
		price = atts.getValue("price");
		flag = atts.getValue("flag");
		if (flag != null) {
			getFlags().addListener(flag, this);
		}
		if (price != null) {
			getFlags().addListener(price, this);
		}
		profession = atts.getValue("profession");

		super.init(atts);
	}
	
	protected void outit(Properties props) {
		super.outit(props);
		if (codeword != null) props.setProperty("codeword", codeword);
		if (god != null) {
			props.setProperty("god", god);
			if (compatibleGods != null) props.setProperty("compatible", compatibleGods);
		}
		if (title != null) {
			props.setProperty("title", title);
			if (titlePattern != null) props.setProperty("titlePattern", titlePattern);
			if (titleInitialValue != 1) saveProperty(props, "titleValue", titleInitialValue);
			if (titleAdjustment != 1) saveProperty(props, "titleAdjust", titleAdjustment);
		}
		saveProperty(props, "count", ticks);
		if (name != null) props.setProperty("name", name);
		if (shards != null) saveVarProperty(props, "shards", shards);
		if (ability >= 0) {
			props.setProperty("ability", Adventurer.getAbilityName(ability));
			if (abilityEffect != null) props.setProperty("effect", abilityEffect);
		}
		if (amount != null) saveVarProperty(props, "amount", amount);
		if (!forced) saveProperty(props, "force", false);
		if (blessing != null) blessing.saveTo(props);
		if (special != null) props.setProperty("special", special);
		if (bonus != null) saveVarProperty(props, "bonus", bonus);
		if (cache != null) props.setProperty("cache", cache);
		if (crew != null) saveVarProperty(props, "crew", crew);
		if (cargo >= 0) saveProperty(props, "cargo", cargo);
		if (addtag != null) props.setProperty("addtag", addtag);
		if (removetag != null) props.setProperty("removetag", removetag);
		if (addbonus != null) saveVarProperty(props, "addbonus", addbonus);
		if (item != null) item.saveProperties(props);
		if (price != null) props.setProperty("price", price);
		if (flag != null) props.setProperty("flag", flag);
		if (profession != null) props.setProperty("profession", profession);
	}

	protected Node createChild(String name) {
		Node n = null;
		if (name.equals(EffectNode.ElementName))
			n = new EffectNode(this); // no source

		if (n == null)
			n = super.createChild(name);
		else
			addChild(n);

		return n;
	}

	private AttributeSet getElementAttributes(boolean italic) {
		SimpleAttributeSet atts = new SimpleAttributeSet();
		StyleConstants.setUnderline(atts, true);
		if (italic)
			StyleConstants.setItalic(atts, true);
		StyleNode.applyActiveStyles(atts);
		addListenerTo(atts);
		return atts;
	}

	private boolean hadContent = false;
	public void handleContent(String text) {
		if (text.trim().length() == 0) return;

		hadContent = true;
		Element[] leaves;
		if (codeword != null) {
			// Assume it's something of the form '... blah codeword ...'
			leaves = getDocument().addStyledText(getElement(), text, codeword, getElementAttributes(false), getElementAttributes(true));
		}
		else
			leaves = getDocument().addLeavesTo(getElement(), new String[] { text }, new AttributeSet[] { getElementAttributes(false) });

		addHighlightElements(leaves);
		addEnableElements(leaves);
	}

	public boolean handleEndTag() {
		System.out.println("Adding TickNode() as child Executable");
		if (!hadContent && !hidden && !getParent().hideChildContent()) {
			String text = null;
			boolean newSentence = getDocument().isNewSentence(getDocument().getLength());
			if (codeword != null)
				text = (newSentence ? "Tick" : "tick") + " the codeword " + codeword;
			else if (shards != null && getAttributeValue(shards) > 0)
				text = shards + " Shards";
			else if (title != null)
				text = title;
			else if (blessing != null)
				text = blessing.getContentString();
			else if (ticks == 1) // default - leave to last case
				text = "put a tick there now";
			else {
				System.err.println("TickNode: no default content specified!");
				text = "NO DEFAULT CONTENT";
			}
			handleContent(text);
		}
		findExecutableGrouper().addExecutable(this);
		return super.handleEndTag();
	}

	private boolean callContinue = false;
	public boolean execute(ExecutableGrouper grouper) {
		if (flag != null && !getFlags().getState(flag))
			return true; // without enabling
		if (price != null && getFlags().getState(price))
			return true; // ditto

		setEnabled(true);
		if (canBeSkipped())
			return true;

		if (codeword == null && (god == null || getAdventurer().isGodless()) &&
			(shards == null || getAttributeValue(shards) <= 0) &&
			ability < 0 && title == null && ticks > 0 && 
			name == null && blessing == null && special == null &&
			crew == null && cargo == Ship.NO_CARGO && profession == null) {
			// The action will add ticks - so enable the right number of checkboxes
			getRoot().enableTicks(ticks, this);
		}

		if (hidden) {
			// Perform now and return immediately
			actionPerformed(null);
			return true;
		}

		callContinue = true;
		return false;
	}
	
	private boolean canBeSkipped() {
		if (hidden) return false;
		if (!forced || flag != null || price != null)
			return true;
		else if (codeword != null && getCodewords().hasCodeword(codeword))
			return true;
		else if (god != null && (getAdventurer().hasGod(god) || getAdventurer().isGodless()))
			return true;
		else if (shards != null)
			// Leave user free to take or ignore
			return true;
		else if (ability >= 0 && abilityEffect == null && getAdventurer().isAbilityMaxed(ability))
			return true;
		else if (title != null && getAdventurer().hasTitle(title) && (titlePattern == null || titleAdjustment == 0))
			return true;
		else if (blessing != null && getBlessings().hasBlessing(blessing) && !blessing.isPermanent())
			return true;
		else if (crew != null) {
			int s = getShips().getSingleShip();
			if (crew != null && s < 0 || getShips().getShip(s).getCrew() == getAttributeValue(crew))
				return true;
		}
		return false;
	}
	
	private boolean actionDoesAnything() {
		if (codeword != null && !getCodewords().hasCodeword(codeword))
			return true;
		else if (god != null && !(getAdventurer().hasGod(god) || getAdventurer().isGodless()))
			return true;
		else if (shards != null && getAttributeValue(shards) != 0)
			// Leave user free to take or ignore
			return true;
		else if (ability >= 0 && (!getAdventurer().isAbilityMaxed(ability) || abilityEffect != null))
			return true;
		else if (title != null && !(getAdventurer().hasTitle(title) && (titlePattern == null || titleAdjustment == 0)))
			return true;
		else if (blessing != null && (!getBlessings().hasBlessing(blessing) || blessing.isPermanent()))
			return true;
		else if (special != null)
			return true;
		else if (name != null)
			return true;
		else if (crew != null || cargo != Ship.NO_CARGO) {
			int s = getShips().getSingleShip();
			if (s >= 0) {
				Ship ship = getShips().getShip(s);
				if (crew != null && ship.getCrew() != getAttributeValue(crew))
					return true;
				if (cargo != Ship.NO_CARGO && !ship.isFull())
					return true;
			}
		}
		else if (profession != null)
			return true;
		return false;
	}

	public void flagChanged(String name, boolean state) {
		System.out.println("TickNode.flagChanged: " + name + " set to " + state);
		if (flag != null && flag.equals(name)) {
			setEnabled(state);
		}
		if (price != null && price.equals(name)) {
			if (state)
				setEnabled(false);
			else if (actionDoesAnything())
				setEnabled(true);
		}
	}
	
	public void resetExecute() {
		setEnabled(false);
		// TODO: Undo whatever actionPerformed did.
		// See 2.345 - TickNode is grouped with RandomNode, so use of a Luck blessing
		// will allow the player to get two bonuses!
	}
	
	protected Element createElement() {
		Element e = super.createElement();
		if (e != null)
			System.out.println("TickNode created Element: " + e);
		return e;
	}
	
	public void actionPerformed(ActionEvent evt) {
		setEnabled(false);
		System.out.println("Tick node activated");
		if (codeword != null)
			getCodewords().addCodeword(codeword);
		else if (god != null) {
			getAdventurer().setGod(god, compatibleGods);
			
			// Look for a god effect and apply it
			for (Iterator<Node> i = getChildren(); i.hasNext(); ) {
				Node n = i.next();
				if (n instanceof EffectNode) {
					Effect eff = ((EffectNode)n).getEffect();
					if (eff instanceof AbilityEffect) {
						getAdventurer().setGodEffect(god, (AbilityEffect)eff);
						break;
					}
				}
			}
		}
		else if (shards != null) {
			int val = getAttributeValue(shards);
			if (cache == null)
				getAdventurer().adjustMoney(val);
			else {
				int balance = CacheNode.getMoneyCache(cache) + val;
				CacheNode.setMoneyCache(cache, balance);
			}
		}
		else if (ability >= 0) {
			if (abilityChosen < 0)
				abilityChosen = ability;
			if (ability == Adventurer.ABILITY_SINGLE) {
				int[]
				    abilities = new int[Adventurer.ABILITY_COUNT],
				    values = new int[Adventurer.ABILITY_COUNT];
				for (int a = 0; a < abilities.length; a++) {
					abilities[a] = a;
					values[a] = getAdventurer().getAbilityValue(a, Adventurer.MODIFIER_NATURAL);
				}
				DocumentChooser dc = new DocumentChooser(FLApp.getSingle(), "Choose Ability", Adventurer.getAbilityDocuments(abilities, values), false);
				dc.setVisible(true);
				
				if (dc.getSelectedIndices() == null) {
					setEnabled(true);
					return;
				}
				abilityChosen = dc.getSelectedIndices()[0];
			}
			
			if (abilityEffect != null) {
				boolean add = true;
				String effect = abilityEffect;
				if (effect.startsWith("-"))
					add = false;
				while (effect.length() > 0 && !Character.isLetter(effect.charAt(0)))
					effect = effect.substring(1);
				Adventurer.Stat stat = getAdventurer().getAbility(abilityChosen);
				effect = effect.toLowerCase();
				if (effect.startsWith("curse"))
					stat.setCursed(add);
				else if (effect.startsWith("fix"))
					stat.setFixed(add);
				else
					System.out.println("Ability effect unrecognised: " + effect);
			}
			else {
				if (Character.isDigit(amount.charAt(0))) {
					DiceExpression exp = new DiceExpression(amount);
					if (exp.dice > 0) {
						Roller r = exp.createRoller();
						r.addListener(this);
						r.startRolling();
						return;
					}
				}
				
				getAdventurer().adjustAbility(abilityChosen, getAttributeValue(amount));
			}
		}
		else if (title != null) {
			if (titlePattern == null)
				getAdventurer().addTitle(title);
			else
				getAdventurer().addTitle(title, titlePattern, titleInitialValue, titleAdjustment);
		}
		else if (blessing != null) {
			getBlessings().addBlessing(blessing);
		}
		else if (special != null) {
			String type = special.toLowerCase();
			if (type.startsWith("defence")) {
				int defenceBonus = (bonus != null ? getAttributeValue(bonus) : 3);
				Blessing.addDefenceBlessing(defenceBonus);
			}
			else if (type.startsWith("attack") && bonus != null) {
				FightNode.setAttackBonus(getAttributeValue(bonus));
			}
			else if (type.equals("armourlock")) {
				System.out.println("Locking armour");
				getItems().lockArmour();
			}
			else if (type.equals("weaponlock")) {
				System.out.println("Locking weapon");
				getItems().lockWeapon();
			}
			else if (type.startsWith("difficulty")) {
				if (type.equals("difficultycurse"))
					getAdventurer().setDifficultyDice(1);
				else if (type.equals("difficultyrestore")) {
					if (getAdventurer().getDifficultyDice() < 2)
						getAdventurer().setDifficultyDice(2);
				}
			}
			else if (type.startsWith("godless")) {
				getAdventurer().setGodless(true);
			}
			else if (cache != null) {
				if (type.equals("freeze") || type.equals("lock"))
					CacheNode.setCacheFrozen(cache, true);
				else if (type.equals("thaw") || type.equals("unlock"))
					CacheNode.setCacheFrozen(cache, false);
			}
		}
		else if (crew != null || cargo != Ship.NO_CARGO) {
			int s = getShips().getSingleShip();
			if (crew != null)
				getShips().setCrew(s, getAttributeValue(crew));
			if (cargo != Ship.NO_CARGO)
				getShips().addCargoTo(s, cargo);
		}
		else if (item != null) {
			ItemList items = (cache == null ? getItems() : CacheNode.getItemCache(cache));
			int[] indices = items.findMatches(item);
			if (indices.length > 0) {
				int index = indices[0];
				if (indices.length > 1 && !items.areItemsSame(indices)) {
					// Choose which item to modify
					StyledDocument[] itemDocs = new StyledDocument[indices.length];
					for (int i = 0; i < itemDocs.length; i++)
						itemDocs[i] = items.getItem(indices[i]).getDocument();
					while (true) {
						String chooserTitle = "Choose an item to modify";
						DocumentChooser chooser = new DocumentChooser(FLApp.getSingle(), chooserTitle, itemDocs, false);
						chooser.setVisible(true);
						int[] selections = chooser.getSelectedIndices();
						if (selections != null && selections.length == 1) {
							index = indices[selections[0]];
							break;
						}
					}
				}

				Item i = items.getItem(index);
				if (addtag != null)
					i.addTag(addtag);
				if (removetag != null)
					i.removeTag(removetag);
				if (addbonus != null) {
					int delta = getAttributeValue(addbonus);
					i.adjustBonus(delta);
					items.modifiedItem(i);
				}
			}
		}
		else if (name != null) {
			if (amount != null)
				getCodewords().adjustValue(name, getAttributeValue(amount));
			else if (ticks > 0)
				getCodewords().adjustValue(name, ticks);
		}
		else if (profession != null) {
			// Change the adventurer's profession
			String[] profStrs = profession.split("\\|");
			int[] profs = new int[profStrs.length];
			for (int i = 0; i < profStrs.length; i++) {
				profs[i] = Adventurer.getProfessionType(profStrs[i]);
				System.out.println("Profession " + i + ": " + profs[i]);
			}
			int profChosen = profs[0];
			if (profs.length > 1) {
				// Choose a profession
				DefaultStyledDocument[] docs = new DefaultStyledDocument[profs.length];
				for (int i = 0; i < docs.length; i++) {
					docs[i] = new DefaultStyledDocument();
					try {
						docs[i].insertString(0, Adventurer.getProfessionName(profs[i]), null);
					}
					catch (BadLocationException e) {}
				}
				
				while (true) {
					DocumentChooser chooser = new DocumentChooser(FLApp.getSingle(), "Choose Profession", docs, false);
					chooser.setVisible(true);
					int[] selections = chooser.getSelectedIndices();
					if (selections != null && selections.length == 1) {
						profChosen = profs[selections[0]];
						break;
					}
				}
			}
			
			getAdventurer().setProfession(profChosen);
		}
		else if (ticks > 0) {
			// This should remain as the last case - ticks=1 is the default
			ignoreItemEvent = true;
			getRoot().addTicks(ticks);
			ignoreItemEvent = false;
		}

		if (flag != null)
			getFlags().setState(flag, false);
		if (price != null)
			getFlags().setState(price, true);

		// Notify the grouper that we're ready to continue
		if (callContinue)
			findExecutableGrouper().continueExecution(this, false);
		else
			callsContinue = false;
	}

	private boolean ignoreItemEvent = false;
	/**
	 * Feedback from checkboxes at section head. The user has pressed a checkbox directly
	 * rather than activating the node; we have to handle that here.
	 */
	public void itemStateChanged(ItemEvent evt) {
		if (ignoreItemEvent) return;
		
		if (--ticks == 0) {
			// Continue execution now
			setEnabled(false);
			if (flag != null)
				getFlags().setState(flag, false);
			if (callContinue)
				findExecutableGrouper().continueExecution(this, false);
		}
	}

	public void dispose() {
		if (flag != null)
			getFlags().removeListener(flag, this);
		if (price != null)
			getFlags().removeListener(price, this);
	}

	private int abilityChosen = -1;
	private int gainAmount = 0;
	public void rollerFinished(Roller r) {
		//int preScore = getAdventurer().getAbility(abilityChosen).natural;
		gainAmount = getAdventurer().adjustAbility(abilityChosen, r.getResult());
		//gainAmount = getAdventurer().getAbility(abilityChosen).natural - preScore;
		
		UndoManager.createNew(this).add(this);
		
		findExecutableGrouper().continueExecution(this, true);
	}

	public void undoOccurred(UndoManager undo) {
		if (gainAmount != 0) {
			getAdventurer().adjustAbility(abilityChosen, -gainAmount);
			gainAmount = 0;
		}
		setEnabled(true);
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
		String text = null;
		if (codeword != null)
			text = "Tick the codeword <i>" + codeword + "</i>";
		else if (god != null)
			text = "Set your god to " + (god.length() == 0 ? "none" : god);
		else if (shards != null) {
			int amount = getAttributeValue(shards);
			if (amount > 1)
				text = "Gain " + amount + " Shards";
			else if (amount == 1)
				text = "Add a Shard";
		}
		else if (ability >= 0) {
			if (abilityEffect == null) {
				if (Character.isDigit(amount.charAt(0))) {
					DiceExpression exp = new DiceExpression(amount);
					if (exp.dice > 0) {
						text = "Roll " + getDiceText(exp.dice);
						if (exp.adjustment > 0)
							text += " and add " + exp.adjustment;
						else if (exp.adjustment < 0)
							text += " and subtract " + (-exp.adjustment);
						
						if (exp.adjustment == 0)
							text += " and add the result to";
						else
							text += ", adding the result to";
					}
					else
						text = "Add " + exp.adjustment + " to";
				}
				else
					text = "Add " + getAttributeValue(amount) + " to";
			}
			else {
				boolean add = true;
				String effect = abilityEffect;
				if (effect.startsWith("-"))
					add = false;
				while (effect.length() > 0 && !Character.isLetter(effect.charAt(0)))
					effect = effect.substring(1);
				if (effect.startsWith("curse"))
					text = (add ? "Curse" : "Remove curse from");
				else if (effect.startsWith("fix"))
					text = (add ? "Fix/Paralyse" : "Remove fix from");
				else {
					System.out.println("Unrecognised ability effect: " + effect);
					return null;
				}
			}
			
			switch (ability) {
			case Adventurer.ABILITY_ALL:
				text += " all your abilities";
				break;
			case Adventurer.ABILITY_SINGLE:
				text += " one of your abilities";
				break;
			default:
				text += " your " + Adventurer.getAbilityName(ability);
			}
		}
		else if (title != null) {
			String titleStr;
			if (titlePattern == null)
				titleStr = title;
			else {
				int titleValue = getAdventurer().getTitleValue(title, -999);
				Title t;
				if (titleValue == -999)
					t = new Title(title, titlePattern, titleInitialValue);
				else
					t = new Title(title, titlePattern, titleValue + titleAdjustment);
				titleStr = t.toString();
			}
			text = "Gain the title '" + titleStr + "'";
		}
		else if (blessing != null)
			text = "Gain a " + blessing.getContentString() + " blessing";
		else if (special != null) {
			String type = special.toLowerCase();
			if (type.startsWith("defence")) {
				int defenceBonus = (bonus != null ? getAttributeValue(bonus) : 3);
				text = "Add a temporary bonus of " + defenceBonus + " to your Defence";
			}
			else if (type.startsWith("attack") && bonus != null) {
				text = "Add a temporary bonus of " + bonus + " to your attack rolls";
			}
			else if (type.equals("armourlock")) {
				text = "Lock your currently worn armour";
			}
			else if (type.equals("weaponlock")) {
				text = "Lock your currently wielded weapon";
			}
			else if (type.startsWith("difficulty")) {
				if (type.equals("difficultycurse"))
					text = "Curse your Difficulty rolls to only use one die";
				else if (type.equals("difficultyrestore")) {
					if (getAdventurer().getDifficultyDice() < 2)
						text = "Restore your Difficulty rolls to use two dice";
				}
			}
			else if (type.startsWith("godless")) {
				text = "Make your character godless, unable to worship any god from now on";
			}
			else if (cache != null) {
				if (type.equals("freeze") || type.equals("lock"))
					text = "Lock the cache [" + cache + "]";
				else if (type.equals("thaw") || type.equals("unlock"))
					text = "Unlock the cache [" + cache + "]";
			}
		}
		else if (crew != null || cargo != Ship.NO_CARGO) {
			if (crew != null)
				text = "Set the crew of a ship to " + Ship.getCrewName(getAttributeValue(crew));
			if (cargo != Ship.NO_CARGO) {
				if (text != null)
					text += ", and gain";
				else
					text = "Gain";
				text += " a cargo unit of " + Ship.getCargoName(cargo);
			}
		}
		else if (item != null) {
			String modText = "";
			if (addtag != null)
				modText = "add '" + addtag + "' tag";
			if (removetag != null)
				modText = "remove '" + removetag + "' tag";
			if (addbonus != null) {
				if (modText.length() > 0)
					modText += ", ";
				int delta = getAttributeValue(addbonus);
				if (delta > 0)
					modText += "add " + delta + " to bonus";
				else if (delta < 0)
					modText += "subtract " + (-delta) + " from bonus";
			}
			text = "Modify an item (" + modText + ")";
			if (cache != null)
				text += " in cache [" + cache + "]";
		}
		else if (name != null) {
			if (amount != null) {
				int delta = getAttributeValue(amount);
				if (delta > 0)
					text = "Add " + delta + " to";
				else if (delta < 0)
					text = "Subtract " + (-delta) + " from";
			}
			else if (ticks > 0)
				text = "Add " + ticks + " to";
			if (text != null)
				text += " the codeword/field <i>" + name + "</i>";
		}
		else if (profession != null) {
			// Change the adventurer's profession
			String[] profStrs = profession.split("\\|");
			int[] profs = new int[profStrs.length];
			for (int i = 0; i < profStrs.length; i++) {
				profs[i] = Adventurer.getProfessionType(profStrs[i]);
				System.out.println("Profession " + i + ": " + profs[i]);
			}
			text = "Change your character's profession";
			if (profs.length == 1)
				text += " to " + Adventurer.getProfessionName(profs[0]);
		}
		else if (ticks > 1)
			text = "Add " + ticks + " ticks to this section";
		else if (ticks > 0)
			text = "Add a tick to this section";
		
		return text;
	}
}
