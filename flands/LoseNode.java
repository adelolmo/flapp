package flands;


import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.xml.sax.Attributes;

/**
 * Action node that generally 'removes' something from the character. The functionality
 * varies depending on the type of thing being affected; the complementary function
 * is usually in TickNode.
 * 
 * @author Jonathan Mann
 */
public class LoseNode extends ActionNode implements Executable, Roller.Listener, UndoManager.Creator, Flag.Listener, ChangeListener {
	public static final String ElementName = "lose";
	private String codeword;
	private String shards;
	private int ability = -1;
	private String amount;
	private boolean fatal = false;
	private DiceExpression staminaExp = null;
	private String staminaVar = null;
	private String staminaTo = null;
	private boolean allItems = false;
	private String chance = null;
	private Item item = null;
	private String itemCountStr = null;
	private String itemAt;
	private Blessing blessing = null;
	private Curse curse = null;
	private boolean ship;
	private boolean resurrection;
	private int crew = 0;
	private int cargo = Ship.NO_CARGO;
	private String price, flag;
	private String cache;
	private String title;
	private String god;
	private boolean forced;
	private List<AdjustNode> adjustments = null;

	public LoseNode(Node parent) {
		super(ElementName, parent);
		setEnabled(false);
	}

	public void init(Attributes atts) {
		codeword = atts.getValue("codeword");
		shards = atts.getValue("shards");
		if (shards != null && shards.equals("*"))
			shards = Integer.toString(Integer.MAX_VALUE);

		String val = atts.getValue("stamina");
		if (val != null) {
			if (Character.isDigit(val.charAt(0)))
				staminaExp = new DiceExpression(val);
			else
				staminaVar = val;
		}
		else
			staminaTo = atts.getValue("staminato");

		val = atts.getValue("ability");
		if (val != null)
			ability = Adventurer.getAbilityType(val);

		amount = atts.getValue("amount");
		fatal = getBooleanValue(atts, "fatal", false);

		val = atts.getValue("item");
		if (val != null && val.equals("*")) {
			allItems = true;
			chance = atts.getValue("chance"); // chance of losing each item, as "x/y"
		}
		else
			item = Item.createItem(atts);
		itemCountStr = atts.getValue("multiple");
		itemAt = atts.getValue("itemAt");

		blessing = Blessing.getBlessing(atts);
		if (blessing != null)
			blessing.setPermanent(getBooleanValue(atts, "permanent", false));
		curse = Curse.createCurse(atts);
		title = atts.getValue("title");
		god = atts.getValue("god");

		ship = getBooleanValue(atts, "ship", false);
		resurrection = getBooleanValue(atts, "resurrection", false);

		val = atts.getValue("cargo");
		if (val != null) {
			cargo = Ship.getCargo(val);
			System.out.println("Cargo type=" + cargo);
		}

		crew = getIntValue(atts, "crew", 0);
		
		price = atts.getValue("price");
		flag = atts.getValue("flag");
		if (price != null && price.length() > 0) {
			getFlags().addListener(price, this);
		}
		if (flag != null) {
			getFlags().addListener(flag, this);
		}
		if (price != null) {
			if (shards != null)
				getAdventurer().addMoneyListener(this);
		}
		cache = atts.getValue("cache");
		
		forced = getBooleanValue(atts, "force", true);
		
		super.init(atts);
	}

	protected Node createChild(String name) {
		Node n = null;
		if (name.equals(AdjustNode.ElementName)) {
			AdjustNode an = new AdjustNode(this);
			if (adjustments == null)
				adjustments = new LinkedList<AdjustNode>();
			adjustments.add(an);
			n = an;
		}

		if (n == null)
			return super.createChild(name);
		else {
			addChild(n);
			return n;
		}
	}

	private int lastAdjustment;
	private int getAdjustment() {
		int delta = 0;
		if (adjustments != null)
			for (Iterator<AdjustNode> i = adjustments.iterator(); i.hasNext(); )
				delta += i.next().getAdjustment();
		
		System.out.println("Adjustment for LoseNode=" + delta);
		lastAdjustment = delta;
		return delta;
	}
	
	protected void outit(Properties props) {
		super.outit(props);
		if (codeword != null) props.setProperty("codeword", codeword);
		if (shards != null) saveVarProperty(props, "shards", shards);
		if (staminaExp != null) props.setProperty("stamina", staminaExp.toString());
		else if (staminaVar != null) saveVarProperty(props, "stamina", staminaVar);
		else if (staminaTo != null) saveVarProperty(props, "staminato", staminaTo);
		if (ability >= 0) props.setProperty("ability", Adventurer.getAbilityName(ability));
		if (amount != null) saveVarProperty(props, "amount", amount);
		if (fatal) saveProperty(props, "fatal", true);
		if (allItems) {
			props.setProperty("item", "*");
			if (chance != null) props.setProperty("chance", chance);
		}
		else if (item != null) item.saveProperties(props);
		if (itemCountStr != null) saveVarProperty(props, "multiple", itemCountStr);
		else if (itemAt != null) saveVarProperty(props, "itemAt", itemAt);
		if (blessing != null)
			blessing.saveTo(props);
		if (curse != null)
			props.setProperty(Curse.getTypeName(curse.getType()), curse.getName());
		if (title != null) props.setProperty("title", title);
		if (god != null) props.setProperty("god", god);
		if (ship) saveProperty(props, "ship", true);
		if (resurrection) saveProperty(props, "resurrection", true);
		if (cargo >= 0) props.setProperty("cargo", Ship.getCargoName(cargo));
		if (crew != 0) saveProperty(props, "crew", crew);
		if (price != null) props.setProperty("price", price);
		if (flag != null) props.setProperty("flag", flag);
		if (cache != null) props.setProperty("cache", cache);
		if (!forced) saveProperty(props, "forced", false);
	}
	
	public void handleContent(String text) {
		Element[] leaves = null;
		MutableAttributeSet atts = createStandardAttributes();
		if (text.length() == 0) {
			if (hidden || getParent().hideChildContent())
				return;

			// Default text
			if (codeword != null) {
				text = (getDocument().isNewSentence(getDocument().getLength()) ? "Erase" : "erase") + " the codeword " + codeword;
				SimpleAttributeSet wordAtts = new SimpleAttributeSet(atts);
				StyleConstants.setItalic(wordAtts, true);
				leaves = getDocument().addStyledText(getElement(), text, codeword, atts, wordAtts);
			}
			else if (staminaExp != null) {
				text = (getDocument().isNewSentence(getDocument().getLength()) ? "L" : "l");
				text += "ose " + staminaExp.toString() + " Stamina point";
				if (staminaExp.dice > 0 || staminaExp.adjustment > 1)
					text += "s";
			}
			else if (item != null) {
				leaves = item.addTo(getDocument(), getElement(), atts, false);
			}
			else if (shards != null) {
				try {
					int val = Integer.parseInt(shards);
					text = (val == 1 ? "1 Shard" : val + " Shards");
				}
				catch (NumberFormatException nfe) {}
			}
			else if (curse != null) {
				text = curse.getName();
			}
			else if (title != null) {
				text = title;
			}
		}
		else if (codeword != null && text.indexOf(codeword) >= 0) {
			SimpleAttributeSet wordAtts = new SimpleAttributeSet(atts);
			StyleConstants.setItalic(wordAtts, true);
			leaves = getDocument().addStyledText(getElement(), text, codeword, atts, wordAtts);
		}

		if (leaves == null)
			leaves = getDocument().addLeavesTo(getElement(), new String[] { text }, new AttributeSet[] { atts });

		addHighlightElements(leaves);
		addEnableElements(leaves);
	}

	public boolean handleEndTag() {
		findExecutableGrouper().addExecutable(this);
		return super.handleEndTag();
	}

	private ItemList getAffectedItems() {
		return (cache == null ? getItems() : CacheNode.getItemCache(cache));
	}
	private int getItemCount() {
		return (itemCountStr == null ? 1 : getAttributeValue(itemCountStr));
	}
	
	private boolean callContinue = false;
	public boolean execute(ExecutableGrouper grouper) {
		if (price != null && price.length() > 0) {
			flagChanged(price, getFlags().getState(price));
		}
		if (flag != null) {
			flagChanged(flag, getFlags().getState(flag));
		}
		
		if (price != null) {
			if (canPayInFull()) {
				System.out.println("LoseNode.execute(): can pay in full now");
				setEnabled(true);
			}
			if (!actionDoesAnything() && item == null && shards == null && cargo == Ship.NO_CARGO) {
				System.out.println("LoseNode.execute(): will set flag now");
				actionPerformed(null); // see 5.365 for an example of why
			}
			return true;
		}
		
		if (flag != null && !hidden)
			return true; // don't block

		if (actionDoesAnything()) {
			setEnabled(true);
			// Return false and pause until user activates this node
			if (hidden) {
				System.out.println("Performing hidden action");
				actionPerformed(null);
				return true;
			}

			if (!forced)
				return true;
			
			callContinue = true;
			return false;
		}
		else
			return true;
	}

	private boolean actionDoesAnything() {
		// Any of these conditions will cause a change
		return ((codeword != null && getCodewords().hasCodeword(codeword)) ||
				(shards != null && getAttributeValue(shards) > 0 && (cache == null ? getAdventurer().getMoney() : CacheNode.getMoneyCache(cache)) > 0) ||
				staminaExp != null ||
				(staminaVar != null && getAttributeValue(staminaVar) > 0) ||
				staminaTo != null ||
				(item != null && getAffectedItems().findMatches(item).length > 0) ||
				(itemAt != null && getAffectedItems().getItemCount() >= getAttributeValue(itemAt)) ||
				(blessing != null && getBlessings().hasBlessing(blessing)) ||
				(curse != null && getCurses().findMatches(curse).length > 0) ||
				(allItems && getAffectedItems().getItemCount() > 0) ||
				(title != null && getAdventurer().hasTitle(title)) ||
				(god != null && getAdventurer().hasGod(god)) ||
				(ship && getShips().findShipsHere().length > 0) ||
				(cargo != Ship.NO_CARGO && getShips().findShipsHere().length > 0 && getShips().getShip(getShips().getSingleShip()).hasCargo(cargo)) ||
				(crew != 0 && getShips().findShipsHere().length > 0
				 && ((crew > 0 && getShips().getShip(getShips().getSingleShip()).getCrew() > Ship.POOR_CREW) ||
					 (crew < 0 && getShips().getShip(getShips().getSingleShip()).getCrew() < Ship.EX_CREW))) ||
				(resurrection && getAdventurer().hasResurrection()) ||
				ability >= 0);		
	}
	private boolean canPayInFull() {
		if (!actionDoesAnything()) return false;
		if (shards != null) {
			int cost = getAttributeValue(shards);
			int available = (cache == null ? getAdventurer().getMoney() : CacheNode.getMoneyCache(cache));
			if (cost > available)
				return false;
		}
		if (item != null && getAffectedItems().findMatches(item).length < getItemCount())
			return false;
		return true;
	}
	
	public void resetExecute() {
		setEnabled(false);
	}

	public void actionPerformed(ActionEvent evt) {
		setEnabled(false);
		callsContinue = callContinue;
		
		// Collect all the info for cancellable actions first
		// - a single lose node may contain multiple actions, but we should only do all
		// (and continue) or do none (and re-enable the node). Hence we need to make sure
		// no actions are cancelled before we actually carry out the actions.
		if (ability >= 0) {
			abilityAffected = ability;
			if (ability == Adventurer.ABILITY_SINGLE) {
				ArrayList<Integer>
					abilities = new ArrayList<Integer>(),
					values = new ArrayList<Integer>();
				for (int a = 0; a < Adventurer.ABILITY_COUNT; a++) {
					int value = getAdventurer().getAbilityValue(a,  Adventurer.MODIFIER_NATURAL);
					if (value > 1) {
						// Don't let the player pick an ability that's already at 1!
						abilities.add(new Integer(a));
						values.add(new Integer(value));
					}
				}
				
				if (abilities.size() > 0) {
					int[]
						abilitiesArr = new int[abilities.size()],
						valuesArr = new int[abilities.size()];
					for (int i = 0; i < abilitiesArr.length; i++) {
						abilitiesArr[i] = abilities.get(i).intValue();
						valuesArr[i] = values.get(i).intValue();
					}
					DocumentChooser dc = new DocumentChooser(FLApp.getSingle(), "Choose Ability", Adventurer.getAbilityDocuments(abilitiesArr, valuesArr), false);
					dc.setVisible(true);
					
					if (dc.getSelectedIndices() == null) {
						setEnabled(true);
						return;
					}
					abilityAffected = dc.getSelectedIndices()[0];
				}
				else {
					abilityAffected = -1;
				}
			}
		}
		
		int[] itemIndices = null;
		int itemCount = getItemCount();
		if (item != null) {
			ItemList items = getAffectedItems();
			int[] indices = items.findMatches(item, true, itemCount);
			if (indices.length == 0 && !item.getName().equals("?"))
				// Handle the special case where a 'kept' item can be lost
				indices = items.findMatches(item);

			if (indices.length > itemCount) {
				// Check whether they're all equal (in which case we can remove any of them)
				if (!items.areItemsSame(indices)) {
					// Choose which item(s) to lose
					StyledDocument[] itemDocs = new StyledDocument[indices.length];
					for (int i = 0; i < itemDocs.length; i++)
						itemDocs[i] = items.getItem(indices[i]).getDocument();
					
					String chooserTitle = (itemCount == 1 ? "Choose an item to lose" : "Choose " + itemCount + " items to lose");
					DocumentChooser chooser = new DocumentChooser(FLApp.getSingle(), chooserTitle, itemDocs, itemCount > 1);
					chooser.setVisible(true);
					int[] selections = chooser.getSelectedIndices();
					if (selections == null) selections = new int[0];
					if (selections.length > itemCount || (selections.length < itemCount && selections.length < items.getItemCount())) {
						String errorTitle = (selections.length > itemCount ? "Too many items" : "Not enough items");
						JOptionPane.showMessageDialog(FLApp.getSingle(), new String[] {"You must select exactly " + itemCount + " item" + (itemCount>1 ? "s":"") + "."}, errorTitle, JOptionPane.INFORMATION_MESSAGE);
						setEnabled(true);
						return;
					}
					
					for (int i = 0; i < selections.length; i++)
						selections[i] = indices[selections[i]];
					indices = selections;
				}
			}
			itemIndices = indices;
		}
		
		int[] curseIndices = null;
		if (curse != null) {
			System.out.println("Looking for curses matching " + curse);
			CurseList curses = getCurses();
			int[] indices = curses.findMatches(curse);
			if (indices.length > 1 && !hidden && !curse.getName().equals("*")) {
				JOptionPane.showMessageDialog(FLApp.getSingle(), new String[] {"Which curse did you want to lift?", "Select that one and try again."}, "Multiple Curses", JOptionPane.INFORMATION_MESSAGE);
				setEnabled(true);
				return;
			}
			curseIndices = indices;
		}

		int removeCargoType = -1;
		if (cargo == Ship.MATCH_SINGLE_CARGO) {
			int[] indices = getShips().findShipsHere();
			if (indices.length > 1)
				System.out.println("Lose ship/cargo/crew: too many ships here - don't know to handle this!");

			if (indices.length > 0) {
				Ship s = getShips().getShip(indices[0]);
				int[] cargoTypes = s.hasCargoTypes();
				int chosenType = -1;
				if (cargoTypes.length == 1)
					chosenType = cargoTypes[0];
				if (cargoTypes.length > 1) {
					DefaultStyledDocument[] docs = new DefaultStyledDocument[cargoTypes.length];
					for (int i = 0; i < cargoTypes.length; i++) {
						DefaultStyledDocument doc = new DefaultStyledDocument();
						try {
							doc.insertString(0, Ship.getCargoName(cargoTypes[i]), null);
						}
						catch (BadLocationException ble) {}
						docs[i] = doc;
					}
					
					DocumentChooser chooser = new DocumentChooser(FLApp.getSingle(), "Choose Cargo Type", docs, false);
					chooser.setVisible(true);
					
					if (chooser.getSelectedIndices() == null) {
						setEnabled(true);
						return;
					}
					chosenType = cargoTypes[chooser.getSelectedIndices()[0]];
				}
				
				removeCargoType = chosenType;
			}
		}
		
		Resurrection removeResurrection = null;
		if (resurrection && getAdventurer().hasResurrection()) {
			Resurrection r = getAdventurer().chooseResurrection("Lose Resurrection");
			if (r == null) {
				setEnabled(true);
				return;
			}
			removeResurrection = r;
		}
		
		// Last cancellable action - if it doesn't cancel, it immediately occurs
		// (maybe should split the method into two)
		if (blessing != null) {
			if (!getBlessings().removeBlessing(blessing)) {
				// Operation failed, for whatever reason
				setEnabled(true);
				return;
			}
		}

		// Carry out all actions
		if (codeword != null)
			getCodewords().removeCodeword(codeword);

		if (shards != null) {
			if (cache == null)
				getAdventurer().adjustMoney(-getAttributeValue(shards));
			else {
				int balance = CacheNode.getMoneyCache(cache);
				balance -= getAttributeValue(shards);
				if (balance < 0)
					balance = 0;
				CacheNode.setMoneyCache(cache, balance);
			}
		}

		if (item != null) {
			// itemIndices selected above
			ItemList items = getAffectedItems();
			for (int i = Math.min(itemCount-1, itemIndices.length-1); i >= 0; i--)
				items.removeItem(itemIndices[i]);
			//if (indices.length > 0)
			//	items.removeItem(indices[0]);
		}
		else if (allItems) {
			boolean doneRemove = false;
			if (chance != null) {
				int index = chance.indexOf("/");
				try {
					int num = Integer.parseInt(chance.substring(0, index));
					int den = Integer.parseInt(chance.substring(index+1));
					double fraction = ((double)num) / den;
					System.out.println("Chance of losing each item is " + fraction);
					ItemList items = getAffectedItems();
					for (int i = items.getItemCount() - 1; i >= 0; i--) {
						double r = Math.random();
						System.out.println("Result for item " + i + "=" + r);
						if (r < fraction && !items.getItem(i).hasKeepTag())
							items.removeItem(i);
					}
					doneRemove = true;
				}
				catch (NumberFormatException nfe) { System.err.println("Couldn't parse 'chance' attribute: " + chance); }
			}
			
			if (!doneRemove && cache != null) {
				// Removing all items from a cache - but money may also be there
				ItemList items = getAffectedItems();
				int moneyIndex = items.getMoneyItem();
				if (moneyIndex >= 0) {
					for (int i = items.getItemCount() - 1; i >= 0; i--)
						if (i != moneyIndex)
							items.removeItem(i);
					doneRemove = true;
				}
			}
			
			if (!doneRemove)
				getAffectedItems().removeAll();
		}

		if (itemAt != null) {
			int index = getAttributeValue(itemAt) - 1;
			ItemList items = getAffectedItems();
			while (index < items.getItemCount() && items.getItem(index).isMoney())
				index++;
			if (index < items.getItemCount() && !items.getItem(index).hasKeepTag())
				items.removeItem(index);
		}
		
		if (curse != null) {
			// curseIndices selected above
			CurseList curses = getCurses();
			if (curseIndices.length > 0) {
				if (!curse.getName().equals("*")) {
					System.out.println("Removing curse " + curseIndices[0]);
					curses.removeCurse(curseIndices[0]);
				}
				else {
					System.out.println("Removing all matching curses");
					for (int i = curseIndices.length - 1; i >= 0; i--)
						curses.removeCurse(curseIndices[i]);
				}
			}
		}

		if (title != null)
			getAdventurer().removeTitle(title);
		
		if (god != null)
			getAdventurer().removeGod(god);
		
		// Handle all ship-related losses here
		if (ship || cargo != Ship.NO_CARGO || crew != 0) {
			int[] indices = getShips().findShipsHere();
			if (indices.length > 1)
				System.out.println("Lose ship/cargo/crew: too many ships here - don't know to handle this!");

			if (indices.length > 0) {
				Ship s = getShips().getShip(indices[0]);
				if (cargo != Ship.NO_CARGO) {
					if (cargo == Ship.MATCH_SINGLE_CARGO)
						// Selected above
						s.removeCargo(removeCargoType);
					else
						while (s.removeCargo(cargo)) ;
				}
				if (crew != 0)
					s.adjustCrew(-crew);
				if (ship)
					getShips().removeShip(indices[0]);
				getShips().refresh();
			}
		}
		
		if (resurrection)
			getAdventurer().removeResurrection(removeResurrection);
		
		// Leave roller-connected actions to last
		// (these can't both be present)
		if (staminaExp != null) {
			staminaExp.addAdjustment(getAdjustment());
			Roller r = staminaExp.createRoller();
			if (r == null) {
				if (getAdventurer().getStamina().damage(staminaExp.adjustment))
					; // TODO: death!
			}
			else {
				r.addListener(this);
				r.startRolling();
				return;
			}
		}
		else if (staminaVar != null) {
			int val = getAttributeValue(staminaVar);
			if (getAdventurer().getStamina().damage(val))
				; // TODO: death!
		}
		else if (staminaTo != null) {
			int currentStamina = getAdventurer().getStamina().current;
			getAdventurer().getStamina().damage(currentStamina - getAttributeValue(staminaTo));
		}

		if (ability >= 0 && abilityAffected >= 0) {
			// abilityAffected should have already been chosen above
			if (Character.isDigit(amount.charAt(0))) {
				DiceExpression exp = new DiceExpression(amount);
				exp.addAdjustment(getAdjustment());
				Roller r = exp.createRoller();
				if (r != null) {
					r.addListener(this);
					r.startRolling();
					return;
				}
			}
			
			getAdventurer().adjustAbility(abilityAffected, -(getAttributeValue(amount)+getAdjustment()), fatal);
			// which will handle the case where this loss is fatal
		}

		if (flag != null)
			getFlags().setState(flag, false);
		if (price != null)
			getFlags().setState(price, true);

		super.actionPerformed(evt);
		
		if (callContinue)
			findExecutableGrouper().continueExecution(this, false);
	}

	protected String getTipText() {
		System.out.println("Entering LoseNode.getTipText() method");
		List<String> lines = new LinkedList<String>();
		
		String text = null;
		boolean allShards = (shards != null && shards.equals(Integer.toString(Integer.MAX_VALUE)));
		if (allItems) {
			text = "Lose all possessions";
			if (chance != null)
				text += " (each with " + chance + " probability)";
			if (allShards)
				text += " and Shards";
			else if (shards != null)
				text += " and " + getAttributeValue(shards) + " Shards";
		}
		else if (item != null) {
			StyledTextList itemText = item.createItemText(null, false);
			text = "Lose " + itemText.toXML();
		}
		else if (allShards)
			text = "Lose all Shards";
		else if (shards != null) {
			int amount = getAttributeValue(shards);
			text = "Lose " + amount + " Shard" + (amount == 1 ? "" : "s");
		}
		if (text != null) {
			if (cache != null)
				text += " from the cache [" + cache + "]";
			lines.add(text);
		}
		
		if (ability >= 0) {
			text = "Lose ";
			if (Character.isDigit(amount.charAt(0))) {
				DiceExpression exp = new DiceExpression(amount);
				exp.addAdjustment(getAdjustment());
				text += exp.toString();
			}
			else
				text += getAttributeValue(amount) + getAdjustment();
			switch (ability) {
			case Adventurer.ABILITY_SINGLE:
				text += " from an ability";
				break;
			case Adventurer.ABILITY_ALL:
				text += " from all abilities";
				break;
			default:
				text += " from " + Adventurer.getAbilityName(ability);
			}
			lines.add(text);
		}
		
		if (curse != null) {
			text = "Remove ";
			if (curse.getName().equals("*"))
				text += "all curses";
			else
				text += curse.getName() + " [" + curse.getType() + "]";
			lines.add(text);
		}
		
		if (cargo != Ship.NO_CARGO) {
			if (cargo == Ship.MATCH_ALL_CARGO)
				text = "Lose all cargo";
			else if (cargo == Ship.MATCH_SINGLE_CARGO)
				text = "Lose a cargo unit";
			else
				text = "Lose a cargo unit of " + Ship.getCargoName(cargo);
			lines.add(text);
		}
		
		if (resurrection)
			lines.add("Lose your resurrection arrangements");
		
		if (blessing != null) {
			if (blessing.getType() == Blessing.MATCHANY_TYPE)
				text = "Lose one of your blessings";
			else if (blessing.getType() == Blessing.MATCHALL_TYPE)
				text = "Lose all of your blessings";
			else
				text = "Lose your " + blessing.getContentString() + " blessing";
			lines.add(text);
		}
		
		if (codeword != null)
			lines.add("Erase the codeword " + codeword);
		
		if (itemAt != null) {
			text = "Lose the item at position " + (getAttributeValue(itemAt)+1);
			if (cache != null)
				text += " from the cache [" + cache + "]";
			lines.add(text);
		}
		
		if (title != null)
			lines.add("Lose the title " + title);
		
		if (god != null) {
			if (god.equals("*"))
				lines.add("Renounce worship of all gods");
			else
				lines.add("Renounce worship of " + god);
		}
		
		if (crew != 0)
			lines.add("Reduce your crew's level by " + crew);
		
		if (ship)
			lines.add("Lose your ship");
		
		if (staminaExp != null) {
			DiceExpression exp = new DiceExpression(staminaExp);
			exp.addAdjustment(getAdjustment());
			lines.add("Lose " + exp + " from your Stamina");
		}
		else if (staminaVar != null) {
			lines.add("Lose " + getAttributeValue(staminaVar) + " from your Stamina");
		}
		else if (staminaTo != null) {
			lines.add("Reduce your Stamina to " + getAttributeValue(staminaTo));
		}
		
		if (lines.size() == 0)
			return null;
		else if (lines.size() == 1)
			return lines.get(0);
		else {
			StringBuffer sb = new StringBuffer("<ul>");
			for (Iterator<String> i = lines.iterator(); i.hasNext(); )
				sb.append("<li>").append(i.next()).append("</li>");
			sb.append("</ul>");
			return sb.toString();
		}
	}
	
	private int staminaLost, damageDone, abilityAffected;
	public void rollerFinished(Roller r) {
		// Take damage, but remember the amount in case a reroll occurs
		r.appendTooltipText(" damage");
		boolean dead = false;
		if (staminaExp != null) {
			Adventurer.StaminaStat stamina = getAdventurer().getStamina();
			int staminaPre = stamina.current;
			dead = stamina.damage(r.getResult());
			staminaLost = staminaPre - stamina.current;
		}
		else {
			int preStamina = getAdventurer().getStamina().current;
			damageDone = -getAdventurer().adjustAbility(abilityAffected, -r.getResult(), fatal);
			staminaLost = preStamina - getAdventurer().getStamina().current;
			if (abilityAffected == Adventurer.ABILITY_STAMINA)
				staminaLost -= damageDone;
			// current stamina may have changed if the loss was fatal
		}

		UndoManager.createNew(this).add(this);
		if (dead)
			; // TODO: death!
		
		if (flag != null)
			getFlags().setState(flag, false);
		if (price != null)
			getFlags().setState(price, true);
		
		super.actionPerformed(null);
		
		if (callContinue)
			findExecutableGrouper().continueExecution(this, true);
	}

	public void undoOccurred(UndoManager undo) {
		// Reroll - reset stamina
		if (damageDone != 0) {
			// TODO: if ability == ABILITY_ALL this is unreliable
			getAdventurer().adjustAbility(abilityAffected, damageDone);
			damageDone = 0;
		}
		if (staminaLost > 0) {
			getAdventurer().getStamina().heal(staminaLost);
			staminaLost = 0;
		}
		if (lastAdjustment != 0) {
			staminaExp.addAdjustment(-lastAdjustment);
			lastAdjustment = 0;
		}
		execute(findExecutableGrouper());
	}

	protected void saveProperties(Properties props) {
		super.saveProperties(props);
		if (staminaLost > 0)
			props.setProperty("staminaLost", Integer.toString(staminaLost));
		if (damageDone > 0) {
			Node.saveProperty(props, "damageDone", damageDone);
			Node.saveProperty(props, "abilityAffected", abilityAffected);
		}
		saveProperty(props, "continue", callContinue);
	}
	protected void loadProperties(Attributes atts) {
		super.loadProperties(atts);
		staminaLost = getIntValue(atts, "staminaLost", 0);
		damageDone = getIntValue(atts, "damageDone", 0);
		abilityAffected = getIntValue(atts, "abilityAffected", 0);
		callContinue = getBooleanValue(atts, "continue", false);
	}

	public void flagChanged(String name, boolean state) {
		if (price != null && price.equals(name)) {
			if (state)
				setEnabled(false);
			else
				setEnabled(canPayInFull());
		}
		if (flag != null && flag.equals(name)) {
			setEnabled(state);
		}
	}
	
	public void dispose() {
		if (flag != null)
			getFlags().removeListener(flag, this);
		if (price != null && price.length() > 0)
			getFlags().removeListener(price, this);
	}

	public void stateChanged(ChangeEvent e) {
		// Only called if price != null, shards != null
		flagChanged(price, getFlags().getState(price));
	}
}

