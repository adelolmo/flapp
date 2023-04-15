package flands;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

import javax.swing.JCheckBox;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.xml.sax.Attributes;

/**
 * In its classic form, a choice node is a description and a section number, listed
 * at the end of a section. Other variations include a checkbox by the choice,
 * and necessary conditions for the choice to be available.
 * 
 * @author Jonathan Mann
 */
public class ChoiceNode extends Node implements Executable, ActionListener, ChangeListener {
	private ParagraphNode descriptionNode;
	private ParagraphNode gotoParagraph;
	private GotoNode gotoNode;
	private String shards;
	private String currency;
	private boolean pay = false;
	private Item item = null;
	private int profession = -1;
	private String god = null;
	private String boxword = null;
	private JCheckBox box = null;
	private String emptyvar = null;
	private boolean flee = false;
	private String book;

	public static final String ElementName = "choice";
	public ChoiceNode(Node parent) { super(ElementName, parent); }

	public void init(Attributes atts) {
		shards = atts.getValue("shards");
		currency = atts.getValue("currency");
		item = Item.createItem(atts);
		if (shards != null)
			pay = true;
		pay = getBooleanValue(atts, "pay", pay);
		gotoParagraph = new ParagraphNode(this, StyleConstants.ALIGN_RIGHT);
		gotoNode = new GotoNode(gotoParagraph, false); // unforced goto
		gotoParagraph.addChild(gotoNode);
		gotoNode.init(atts);
		if (shards != null || item != null) {
			gotoNode.addActionListener(this);
			if (shards != null) {
				if (currency == null)
					getAdventurer().addMoneyListener(this);
				else
					getItems().addCurrencyListener("Mithral", this);
			}
		}
		String val = atts.getValue("profession");
		if (val != null)
			profession = Adventurer.getProfessionType(val);
		god = atts.getValue("god");
		boxword = atts.getValue("box");
		emptyvar = atts.getValue("emptyvar");
		flee = getBooleanValue(atts, "flee", false);
		if (flee)
			getRoot().setFleeChoice(this);
		book = atts.getValue("book");

		super.init(atts);

		descriptionNode = new ParagraphNode(this, StyleConstants.ALIGN_LEFT);
		addChild(descriptionNode);
		gotoNode.setDescriptionNode(descriptionNode);
	}

	/**
	 * Not sure why ChoiceNode would be written out since a GotoNode would
	 * probably suffice - perhaps an item effect with some constraints handled here?
	 */
	protected void outit(Properties props) {
		super.outit(props);
		
		if (shards != null) saveVarProperty(props, "shards", shards);
		if (currency != null) props.setProperty("currency", currency);
		if (item != null) item.saveProperties(props);
		if (shards != null || item != null) saveProperty(props, "pay", pay);
		gotoNode.outit(props);
		if (profession >= 0) props.setProperty("profession", Adventurer.getProfessionName(profession));
		if (god != null) props.setProperty("god", god);
		if (boxword != null) props.setProperty("box", boxword);
		if (emptyvar != null) props.setProperty("emptyvar", emptyvar);
		if (flee) saveProperty(props, "flee", flee);
		if (book != null) props.setProperty("book", book);
	}
	
	public void resetExecute() {
		// TODO: What the hell? Why is this check here?
		// Added 'flee' check to end - otherwise rerolling an enemy's parting shot
		// will leave the goto flee choice enabled
		// Not sure if these checks need to be here at all, but if in doubt...
		if (shards != null || item != null || profession >= 0 || god != null || flee)
			setEnabled(false);
	}

	private static final String BoxString = "{box}";
	public void handleContent(String content) {
		// Add some more text to the choice text
		if (boxword != null && content.indexOf(BoxString) >= 0) {
			int index = content.indexOf(BoxString);
			if (index > 0)
				// Add text before the checkbox
				descriptionNode.handleContent(content.substring(0, index));
			
			// Add the checkbox
			box = new JCheckBox();
			box.setSelected(getCodewords().hasCodeword(boxword));
			box.setEnabled(false);
			box.setMargin(new Insets(0, 0, 0, 0));
			box.setAlignmentY(SectionDocument.getFontVerticalAlignment());
			getCodewords().addChangeListener(boxword, this);
			SimpleAttributeSet tickAtts = new SimpleAttributeSet();
			setViewType(tickAtts, ComponentViewType);
			//StyleConstants.setSpaceBelow(tickAtts, 6);
			StyleConstants.setComponent(tickAtts, box);
			getDocument().addLeavesTo(descriptionNode.getElement(), new StyledText[] { new StyledText("q", tickAtts) });
			
			if (index + BoxString.length() == content.length())
				return;
			else
				content = content.substring(index + BoxString.length());
		}
		
		descriptionNode.handleContent(content);
	}

	public void setEnabled(boolean b) {
		if (book != null && !Books.getCanon().getBook(book).hasBook())
			b = false;
		
		super.setEnabled(b);
		gotoNode.setEnabled(b);
		if (b) {
			Node parent = gotoNode.getParent();
			while (!gotoNode.isEnabled() && parent != null) {
				//System.out.println("ChoiceNode still not enabled");
				parent.setEnabled(true);
				parent = parent.getParent();
			}
		}
	}
	
	public boolean handleEndTag() {
		descriptionNode.handleEndTag();
		//choiceElement.endWithNewline();

		// Add the goto element
		addChild(gotoParagraph);
		gotoNode.handleEndTag();
		gotoParagraph.handleEndTag();

		// Make sure the highlight happens on the choice description
		gotoNode.setHighlightElements(descriptionNode.getLeaves());
//		Element[] choiceLeaves = new Element[choiceElement.getElementCount()];
//		for (int i = 0; i < choiceLeaves.length; i++)
//			choiceLeaves[i] = choiceElement.getElement(i);
//		gotoNode.setHighlightElements(choiceLeaves);

		if (shards != null || item != null || profession >= 0 || god != null || emptyvar != null) {
			setEnabled(false);
			findExecutableGrouper().addExecutable(this);
		}
		if (boxword != null) {
			setEnabled(getCodewords().hasCodeword(boxword));
			getCodewords().addChangeListener(boxword, this);
		}
		
		return true;
	}

	private int getMoney() {
		if (currency == null)
			return getAdventurer().getMoney();
		else {
			int moneyIndex = getItems().getMoneyItem(currency);
			return (moneyIndex < 0 ? 0 : getItems().getItem(moneyIndex).getMoney());
		}
	}
	public boolean execute(ExecutableGrouper grouper) {
		// Return without enabling if we don't meet any conditions
		System.out.println("Disabling choicenode");
		setEnabled(false);
		if (book != null && !Books.getCanon().getBook(book).hasBook())
			return true;
		if (shards != null && getMoney() < getAttributeValue(shards))
			return true;
		if (item != null && getItems().findMatches(item).length == 0)
			return true;
		if (profession >= 0 && getAdventurer().getProfession() != profession)
			return true;
		if (god != null && !getAdventurer().hasGod(god))
			return true;
		if (boxword != null && !getCodewords().hasCodeword(boxword))
			return true;
		if (emptyvar != null && isVariableDefined(emptyvar))
			return true;

		System.out.println("Enabling choicenode");
		setEnabled(true);
		return true;
	}

	public void actionPerformed(ActionEvent e) {
		// Action performed on GotoNode
		if (shards != null && pay) {
			if (currency == null)
				getAdventurer().adjustMoney(-getAttributeValue(shards));
			else
				getItems().adjustMoney(-getAttributeValue(shards), currency);
		}
		
		if (item != null && pay) {
			// Some problems here, since goto has already been triggered;
			// there are cases where it would be better if we could veto the goto.
			int[] indices = getItems().findMatches(item);
			if (indices.length > 1 && getItems().areItemsSame(indices))
				System.err.println("ChoiceNode.action(): need to take one item, not all are equal!");
			if (indices.length > 0)
				getItems().removeItem(indices[indices.length-1]);
		}
	}

	public void addActionListener(ActionListener l) {
		gotoNode.addActionListener(l);
	}
	public void removeActionListener(ActionListener l) {
		gotoNode.removeActionListener(l);
	}
	
	public void stateChanged(ChangeEvent evt) {
		if (boxword != null && evt.getSource().equals(boxword)) {
			boolean selected = getCodewords().hasCodeword(boxword);
			if (box != null)
				box.setSelected(selected);
			setEnabled(selected);
		}
		else {
			System.out.println("GotoNode: stateChanged; shards required=" + shards);
			setEnabled(getAdventurer().getMoney() >= getVariableValue(shards));
		}
	}

	protected String getElementViewType() { return RowViewType; }
	
	public void dispose() {
		if (shards != null) {
			if (currency == null)
				getAdventurer().removeMoneyListener(this);
			else
				getItems().removeCurrencyListener(this);
		}
		
		if (boxword != null) {
			getCodewords().removeChangeListener(this);
			if (box != null && box.getParent() != null)
				box.getParent().remove(box);
		}
	}
}
