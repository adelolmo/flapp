package flands;

import java.io.IOException;
import java.util.Iterator;

import javax.swing.text.Element;

import org.xml.sax.Attributes;

/**
 * Contains all XML-based information in a saved game - the items, curses,
 * and caches.
 * @author Jonathan Mann
 */
public class LoadableNode extends Node  {
	public static final String ElementName = "saved";
	
	public LoadableNode() {
		super(ElementName, null);
	}
	
	public void init(Attributes atts) {
		// Clear out the caches before reading in the saved ones
		CacheNode.clearCaches();
		super.init(atts);
	}
	
	protected Node createChild(String name) {
		Node child = null;
		if (name.equals(ItemListNode.ElementName)) {
			child = new ItemListNode();
		}
		else if (name.equals(CurseListNode.ElementName)) {
			child = new CurseListNode();
		}
		else if (name.equals(MoneyCacheNode.ElementName)) {
			child = new MoneyCacheNode();
		}
		
		if (child != null)
			addChild(child);
		else
			System.err.println("LoadableNode.createChild(" + name + "): what sort of child node is this?");
		
		return child;
	}
	
	protected Element createElement() {
		SectionDocument.RootElement root = getDocument().createRootElement();
		return root;
	}
	
	/** Catch and ignore attempts to add an Executable. */
	public ExecutableGrouper getExecutableGrouper() {
		return new ExecutableRunner();
	}
	
	private SectionDocument dummyDoc;
	public SectionDocument getDocument() {
		if (dummyDoc == null) {
			dummyDoc = new SectionDocument();
			dummyDoc.grabWriteLock();
		}
		return dummyDoc;
	}

	public boolean handleEndTag() {
		if (dummyDoc != null)
			dummyDoc.releaseWriteLock();
		return false;
	}
	
	/**
	 * Variable manipulation methods.
	 * Just in case these get called via a UseEffect, we'll define
	 * 'empty' versions of these methods that are normally handled by SectionNode.
	 */
	public boolean isVariableDefined(String name) { return false; }
	public int getVariableValue(String name) { return Integer.MIN_VALUE; }
	public void setVariableValue(String name, int value) {}
	public void adjustVariableValue(String name, int delta) {}
	public void removeVariable(String name) {}

	public class ItemListNode extends Node {
		public static final String ElementName = "items";
		private String listName;
		
		public ItemListNode() {
			super(ElementName, LoadableNode.this);
		}
		
		public void init(Attributes atts) {
			listName = atts.getValue("name");
			super.init(atts);
		}

		protected Element createElement() { return null; }
		
		public boolean handleEndTag() {
			ItemList items = (listName == null ?
					XMLPool.getPool().getAdventurer().getItems() :
					CacheNode.getItemCache(listName));
			
			items.removeAll(false);
			
			for (Iterator<Node> i = getChildren(); i.hasNext(); ) {
				Node n = i.next();
				if (n instanceof ItemNode) {
					Item item = ((ItemNode)n).getItem();
					System.out.print("Item in list: ");
					try {
						item.outputXML(System.out, "");
					}
					catch (IOException ioe) {}
					items.addItem(item);
				}
			}
			return false;
		}
	}
	
	public class CurseListNode extends Node {
		public static final String ElementName = "curses";
		public CurseListNode() {
			super(ElementName, LoadableNode.this);
		}
		
		protected Element createElement() { return null; }
		
		public boolean handleEndTag() {
			CurseList curses = XMLPool.getPool().getAdventurer().getCurses();
			curses.removeAll();
			
			for (Iterator<Node> i = getChildren(); i.hasNext(); ) {
				Node n = i.next();
				if (n instanceof CurseNode) {
					Curse c = ((CurseNode)n).getCurse();
					curses.addCurse(c);
				}
			}
			
			return false;
		}
	}
	
	public class MoneyCacheNode extends Node {
		public static final String ElementName = "moneycache";
		private String cacheName;
		private int amount;
		public MoneyCacheNode() {
			super(ElementName, LoadableNode.this);
		}
		public void init(Attributes atts) {
			cacheName = atts.getValue("name");
			amount = getIntValue(atts, "shards", 0);
			super.init(atts);
		}
		protected Element createElement() { return null; }
		public boolean handleEndTag() {
			if (cacheName != null && amount > 0)
				CacheNode.setMoneyCache(cacheName, amount);
			return false;
		}
	}
}
