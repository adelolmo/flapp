package flands;


import java.util.Properties;

import javax.swing.text.Element;

import org.xml.sax.Attributes;

/**
 * Contains part of the section that will be repeatedly executed until the given variable
 * has been assigned a value. Not much used, probably buggy, and most likely restricted
 * to being part of a single paragraph.
 * 
 * @author Jonathan Mann
 */
public class WhileNode extends Node implements Executable, ExecutableGrouper {
	public static final String ElementName = "while";
	private String var;
	private ExecutableRunner runner = new ExecutableRunner(ElementName, this);

	public WhileNode(Node parent) {
		super(ElementName, parent);
		setEnabled(false);
		findExecutableGrouper().addExecutable(this);
	}

	public void init(Attributes atts) {
		var = atts.getValue("var");
	}

	protected void outit(Properties props) {
		super.outit(props);
		props.setProperty("var", var);
	}
	
	public void handleContent(String text) {
		if (text.trim().length() == 0) return;
		addEnableElements(getDocument().addLeavesTo(getElement(), new String[] { text }, null));
	}

	public ExecutableGrouper getExecutableGrouper() {
		return runner;
	}

	public boolean execute(ExecutableGrouper grouper) {
		if (isVariableDefined(var))
			return true;

		System.out.println("Starting while loop");
		setEnabled(true);
		return runWhileLoop();
	}

	public void resetExecute() {
		runner.resetExecute();
		setEnabled(false);
	}

	/* ExecutableGrouper methods */
	public boolean isSeparateThread() { return false; }
	public void addIntermediateNode(Node n) {}
	public void addExecutable(Executable e) {}

	private boolean doReset = false;
	private boolean runWhileLoop() {
		while (!isVariableDefined(var)) {
			if (doReset) {
				System.out.println("Resetting children");
				doReset = false;
				runner.resetExecute(); // ready for another loop
			}

			System.out.println("Executing children");
			doReset = true;
			if (!runner.execute(this)) return false; // continueExecution will be called later
		}
		return true;
	}

	public void continueExecution(Executable eDone, boolean inSeparateThread) {
		// Called by runner
		if (runWhileLoop())
			findExecutableGrouper().continueExecution(this, false);
	}

	protected Element createElement() { return null; }
	
	public void saveProperties(Properties props) {
		super.saveProperties(props);
		if (runner != null && runner.willCallContinue())
			saveProperty(props, "continue", true);
	}
	
	public void loadProperties(Attributes atts) {
		super.loadProperties(atts);
		if (getBooleanValue(atts, "continue", false))
			((ExecutableRunner)getExecutableGrouper()).setCallback(this);
	}
}
