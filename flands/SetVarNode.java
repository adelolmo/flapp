package flands;


import java.awt.event.ActionEvent;
import java.util.Properties;

import javax.swing.text.Element;

import org.xml.sax.Attributes;

/**
 * An 'invisible' node that gets automatically executed.
 * When executed, it sets a variable. The value can be a full expression.
 *
 * @see Expression
 * @author Jonathan Mann
 */
public class SetVarNode extends ActionNode implements Executable, Expression.Resolver {
	public static final String ElementName = "set";
	private String var;
	private String value = null;
	private String dock = null;
	private String modifier;
	private String cache = null;
	private String codeword = null;
	private Item item = null;
	private boolean force = true;

	public SetVarNode(Node parent) {
		super(ElementName, parent);
		findExecutableGrouper().addExecutable(this);
		setEnabled(false);
	}

	public void init(Attributes atts) {
		var = atts.getValue("var");
		value = atts.getValue("value");
		codeword = atts.getValue("codeword");
		if (value == null && codeword == null) {
			System.err.println("SetVarNode.init(): no value attribute!");
			value = "0";
		}
		dock = atts.getValue("dock");
		modifier = atts.getValue("modifier");
		cache = atts.getValue("cache");
		item = Item.createItem(atts);
		force = getBooleanValue(atts, "force", true);
		
		super.init(atts);
		hidden = true; // by default
		if (getParent() instanceof GroupNode) hidden = false; // an exception
	}

	protected void init(Properties props) {
		super.outit(props);
		if (var != null) props.setProperty("var", var);
		if (value != null)
			// TODO: May be an expression containing variables - ideally we
			// should resolve the expression as far as possible, leaving any
			// undefined variables as is
			props.setProperty("value", value);
		if (codeword != null) props.setProperty("codeword", codeword);
		if (dock != null) props.setProperty("dock", dock);
		if (modifier != null) props.setProperty("modifier", modifier);
		if (cache != null) props.setProperty("cache", cache);
		if (item != null) item.saveProperties(props);
		if (!force) saveProperty(props, "force", false);
	}
	
	public void handleContent(String text) {
		if (text.length() > 0) {
			hidden = false;
			Element[] leaves = getDocument().addLeavesTo(getElement(), new StyledText[] { new StyledText(text, createStandardAttributes())});
			addEnableElements(leaves);
			addHighlightElements(leaves);
		}
	}

	private Item getSingleItem() {
		if (item != null) {
			ItemList items = (cache == null ? getItems() : CacheNode.getItemCache(cache));
			int[] matches = items.findMatches(item);
			System.out.println("SetVarNode: found " + matches + " item matches");
			if (matches.length == 1)
				return items.getItem(matches[0]);
		}
		return null;
	}
	
	public int resolveIdentifier(String ident) {
		if (ident.equals("armour")) {
			Item.Armour a = null;
			try {
				a = (Item.Armour)getSingleItem();
			}
			catch (ClassCastException cce) {}
			if (a == null && cache == null)
				a = getItems().getWorn();
			return (a == null ? 0 : a.getBonus());
		}
		else if (ident.equals("weapon")) {
			Item.Weapon w = null;
			try {
				w = (Item.Weapon)getSingleItem();
			}
			catch (ClassCastException cce) {}
			if (w == null && cache == null)
				w = getItems().getWielded();
			System.out.println("Weapon item=" + w);
			return (w == null ? 0 : w.getBonus());
		}
		else if (ident.equals("matches")) {
			if (item != null) {
				ItemList items = (cache == null ? getItems() : CacheNode.getItemCache(cache));
				int[] matches = items.findMatches(item);
				System.out.println("Matches=" + matches.length + " to item " + item);
				return matches.length;
			}
		}
		else if (ident.equals("stamina") && modifier == null) {
			return getAdventurer().getStamina().current;
		}
		else if (ident.equals("crew")) {
			int shipIndex = getShips().getSingleShip();
			if (shipIndex >= 0)
				return getShips().getShip(shipIndex).getCrew();
			else
				System.out.println("SetVarNode: asked for crew, but no single ship here!");
		}
		else if (ident.equals("shards")) {
			if (cache == null)
				return getAdventurer().getMoney();
			else
				return CacheNode.getMoneyCache(cache);
		}
		else {
			int ability = Adventurer.getAbilityType(ident);
			if (ability >= 0) {
				int abilityModifier = Adventurer.getAbilityModifier(modifier);
				return getAdventurer().getAbilityValue(ability, abilityModifier, Adventurer.PURPOSE_VALUE);
			}
		}
		
		return super.resolveIdentifier(ident);
	}
	
	public boolean execute(ExecutableGrouper grouper) {
		if (!hidden) {
			setEnabled(true);
			if (force)
				return false;
		}
		else
			actionPerformed(null);
		return true;
	}
	
	public void actionPerformed(ActionEvent evt) {
		if (!hidden)
			setEnabled(false);
		
		if (value != null) {
			Expression exp = new Expression(value);
			int val = exp.getRoot().evaluate(this);
			System.out.println("SetVar (" + var + ") = " + val);
			setVariableValue(var, val);
		}
		else if (codeword != null) {
			int val = getCodewords().getValue(codeword);
			System.out.println("SetVar (" + var + ") = " + val);
			setVariableValue(var, val);
		}

		if (dock != null) {
			int[] indices = getShips().findShipsHere();
			for (int i = 0; i < indices.length; i++)
				getShips().getShip(indices[i]).setDocked(dock);
			getShips().refresh();
		}
		
		if (force && !hidden)
			findExecutableGrouper().continueExecution(this, false);
	}

	public void resetExecute() {
		removeVariable(var);
	}
	
	protected String getTipText() {
		String text = null;
		if (value != null || codeword != null) {
			text = "Set the ";
			if (var == null)
				text += "anonymous variable";
			else
				text += "variable [" + var + "]";
			if (value != null)
				text += " to " + new Expression(value).getRoot().evaluate(this);
			else {
				text += " to the value of the codeword <i>" + codeword + "</i>";
				text += " [" + getCodewords().getValue(codeword) + "]";
			}
		}
		
		if (dock != null) {
			if (text == null)
				text = "Dock";
			else
				text += ", and dock";
		    text += " any ships here at " + dock;
		}
		
		return text;
	}
}
