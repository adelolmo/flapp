package flands;


import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.Element;

import org.xml.sax.Attributes;

/**
 * Limits the number of items, belonging to a named group, that can be taken
 * in one section. Apparently unused.
 * @author Jonathan Mann
 */
public class ItemGroupNode extends Node {
	public static String ElementName = "items";

	private static List<ItemGroupNode> groupNodes;
	private static void addGroupNode(ItemGroupNode node) {
		if (groupNodes == null)
			groupNodes = new LinkedList<ItemGroupNode>();
		groupNodes.add(node);
	}

	public static ItemGroupNode getGroupNode(String name) {
		if (groupNodes != null) {
			for (Iterator<ItemGroupNode> i = groupNodes.iterator(); i.hasNext(); ) {
				ItemGroupNode n = i.next();
				if (n.name.equals(name))
					return n;
			}
		}
		return null;
	}

	private static void removeGroupNode(ItemGroupNode node) {
		groupNodes.remove(node);
	}

	private String name;
	private int limit = 0;

	public ItemGroupNode(Node parent) {
		super(ElementName, parent);
		addGroupNode(this);
	}

	public void init(Attributes atts) {
		name = atts.getValue("group");
		limit = getIntValue(atts, "limit", 0);
	}

	public int getLimit() { return limit; }
	public void adjustLimit(int delta) {
		limit += delta;
		if (delta != 0)
			fireChangeEvent();
	}

	private List<ChangeListener> listeners;
	public void addChangeListener(ChangeListener l) {
		if (listeners == null)
			listeners = new LinkedList<ChangeListener>();
		listeners.add(l);
	}
	public void removeChangeListener(ChangeListener l) {
		listeners.remove(l);
	}
	protected void fireChangeEvent() {
		if (listeners != null) {
			ChangeEvent e = new ChangeEvent(this);
			for (Iterator<ChangeListener> i = listeners.iterator(); i.hasNext(); )
				i.next().stateChanged(e);
		}
	}

	public void dispose() {
		removeGroupNode(this);
	}

	protected Element createElement() { return null; } // invisible

	protected void saveProperties(Properties props) {
		super.saveProperties(props);
		props.setProperty("limit", Integer.toString(limit));
	}
	protected void loadProperties(Attributes atts) {
		super.loadProperties(atts);
		limit = getIntValue(atts, "limit", limit);
	}
}