package flands;


import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.swing.AbstractListModel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * A list of items.
 * The usual case is the one held by the other player, but then I realised I could reuse
 * this class for any item caches ie. items left at a particular location.
 * 
 * @author Jonathan Mann
 */
public class ItemList extends AbstractListModel implements GameListener, XMLOutput {
	private Adventurer owner;
	private EffectSet effects = null;
	private CacheNode itemCache = null;

	private int itemLimit = -1;
	private int shardsLimit = -1;
	private String name;
	private boolean weaponLock = false, armourLock = false, frozen = false;
	private List<Object> currencyListeners = null;

	public static final int MAX_ITEM_COUNT = 12;
	private ArrayList<Item> items = new ArrayList<Item>(MAX_ITEM_COUNT);

	// The currently 'active' items
	private Item.Weapon wielded = null;
	private Item.Armour worn = null;
	private Item.Tool[] tools;

	public ItemList(Adventurer owner) {
		this.owner = owner;
		tools = new Item.Tool[Adventurer.ABILITY_COUNT];
		items = new ArrayList<Item>(MAX_ITEM_COUNT);
		itemLimit = 0; // counts number of 'money' items in the list
	}

	public ItemList(String name) {
		this.name = name;
		items = new ArrayList<Item>();
	}

	public Adventurer getAdventurer() { return owner; }
	private EffectSet getEffects() {
		return (owner != null ? owner.getEffects() : effects); }

	/**
	 * Get the combined effects of all items in the list.
	 * This is intended for use with item caches, which normally don't have
	 * an owner to affect.
	 */
	public EffectSet createTempEffects() {
		if (effects == null) {
			effects = new EffectSet(null);
			for (int i = 0; i < items.size(); i++)
				addItemEffects(items.get(i));
			// TODO: make sure the best weapon and armour are selected
		}
		return effects;
	}
	
	/**
	 * Remove the object used to track the effects of items in the list.
	 * This only affects the object returned by {@link #createTempEffects()}.
	 */
	public void dumpTempEffects() {
		if (effects != null)
			effects = null;
	}

	public int getSize() {
		return (getAdventurer() != null ? MAX_ITEM_COUNT + itemLimit :
			    (itemLimit > 0 ? itemLimit : getItemCount()));
	}
	public int getItemCount() { return items.size(); }
	public Item getItem(int i) {
		return (i < getItemCount() ? items.get(i) : null);
	}
	public int getFreeSpace() {
		return (getAdventurer() != null ? MAX_ITEM_COUNT + itemLimit - getItemCount() :
			(itemLimit > 0 ? itemLimit - getItemCount() : 10));
	}
	public void setItemLimit(int limit) {
		if (getAdventurer() == null) {
			itemLimit = limit;
			
			// Make sure any money items present don't count towards this limit
			for (int i = 0; i < getItemCount(); i++) {
				Item item = getItem(i);
				if (item.isMoney())
					itemLimit++;
			}
		}
	}

	private List<ChangeListener> listeners = null;
	public void addChangeListener(ChangeListener l) {
		if (listeners == null)
			listeners = new LinkedList<ChangeListener>();
		if (!listeners.contains(l))
			listeners.add(l);
	}
	public void removeChangeListener(ChangeListener l) {
		if (listeners != null)
			listeners.remove(l);
	}
	private void notifyListeners() {
		if (listeners != null) {
			ChangeEvent evt = new ChangeEvent(this);
			for (Iterator<ChangeListener> i = listeners.iterator(); i.hasNext(); )
				i.next().stateChanged(evt);
		}

		if (getEffects() != null)
			getEffects().notifyOwner();
	}

	public Object getElementAt(int i) { return getItemDocument(i); }
	private StyledDocument getItemDocument(int i) {
		Item item = getItem(i);
		return (item == null ? getEmptyDocument() : item.getDocument());
	}

	private static DefaultStyledDocument emptyDoc = null;
	private static StyledDocument getEmptyDocument() {
		if (emptyDoc == null) {
			emptyDoc = new DefaultStyledDocument();
			SimpleAttributeSet atts = new SimpleAttributeSet(Item.getStandardAttributes());
			StyleConstants.setAlignment(atts, StyleConstants.ALIGN_CENTER);
			try {
				// long dash
				emptyDoc.insertString(0, "\u2014", atts);
			}
			catch (BadLocationException ble) {}
		}
		return emptyDoc;
	}

	public ItemList getItemCache() {
		return (itemCache != null &&
			itemCache.getSectionName().equals(FLApp.getSingle().getCurrentSection()) ? itemCache.getCachedItems() : null);
	}
	CacheNode getItemCacheNode() {
		return itemCache;
	}
	public void setItemCache(CacheNode itemCache) {
		this.itemCache = itemCache;
	}
	public void removeItemCache(CacheNode itemCache) {
		if (this.itemCache == itemCache)
			this.itemCache = null;
	}

	/**
	 * Block the player from wielding another weapon, until the next section.
	 */
	public void lockWeapon() {
		weaponLock = true;
		FLApp.getSingle().addGameListener(this);
	}
	private boolean canChangeWeapons() {
		return (!weaponLock && (wielded == null || wielded.getBonus() >= 0));
	}
	
	/**
	 * Block the player from wearing different armour, until the next section.
	 */
	public void lockArmour() {
		armourLock = true;
		FLApp.getSingle().addGameListener(this);
	}
	
	/**
	 * Set whether an ItemList is currently frozen - not allowing items to be removed
	 * or added. This is ordinarily only done to item caches.
	 */
	public void setFrozen(boolean b) {
		frozen = b;
	}
	
	// Available event-firing methods:
	// fireContentsChanged(Object src, int index0, int index1)
	// fireIntervalAdded(Object src, int index0, int index1)
	// fireIntervalRemoved(Object src, int index0, int index1)
	// Both index0 and index1 are included in the interval

	private void wieldedStateChanged(Item.Weapon w) {
		if (getEffects() == null) return;

		if (w.isWielded()) {
			if (w.getBonus() != 0) {
				getEffects().addStatRelated(Adventurer.ABILITY_COMBAT, w);
			}
		}
		else {
			if (w.getBonus() != 0) {
				getEffects().removeStatRelated(Adventurer.ABILITY_COMBAT, w);
			}
		}

		adjustEffects(w, Effect.TYPE_WIELDED, w.isWielded());
	}

	private void wornStateChanged(Item.Armour a) {
		if (getEffects() == null) return;

		if (a.isWorn()) {
			if (a.getBonus() != 0)
				getEffects().addStatRelated(Adventurer.ABILITY_DEFENCE, a);
		}
		else {
			if (a.getBonus() != 0)
				getEffects().removeStatRelated(Adventurer.ABILITY_DEFENCE, a);
		}

		// TODO: Include magic effects that apply when the armour is worn?
		// There's no magic armour in book 5, so I haven't done this.
	}

	private void adjustEffects(Item i, int type, boolean add) {
		Effect e = i.getEffects();
		EffectSet effects = getEffects();
		if (effects == null) return;
		while (e != null && (e = e.firstEffect(type)) != null) {
			// This ability is of interest
			if (e instanceof AbilityEffect) {
				AbilityEffect ae = (AbilityEffect)e;
				if (add)
					effects.addStatRelated(ae.getAbility(), i, e);
				else
					effects.removeStatRelated(ae.getAbility(), i, e);
			}
			e = e.nextEffect();
		}
	}

	public int getMoneyItem() {
		return getMoneyItem("Shard");
	}
	public int getMoneyItem(String currency) {
		for (int i = 0; i < getItemCount(); i++) {
			Item item = getItem(i);
			if (item.isMoney() && item.getCurrency().equals(currency))
				return i;
		}
		return -1;
	}
	
	public void addCurrencyListener(ChangeListener listener) {
		addCurrencyListener("Shard", listener);
	}
	public void addCurrencyListener(String currency, ChangeListener listener) {
		if (currencyListeners == null)
			currencyListeners = new LinkedList<Object>();
		currencyListeners.add(listener);
		currencyListeners.add(currency);
	}
	
	private void notifyCurrencyListeners(String currency) {
		if (currencyListeners != null) {
			ChangeEvent evt = new ChangeEvent(this);
			for (Iterator<Object> i = currencyListeners.iterator(); i.hasNext(); ) {
				ChangeListener l = (ChangeListener)i.next();
				String c = (String)i.next();
				if (currency == null || currency.equals(c))
					l.stateChanged(evt);
			}
		}
	}
	
	public void removeCurrencyListener(ChangeListener listener) {
		if (currencyListeners != null) {
			for (Iterator<Object> i = currencyListeners.iterator(); i.hasNext(); ) {
				ChangeListener l = (ChangeListener)i.next();
				if (l == listener) {
					i.remove();
					i.next();
					i.remove();
				}
				else
					i.next();
			}
		}
	}
	
	public boolean addItem(Item i) {
		return insertItem(i, items.size());
	}

	public boolean insertItem(Item i, int index) {
		if (i.isMoney()) {
			if (getAdventurer() != null && i.getCurrency().equals("Shard")) {
				// Add to regular 'Shards' field
				getAdventurer().adjustMoney(i.getMoney());
				return true;
			}
			
			// Adding money to a list - show it as a single item
			int moneyIndex = getMoneyItem(i.getCurrency());
			if (shardsLimit >= 0 &&
				(moneyIndex < 0 ? 0 : getItem(moneyIndex).getMoney() + i.getMoney()) > shardsLimit) {
				// this money would exceed our limit
				System.out.println("Can't add this much money to the cache - refuse");
				return false;
			}

			if (moneyIndex < 0) {
				items.add(index, i);
				if (itemLimit >= 0)
					// Money shouldn't count as a 'real' item
					itemLimit++;
				
				fireIntervalAdded(this, index, index);
				notifyListeners();

				notifyCurrencyListeners(i.getCurrency());
			}
			else {
				// Add to existing total
				Item moneyItem = getItem(moneyIndex);
				moneyItem.adjustMoney(i.getMoney());
				if (moneyItem.getMoney() <= 0) {
					items.remove(moneyIndex);
					fireIntervalRemoved(this, moneyIndex, moneyIndex);
					moneyItem = null;
					if (itemLimit > 0)
						itemLimit--;
				}
				else
					fireContentsChanged(this, moneyIndex, moneyIndex);
				notifyCurrencyListeners(i.getCurrency());
			}
			return true;
		}

		if (getFreeSpace() > 0) {
			items.add(index, i);

			// Figure out how this affects abilities
			if (getAdventurer() != null)
				addItemEffects(i);
			
			if (i.getCurse() != null && getAdventurer() != null)
				getAdventurer().getCurses().addCurse(i.getCurse());

			fireContentsChanged(this, index, index);
			notifyListeners();
			return true;
		}
		else
			return false;
	}

	private void addItemEffects(Item i) {
		if (i.canBeWielded()) {
			Item.Weapon w = (Item.Weapon)i;
			if (w.isWielded() || (wielded == null && canChangeWeapons()) || w.getBonus() < 0) {
				if (wielded != null) {
					// Unwield the old weapon
					wielded.setWielded(false);
					wieldedStateChanged(wielded);
					int oldIndex = items.indexOf(wielded);
					fireContentsChanged(ItemList.this, oldIndex, oldIndex);
				}
				
				wielded = w;
				w.setWielded(true);
				// This is not necessarily true - what if we're adding items from a saved character?
				// They could have a later weapon with a higher bonus that is recorded as 'wielded'.
				// In that case, we should check here if a weapon is already recorded as 'wielded',
				// and replace our guess with that one.
				// The same applies for armour.
				wieldedStateChanged(w);
			}
		}
		if (i.getType() == Item.ARMOUR_TYPE) {
			Item.Armour a = (Item.Armour)i;
			if (a.isWorn() || (worn == null && !armourLock)) {
				if (worn != null) {
					// Take off the old armour
					worn.setWorn(false);
					wornStateChanged(worn);
					int oldIndex = items.indexOf(worn);
					fireContentsChanged(ItemList.this, oldIndex, oldIndex);
				}
				
				worn = a;
				a.setWorn(true);
				wornStateChanged(a);
			}
		}
		if (i.getType() == Item.TOOL_TYPE) {
			Item.Tool t = (Item.Tool)i;
			int a1 = t.getAbility(), a2 = t.getAbility();
			if (a1 == Adventurer.ABILITY_ALL) {
				a1 = Adventurer.ABILITY_CHARISMA;
				a2 = Adventurer.ABILITY_THIEVERY;
			}
			for (int a = a1; a <= a2; a++) {
				if (a == Adventurer.ABILITY_COMBAT) continue;
				if (tools[a] == null || tools[a].getBonus() < t.getBonus()) {
					if (tools[a] != null)
						getEffects().removeStatRelated(a, tools[a]);
					tools[a] = t;
					getEffects().addStatRelated(a, tools[a]);
				}
			}
		}
		adjustEffects(i, Effect.TYPE_AURA, true);
		adjustEffects(i, Effect.TYPE_TOOL, true);
	}
	
	public boolean adjustMoney(int money, String currency) {
		if (getAdventurer() != null && (currency == null || currency.equals("Shard"))) {
			getAdventurer().adjustMoney(money);
			return true;
		}
		
		int moneyIndex = getMoneyItem(currency);
		if (moneyIndex < 0) {
			if (money > 0) {
				addItem(Item.createMoneyItem(money, currency));
				//if (itemLimit >= 0)
				//	itemLimit++;
			}
		}
		else {
			Item moneyItem = getItem(moneyIndex);
			int newTotal = money + moneyItem.getMoney();
			if (shardsLimit >= 0 && money + moneyItem.getMoney() > shardsLimit)
				return false;
			
			if (newTotal <= 0) {
				removeItem(moneyIndex);
				// Just so you remember - the adjustment below is done in removeItem()
				//if (itemLimit > 0)
				//	itemLimit--;
			}
			else {
				moneyItem.adjustMoney(money);
				fireContentsChanged(this, moneyIndex, moneyIndex);
			}
		}
		notifyCurrencyListeners(currency);
		return true;
	}
	
	public int[] findMatches(Item match) {
		return findMatches(match, false);
	}
	
	public int[] findMatches(Item match, boolean respectKeepTag) {
		return findMatches(match, respectKeepTag, 1);
	}
	
	public int[] findMatches(Item match, boolean respectKeepTag, int minNumber) {
		String firstName = match.getName();
		int[] indices = new int[getItemCount()];
		int count = 0;
		while (match != null) {
			for (int i = 0; i < getItemCount(); i++) {
				Item item = getItem(i);
				if (match.matches(item)) {
					System.out.println("Item " + i + " " + item.toDebugString() + " matched by " + match.toDebugString());
					if (!respectKeepTag || !item.hasKeepTag())
						indices[count++] = i;
				}
			}
			match = match.getNextItem();
		}

		if (count > 1 && configuredList != null && (firstName == null || !firstName.equals("*"))) {
			// See if we can trim this down using selections
			int[] selections = configuredList.getSelectedIndices();
			int[] newIndices = new int[count];
			int newCount = 0;
			for (int i = 0; i < count; i++)
				if (Arrays.binarySearch(selections, indices[i]) >= 0)
					newIndices[newCount++] = indices[i];

			if (newCount >= minNumber) {
				count = newCount;
				indices = newIndices;
			}
		}

		if (count == 0)
			return new int[0];
		else {
			int[] temp = indices;
			indices = new int[count];
			System.arraycopy(temp, 0, indices, 0, count);
			return indices;
		}
	}

	public boolean areItemsSame(int[] indices) {
		for (int i = 1; i < indices.length; i++)
			if (!getItem(indices[i-1]).matches(getItem(indices[i]))) {
				System.out.println("Item " + indices[i-1] + " != " + indices[i]);
				return false;
			}
		return true;
	}

	public boolean removeItem(Item i) {
		int index = items.indexOf(i);
		if (index >= 0) {
			removeItem(index);
			return true;
		}
		else
			return false;
	}

	public void removeItem(int index) {
		if (index < getItemCount()) {
			Item removed = items.remove(index);
			fireIntervalRemoved(this, index, index);
			if (getAdventurer() != null) {
				fireIntervalAdded(this, MAX_ITEM_COUNT-1, MAX_ITEM_COUNT-1); // keep #spaces constant
				removeItemEffects(removed);
			}
			
			if (removed.isMoney()) {
				// Need to reduce our itemlimit again
				if (itemLimit > 0)
					itemLimit--;
				notifyCurrencyListeners(removed.getCurrency());
			}
			notifyListeners();
		}
	}

	private void removeItemEffects(Item removed) {
		if (removed == wielded) {
			wielded.setWielded(false);
			wieldedStateChanged(wielded);
			wielded = null;
			if (weaponLock)
				// Removing the 'locked' weapon will cancel the lock
				weaponLock = false;
			getWielded(true);
		}
		else if (removed == worn) {
			worn.setWorn(false);
			wornStateChanged(worn);
			worn = null;
			if (armourLock)
				// See above
				armourLock = false;
			getWorn(true);
		}
		if (removed.getType() == Item.TOOL_TYPE) {
			Item.Tool t = (Item.Tool)removed;
			int a1 = t.getAbility(), a2 = a1;
			if (a1 == Adventurer.ABILITY_ALL) {
				a1 = Adventurer.ABILITY_CHARISMA;
				a2 = Adventurer.ABILITY_THIEVERY;
			}
			for (int a = a1; a <= a2; a++) {
				if (a == Adventurer.ABILITY_COMBAT) continue;
				if (tools[a] == t) {
					tools[a] = null;
					getEffects().removeStatRelated(a, t);
					getTool(a, true);
				}
			}
		}
		adjustEffects(removed, Effect.TYPE_AURA, false);	
		adjustEffects(removed, Effect.TYPE_TOOL, false);
	}
	
	public void removeAll() { removeAll(true); }
	
	public void removeAll(boolean respectKeepTag) {
		int itemCount = getItemCount();
		if (itemCount == 0) return;

		if (respectKeepTag) {
			System.out.println("Removing items, respecting keep tag");
			for (int i = items.size() - 1; i >= 0; i--)
				if (!getItem(i).hasKeepTag())
					removeItem(i);
				else
					System.out.println("Keep tag on item " + getItem(i));
		}
		else {
			if (itemLimit > 0) {
				// Make sure item limit is reduced if any money items are present
				boolean anyMoneyItems = false;
				for (int i = 0; i < items.size(); i++)
					if (getItem(i).isMoney()) {
						itemLimit--;
						anyMoneyItems = true;
					}
				
				if (anyMoneyItems)
					notifyCurrencyListeners(null);
			}

			items.clear();
			fireIntervalRemoved(this, 0, itemCount - 1);

			if (getAdventurer() != null) {
				wielded = null;
				worn = null;
				for (int a = 0; a < Adventurer.ABILITY_COUNT; a++)
					tools[a] = null;
	
				weaponLock = armourLock = false;
				getEffects().removeAllItems();
				fireIntervalAdded(this, MAX_ITEM_COUNT - itemCount, MAX_ITEM_COUNT - 1);
			}
		}

		notifyListeners();
	}

	public Item.Weapon getWielded() { return getWielded(false); }
	public Item.Weapon getWielded(boolean findNew) {
		if (wielded == null && findNew) {
			int maxBonus = -1;
			for (int i = 0; i < getItemCount(); i++) {
				Item item = getItem(i);
				if (item.canBeWielded()) {
					int bonus = ((Item.Weapon)item).getBonus();
					if (bonus > maxBonus) {
						maxBonus = bonus;
						wielded = (Item.Weapon)item;
					}
				}
			}

			if (wielded != null) {
				wielded.setWielded(true);
				wieldedStateChanged(wielded);
			}
		}
		return wielded;
	}

	public Item.Armour getWorn() { return getWorn(false); }
	public Item.Armour getWorn(boolean findNew) {
		if (worn == null && findNew) {
			int maxBonus = 0;
			for (int i = 0; i < getItemCount(); i++) {
				Item item = getItem(i);
				if (item.getType() == Item.ARMOUR_TYPE) {
					int bonus = ((Item.Armour)item).getBonus();
					if (bonus > maxBonus) {
						maxBonus = bonus;
						worn = (Item.Armour)item;
					}
				}
			}

			if (worn != null) {
				worn.setWorn(true);
				wornStateChanged(worn);
			}
		}
		return worn;
	}

	public Item.Tool getTool(int ability) { return getTool(ability, false); }
	/**
	 * Find the best tool for one of the abilities (probably not COMBAT).
	 * If the player doesn't currently have any tools related to that ability,
	 * this method will always look through the item list for one.
	 * This isn't expensive, but should probably be avoided.
	 * @param ability one of the basic 6 ability types.
	 * @param findNew whether to look for a valid tool, if none is currently selected
	 */
	public Item.Tool getTool(int ability, boolean findNew) {
		if (tools[ability] == null && findNew) {
			int maxBonus = 0;
			for (int i = 0; i < getItemCount(); i++) {
				Item item = getItem(i);
				if (item.getType() == Item.TOOL_TYPE) {
					Item.Tool t = (Item.Tool)item;
					if (t.getAbility() == ability && t.getBonus() > maxBonus) {
						tools[ability] = t;
						maxBonus = t.getBonus();
					}
				}
			}

			if (tools[ability] != null)
				getEffects().addStatRelated(ability, tools[ability]);
		}
		return tools[ability];
	}

	/**
	 * Notification that an item has been modified 'under the radar'.
	 * Besides checking its document, its effects may have changed.
	 * @param i
	 */
	public void modifiedItem(Item i) {
		int index = items.indexOf(i);
		if (index >= 0) {
			fireContentsChanged(this, index, index);
			switch (i.getType()) {
			case Item.ARMOUR_TYPE:
				Item.Armour a = (Item.Armour)i;
				if (a == worn)
					wornStateChanged(a);
				break;
			case Item.TOOL_TYPE:
				Item.Tool t = (Item.Tool)i;
				int ability = t.getAbility();
				int a1 = ability, a2 = ability;
				if (ability == Adventurer.ABILITY_ALL) {
					a1 = Adventurer.ABILITY_CHARISMA;
					a2 = Adventurer.ABILITY_THIEVERY;
				}
				for (int ai = a1; ai <= a2; ai++) {
					if (ai == Adventurer.ABILITY_COMBAT) continue;
					if (tools[ai] == null || tools[ai].getBonus() < t.getBonus()) {
						if (tools[ai] != null)
							getEffects().removeStatRelated(ai, tools[ai]);
						tools[ai] = t;
						getEffects().addStatRelated(ai, tools[ai]);
					}
				}
				if (!t.canBeWielded())
					break;
			case Item.WEAPON_TYPE:
				Item.Weapon w = (Item.Weapon)i;
				if (w == wielded)
					wieldedStateChanged(w);
				break;
			}

			adjustEffects(i, Effect.TYPE_AURA, true);
			adjustEffects(i, Effect.TYPE_TOOL, true);
			notifyListeners();
		}
	}

	private JList configuredList = null;
	private MouseListener listListener = null;
	public void configureList(JList list) {
		if (list.getModel() instanceof ItemList) {
			ItemList oldModel = (ItemList)list.getModel();
			list.removeMouseListener(oldModel.listListener);
			oldModel.configuredList = null;
		}

		list.setModel(this);
		list.setCellRenderer(new ItemRenderer());
		list.addMouseListener(listListener = new MouseListener());
		list.addMouseMotionListener(listListener);
		configuredList = list; // useful to have a pointer here
	}

	private class ItemRenderer extends DocumentCellRenderer {
		protected Color getBackground(JList list, int index, boolean selected) {
			Item item = getItem(index);
			if (item != null && (item == wielded || item == worn)) {
				return (selected ? new Color(127, 207, 127) : new Color(127, 255, 127));
			}
			else
				return super.getBackground(list, index, selected);
		}
	}

	private static final String dropCommand = "drop",
		wieldCommand = "wield",
		unwieldCommand = "unwield",
		wearCommand = "wear",
		unwearCommand = "unwear",
		sellCommand = "sell",
		useCommand = "use",
		transferCommand = "transfer",
		xmlCommand = "xml",
		messageCommand = "message";

	private static JMenuItem createMenuItem(String text, String command, ActionListener l) {
		JMenuItem item = new JMenuItem(text);
		item.setActionCommand(command);
		item.addActionListener(l);
		return item;
	}

	private class MouseListener extends MouseAdapter implements ActionListener, MouseMotionListener {
		private int currentIndex;
		private Item currentItem;
		private void changeWieldedState(int index) {
			if (!canChangeWeapons()) return;
			
			Item.Weapon w = (Item.Weapon)getItem(index);
			boolean wieldNow = !w.isWielded();
			if (wielded != null) {
				// Un-wield the current weapon first
				wielded.setWielded(false);
				wieldedStateChanged(wielded);
				int oldIndex = items.indexOf(wielded);
				wielded = null;
				fireContentsChanged(ItemList.this, oldIndex, oldIndex);
			}
					
			if (wieldNow) {
				w.setWielded(true);
				wielded = w;
				wieldedStateChanged(w);
			}

			fireContentsChanged(ItemList.this, index, index);
			getEffects().notifyOwner();
			FLApp.getSingle().actionTaken();
		}

		private void changeWornState(int index) {
			if (armourLock) return;
			
			Item.Armour a = (Item.Armour)getItem(index);
			boolean wearNow = !a.isWorn();
			if (worn != null) {
				worn.setWorn(false);
				wornStateChanged(worn);
				int oldIndex = items.indexOf(worn);
				worn = null;
				fireContentsChanged(ItemList.this, oldIndex, oldIndex);
			}

			if (wearNow) {
				a.setWorn(true);
				worn = a;
				wornStateChanged(a);
			}

			fireContentsChanged(ItemList.this, index, index);
			getEffects().notifyOwner();
			FLApp.getSingle().actionTaken();
		}

		private UseEffect getUseEffect(Item i) {
			Effect e = i.getEffects();
			return (e == null ? null : (UseEffect)e.firstEffect(Effect.TYPE_USE));
		}

		private int getItemIndex(Point p) {
			int index = configuredList.locationToIndex(p);
			if (index >= 0 &&
				getItem(index) != null &&
				configuredList.getCellBounds(index, index).contains(p))
				return index;
			return -1;
		}

		public void mouseMoved(MouseEvent evt) {
			FLApp.getSingle().setToolTipContext(configuredList);
			FLApp.getSingle().setMouseAtX(evt.getX());
			FLApp.getSingle().setMouseAtY(evt.getY());
			// TODO: Will I also need to update these in the actionPerformed() method
			// (which handles popup MenuItems)?
		}
		public void mouseDragged(MouseEvent evt) {
			mouseMoved(evt);
		}
		
		public void mouseClicked(MouseEvent evt) {
			if (evt.getClickCount() == 2) {
				// Double-click should trigger main event for that item
				int index = getItemIndex(evt.getPoint());
				if (index < 0) return;
				Item item = getItem(index);

				if (getAdventurer() != null) {
					// Possessions - default is to 'use' that item
					if (item.canBeWielded()) {
						changeWieldedState(index);
					}
					else if (item.getType() == Item.ARMOUR_TYPE) {
						changeWornState(index);
					}
					else if (item.isMoney() && getItemCache() != null) {
						doDeposit(index);
					}
					else {
						UseEffect effect = getUseEffect(item);
						if (effect != null && effect.canUse()) {
							boolean result = effect.use();
							FLApp.getSingle().actionTaken();
							if (!result)
								removeItem(index);
							else {
								item.updateDocument();
								fireContentsChanged(this, index, index);
							}
						}
					}
				}
				else {
					// Cache - default is to transfer the item to the player
					if (frozen) return;
					
					removeItem(index);
					FLApp.getSingle().getAdventurer().getItems().addItem(item);
					FLApp.getSingle().actionTaken();
				}
			}
		}

		private String reason = null;
		private void handlePopup(MouseEvent evt) {
			int index = getItemIndex(evt.getPoint());
			if (index < 0) return;
			Item item = getItem(index);
			currentItem = item;
			currentIndex = index;

			JPopupMenu itemMenu = new JPopupMenu("Item Options");
			if (getAdventurer() != null) {
				ItemList cache = getItemCache();
				if (cache != null) {
					// Transferring items is a possibility
					JMenuItem transferItem;
					IndexSet matches = getItemCacheNode().getLegalItems();
					if (!matches.contains(index) && matches.getExcludedReason(index) != null) {
						transferItem = createMenuItem("Transfer", messageCommand, this);
						reason = matches.getExcludedReason(index);
					}
					else {
						transferItem = createMenuItem("Transfer", transferCommand, this);
						if (itemCache.isFrozen() ||
							!matches.contains(index) ||
							(item.canBeWielded() &&
							 ((Item.Weapon)item).isWielded() &&
							 !canChangeWeapons()))
							transferItem.setEnabled(false);
					}
					itemMenu.add(transferItem);
				}

				UseEffect effect = getUseEffect(item);
				if (effect != null) {
					JMenuItem useItem = createMenuItem(effect.getVerb(), useCommand, this);
					if (!effect.canUse())
						useItem.setEnabled(false);
					itemMenu.add(useItem);
				}

				if (item.canBeWielded()) {
					JMenuItem wieldItem =
						((Item.Weapon)item).isWielded() ?
								createMenuItem("Unwield", unwieldCommand, this) :
								createMenuItem("Wield", wieldCommand, this);
					if (!canChangeWeapons())
						wieldItem.setEnabled(false);
					itemMenu.add(wieldItem);
					itemMenu.addSeparator();
				}
				else if (item.getType() == Item.ARMOUR_TYPE) {
					JMenuItem wearItem =
						((Item.Armour)item).isWorn() ?
								createMenuItem("Take off", unwearCommand, this) :
									createMenuItem("Wear", wearCommand, this);
					if (armourLock)
						wearItem.setEnabled(false);
					itemMenu.add(wearItem);
					itemMenu.addSeparator();
				}

				if (item.isSaleable()) {
					int money = item.getSalePrice();
					String text = "Sell for " + money + " ";
					text += ((SectionNode)FLApp.getSingle().getRootNode()).getMarketCurrency();
					if (money != 1)
						text += "s";
					itemMenu.add(createMenuItem(text, sellCommand, this));
				}

				JMenuItem dropItem = createMenuItem("Drop", dropCommand, this);
				if (item.getCurse() != null ||
					(item.canBeWielded() && ((Item.Weapon)item).isWielded() && !canChangeWeapons()) ||
					(armourLock && item.getType() == Item.ARMOUR_TYPE && ((Item.Armour)item).isWorn()))
					dropItem.setEnabled(false);
				itemMenu.add(dropItem);
			}
			else {
				if (!frozen)
					itemMenu.add(createMenuItem(item.isMoney() ? "Take..." : "Transfer", transferCommand, this));
			}

			if (FLApp.debugging)
				itemMenu.add(createMenuItem("XML output", xmlCommand, this));
			
			if (itemMenu.getComponentCount() > 0)
				itemMenu.show(configuredList, evt.getX(), evt.getY());
		}

		public void mousePressed(MouseEvent evt) {
			if (evt.isPopupTrigger())
				handlePopup(evt);
		}
		public void mouseReleased(MouseEvent evt) {
			if (evt.isPopupTrigger())
				handlePopup(evt);
		}

		public void actionPerformed(ActionEvent evt) {
			if (currentItem == null || currentIndex < 0 || getItem(currentIndex) != currentItem) return;
			int index = currentIndex;
			Item item = currentItem;
			currentIndex = -1;
			currentItem = null;
			String command = evt.getActionCommand();
			if (command.equals(dropCommand) || command.equals(sellCommand)) {
				removeItem(index);
				if (command.equals(sellCommand)) {
					int price = item.getSalePrice();
					if (price > 0) {
						String currency = ((SectionNode)FLApp.getSingle().getRootNode()).getMarketCurrency();
						adjustMoney(price, currency);
						//getAdventurer().adjustMoney(price);
					}
					item.soldItem();
				}
				FLApp.getSingle().actionTaken();
			}
			else if (command.equals(wieldCommand) || command.equals(unwieldCommand)) {
				changeWieldedState(index);
			}
			else if (command.equals(wearCommand) || command.equals(unwearCommand)) {
				changeWornState(index);
			}
			else if (command.equals(useCommand)) {
				UseEffect effect = getUseEffect(item);
				if (effect != null) {
					boolean result = effect.use();
					if (!result)
						removeItem(index);
					else {
						item.updateDocument();
						fireContentsChanged(this, index, index);
					}
				}
				FLApp.getSingle().actionTaken();
			}
			else if (command.equals(transferCommand)) {
				if (item.isMoney()) {
					if (getAdventurer() != null) {
						doDeposit(index);
						return;
					}
					
					// Taking money from cache - choose money amount
					MoneyChooser chooser = new MoneyChooser(FLApp.getSingle(), "Transfer Money", "How much do you want to take:", 0, item.getMoney());
					chooser.setVisible(true);

					if (chooser.getResult() == 0) return;
					if (FLApp.getSingle().getAdventurer().getItems().adjustMoney(chooser.getResult(), item.getCurrency())) {
						adjustMoney(-chooser.getResult(), item.getCurrency());
						FLApp.getSingle().actionTaken();
					}
					return;
				}
				
				boolean added = false;
				if (getAdventurer() == null)
					added = FLApp.getSingle().getAdventurer().getItems().addItem(item);
				else 
					added = getItemCache().addItem(item);
				
				if (added) {
					removeItem(index);
					FLApp.getSingle().actionTaken();
				}
			}
			else if (command.equals(xmlCommand)) {
				try {
					item.outputXML(System.out, "");
				}
				catch (IOException ioe) {
					System.err.println("Error in outputting item " + item.toDebugString());
					ioe.printStackTrace();
				}
			}
			else if (command.equals(messageCommand)) {
				JOptionPane.showMessageDialog(configuredList, reason, "Can't Transfer Item", JOptionPane.INFORMATION_MESSAGE);
				reason = null;
			}
		}
		
		private void doDeposit(int index) {
			ItemList cache = getItemCache();
			if (cache != null) {
				// Choose money amount
				Item moneyItem = getItem(index);
				MoneyChooser chooser = new MoneyChooser(FLApp.getSingle(), "Transfer Money", "How much do you want to leave here:", 0, moneyItem.getMoney());
				chooser.setVisible(true);

				if (chooser.getResult() == 0) return;
				if (cache.adjustMoney(chooser.getResult(), moneyItem.getCurrency())) {
					adjustMoney(-chooser.getResult(), moneyItem.getCurrency());
					FLApp.getSingle().actionTaken();
				}
			}
		}
	}

	public String toDebugString() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < getItemCount(); i++) {
			if (i > 0)
				sb.append('\n');
			sb.append(" Item " + (i+1) + ": " + getItem(i).toDebugString());
		}
		return sb.toString();
	}

	public void eventOccurred(GameEvent evt) {
		if (evt.getID() == GameEvent.NEW_SECTION) {
			System.out.println("ItemList got NewSection event");
			armourLock = false;
			weaponLock = false;
		}
	}

	/* ****************
	 * Loadable methods
	 **************** */
	public String getXMLTag() {
		return "items";
	}

	public void storeAttributes(Properties atts, int flags) {
		if (name != null)
			atts.setProperty("name", name);
	}

	public Iterator<XMLOutput> getOutputChildren() {
		LinkedList<XMLOutput> l = new LinkedList<XMLOutput>();
		for (int i = 0; i < getItemCount(); i++)
			l.add(getItem(i));
		return l.iterator();
	}

	public void outputTo(PrintStream out, String indent, int flags) throws IOException {
		Node.output(this, out, indent, flags);
	}
}

