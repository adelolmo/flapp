package flands;


import java.awt.event.ActionEvent;


import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.Element;

import org.xml.sax.Attributes;

/**
 * Now that Flag has been moved into a separate class, LoseNode handles pretty much
 * everything (and more) that might be done here. So this class can be removed once I'm sure.
 * @author Jonathan Mann
 */
public class PriceNode extends ActionNode implements Executable, ChangeListener, Flag.Listener {
	public static final String ElementName = "price";

	private String flag;
	private String shards = null;
	private Item item = null;

	public PriceNode(Node parent) {
		super(ElementName, parent);
		setEnabled(false);
		findExecutableGrouper().addExecutable(this);
	}

	public void init(Attributes atts) {
		flag = atts.getValue("flag");
		shards = atts.getValue("shards");
		item = Item.createItem(atts);
		super.init(atts);
	}

	protected Element createElement() { return null; }

	public void handleContent(String text) {
		if (text.trim().length() == 0) return;

		Element[] leaves = getDocument().addLeavesTo(getElement(), new StyledText[] { new StyledText(text, createStandardAttributes()) });
		addEnableElements(leaves);
		addHighlightElements(leaves);
	}

	public boolean execute(ExecutableGrouper grouper) {
		getFlags().addListener(flag, this);
		if (shards != null)
			getAdventurer().addMoneyListener(this);
		if (item != null)
			getItems().addChangeListener(this);
		stateChanged(null);
		if (hidden && isEnabled())
			// execute now - this is a good way to set the flag to true immediately
			actionPerformed(null);
		return true;
	}

	public void resetExecute() {
		setEnabled(false);
	}

	private void doEnable() {
		// Check whether we've got the money and items to pay this price
		boolean enable = true;
		if (shards != null && getAttributeValue(shards) > getAdventurer().getMoney())
			enable = false;
		if (item != null && getItems().findMatches(item).length == 0)
			enable = false;

		setEnabled(enable);
	}

	public void stateChanged(ChangeEvent evt) {
		if (getFlags().getState(flag)) {
			// Price has been paid - disable for now
			setEnabled(false);
			return;
		}

		doEnable();
	}
	
	public void flagChanged(String name, boolean state) {
		if (name.equals(flag)) {
			if (!state) {
				setEnabled(false);
				return;
			}
			
			doEnable();
		}
	}

	public void actionPerformed(ActionEvent evt) {
		if (item != null) {
			int[] indices = getItems().findMatches(item);
			if (indices.length > 1) {
				JOptionPane.showMessageDialog(FLApp.getSingle(), new String[] {"Which item do you want to pay with?", "Please select one and try again."}, "Choose Item", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			getItems().removeItem(indices[0]);
		}
		if (shards != null)
			getAdventurer().adjustMoney(-getAttributeValue(shards));

		getFlags().setState(flag, true); // paid
	}

	public void dispose() {
		getFlags().removeListener(flag, this);
	}
}
