package flands;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.text.Element;

import org.xml.sax.Attributes;

/**
 * Special hidden node attached to a market - if it matches an item being bought or sold,
 * the child action nodes will be activated.
 * 
 * @author Jonathan Mann
 */
public class TradeEventNode extends Node {
	public static final String BoughtElementName = "bought";
	public static final String SoldElementName = "sold";
	private final boolean bought;
	private final List<ActionNode> events;
	private Item item = null;
	
	public TradeEventNode(String name, Node parent) {
		super(name, parent);
		if (name.equalsIgnoreCase(BoughtElementName))
			bought = true;
		else if (name.equalsIgnoreCase(SoldElementName))
			bought = false;
		else {
			System.err.println("TradeEventNode: unrecognised element name: " + name);
			bought = false;
		}
		events = new LinkedList<ActionNode>();
	}

	protected Node createChild(String name) {
		Node n = super.createChild(name);
		if (n instanceof ActionNode) {
			System.out.println("TradeNode: adding action as child: " + n);
			events.add((ActionNode)n);
		}
		return n;
	}

	private ExecutableRunner runner = null;
	public ExecutableGrouper getExecutableGrouper() {
		if (runner == null)
			runner = new ExecutableRunner();
		return runner;
	}
	
	protected Element createElement() { return null; }

	public void setItem(Item i) {
		this.item = i;
	}

	public void init(Attributes atts) {
		if (item == null)
			item = Item.createItem(atts);
		
		super.init(atts);
	}
	public boolean hideChildContent() { return true; }
	
	public boolean handleEndTag() {
		runner = null;
		return false;
	}
	
	public void itemTraded(boolean bought, Item trade) {
		if (this.bought == bought &&
			(item == null || item.matches(trade))) {
			System.out.println("TradeEventNode triggered");
			for (Iterator<ActionNode> i = events.iterator(); i.hasNext(); )
				i.next().actionPerformed(null);
		}
	}
}
