package flands;

import java.awt.event.ActionEvent;
import java.util.Iterator;
import java.util.Properties;
import java.util.Map.Entry;

import javax.swing.JOptionPane;
import javax.swing.text.Element;
import javax.swing.text.StyledDocument;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Action node to automatically transfer items between caches and the character.
 * Convenient when we want possessions to be temporarily removed from the character.
 * 
 * -@author Jonathan Mann
 */
public class TransferNode extends ActionNode implements Executable, Flag.Listener {
	public static final String ElementName = "transfer";
	private String from, to;
	private Item include, exclude;
	private boolean includeAll;
	private String limit;
	private String shards;
	private String price;
	private boolean forced;
	
	public TransferNode(Node parent) {
		super(ElementName, parent);
		setEnabled(false);
	}

	private static final String ExcludePrefix = "x";
	public void init(Attributes atts) {
		from = atts.getValue("from");
		to = atts.getValue("to");
		
		include = Item.createItem(atts);
		if (include != null &&
			include.getType() == Item.PLAIN_TYPE &&
			include.getName().equals("*"))
			includeAll = true;
		limit = atts.getValue("limit");

		AttributesImpl exAtts = new AttributesImpl();
		for (int i = atts.getLength(); i >= 0; i--) {
			System.out.println("Attribute " + i + ": uri=" + atts.getURI(i) + ", localname=" + atts.getLocalName(i) + ", QName=" + atts.getQName(i) + ", type=" + atts.getType(i) + ", value=" + atts.getValue(i));
			String qname = atts.getQName(i);
			if (qname != null && qname.startsWith(ExcludePrefix)) {
				exAtts.addAttribute(atts.getURI(i),
									atts.getLocalName(i),
									qname.substring(ExcludePrefix.length()),
									atts.getType(i),
									atts.getValue(i));
			}
		}
		exclude = Item.createItem(exAtts);
		
		shards = atts.getValue("shards");
		
		price = atts.getValue("price");
		if (price != null) {
			getFlags().addListener(price, this);
		}
		
		hidden = getBooleanValue(atts, "hidden", false);
		forced = getBooleanValue(atts, "force", true);
		
		super.init(atts);
	}
	
	protected void outit(Properties props) {
		super.outit(props);
		if (from != null) props.setProperty("from", from);
		if (to != null) props.setProperty("to", to);
		if (include != null) include.saveProperties(props);
		if (limit != null) saveVarProperty(props, "limit", limit);
		if (exclude != null) {
			Properties excludeProps = new Properties();
			exclude.saveProperties(excludeProps);
			for (Iterator<Entry<Object,Object>> i = excludeProps.entrySet().iterator(); i.hasNext(); ) {
				Entry<Object,Object> e = i.next();
				props.put(ExcludePrefix + e.getKey(), e.getValue());
			}
		}
		if (shards != null) saveVarProperty(props, "shards", shards);
		if (price != null) props.setProperty("price", price);
		if (hidden) saveProperty(props, "hidden", true);
		if (!forced) saveProperty(props, "force", false);
	}
	
	private boolean hadContent = false;
	public void handleContent(String content) {
		if (!hadContent && content.trim().length() == 0)
			return;
		hadContent = true;
		
		Element[] leaves = getDocument().addLeavesTo(getElement(), new StyledText[] {
			new StyledText(content, createStandardAttributes()) });
		addEnableElements(leaves);
		addHighlightElements(leaves);
	}
	
	public boolean handleEndTag() {
		findExecutableGrouper().addExecutable(this);
		return super.handleEndTag();
	}
	
	private ItemList getItems(String cache) {
		if (cache == null)
			return getAdventurer().getItems();
		else
			return CacheNode.getItemCache(cache);
	}
	
	private ItemList getFromItems() { return getItems(from); }
	private ItemList getToItems() { return getItems(to); }
	
	private int[] getItemIndices() {
		if (include == null && !includeAll)
			return new int[0];
		
		ItemList items = getFromItems();
		int[] indices;
		int count = 0;
		if (includeAll) {
			indices = new int[items.getItemCount()];
			for (int i = 0; i < items.getItemCount(); i++)
				if (!items.getItem(i).hasKeepTag() || from != null)
					indices[count++] = i;
		}
		else  {
			indices = items.findMatches(include);
			count = indices.length;
		}
		
		if (exclude != null) {
			for (int i = count - 1; i >= 0; i--) {
				if (exclude.matches(items.getItem(indices[i])))
					System.arraycopy(indices, i+1, indices, i, --count - i);
			}
		}
		
		if (count < indices.length) {
			int[] temp = indices;
			indices = new int[count];
			System.arraycopy(temp, 0, indices, 0, count);
		}
		
		return indices;
	}

	private int getFromShards() {
		if (from == null)
			return getAdventurer().getMoney();
		else {
			ItemList items = CacheNode.getItemCache(from);
			int index = items.getMoneyItem();
			return (index < 0 ? 0 : items.getItem(index).getMoney());
		}
	}
	
	private boolean callContinue = false;
	public boolean execute(ExecutableGrouper grouper) {
		if (price != null) {
			flagChanged(price, getFlags().getState(price));
			return true;
		}

		if (actionDoesAnything()) {
			if (hidden) {
				actionPerformed(null);
				return true;
			}
			setEnabled(true);
			if (forced) {
				callContinue = true;
				return false;
			}
			else
				return true;
		}
		else {
			setEnabled(false);
			return true;
		}
	}

	private boolean actionDoesAnything() {
		int[] items = getItemIndices();
		return (items.length > 0 || (shards != null && getFromShards() > 0));
	}
	
	private boolean canPayInFull() {
		int[] items = getItemIndices();
		return (items.length > 0 &&
				(limit == null || items.length >= getAttributeValue(limit)) &&
				(shards == null || getFromShards() >= getAttributeValue(shards)));
	}
	
	public void resetExecute() { setEnabled(false); }
	
	public void actionPerformed(ActionEvent evt) {
		int[] indices = getItemIndices();
		ItemList fromItems = getFromItems();
		ItemList toItems = getToItems();
		if (limit != null && indices.length > getAttributeValue(limit)) {
			if (!fromItems.areItemsSame(indices)) {
				// Choose which item(s) to lose
				int itemCount = getAttributeValue(limit);
				StyledDocument[] itemDocs = new StyledDocument[indices.length];
				for (int i = 0; i < itemDocs.length; i++)
					itemDocs[i] = fromItems.getItem(indices[i]).getDocument();
				
				String chooserTitle = (itemCount == 1 ? "Choose an item to lose" : "Choose " + itemCount + " items to lose");
				DocumentChooser chooser = new DocumentChooser(FLApp.getSingle(), chooserTitle, itemDocs, itemCount > 1);
				chooser.setVisible(true);
				int[] selections = chooser.getSelectedIndices();
				if (selections.length > itemCount || (selections.length < itemCount && selections.length < fromItems.getItemCount())) {
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
		
		for (int i = indices.length - 1; i >= 0; i--) {
			System.out.println("Transferring item " + indices[i]);
			Item item = fromItems.getItem(indices[i]);
			fromItems.removeItem(indices[i]);
			toItems.addItem(item);
		}
		
		if (shards != null) {
			int money = getFromShards();
			if (!shards.equals("*"))
				money = Math.min(getAttributeValue(shards), money);
			if (from == null)
				getAdventurer().adjustMoney(-money);
			else {
				Item deductionItem = Item.createMoneyItem(-money);
				fromItems.addItem(deductionItem);
			}
			
			if (to == null)
				getAdventurer().adjustMoney(money);
			else {
				Item additionItem = Item.createMoneyItem(money);
				toItems.addItem(additionItem);
			}
		}
		
		setEnabled(false);
		
		if (price != null)
			getFlags().setState(price, true);
		
		if (callContinue) {
			callContinue = false;
			findExecutableGrouper().continueExecution(this, false);
		}
		else
			callsContinue = false;
	}

	public void flagChanged(String name, boolean state) {
		if (price != null && price.equals(name))
			setEnabled(!state && canPayInFull());
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
		if (include != null || exclude != null) {
			text = "Transfer ";
			if (limit != null)
				text += "at least " + getAttributeValue(limit);
			text += "items";
		}
		
		if (shards != null) {
			if (text == null)
				text = "Transfer ";
			else
				text += " and ";
			if (shards.equals("*"))
				text += "all your";
			else
				text += getAttributeValue(shards);
			text += " Shards";
		}
		
		if (from == null)
			text += " from your possession";
		else
			text += " from the cache [" + from + "]";
		
		if (to == null)
			text += " to your possession";
		else
			text += " to the cache [" + to + "]";
		
		return text;
	}
	
	public void dispose() {
		if (price != null) {
			getFlags().removeListener(price, this);
		}
	}
}
