package flands;

import javax.swing.text.Element;

import org.xml.sax.Attributes;

/**
 * Modify which Items can be matched by a parent Node.
 * Item matching can be modified by a series of child ItemFilterNodes, which may include
 * or exclude items.
 * 
 * @author Jonathan Mann
 */
public class ItemFilterNode extends Node {
	public static final String IncludeName = "include";
	public static final String ExcludeName = "exclude";
	private final boolean include;
	private Item item;
	//private ItemList cache;
	private String reason;
	
	public ItemFilterNode(boolean include, Node parent) {
		super(include ? IncludeName : ExcludeName, parent);
		this.include = include;
	}
	
	public boolean isInclude() { return include; }
	public int[] getMatchedItems() {
		return getItems().findMatches(item);
	}
	public void filterItems(IndexSet set) {
		int[] matches = getMatchedItems();
		if (include)
			set.add(matches);
		else
			set.remove(matches, reason);
	}
	
	public void init(Attributes atts) {
		item = Item.createItem(atts);
		if (item == null) {
			System.err.println("Error: FilterNode didn't include item attributes!");
			item = new Item("&%"); // match nothing
		}
		reason = atts.getValue("reason");
		
		super.init(atts);
	}

	protected Element createElement() { return null; }
}
