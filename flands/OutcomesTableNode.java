package flands;


import java.util.Iterator;

import org.xml.sax.Attributes;

/**
 * Groups a set of outcomes as a table. Since enableMatchingChild is unused, this
 * simply acts as a grouping node, so it could contain ChoiceNodes or DifficultyResultNodes
 * with equal ease.
 * 
 * @author Jonathan Mann
 */
public class OutcomesTableNode extends TableNode implements Executable {
	public static final String ElementName = "outcomes";
	private String var;

	public OutcomesTableNode(Node parent) {
		super(ElementName, parent);
	}

	public void init(Attributes atts) {
		var = atts.getValue("var");
		findExecutableGrouper().addExecutable(this);
	}

	/**
	 * Unused: OutcomeNodes will individually check whether they match the result.
	 */
	public boolean enableMatchingChild(String variable, int value) {
		if (var != variable &&
			(var == null || variable == null || !var.equals(variable)))
			return false;

		for (Iterator<Node> i = getChildren(); i.hasNext(); ) {
			Node n = i.next();
			if (n instanceof OutcomeNode) {
				OutcomeNode outcome = (OutcomeNode)n;
				if (outcome.matches(value)) {
					System.out.println("Found matching outcome: " + outcome);
					outcome.setEnabled(true);
					return true;
				}
			}
		}

		return false;
	}
	
	public boolean execute(ExecutableGrouper grouper) {
		System.out.println("Executing Outcomes node");
		// Make sure all child OutcomeNodes are in initial state
		for (Iterator<Node> i = getChildren(); i.hasNext(); ) {
			Node n = i.next();
			if (n instanceof Executable) {
				((Executable)n).resetExecute();
			}
		}
		
		return true;
	}

	public void resetExecute() {}
}
