package flands;


import java.util.Properties;

import javax.swing.text.Element;

import org.xml.sax.Attributes;

/**
 * General effect node, used as child of a 'thing' which has an effect attached.
 * @author Jonathan Mann
 */
public class EffectNode extends Node {
	public static final String ElementName = "effect";

	private Item item;
	private Curse curse;
	private Effect effect;
	private TextNode textNode = null;

	public EffectNode(Node parent) {
		super(ElementName, parent);
	}
	
	public EffectNode(Node parent, Item item) {
		super(ElementName, parent);
		this.item = item;
	}

	public EffectNode(Node parent, Curse curse) {
		super(ElementName, parent);
		this.curse = curse;
	}

	public Effect getEffect() { return effect; }
	
	private ExecutableRunner runner = null;
	public ExecutableGrouper getExecutableGrouper() {
		if (runner == null)
			runner = new ExecutableRunner();
		return runner;
		// This is here to catch UseEffects, because they're not meant to be executed _now_
	}

	public void init(Attributes atts) {
		effect = Effect.createEffect(atts);
		if (item != null)
			item.addEffect(effect);
		else if (curse != null)
			curse.addEffect(effect);
		else
			System.out.println("EffectNode: didn't have item or curse to add effect to!");
	}

	protected Node createChild(String name) {
		Node n = super.createChild(name);
		if (n != null && n instanceof ActionNode) {
			if (effect != null && effect instanceof UseEffect)
				((UseEffect)effect).addActionNode((ActionNode)n);
		}
		return n;
	}

	protected void addChild(Node child) {
		if (child instanceof TextNode)
			textNode = (TextNode)child;

		super.addChild(child);
	}
	
	public boolean hideChildContent() { return true; }
	
	protected Element createElement() { return null; }
	
	public boolean handleEndTag() {
		if (textNode != null)
			effect.setStyledDescription(textNode.getText());
		return false;
	}
	
	public void saveProperties(Properties props) {
		super.saveProperties(props);
		if (runner != null && runner.willCallContinue())
			saveProperty(props, "continue", true);
	}
	
	public void loadProperties(Attributes atts) {
		super.loadProperties(atts);
		if (getBooleanValue(atts, "continue", false))
			((ExecutableRunner)getExecutableGrouper()).setCallback(findExecutableGrouper());
	}
}
