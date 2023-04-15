package flands;

import java.util.Properties;

import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.xml.sax.Attributes;

/**
 * Conditional node, one of a set, most often in the form 'Score a-b, turn to X'.
 * Usually occurs after some kind of random node, with each OutcomeNode handling
 * one result.
 * @author Jonathan Mann
 */
public class OutcomeNode extends ActionNode implements Executable, Flag.Listener {
	private int rangeMin = 0, rangeMax = -1;
	private boolean orRange = false;
	private String varName = null;
	private String[] codewords = null;
	private boolean andCodewords;
	private Blessing blessing = null;
	private String flag;
	private ParagraphNode rangeNode;
	private ParagraphNode gotoParagraph = null;
	private GotoNode gotoNode = null;
	private ParagraphNode descriptionNode = null;
	private BoxNode descriptionBox = null;
	private ExecutableRunner runner = null;

	public static final String ElementName = "outcome";

	public OutcomeNode(Node parent) {
		super(ElementName, parent);
	}

	public ExecutableGrouper getExecutableGrouper() {
		if (runner == null)
			runner = new ExecutableRunner("outcome", this);
		return runner;
	}

	public void setRange(int min) {
		setRange(min, Integer.MAX_VALUE);
	}
	public void setRange(int min, int max) {
		if (min > max)
			System.out.println("Error: " + min + " > " + max + "!");
		else {
			rangeMin = min;
			rangeMax = max;
		}
	}

	public boolean matches(int val) {
		if (hasRange()) {
			if (!orRange)
				return (rangeMin <= val && val <= rangeMax);
			else
				return (val == rangeMin || val == rangeMax);
		}
		else if (codewords != null && codewords.length > 0) {
			for (int c = 0; c < codewords.length; c++) {
				boolean hasCodeword = getCodewords().hasCodeword(codewords[c]);
				if (!hasCodeword && andCodewords)
					return false;
				else if (hasCodeword && !andCodewords)
					return true;
			}
			return andCodewords;
		}
		
		return true; // default outcome
	}

	public boolean hasRange() {
		return (orRange || rangeMin <= rangeMax);
	}

	public String getRange() {
		if (rangeMin == rangeMax)
			return Integer.toString(rangeMin);
		else if (rangeMax == Integer.MAX_VALUE)
			return rangeMin + "+";
		else if (orRange)
			return rangeMin + " or " + rangeMax;
		else if (rangeMin < rangeMax)
			return rangeMin + "-" + rangeMax;

		return "<parsing error>";
	}

	public void init(Attributes xmlAtts) {
		String range = xmlAtts.getValue("range");
		varName = xmlAtts.getValue("var");
		flag = xmlAtts.getValue("flag");
		if (flag != null)
			getFlags().addListener(flag, this);
		codewords = split(xmlAtts.getValue("codeword"));
		andCodewords = andSplitter;
		blessing = Blessing.getBlessing(xmlAtts);
		if (range != null) {
			try {
				int dashIndex = range.indexOf('-');
				if (dashIndex >= 0) {
					rangeMin = Integer.parseInt(range.substring(0, dashIndex));
					rangeMax = Integer.parseInt(range.substring(dashIndex+1));
				}
				else if (range.indexOf(',') >= 0) {
					int commaIndex = range.indexOf(',');
					rangeMin = Integer.parseInt(range.substring(0,commaIndex));
					rangeMax = Integer.parseInt(range.substring(commaIndex+1));
					orRange = true;
				}
				else {
					rangeMin = rangeMax = Integer.MAX_VALUE;
					int plusIndex = range.indexOf('+');
					if (plusIndex < 0)
						rangeMin = rangeMax = Integer.parseInt(range);
					else
						rangeMin = Integer.parseInt(range.substring(0, plusIndex));
				}
			}
			catch (NumberFormatException nfe) {
				System.err.println("Problems parsing the outcome range: " + range);
			}
		}
		else if (codewords != null) {
			// This is a pretty rare case - seen in 5.303, 4.2
			// The first outcome with a codeword that the player has should return as 'matched'
			// We automatically add the codewords below
		}

		super.init(xmlAtts);

		if (xmlAtts.getValue(GotoNode.SectionAttribute) != null) {
			gotoParagraph = new ParagraphNode(this, StyleConstants.ALIGN_RIGHT);
			gotoNode = new GotoNode(gotoParagraph); // forced
			gotoNode.ignoreFlags(true); // we'll handle them
			gotoNode.init(xmlAtts);
			gotoNode.setEnabled(false);
			gotoParagraph.addChild(gotoNode);
			// But leave off adding this as a child until after the description
		}

		// Add the first cell, containing an automatic description
		// For codewords, only add this later, if a description is missing
		if (range != null) {
			rangeNode = new ParagraphNode(this, StyleConstants.ALIGN_LEFT);
			super.addChild(rangeNode);
			Element[] leaves = getDocument().addLeavesTo(rangeNode.getElement(), new String[] { "Score " + getRange() + "\n" }, null);
			setHighlightElements(leaves);
			rangeNode.setEnabled(true);
		}
	}

	protected void outit(Properties props) {
		super.outit(props);
		if (rangeMin < rangeMax)
			props.setProperty("range", rangeMin + (orRange ? "," : "-") + rangeMax);
		if (varName != null) props.setProperty("var", varName);
		if (flag != null) props.setProperty("flag", flag);
		if (codewords != null) props.setProperty("codeword", concatenate(codewords, andSplitter));
		if (blessing != null) blessing.saveTo(props);
		if (gotoNode != null) gotoNode.outit(props);
	}
	
	protected Node createChild(String name) {
		if (descriptionBox == null) {
			descriptionBox = new BoxNode(this, BoxNode.Y_AXIS);
			super.addChild(descriptionBox);
		}

		if (SectionNode.isTopLevelNode(name)) {
			if (descriptionNode != null) {
				descriptionNode.handleEndTag();
				descriptionNode = null;
			}
			
			//return super.createChild(name);
			return descriptionBox.createChild(name);
		}
		else {
			if (descriptionNode == null) {
				descriptionNode = new ParagraphNode(descriptionBox, StyleConstants.ALIGN_LEFT);
				//super.addChild(descriptionNode);
				descriptionBox.addChild(descriptionNode);
			}
			return descriptionNode.createChild(name);
		}
	}
	protected void addChild(Node child) {
		if (descriptionNode == null) {
			//super.addChild(child);
			descriptionBox.addChild(child);
		}
		else
			descriptionNode.addChild(child);
	}

	public void handleContent(String text) {
		if (text.trim().length() == 0) return;

		if (descriptionBox == null) {
			descriptionBox = new BoxNode(this, BoxNode.Y_AXIS);
			super.addChild(descriptionBox);
		}
		
		if (descriptionNode == null) {
			// Add a second child now to handle the outcome description
			descriptionNode = new ParagraphNode(descriptionBox, StyleConstants.ALIGN_LEFT);//(gotoNode == null ? StyleConstants.ALIGN_RIGHT : StyleConstants.ALIGN_LEFT));
			//super.addChild(descriptionNode);
			descriptionBox.addChild(descriptionNode);
			if (gotoNode != null)
				gotoNode.setDescriptionNode(descriptionNode);
				// ie. handleContent calls get passed via gotoNode
		}
		if (gotoNode == null)
			descriptionNode.handleContent(text);
		else
			descriptionNode.handleContent(text);
	}


	public boolean handleEndTag() {
		if (descriptionBox == null) {
			// No description here - maybe add one now
			if (!hasRange() && codewords != null) {
				rangeNode = new ParagraphNode(this, StyleConstants.ALIGN_LEFT);
				super.addChild(rangeNode);
				SimpleAttributeSet italicAtts = new SimpleAttributeSet();
				StyleConstants.setItalic(italicAtts, true);
				StyledTextList textList = new StyledTextList();
				for (int c = 0; c < codewords.length; c++) {
					if (c > 0) {
						if (c < codewords.length - 1)
							textList.add(", ", null);
						else
							textList.add(andCodewords ? " and " : " or ", null);
					}
					textList.add(codewords[c], italicAtts);
				}
				Element[] leaves = textList.addTo(getDocument(), rangeNode.getElement());
				setHighlightElements(leaves);
				rangeNode.setEnabled(true);
			}
		}
		
		if (descriptionNode != null) {
			descriptionNode.handleEndTag();
			//descriptionNode.setEnabled(false);
		}

		if (descriptionBox != null) {
			descriptionBox.handleEndTag();
			descriptionBox.setEnabled(false);
		}
		
		if (gotoParagraph != null) {
			gotoNode.handleEndTag();
			gotoParagraph.handleEndTag();
			super.addChild(gotoParagraph);
			
			if (runner != null) {
				// Add gotoNode as final executable child, to be executed after
				// all other children
				runner.addExecutable(gotoNode);
			}
		}

		System.out.println("Adding OutcomeNode(" + getRange() + ") as child executable");
		findExecutableGrouper().addExecutable(this);
		
		return (descriptionNode != null || gotoNode != null);
	}

	public boolean execute(ExecutableGrouper grouper) {
		System.out.println("Executing OutcomeNode, range " + getRange());

		return activate(grouper);
	}

	private boolean activated = false;
	private ExecutableGrouper grouper = null;
	public boolean activate(ExecutableGrouper grouper) {
		this.grouper = grouper;
		if (flag != null && !getFlags().getState(flag)) {
			// This OutcomeNode is dependent on an earlier price being paid,
			// though not directly. Example at 5.674.
			System.out.println("OutcomeNode: flag is false - skipping");
			return true;
		}

		int val = getVariableValue(varName);
		if (matches(val)) {
			setHighlighted(true); // will highlight as soon as it is enabled
			activated = true;
			if (descriptionBox != null)
				descriptionBox.setEnabled(true);
			if (descriptionNode != null)
				descriptionNode.setEnabled(true);
			if (gotoNode != null) {
				if (gotoNode.canUse()) {
					System.out.println("Can use gotoNode now");
					if (runner == null)
						// enable the gotoNode now
						gotoNode.setEnabled(true);
				}
			}
			
			if (flag != null) {
				//System.out.println("OutcomeNode, range " + getRange() + ", matches - setting flag to false");
				//getFlags().setState(flag, false);
			}

			if (blessing != null && getBlessings().hasBlessing(blessing)) {
				// This is probably Safety from Storms
				//getBlessings().removeBlessing(blessing); // this should happen elsewhere
				return true;
			}

			if (runner != null) {
				System.out.println("OutcomeNode will run executable children");
				if (!runner.execute(grouper))
					return false;
			}
		}

		return true;
	}

	public void resetExecute() {
		//if (isEnabled()) {
		if (activated) {
			activated = false;

			if (runner != null)
				runner.resetExecute();
			if (gotoNode != null)
				gotoNode.setEnabled(false);
			if (descriptionBox != null)
				descriptionBox.setEnabled(false); // correct initial state?
			if (descriptionNode != null)
				descriptionNode.setEnabled(false);
			setHighlighted(false);
			if (rangeNode != null && !rangeNode.isEnabled()) {
				System.out.println("rangeNode isNotEnabled!");
				rangeNode.setEnabled(true);
			}
		}
		//}
	}

	protected String getElementViewType() { return RowViewType; }

	public void setEnabled(boolean b) {
		super.setEnabled(b);
		//if (gotoNode != null)
		//	gotoNode.setEnabled(b);
	}

	public void actionPerformed(java.awt.event.ActionEvent evt) {
		System.out.println("Something triggered this Outcome: huh?");
	}

	public void dispose() {
		if (flag != null)
			getFlags().removeListener(flag, this);
	}

	public void flagChanged(String name, boolean state) {
		if (flag.equals(name)) {
			if (state) {
				System.out.println("OutcomeNode: flag is now true - resetting");
				resetExecute();
				if (!hasRange() && codewords == null)
					execute(grouper == null ? findExecutableGrouper() : grouper);
			}
		}
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
