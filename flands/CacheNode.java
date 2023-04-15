package flands;


import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.xml.sax.Attributes;

/**
 * A cache node is one in which the character can store items or money.
 * Each cache has a name, and can be accessed from any location by using that
 * same name (hence the book and section number are usually used as a name).
 * The contents of all used caches are part of each saved game.
 * 
 * @author Jonathan
 */
public class CacheNode extends Node implements ActionListener, MouseListener, ChangeListener {
	public static final String ItemElementName = "itemcache";
	public static final String MoneyElementName = "moneycache";
	private static Map<String,ItemList> loadedItemCaches;
	private static Map<String,Integer> loadedMoneyCaches;
	private static Map<String, List<ChangeListener> > cacheListeners =
		new HashMap<String, List<ChangeListener> > ();
	
	static ItemList getItemCache(String name) {
		return getItemCache(name, true);
	}
	private static ItemList getItemCache(String name, boolean create) {
		if (loadedItemCaches == null)
			loadedItemCaches = new HashMap<String,ItemList>();
		ItemList items = loadedItemCaches.get(name);
		if (items == null && create) {
			items = new ItemList(name);
			loadedItemCaches.put(name, items);
		}
		return items;
	}
	static Iterator<ItemList> getItemCaches() {
		if (loadedItemCaches == null)
			loadedItemCaches = new HashMap<String,ItemList>();
		return loadedItemCaches.values().iterator();
	}

	static int getMoneyCache(String name) {
		// See if there's an ItemCache of the same name - this takes priority
		ItemList items = getItemCache(name, false);
		if (items != null) {
			int moneyIndex = items.getMoneyItem();
			return (moneyIndex < 0 ? 0 : items.getItem(moneyIndex).getMoney());
		}
		
		if (loadedMoneyCaches == null)
			loadedMoneyCaches = new HashMap<String,Integer>();
		Integer shards = loadedMoneyCaches.get(name);
		if (shards == null) {
			shards = Integer.valueOf(0);
			loadedMoneyCaches.put(name, shards);
		}
		return shards.intValue();
	}
	static void setMoneyCache(String name, int amount) {
		// Check if there's an ItemCache of the same name - it takes priority
		ItemList items = getItemCache(name, false);
		if (items != null) {
			int moneyIndex = items.getMoneyItem();
			if (amount == 0) {
				if (moneyIndex >= 0)
					items.removeItem(moneyIndex);
			}
			else if (moneyIndex < 0)
				items.addItem(Item.createMoneyItem(amount));
			else
				items.getItem(moneyIndex).setMoney(amount);
			return;
		}

		if (loadedMoneyCaches == null)
			loadedMoneyCaches = new HashMap<String,Integer>();
		loadedMoneyCaches.put(name, Integer.valueOf(amount));
		for (Iterator<ChangeListener> i = getCacheListeners(name).iterator(); i.hasNext(); )
			i.next().stateChanged(new ChangeEvent(name));
	}
	static Iterator<Map.Entry<String,Integer>> getMoneyCaches() {
		if (loadedMoneyCaches == null)
			loadedMoneyCaches = new HashMap<String,Integer>();
		return loadedMoneyCaches.entrySet().iterator();
	}
	
	private static Set<String> frozenCaches = null;
	private static boolean isCacheFrozen(String name) {
		return (frozenCaches == null ? false : frozenCaches.contains(name));
	}
	static void setCacheFrozen(String name, boolean b) {
		if (b) {
			if (frozenCaches == null)
				frozenCaches = new HashSet<String>();
			frozenCaches.add(name);
		}
		else if (frozenCaches != null)
			frozenCaches.remove(name);
		
		ItemList items = getItemCache(name, false);
		if (items != null)
			items.setFrozen(b);
	}
	
	static void clearCaches() {
		if (loadedItemCaches != null)
			loadedItemCaches.clear();
		if (loadedMoneyCaches != null)
			loadedMoneyCaches.clear();
		cacheListeners.clear();
		if (frozenCaches != null)
			frozenCaches.clear();
	}
	
	public static void addCacheListener(String cache, ChangeListener l) {
		getCacheListeners(cache).add(l);
	}
	public static void removeCacheListener(String cache, ChangeListener l) {
		getCacheListeners(cache).remove(l);
	}
	private static List<ChangeListener> getCacheListeners(String name) {
		List<ChangeListener> l = cacheListeners.get(name);
		if (l == null) {
			l = new LinkedList<ChangeListener>();
			cacheListeners.put(name, l);
		}
		return l;
	}

	public static final int ITEM_CACHE = 0;
	public static final int MONEY_CACHE = 1;
	private int cacheType;
	private String name;
	private String text;
	private ItemList items;
	private int shards;
	private float withdrawCharge = 0f;
	private int moneyStep = 1;
	private int moneyLimit = -1;
	private int itemLimit = -1;
	private JTextField shardsField;

	public CacheNode(String name, Node parent) {
		super(name, parent);
		if (name.equals(ItemElementName))
			cacheType = ITEM_CACHE;
		else if (name.equals(MoneyElementName))
			cacheType = MONEY_CACHE;
	}

	public ItemList getCachedItems() { return items; }

	public void init(Attributes xmlAtts) {
		name = xmlAtts.getValue("name");
		text = xmlAtts.getValue("text");
		String val = xmlAtts.getValue("withdrawCharge");
		if (val != null)
			try {
				withdrawCharge = Float.parseFloat(val);
			}
			catch (NumberFormatException nfe) {
				System.err.println("Bad number for withdraw charge: " + val);
			}
		moneyStep = getIntValue(xmlAtts, "multiples", 1);
		moneyLimit = getIntValue(xmlAtts, "max", -1);
		itemLimit = getIntValue(xmlAtts, "itemlimit", -1);

		super.init(xmlAtts);

		SectionDocument.Branch element = (SectionDocument.Branch)getElement();

		switch (cacheType) {
			case ITEM_CACHE:
				items = getItemCache(name);
				getItems().setItemCache(this);
				if (itemLimit >= 0)
					items.setItemLimit(itemLimit);
				break;
			case MONEY_CACHE:
				shards = getMoneyCache(name);
				break;
		}
		addCacheListener(name, this);

		SimpleAttributeSet labelAtts = new SimpleAttributeSet();
		StyleConstants.setItalic(labelAtts, true);
		StyleConstants.setAlignment(labelAtts, StyleConstants.ALIGN_CENTER);
		setViewType(labelAtts, ParagraphViewType);
		SectionDocument.Branch labelBranch = getDocument().createBranchElement(element, labelAtts);
		getDocument().addLeavesTo(labelBranch, new String[] { new String(text + "\n") }, null);
		element.addChild(labelBranch);

		Component cacheComp = null;
		switch (cacheType) {
			case ITEM_CACHE:
				JLabel hackLabel = new JLabel("Splint armour (Defence +4)"); // a nice length
				hackLabel.setFont(SectionDocument.getPreferredFont());

				JList itemList = new JList(items);
				itemList.setFont(SectionDocument.getPreferredFont());
				items.configureList(itemList);
				//itemList.setToolTipText("Double-click to take item; double-click in your Possessions to leave an item here");
				JScrollPane itemPane = new JScrollPane(itemList);
				itemPane.setPreferredSize(new Dimension(hackLabel.getPreferredSize().width, 300));
				// a better way to do this would be to size it according to the parent window (FLApp.getSingle())
				// we'd also need to listen for resize events in this case...
				cacheComp = itemPane;
				break;
			case MONEY_CACHE:
				shardsField = new JTextField(Integer.toString(shards), 6);
				shardsField.setFont(SectionDocument.getPreferredFont());
				shardsField.setEditable(false);
				shardsField.setHorizontalAlignment(JTextField.RIGHT);
				shardsField.addMouseListener(this);
				shardsField.setToolTipText("Right-click to withdraw or deposit money; double-click for default action");
				cacheComp = shardsField;
				SectionDocument.addComponentFontUser(shardsField);
				break;
		}
		SimpleAttributeSet compAtts = new SimpleAttributeSet();
		StyleConstants.setComponent(compAtts, cacheComp);
		setViewType(compAtts, ComponentViewType);
		SectionDocument.Branch compBranch = getDocument().createBranchElement(element, compAtts);
		getDocument().addLeavesTo(compBranch, new StyledText[] { new StyledText("x\n", compAtts) });
		element.addChild(compBranch);
	}

	public boolean isFrozen() { return isCacheFrozen(name); }
	
	public IndexSet getLegalItems() {
		IndexSet itemMatches = findItemMatches();
		if (moneyLimit == 0) {
			ItemList items = getItems();
			for (int i = items.getItemCount() - 1; i >= 0; i--)
				if (items.getItem(i).isMoney())
					itemMatches.remove(i, "You can't leave any money here!");
		}
		return itemMatches;
	}
	
	private static final String withdrawCommand = "w";
	private static final String depositCommand = "d";
	private void handlePopupEvent(MouseEvent evt) {
		if (isFrozen()) return;
		
		doingPopup = true;
		JPopupMenu menu = new JPopupMenu();
		if (shards > 0) {
			JMenuItem item = new JMenuItem("Withdraw...");
			item.setActionCommand(withdrawCommand);
			item.addActionListener(this);
			menu.add(item);
		}
		if (getAdventurer().getMoney() > 0) {
			JMenuItem item = new JMenuItem("Deposit...");
			item.setActionCommand(depositCommand);
			item.addActionListener(this);
			menu.add(item);
		}
		menu.show(shardsField, evt.getX(), evt.getY());
	}
	public void mouseEntered(MouseEvent evt) {}
	public void mouseExited(MouseEvent evt) {}

	private boolean doingPopup = false;
	public void mousePressed(MouseEvent evt) {
		if (evt.isPopupTrigger())
			handlePopupEvent(evt);
	}
	public void mouseReleased(MouseEvent evt) {
		if (evt.isPopupTrigger())
			handlePopupEvent(evt);
		else
			doingPopup = false;
	}
	public void mouseClicked(MouseEvent evt) {
		if (evt.getClickCount() == 1 && !doingPopup) {
			if (isFrozen()) return;
			
			if (shards > 0)
				// Withdraw
				doWithdraw();
			else
				doDeposit();
		}
	}
	public void actionPerformed(ActionEvent evt) {
		if (evt.getActionCommand().equals(withdrawCommand))
			doWithdraw();
		else if (evt.getActionCommand().equals(depositCommand))
			doDeposit();
	}

	private void updateShards(int delta) {
		shards += delta;
		setMoneyCache(name, shards);
	}

	private void doWithdraw() {
		MoneyChooser moneyChooser;
		if (moneyStep == 1)
			moneyChooser = new MoneyChooser(FLApp.getSingle(), "Withdraw Money", "How much do you want to withdraw:", 0, shards, withdrawCharge, 1);
		else {
			int min = shards % moneyStep;
			moneyChooser = new MoneyChooser(FLApp.getSingle(), "Withdraw Money", "How much do you want to withdraw:", min, min, shards, moneyStep);
		}
		moneyChooser.setVisible(true);

		if (moneyChooser.getResult() > 0) {
			updateShards(-moneyChooser.getResult());
			getAdventurer().adjustMoney(moneyChooser.getResultLessCharge());
		}
	}
	private void doDeposit() {
		MoneyChooser moneyChooser;
		if (moneyStep == 1) {
			int maxDeposit = getAdventurer().getMoney();
			if (moneyLimit >= 0)
				maxDeposit = Math.min(maxDeposit, moneyLimit - shards);
			moneyChooser = new MoneyChooser(FLApp.getSingle(), "Deposit Money", "How much do you want to deposit:", 0, maxDeposit, 0f, moneyStep);
		}
		else {
			int minDeposit = moneyStep - (shards % moneyStep);
			if (minDeposit == moneyStep)
				minDeposit = 0;
			int maxDeposit = getAdventurer().getMoney();
			if (moneyLimit >= 0)
				maxDeposit = Math.min(maxDeposit, moneyLimit - shards);
			maxDeposit = moneyStep * ((maxDeposit - minDeposit)/moneyStep);
			moneyChooser = new MoneyChooser(FLApp.getSingle(), "Deposit Money", "How much do you want to deposit:", minDeposit, minDeposit, maxDeposit, moneyStep);
		}
		moneyChooser.setVisible(true);

		if (moneyChooser.getResult() > 0) {
			updateShards(moneyChooser.getResult());
			getAdventurer().adjustMoney(-moneyChooser.getResult());
		}
	}

	/** Notification of a change to a money cache. */
	public void stateChanged(ChangeEvent e) {
		if (e.getSource().equals(name)) {
			shards = getMoneyCache(name);
			if (shardsField == null)
				;
			else
				shardsField.setText(Integer.toString(shards));
		}
	}
	
	public void dispose() {
		if (items != null)
			getItems().removeItemCache(this);
		removeCacheListener(name, this);
		if (shardsField != null)
			SectionDocument.removeComponentFontUser(shardsField);
	}

	protected String getElementViewType() { return BoxYViewType; }
	protected MutableAttributeSet getElementStyle() {
		return new SimpleAttributeSet();
	}

	/**
	 * Multiply the money held in a cache by a floating-point number.
	 * This is used for investments and bets.
	 * The 'name' attribute can also be missing, in which case the player's money
	 * is modified.
	 */
	public static class AdjustMoneyNode extends ActionNode implements Executable {
		public static final String ElementName = "adjustmoney";

		private String name;
		private float multiplier = 1f;
		private boolean forced;

		public AdjustMoneyNode(Node parent) {
			super(ElementName, parent);
			setEnabled(false);
		}

		public void init(Attributes atts) {
			name = atts.getValue("name");
			if (name == null)
				name = atts.getValue("cache");
			String val = atts.getValue("multiply");
			if (val != null)
				try {
					multiplier = Float.parseFloat(val);
				}
				catch (NumberFormatException nfe) {
					System.err.println("Bad value for multiply: " + val);
				}
			forced = getBooleanValue(atts, "force", true);
			
			super.init(atts);
		}

		public void handleContent(String text) {
			Element[] leaves = getDocument().addLeavesTo(getElement(), new StyledText[] { new StyledText(text, createStandardAttributes()) });
			addEnableElements(leaves);
			addHighlightElements(leaves);
		}
		public boolean handleEndTag() {
			findExecutableGrouper().addExecutable(this);
			return super.handleEndTag();
		}

		private int getMoney() {
			if (name != null)
				return getMoneyCache(name);
			else
				return getAdventurer().getMoney();
		}
		private void setMoney(int amount) {
			if (name != null)
				setMoneyCache(name, amount);
			else
				getAdventurer().setMoney(amount);
		}
		
		private boolean callContinue = false;
		public boolean execute(ExecutableGrouper grouper) {
			if (grouper != findExecutableGrouper())
				System.err.println("AdjustMoneyNode.execute(): non-matching groupers");
			setEnabled(true);
			int amount = getMoney();
			if (amount == 0)
				return true;
			
			if (forced) {
				callContinue = true;
				return false;
			}
			else
				return true;
		}

		private int undoAmount = -1;
		public void actionPerformed(ActionEvent evt) {
			setEnabled(false);
			int amount = getMoney();
			undoAmount = amount;
			amount = (int)(multiplier * amount);
			setMoney(amount);
			UndoManager.getCurrent().add(this);
			
			if (callContinue)
				findExecutableGrouper().continueExecution(this, false);
			else
				callsContinue = false;
		}

		public void resetExecute() {
			if (undoAmount >= 0) {
				System.out.println("Undoing adjustmoney, multiply=" + multiplier);
				setMoney(undoAmount);
				undoAmount = -1;
			}
			setEnabled(false);
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
			String text = "Multiplies ";
			if (name == null)
				text += "your Shards";
			else
				text += "the money in cache [" + name + "]";
			text += " by " + multiplier;
			return text;
		}
	}
}
