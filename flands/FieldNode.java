package flands;

import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.xml.sax.Attributes;

/**
 * A Node that displays a (non-editable) text-field, containing the current
 * value of one of the codewords.
 * @author Jonathan Mann
 */
public class FieldNode extends Node implements ChangeListener {
	public static final String ElementName = "field";
	private String name;
	private String label;
	private JTextField field;

	public FieldNode(Node parent) {
		super(ElementName, parent);
	}

	public void init(Attributes atts) {
		name = atts.getValue("name");
		label = atts.getValue("label");
		if (label == null)
			label = atts.getValue("text");
		if (!label.endsWith(":"))
			label += ':';

		super.init(atts);

		getDocument().addLeavesTo(getElement(), new String[] { label + " " }, null);
		field = new JTextField(Integer.toString(getCodewords().getValue(name)), 3);
		field.setFont(SectionDocument.getPreferredFont());
		field.setEditable(false);
		field.setPreferredSize(field.getPreferredSize());
		SectionDocument.addComponentFontUser(field);
		SimpleAttributeSet fieldAtts = new SimpleAttributeSet();
		StyleConstants.setComponent(fieldAtts, field);
		setViewType(fieldAtts, ComponentViewType);
		getDocument().addLeavesTo(getElement(), new StyledText[] { new StyledText(" ", fieldAtts) });
		getDocument().addLeavesTo(getElement(), new String[] { "\n" }, null);
	}

	public boolean handleEndTag() {
		getCodewords().addChangeListener(name, this);
		return true;
	}
	
	protected String getElementViewType() { return ParagraphViewType; }

	protected MutableAttributeSet getElementStyle() {
		SimpleAttributeSet atts = new SimpleAttributeSet();
		StyleConstants.setAlignment(atts, StyleConstants.ALIGN_JUSTIFIED);
		StyleConstants.setFirstLineIndent(atts, 25.0f);
		return atts;
	}

	public void stateChanged(ChangeEvent e) {
		if (e.getSource().equals(name))
			field.setText(Integer.toString(getCodewords().getValue(name)));
	}
	
	public void dispose() {
		getCodewords().removeChangeListener(this);
		SectionDocument.removeComponentFontUser(field);
	}
}
