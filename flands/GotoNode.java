package flands;


import java.awt.event.ActionEvent;
import java.util.Properties;

import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.xml.sax.Attributes;

/**
 * An action node that sends the character to a different section (and possibly book).
 * @author Jonathan Mann
 */
public class GotoNode extends ActionNode implements Executable, ChangeListener, Flag.Listener {
	public static final String ElementName = "goto";
	public static final String SectionAttribute = "section";

	private String section;
	private String book;
	private boolean forced;
	private boolean setSail;
	private boolean visit;
	private boolean dead;
	private String[] codewords;
	private boolean andCodewords;
	private boolean flee;
	private String emptyvar;
	private Node descriptionNode = null;
	private boolean executionReached = false;
	private String flag = null, price = null;
	private boolean ignoreFlags = false;
	private boolean keepEnabled = false;

	public GotoNode(Node parent) {
		this(parent, true);
	}

	public GotoNode(Node parent, boolean forced) {
		super(ElementName, parent);
		this.forced = forced;
		setEnabled(false);
	}

	void ignoreFlags(boolean b) { ignoreFlags = b; }
	
	private static final String ForcedAttribute = "force";
	public void init(Attributes atts) {
		this.book = atts.getValue("book");
		this.section = atts.getValue(SectionAttribute);
		if (section == null)
			System.err.println("GotoNode: missing section attribute!");

		setSail = getBooleanValue(atts, "sail", false);
		if (setSail) {
			getShips().addShipListener(this);
			forced = false; // by default
		}
		forced = getBooleanValue(atts, ForcedAttribute, forced);
		visit = getBooleanValue(atts, "visit", false);
		dead = getBooleanValue(atts, "dead", false);
		flee = getBooleanValue(atts, "flee", false);
		if (flee) {
			SectionNode root = getRoot();
			if (root != null)
				root.setFleeGoto(this);
		}
		codewords = OutcomeNode.split(atts.getValue("codeword"));
		andCodewords = OutcomeNode.andSplitter;
		emptyvar = atts.getValue("emptyvar");
		if (!ignoreFlags) {
			flag = atts.getValue("flag");
			price = atts.getValue("price");
			if (flag != null) {
				getFlags().addListener(flag, this);
			}
			if (price != null) {
				getFlags().addListener(price, this);
			}
		}
		keepEnabled = getBooleanValue(atts, "revisit", false);
		
		super.init(atts);
	}

	protected void outit(Properties props) {
		super.outit(props);
		if (book != null)
			props.setProperty("book", book);
		if (section != null)
			props.setProperty(SectionAttribute, section);
		if (setSail)
			saveProperty(props, "sail", true);
		if (visit)
			saveProperty(props, "visit", true);
	}
	
	/**
	 * Set up the GotoNode to allow its use directly by other Nodes.
	 * Any content (description of the Goto action) will be added to the given node.
	 * The usual 'turn to X' will be added to the local or parent's paragraph Element.
	 * This needs to be called before handleContent() is called.
	 */
	public void setDescriptionNode(Node node) {
		if (node != null)
			descriptionNode = node;
	}

	protected AttributeSet getElementAttributes(boolean isSection) {
		return getElementAttributes(isSection, false);
	}
	protected AttributeSet getElementAttributes(boolean isSection, boolean isBook) {
		SimpleAttributeSet atts = new SimpleAttributeSet();
		StyleConstants.setUnderline(atts, true);
		if (isSection)
			StyleConstants.setBold(atts, true);
		if (isBook)
			StyleConstants.setItalic(atts, true);
		StyleNode.applyActiveStyles(atts);
		addListenerTo(atts);
		return atts;
	}

	private boolean addedContent = false;
	public void handleContent(String content) {
		//content = content.trim();
		if (content.trim().length() == 0) return;

		if (descriptionNode == null) {
			// Assume it's something of the form '... turn to X ...'
			Element[] leaves = getDocument().addStyledText(getElement(), content, section, getElementAttributes(false), getElementAttributes(true));
			addedContent = true;
			addHighlightElements(leaves);
			addEnableElements(leaves);
		}
		else {
			// Pass the description of the choice off to the given description Element
			descriptionNode.handleContent(content);
		}
	}

	public boolean handleEndTag() {
		if (descriptionNode != null)
			descriptionNode.handleEndTag();

		if (!addedContent && !hidden && !getParent().hideChildContent()) {
			Element parent = getElement();
			Element[] leaves = new Element[2];
			SectionDocument.Branch branch = (SectionDocument.Branch)parent;
			if (book == null) {
				String text1 = (getDocument().isNewSentence(getDocument().getLength()) ? "Turn to " : "turn to ");
				leaves[0] = branch.addLeaf(text1, getElementAttributes(false));
			}
			else {
				String text = Books.getCanon().getBook(book).getTitle();
				leaves[0] = branch.addLeaf(text + " ", getElementAttributes(false, true));
			}

			leaves[1] = branch.addLeaf(section == null ? "MISSING" : section, getElementAttributes(true));
			addEnableElements(leaves);
			if (!gotHighlightElements()) {
				if (descriptionNode == null)
					setHighlightElements(leaves);
				else 
					setHighlightElements(descriptionNode.getLeaves());
			}
		}

		System.out.println("Goto(" + section + ") adding itself as child");
		findExecutableGrouper().addExecutable(this);
		
		return super.handleEndTag();
	}

	protected void enabledStateChange() {
		//System.out.println("GotoNode: enabledStateChange() called");
		super.enabledStateChange();
	}

	public void setEnabled(boolean b) {
		super.setEnabled(b);
		if (descriptionNode != null)
			descriptionNode.setEnabled(b);
	}
	
	public boolean canUse() {
		if (book != null && !Books.getCanon().getBook(book).hasBook())
			return false;
		if (setSail && getShips().findShipsHere().length == 0)
			return false;
		if (getAdventurer() != null && getAdventurer().isDead() != dead)
			return false;
		if (emptyvar != null && isVariableDefined(emptyvar))
			return false;
		if (flag != null && !getFlags().getState(flag))
			return false;
		if (price != null && getFlags().getState(price))
			return false;
		return true;
	}

	private boolean callContinue = false;
	public boolean execute(ExecutableGrouper grouper) {
		executionReached = true;
		if (canUse()) {
			setEnabled(true);

			if (forced /*&& !section.equals(FLApp.getSingle().getCurrentSection())*/) {
				// User must follow this goto - block further execution
				/*
				 * 9/9/07 - I commented out the extra check here, because
				 * it allowed the player to ignore the goto in 5.113.
				 * Hopefully this doesn't break something else...
				 */
				callContinue = true;
				return false;
			}
		}

		// Disabled or optional - allow execution to continue
		return true;
	}

	public void resetExecute() { setEnabled(false); }

	protected Element createElement() {
		Node parent = getParent();
		while (parent != null && !(parent instanceof ParagraphNode))
			parent = parent.getParent();

		if (parent == null)
			// We need a paragraph as a parent element
			return super.createElement();
		else
			return null;
	}

	public void actionPerformed(ActionEvent e) {
		if (!canUse()) {
			setEnabled(false);
			return;
		}
		
		int currentShipIndex = -1;
		if (setSail) {
			int[] ships = getShips().findShipsHere();
			if (ships.length > 1) {
				JOptionPane.showMessageDialog(FLApp.getSingle(), new Object[] {"You have multiple ships docked here.", "Please pick one."}, "Multiple Ships", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			System.out.println("Setting sail in ship " + ships[0]);
			currentShipIndex = ships[0];
		}
		/*
		 * Note: We have to be careful in ActionNodes that are present in item UseEffects.
		 * When loaded, they won't have any reference to a SectionNode root.
		 * Any calls that depend on finding a SectionNode root will cause exceptions.
		 */
		String todock = (getRoot() == null ? null : getRoot().getToDockLocation());
		if (todock != null) {
			// All ships at sea will move on to this dock
			System.out.println("Docking ships at " + todock);
			getShips().setAtDock(todock);
		}
		if (currentShipIndex >= 0) {
			// Except for the ship we're sailing in
			System.out.println("Setting ship " + currentShipIndex + " to be at sea");
			getShips().getShip(currentShipIndex).setDocked(null); // ie. at sea
		}
		if (codewords != null && codewords.length > 0) {
			boolean proceed = andCodewords;
			for (int c = 0; c < codewords.length; c++) {
				boolean hasCodeword = getCodewords().hasCodeword(codewords[c]);
				if (!hasCodeword && andCodewords)
					return;
				else if (hasCodeword && !andCodewords) {
					proceed = true;
					break;
				}
			}
			if (!proceed)
				return;
			// TODO:
			// This handles 4.457, in which being an initiate or paying a fee allows the goto
			// It would be better if we could be notified if the flag is triggered,
			// rather than just pretending to be enabled and ignoring the click if the
			// codeword isn't present
		}
		
		//super.actionPerformed(e); // original location of this call - did it matter?
		if (getDockLocation() != null || todock != null) {
			// ie. we're leaving a dock
			if (setSail)
				getShips().setAtSea();
			else
				getShips().setOnLand();
		}
		Address address = new Address(book, section);
		System.out.println("Goto node activated with event=" + e);
		System.out.println("I will jump to " + address + " now...");
		boolean went = FLApp.getSingle().gotoAddress(address);
		if (!went) {
			System.err.println("Goto failure: " + address);
			// TODO: Undo all the ship dock changes
			return;
		}

		super.actionPerformed(e); // tell any listeners

		if (!keepEnabled)
			setEnabled(false);
		if (callContinue)
			// This makes 'visits' work - but it's not really necessary in the majority of cases
			findExecutableGrouper().continueExecution(this, false);
		
		/* Doing this breaks 6.628 (if you immediately revisit it)
		if (flag != null)
			Flag.getFlag(flag).setState(false);
		if (price != null)
			Flag.getFlag(price).setState(true);
		*/
	}

	public void stateChanged(ChangeEvent evt) {
		// setSail must be true
		setEnabled(book == null && getShips().findShipsHere().length > 0);
	}

	public void dispose() {
		if (setSail)
			getShips().removeShipListener(this);
		if (flag != null)
			getFlags().removeListener(flag, this);
		if (price != null)
			getFlags().removeListener(price, this);
	}
	
	protected void loadProperties(Attributes atts) {
		super.loadProperties(atts);
		callContinue = getBooleanValue(atts, "continue", false);
		executionReached = getBooleanValue(atts, "reached", false);
	}
	
	protected void saveProperties(Properties props) {
		super.saveProperties(props);
		saveProperty(props, "continue", callContinue);
		saveProperty(props, "reached", executionReached);
	}
	
	protected String getTipText() {
		StringBuffer sb = new StringBuffer();
		if (setSail)
			sb.append("Sail a ship to ");
		else
			sb.append("Go to ");
		if (book != null)
			sb.append("book <b>").append(book).append("</b>, ");
		sb.append("section <b>").append(section).append("</b>");
		return sb.toString();
	}

	public void flagChanged(String name, boolean state) {
		// Execution must have reached the goto node in the normal way
		// before flag/price changes have an effect
		if (!executionReached) return;

		setEnabled(canUse());
	}
}
