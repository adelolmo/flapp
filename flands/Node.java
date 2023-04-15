package flands;


import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.text.AbstractDocument.AbstractElement;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;

import org.xml.sax.Attributes;

/**
 * Superclass of a hierarchy of program nodes.
 * Some nodes represent purely textual elements, others represent purely
 * control-flow artifacts, while others are mixed.  I'm working this out as I go along,
 * so hopefully it comes together.
 * 
 * @author Jonathan Mann
 */
public abstract class Node implements XMLOutput, Expression.Resolver {
	protected String nodeName;
	private Node parent;
	private List<Node> children;
	private boolean noElement = false;
	private Element element = null;
	protected boolean enabled = true;
	protected boolean hidden = false;

	private List<Element> enableElements = null;

	public Node(String name, Node parent) {
		this.nodeName = name;
		this.parent = parent;
		this.children = new ArrayList<Node>();
	}

	public Node getParent() { return parent; }
	public SectionNode getRoot() {
		Node n = this;
		while (n.getParent() != null)
			n = n.getParent();
		try {
			return (SectionNode)n;
		}
		catch (ClassCastException cce) {
			return null;
		}
	}
	public String getSectionName() { return parent.getSectionName(); }
	public Iterator<Node> getChildren() { return children.iterator(); }
	public int getChildCount() { return children.size(); }
	public Node getChild(int index) { return children.get(index); }

	protected void insertChild(int index, Node n) { children.add(index, n); }

	protected void addChild(Node n) { children.add(n); }

	public boolean isEnabled() {
		if (!enabled) return false;
		if (parent == null) return true;
		return parent.isEnabled();
	}
	public void setEnabled(boolean b) {
		if (enabled != b) {
			//System.out.println("Node (" + name + ").setEnabled("+b+")");
			enabled = b;
			enabledStateChange();
		}
	}
	/**
	 * Called to set up the node tree for reading.
	 * Used by SectionBrowser.
	 */
	public void enableAll() {
		setEnabled(true);
		for (Iterator<Node> i = getChildren(); i.hasNext(); )
			i.next().enableAll();
	}

	protected void addEnableElements(Element[] es) {
		if (enableElements == null)
			enableElements = new LinkedList<Element>();
		for (int i = 0; i < es.length; i++) {
			if (!enabled)
				makeEnabled(es[i], false);
			enableElements.add(es[i]);
		}
	}

	protected void enabledStateChange() {
		Element[] es;

		if (enableElements != null) {
			es = new Element[enableElements.size()];
			enableElements.toArray(es);
			//System.out.println("Will enable " + es.length + " elements");
		}
		else if (element != null) {
			es = new Element[] { element };
			//System.out.println("Will enable Node's main element");
		}
		else
			return;
		
		getDocument().grabWriteLock();
		for (int i = 0; i < es.length; i++)
			makeEnabled(es[i], enabled);
		getDocument().releaseWriteLock();

		getDocument().fireChangeEvents(es);
	}

	protected void makeEnabled(Element e, boolean b) {
		if (e instanceof AbstractElement) {
			if (b)
				((AbstractElement)e).removeAttribute(StyleConstants.Foreground);
			else
				((AbstractElement)e).addAttribute(StyleConstants.Foreground, Color.gray);
		}

	}

	/**
	 * Get the grouper used to step through child Executables.
	 * Most Nodes don't need their own, and so the default here returns <code>null</code>.
	 */
	public ExecutableGrouper getExecutableGrouper() { return null; }
	/**
	 * Travel up the tree until a parent Node is found that returns a non-null
	 * ExecutableGrouper.  This is a utility method.
	 */
	protected final ExecutableGrouper findExecutableGrouper() {
		Node n = getParent();
		while (n != null) {
			ExecutableGrouper grouper = n.getExecutableGrouper();
			if (grouper != null)
				return grouper;
			n = n.getParent();
		}
		return null;
	}

	/**
	 * This implementation creates any known Node type and adds it as a child
	 * to this Node (unless it is a StyleNode).
	 * @return the child created.
	 */
	protected Node createChild(String name) {
		Node child;
		if (name.equals(ParagraphNode.ElementName))
			child = new ParagraphNode(this);
		else if (name.equals("choices") || name.equals("table"))
			child = new TableNode(name, this);
		else if (name.equals(ChoiceNode.ElementName))
			child = new ChoiceNode(this);
		else if (name.equals(OutcomesTableNode.ElementName))
			child = new OutcomesTableNode(this);
		else if (name.equals(GotoNode.ElementName))
			child = new GotoNode(this);
		else if (name.equals(ReturnNode.ElementName))
			child = new ReturnNode(this);
		else if (name.equals(RandomNode.ElementName))
			child = new RandomNode(this);
		else if (name.equals(OutcomeNode.ElementName))
			child = new OutcomeNode(this);
		else if (name.equals(SectionNode.ElementName))
			child = new SectionNode(this);
		else if (name.equals(IfNode.ElementName) ||
			     name.equals(IfNode.ElseIfElementName) ||
			     name.equals(IfNode.ElseElementName))
			child = new IfNode(name, this);
		else if (name.equals(TickNode.TickElementName) || name.equals(TickNode.GainElementName))
			child = new TickNode(name, this);
		else if (name.equals(LoseNode.ElementName))
			child = new LoseNode(this);
		else if (name.equals(DifficultyNode.ElementName))
			child = new DifficultyNode(this);
		else if (name.equals(TrainingNode.ElementName))
			child = new TrainingNode(this);
		else if (name.equals(DifficultyResultNode.SuccessElementName))
			child = new DifficultyResultNode(true, this);
		else if (name.equals(DifficultyResultNode.FailureElementName))
			child = new DifficultyResultNode(false, this);
		else if (name.equals(RankCheckNode.ElementName))
			child = new RankCheckNode(this);
		else if (name.equals(FightNode.ElementName))
			child = new FightNode(this);
		else if (name.equals(FightNode.RoundNode.ElementName))
			child = FightNode.createRoundNode(this);
		else if (name.equals(FightNode.DamageNode.ElementName))
			child = FightNode.createDamageNode(this);
		else if (name.equals(FightNode.FleeNode.ElementName))
			child = FightNode.createFleeNode(this);
		else if (name.equals(TrainingNode.ElementName))
			child = new TrainingNode(this);
		else if (name.equals(MarketNode.ElementName))
			child = new MarketNode(this);
		else if (name.equals(TradeNode.ElementName))
			child = new TradeNode(this);
		else if (name.equals(TradeNode.BuyNode.ElementName))
			child = new TradeNode.BuyNode(this);
		else if (name.equals(TradeNode.SellNode.ElementName))
			child = new TradeNode.SellNode(this);
		else if (name.equals(RestNode.ElementName))
			child = new RestNode(this);
		else if (name.equals(AdjustNode.ElementName))
			child = new AdjustNode(this);
		else if (name.equals(FieldNode.ElementName))
			child = new FieldNode(this);
		else if ((child = ItemNode.createItemNode(name, this)) != null)
			; // since we've already just created it
		else if ((child = CurseNode.createCurseNode(name, this)) != null)
			;
		else if (name.equals(GroupNode.ElementName))
			child = new GroupNode(this);
		else if (name.equals(TextNode.TextElementName))
			child = new TextNode(this, true);
		else if (name.equals(TextNode.DescElementName))
			child = new TextNode(this, false);
		else if (name.equals(SetVarNode.ElementName))
			child = new SetVarNode(this);
		else if (name.equals(WhileNode.ElementName))
			child = new WhileNode(this);
		else if (name.equals(RerollNode.ElementName))
			child = new RerollNode(this);
		else if (name.equals(ItemGroupNode.ElementName))
			child = new ItemGroupNode(this);
		else if (name.equals(CacheNode.ItemElementName) || name.equals(CacheNode.MoneyElementName))
			child = new CacheNode(name, this);
		else if (name.equals(CacheNode.AdjustMoneyNode.ElementName))
			child = new CacheNode.AdjustMoneyNode(this);
		else if (name.equals(ResurrectionNode.ElementName))
			child = new ResurrectionNode(this);
		else if (name.equals(PriceNode.ElementName))
			child = new PriceNode(this);
		else if (name.equals(TransferNode.ElementName))
			child = new TransferNode(this);
		else if (name.equals(ItemFilterNode.IncludeName) ||
				 name.equals(ItemFilterNode.ExcludeName))
			child = new ItemFilterNode(name.equals(ItemFilterNode.IncludeName), this);
		else if (name.equals(ImageNode.ElementName))
			child = new ImageNode(this);
		else if ((child = HeadingNode.createHeadingNode(name, this)) != null)
			;
		else if (name.equals(RowNode.ElementName))
			child = new RowNode(this);
		else if (name.equals(RowNode.CellNode.ElementName))
			child = new RowNode.CellNode(this);
		else if (name.equals(StyleNode.BoldElementName))
			return new StyleNode.Bold(this);
		else if (name.equals(StyleNode.ItalicElementName))
			return new StyleNode.Italic(this);
		else if (name.equals(StyleNode.CapsElementName))
			return new StyleNode.Caps(this);
		else if (name.equals(StyleNode.UnderlineElementName))
			return new StyleNode.Underline(this);
		// Getting down to the really rare Node types now...
		else if (name.equals(ExtraChoice.ElementName))
			child = new ExtraChoice(this);
		else if (name.equals(SectionViewNode.ElementName))
			child = new SectionViewNode(this);
		else {
			System.out.println("Failed to recognise node for element: " + name);
			child = new UnrecognisedNode(name, this);
		}

		addChild(child);
		return child;
	}

	/**
	 * Creates an appropriate child Node given the name and attributes.
	 * This node is then added as a child, for most types of node (notably not StyleNodes).
	 */
	public static Node createNode(String name, Node parent) {
		Node n;
		if (parent == null) {
			if (name.equals(SectionNode.ElementName))
				n = new SectionNode();
			else if (name.equals(LoadableNode.ElementName))
				n = new LoadableNode();
			else {
				System.out.println("createNode() called when parent=null, " + name + " is not a valid root element");
				n = null;
			}
		}
		else
			n = parent.createChild(name);

		return n;
	}

	private static class UnrecognisedNode extends Node {
		public UnrecognisedNode(String name, Node parent) { super(name, parent); }
		protected Element createElement() { return null; }
	}

	/**
	 * Called directly after createNode by the ContentHandler on the node returned.
	 * The node can access known attributes, and do any other initialisation duties.
	 * This default implementation sets the related Element by calling {@link #createElement()}
	 */
	public void init(Attributes xmlAtts) {
		hidden = getBooleanValue(xmlAtts, "hidden", false);
		element = createElement();
		if (element == null)
			noElement = true;
	}

	public static int getIntValue(Attributes xmlAtts, String name, int defaultVal) {
		if (xmlAtts != null) {
			String val = xmlAtts.getValue(name);
			if (val != null)
				try {
					return Integer.parseInt(val);
				}
				catch (NumberFormatException nfe) {}
		}
		return defaultVal;
	}

	public static boolean getBooleanValue(Attributes xmlAtts, String name, boolean defaultVal) {
		if (xmlAtts != null) {
			String val = xmlAtts.getValue(name);
			if (val != null) {
				if (val.toLowerCase().startsWith("t") || val.startsWith("1"))
					return true;
				else if (val.toLowerCase().startsWith("f") || val.startsWith("0"))
					return false;
				else
					System.out.println("Unrecognised attribute value for " + name + ": " + val);
			}
		}
		return defaultVal;
	}

	/**
	 * Indicates the type of logical operation used in the last argument
	 * passed to split.
	 * If <code>true</code>, it was an AND;
	 * if <code>false</code>, an OR.
	 */
	protected static boolean andSplitter;
	/**
	 * Split a set of strings passed in an attribute value.
	 * These will be separated by a '|' or a '&', to indicate logical OR or AND.
	 * The logical condition used will be cached in andSplitter until
	 * this method is called next.
	 */
	protected static String[] split(String val) {
		String[] results;
		if (val == null)
			return null;
		else if (val.indexOf('|') >= 0) { // or
			andSplitter = false;
			results = val.split("\\|");
		}
		else if (val.indexOf('&') >= 0) { // and
			andSplitter = true;
			results = val.split("&");
		}
		else
			results = new String[] { val };

		for (int s = 0; s < results.length; s++)
			results[s] = results[s].trim();

		return results;
	}

	protected static String concatenate(String[] strs, boolean andSplitter) {
		StringBuffer sb = new StringBuffer(strs[0]);
		for (int i = 1; i < strs.length; i++) {
			sb.append(andSplitter ? '&' : '|');
			sb.append(strs[i]);
		}
		return sb.toString();
	}
	
	/**
	 * Always called between elements, with a string that may be empty
	 * but always contains all accumulated content between two tags.
	 */
	public void handleContent(String str) {}

	/**
	 * Returns <code>true</code> if child nodes without content shouldn't create
	 * their default content. This is for use by GroupNodes and similar cases,
	 * where Nodes are grouped with one overall description (given elsewhere).
	 */
	public boolean hideChildContent() { return false; }
	
	/**
	 * Handle the 'end tag' of this node.
	 * @return <code>true</code> if any content was added to the document by this node;
	 * <code>false</code> otherwise.
	 */
	public boolean handleEndTag() {
		if (enableElements != null && enableElements.size() > 0) return true;
		return false;
	}

	/**
	 * Dispose of the node.
	 * This was added so we could disconnect any listeners.
	 * This implementation recursively calls all known child Nodes;
	 * any subclasses should remember to call any indirectly-used Nodes.
	 */
	public void dispose() {
		for (Iterator<Node> i = getChildren(); i.hasNext(); ) {
			try {
				i.next().dispose();
			}
			catch (NullPointerException npe) {
				StackTraceElement[] stack = npe.getStackTrace();
				System.out.println("Node.dispose(): NPE at " + stack[0] + "\n  at " + stack[1]);
			}
		}
	}

	/** Trim whitespace from the beginning of the String only. */
	public static String trimStart(String str) {
		int len = str.length();
		int i = 0;
		for (; i < len; i++)
			if (str.charAt(i) > ' ')
				return str.substring(i);
		return "";
	}

	/** Trim whitespace from the end of the String only. */
	public static String trimEnd(String str) {
		int len = str.length() - 1;
		for (; len >= 0; len--)
			if (str.charAt(len) > ' ')
				return str.substring(0, len+1);
		return "";
	}

	public static final int BOLD_STYLE = Font.BOLD;
	public static final int ITALIC_STYLE = Font.ITALIC;
	public static final int CAPS_STYLE = 256; // additional mask, to go with BOLD and ITALIC
	public static final String CapsAttribute = "caps";

	public SectionDocument getDocument() { return getParent().getDocument(); }
	public static final String ViewTypeAttribute = "view";
	public static final String BoxYViewType = "boxy";
	public static final String BoxXViewType = "boxx";
	public static final String TableViewType = "table";
	public static final String ParagraphViewType = "p";
	public static final String ComponentViewType = "comp";
	public static final String RowViewType = "row";
	public static final String ImageViewType = "image";
	public static final String NoViewType = "null";
	protected String getElementViewType() { return null; }

	public static String getViewType(Element e) {
		AttributeSet atts = e.getAttributes();
		if (atts.isDefined(ViewTypeAttribute))
			return atts.getAttribute(ViewTypeAttribute).toString();
		else
			return null;
	}

	public static void setViewType(MutableAttributeSet atts, String viewType) {
		atts.addAttribute(ViewTypeAttribute, viewType);
	}

	public static View createViewFor(Element e) {
		String viewType = getViewType(e);
		if (viewType != null) {
			if (viewType.equals(ParagraphViewType)) {
				/*System.out.println("Node creating AdvancedParagraphView for " + e);*/
				return new AdvancedParagraphView(e);
			}
			else if (viewType.equals(BoxYViewType)) {
				/*System.out.println("Node creating BoxYView for " + e);*/
				return new flands.BoxView(e, View.Y_AXIS);
			}
			else if (viewType.equals(BoxXViewType)) {
				return new flands.BoxView(e, View.X_AXIS);
			}
			else if (viewType.equals(TableViewType)) {
				/*System.out.println("Node creating TableView for " + e);*/
				return new TableView(e);
			}
			else if (viewType.equals(ComponentViewType)) {
				/*System.out.println("Node creating ComponentView for " + e);*/
				return new ComponentView(e) {
					public float getMaximumSpan(int axis) {
						return super.getPreferredSpan(axis);
					}
				};
			}
			else if (viewType.equals(NoViewType)) {
				/*System.out.println("Node deliberately not creating a View for " + e);*/
				return null;
			}
			else if (viewType.equals(ImageViewType)) {
				return new ImageView(e);
			}
		}
		return null;
	}
	
	public static final Object ImageAttribute = "image";
	public static Image getImage(AttributeSet a) {
		return (Image)a.getAttribute(ImageAttribute);
	}
	public static void setImage(MutableAttributeSet a, Image i) {
		a.addAttribute(ImageAttribute, i);
	}
	
	public Element getElement() {
		if (element == null && !noElement) {
			element = createElement();
			if (element == null)
				noElement = true;
		}

		return (noElement ? getParent().getElement() : element);
	}

	/**
	 * Default behaviour: creates a branch element, assigns it a view type,
	 * adds it as a child to the parent node's element, and returns the branch.
	 */
	protected Element createElement() {
		Element parentElement = getParent().getElement();
		MutableAttributeSet atts = getElementStyle();
		String viewType = getElementViewType();
		if (viewType != null) {
			if (atts == null)
				atts = new SimpleAttributeSet();
			atts.addAttribute(ViewTypeAttribute, viewType);
		}
		SectionDocument.Branch branch = getDocument().createBranchElement(parentElement, atts);
		if (branch != null) {
			if (parentElement != null)
				((SectionDocument.Branch)parentElement).addChild(branch);
		}
		return branch;
	}

	protected MutableAttributeSet getElementStyle() {
		return null;
	}

	protected AttributeSet modifyEnabledAttributes(AttributeSet atts) {
		MutableAttributeSet atts2;
		if (atts instanceof MutableAttributeSet)
			atts2 = (MutableAttributeSet)atts;
		else
			atts2 = new SimpleAttributeSet(atts);
		if (!isEnabled()) {
			StyleConstants.setItalic(atts2, true);
			StyleConstants.setForeground(atts2, Color.darkGray);
		}
		return atts2;
	}

	public boolean isStyleNode() { return false; }

	public Element[] getLeaves() {
		SectionDocument.Branch element = (SectionDocument.Branch)getElement();
		return element.getLeaves();
	}

	/* ****************************
	 * Variable maintenance methods
	 **************************** */
	public boolean isVariableDefined(String name) { return getRoot().isVariableDefined(name); }
	public int getVariableValue(String name) { return getRoot().getVariableValue(name); }
	public void setVariableValue(String name, int value) { getRoot().setVariableValue(name, value); }
	public void adjustVariableValue(String name, int delta) { getRoot().adjustVariableValue(name, delta); }
	public void removeVariable(String name) { getRoot().removeVariable(name); }
	public int resolveIdentifier(String ident) { return getVariableValue(ident); }

	/**
	 * Get a variable's value at runtime.
	 * If the given value can be parsed as an integer, that number will be returned.
	 * Otherwise it will be used as a variable name, and the value of that variable returned.
	 */
	public int getAttributeValue(String attValue) {
		try {
			return Integer.parseInt(attValue);
		}
		catch (NumberFormatException nfe) {
			if (attValue.startsWith("-"))
				return -getVariableValue(attValue.substring(1));
			else
				return getVariableValue(attValue);
		}
	}

	public String getDockLocation() {
		SectionNode root = getRoot();
		return (root == null ? null : root.getDockLocation());
	}

	/* Convenience methods */
	static Adventurer dummyAdventurer = null;
	static Adventurer getAdventurer() {
		Adventurer adv = FLApp.getSingle().getAdventurer();
		if (adv != null) return adv;
		
		if (dummyAdventurer == null)
			dummyAdventurer = new Adventurer();
		return dummyAdventurer;
	}
	static Codewords getCodewords() { return getAdventurer().getCodewords(); }
	static ItemList getItems() { return getAdventurer().getItems(); }
	static BlessingList getBlessings() { return getAdventurer().getBlessings(); }
	static CurseList getCurses() { return getAdventurer().getCurses(); }
	static ShipList getShips() { return getAdventurer().getShips(); }
	static Flag.Set getFlags() { return getAdventurer().getFlags(); }

	public IndexSet findItemMatches() {
		IndexSet result = modifyItemMatches(null);
		if (result == null) {
			result = new IndexSet();
			result.addAll(getItems().getItemCount());
		}
		return result;
	}
	
	public IndexSet modifyItemMatches(int[] matches) {
		IndexSet matchSet = null;
		if (matches != null)
			matchSet = new IndexSet(matches);
		for (Iterator<Node> i = getChildren(); i.hasNext(); ) {
			Node n = i.next();
			if (n instanceof ItemFilterNode) {
				ItemFilterNode filter = (ItemFilterNode)n;
				if (matchSet == null) {
					matchSet = new IndexSet();
					if (!filter.isInclude())
						// First filter was an exclude - so include all to start with
						matchSet.addAll(getItems().getItemCount());
				}
				filter.filterItems(matchSet);
			}
		}

		return matchSet;
	}
	
	/* ****************************************************
	 * Loading/saving dynamic properties (for saved games).
	 **************************************************** */

	/**
	 * Load and apply dynamic properties from the given attribute set.
	 * This default implementation handles the node's enabled state.
	 */
	protected void loadProperties(Attributes props) {
		setEnabled(getBooleanValue(props, "enabled", isEnabled()));
	}

	protected static void saveProperty(Properties props, String name, boolean value) {
		props.setProperty(name, value ? "1" : "0");
	}

	protected static void saveProperty(Properties props, String name, int value) {
		props.setProperty(name, Integer.toString(value));
	}
	
	/**
	 * Store the values of all dynamic properties in the given properties object,
	 * for output. This default implementation outputs the enabled state.
	 */
	protected void saveProperties(Properties props) {
		saveProperty(props, "enabled", enabled);
	}

	/**
	 * Save static properties to the passed object. This is generally used for nodes
	 * within items or curses; e.g. a treasure map with a use effect might trigger
	 * a GotoNode - when the game is saved, the attributes of that node need to be
	 * saved along with the item details.
	 * 'outit' is the opposite of 'init'; a poor pun, but it makes
	 * the name memorable.
	 */
	protected void outit(Properties props) {}
	
	protected void saveVarProperty(Properties props, String propName, String varStr) {
		if (varStr != null) {
			// Property is defined
			props.setProperty(propName, varStr); // fall-through case - overwritten if incorrect
			if (!Character.isDigit(varStr.charAt(0))) {
				boolean negate = false;
				if (varStr.charAt(0) == '-') {
					if (Character.isDigit(varStr.charAt(1))) { // assume string is more than "-"
						// negative integer - we've already stored it correctly
						return;
					}
					negate = true;
					varStr = varStr.substring(1);
				}
				if (isVariableDefined(varStr)) {
					int val = getVariableValue(varStr);
					if (negate) val = -val;
					saveProperty(props, propName, val);
				}
				else
					System.out.println("Node.outit: couldn't resolve variable " + varStr + " for output; may be intentional.");
			}
		}
	}
	
	public void outputTo(PrintStream out, String indent, int flags) throws IOException {
		output(this, out, indent, flags);
	}
	
	public void outputStaticNode(PrintStream out, String indent) throws IOException {
		outputTo(out, indent, XMLOutput.OUTPUT_PROPS_STATIC | XMLOutput.OUTPUT_PROPS_DYNAMIC);
	}
	
	/* ************************
	 * XMLOutput implementation
	 ************************ */
	public String getXMLTag() { return nodeName; }
	public Iterator<XMLOutput> getOutputChildren() {
		return XMLCast(getChildren());
	}
	
	/**
	 * Create an Iterator<XMLOutput> from an Iterator<Node>.
	 */
	protected static Iterator<XMLOutput> XMLCast(Iterator<Node> i) {
		return new XMLCasterIterator(i);
	}
	
	/**
	 * I can't cast an Iterator<Node> to an Iterator<XMLOutput>, even though they
	 * Node implements XMLOutput. I can't see a way around this, so I've created
	 * this wrapped to do the work.
	 */
	private static final class XMLCasterIterator implements Iterator<XMLOutput> {
		private Iterator<Node> i;
		private XMLCasterIterator(Iterator<Node> i) { this.i = i; }
		public boolean hasNext() { return i.hasNext(); }
		public XMLOutput next() {
			Node n = i.next();
			return n;
		}
		public void remove() { i.remove(); }
	}
	
	public void storeAttributes(Properties atts, int flags) {
		if ((flags & XMLOutput.OUTPUT_PROPS_STATIC) != 0)
			outit(atts);
		if ((flags & XMLOutput.OUTPUT_PROPS_DYNAMIC) != 0)
			saveProperties(atts);
	}

	public static void output(XMLOutput obj, PrintStream out, String indent, int flags) throws IOException {
		out.print(indent);
		out.print("<");
		out.print(obj.getXMLTag());
		
		Properties atts = new Properties();
		obj.storeAttributes(atts, flags);
		outputAttributes(out, atts);
		
		Iterator<XMLOutput> i = obj.getOutputChildren();
		if (i != null && i.hasNext()) {
			out.println(">");
			
			String innerIndent = indent + "  ";
			while (i.hasNext())
				i.next().outputTo(out, innerIndent, flags);
			
			out.print(indent);
			out.print("</");
			out.print(obj.getXMLTag());
			out.println('>');
		}
		else
			out.println("/>");
	}

	public static void outputAttributes(PrintStream out, Properties atts) throws IOException {
		for (Iterator<Map.Entry<Object,Object>> i = atts.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry e = i.next();
			out.print(" ");
			out.print(e.getKey());
			out.print("=\"");
			out.print(e.getValue());
			out.print('"');
		}
	}	
}