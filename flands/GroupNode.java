package flands;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.swing.text.Element;

import org.xml.sax.Attributes;

/**
 * An action node that collects together one or more other action nodes;
 * when execution reaches it, its behaviour depends on the first node within it;
 * clicking it will activate each of the contained nodes. Hard to get right and
 * probably still buggy as hell, but essential.
 * 
 * @author Jonathan Mann
 */
public class GroupNode extends ActionNode implements Executable, ExecutableGrouper {
	public static final String ElementName = "group";
	private List<Executable> execChildren = new ArrayList<Executable>();
	private TextNode textNode = null;
	private boolean forced;

	public GroupNode(Node parent) {
		super(ElementName, parent);
		setEnabled(false);
	}

	public ExecutableGrouper getExecutableGrouper() {
		return this;
	}

	protected void addChild(Node child) {
		/*if (child instanceof ActionNode)
			actionChildren.add((ActionNode)child);
		else
		*/
		if (child instanceof TextNode) {
			textNode = (TextNode)child;
			textNode.setTextAttributes(createStandardAttributes());
		}

		super.addChild(child);
	}

	public void init(Attributes atts) {
		forced = getBooleanValue(atts, "force", false);
		super.init(atts);
	}

	public void handleContent(String text) {
		if (text.trim().length() == 0)
			return;

		Element[] leaves = getDocument().addLeavesTo(getElement(), new StyledText[] { new StyledText(text, createStandardAttributes()) });
		addHighlightElements(leaves);
		addEnableElements(leaves);
	}

	public boolean hideChildContent() { return true; }
	
	public boolean handleEndTag() {
		if (textNode != null) {
			Element[] textLeaves = textNode.getLeaves();
			addHighlightElements(textLeaves);
			addEnableElements(textLeaves);
		}

		findExecutableGrouper().addExecutable(this);
		
		return super.handleEndTag();
	}

	private boolean childCallingParent = false;
	public boolean isEnabled() {
		if (childCallingParent)
			return true;
		
		ActionNode queryNode = currentNode;
		if (queryNode == null && execChildren.size() > 0) {
			for (int i = 0; i < execChildren.size(); i++) {
				if (execChildren.get(i) instanceof ActionNode) {
					queryNode = (ActionNode)execChildren.get(i);
					break;
				}
			}
		}
		//queryNode = actionChildren.get(0);
		
		if (queryNode != null) {
			System.out.println("GroupNode.isEnabled() called, current child=" + execChildren.indexOf(queryNode));
			childCallingParent = true;
			boolean result = queryNode.isEnabled();
			childCallingParent = false;
			if (result && getParent() != null && !getParent().isEnabled())
				result = false;
			if (result != enabled)
				// Make our display reflect our 'internal' knowledge
				setEnabled(result);
			return result;
		}
		
		return enabled;
	}

	public boolean execute(ExecutableGrouper eg) {
		System.out.println("GroupNode.execute() called");
		if (execChildren.size() > 0) {
			// Start executing each of our children
			// This will most likely enable the first only, which is
			// blocking until actionPerformed is called.
			for (int i = 0; i < execChildren.size(); i++) {
				Executable e = execChildren.get(i);
				if (e instanceof ActionNode) {
					currentNode = (ActionNode)e;
					UndoManager.getCurrent().add(new ExecutableWrapper(currentNode));
					((Executable)currentNode).execute(this); // don't care about result
					if (currentNode.isEnabled() || !forced) {
						isEnabled(); // trigger an evaluation of enabled state
						return !forced; // forced means we don't allow execution to continue
					}
					// else - forced execution, which means whichever child we settle on
					// must be enabled.
				}
			}

			// We can have it both ways here - for a normal GroupNode (non-forced),
			// if the first child isn't enabled then neither will be the GroupNode.
			// This lets us prices and flags.
			// On the other hand, a forced GroupNode must be executed; if the first child
			// isn't enabled (eg. for losing a codeword we don't have, 5.453) then we
			// keep looking at subsequent children until one is found that is enabled.

			/*
			// Revised - call only the first, and return regardless of the result
			currentNode = actionChildren.get(0);
			UndoManager.getCurrent().add(new ExecutableWrapper(currentNode));
			((Executable)currentNode).execute(this);
			isEnabled(); // trigger an evaluation of enabled state
			return !forced;
			*/
		}

		return true;
	}
	
	public void resetExecute() {
		//if (runner != null)
		//	runner.resetExecute();
		setEnabled(false);
	}

	private boolean doingAction = false;
	public void actionPerformed(ActionEvent evt) {
		if (doingAction) return; // currently not used
		
		if (currentNode == null || !currentNode.isEnabled()) {
			setEnabled(false);
			return;
		}
		
		boolean continues = currentNode.doGroupAction(this);
		if (!continues)
			// Child didn't continue execution - we'll do it manually
			continueExecution((Executable)currentNode, false);
	}

	protected String getTipText() {
		if (currentNode == null || !currentNode.isEnabled())
			return null;
		
		List<String> lines = new LinkedList<String>();
		int index = execChildren.indexOf(currentNode);
		for (int i = index; i < execChildren.size(); i++) {
			Executable e = execChildren.get(i);
			if (e instanceof ActionNode) {
				ActionNode action = (ActionNode)e;
				if (!action.hidden) {
					String line = action.getTipText();
					if (line != null && line.length() > 0)
						lines.add(line);
				}
			}
		}

		if (lines.size() == 0)
			return null;
		else if (lines.size() == 1)
			return lines.get(0);
		else {
			StringBuffer sb = new StringBuffer("Do the following:<ul>");
			for (Iterator<String> i = lines.iterator(); i.hasNext(); )
				sb.append("<li>").append(i.next()).append("</li>");
			sb.append("</ul>");
			return sb.toString();
		}
	}
	
	/* *************************
	 * ExecutableGrouper methods
	 * If we want to track which is the 'current' node, especially after an undo,
	 * we need to keep track of child execution ourselves.
	 ************************* */
	public void addExecutable(Executable e) {
		execChildren.add(e);
	}

	public void addIntermediateNode(Node n) {
		System.out.println("GroupNode.addIntermediateNode(" + n + ") called");
	}

	private ActionNode currentNode = null;
	public void continueExecution(Executable eDone, boolean inSeparateThread) {
		separateThread = inSeparateThread;
		int index = -1;
		if (eDone != null) {
			// Figure out which child just executed
			index = execChildren.indexOf(eDone);
			if (index < 0)
				System.out.println("GroupNode.continueExecution(" + eDone + "): child not recognised");
		}
		
		for (int i = index+1; i < execChildren.size(); i++) {
			// Execute the next child
			System.out.println("GroupNode: continuing to child " + i);
			Executable e = execChildren.get(i);
			if (e instanceof ActionNode) {
				currentNode = (ActionNode)e;
				UndoManager.getCurrent().add(new ExecutableWrapper(currentNode));
				boolean finished = ((Executable)currentNode).execute(this);
				if (!finished && !currentNode.isEnabled())
					System.out.println("GroupNode.continueExecution(): child " + i + " blocked, but isn't enabled");
				boolean continues = currentNode.doGroupAction(this);
				if (continues) {
					// continueExecution was/will be called in the meantime
					separateThread = false;
					return;
				}
			}
			else {
				// execute this non-ActionNode immediately
				UndoManager.getCurrent().add(e);
				boolean finished = e.execute(this);
				if (finished) // expected
					;
				else {
					System.err.println("GroupNode.continue: non-ActionNode child didn't finish execution!");
					currentNode = null;
					setEnabled(false);
					return;
				}
			}
		}
		
		// TODO: To catch undo events, we need to add a mock Executable at the end here.
		// That way it gets resetExecute() called, even if it's the last child that
		// created the undo (like, say, a DifficultyNode).
		// Then we can set currentNode correctly and also update the enabled state.
		UndoManager.getCurrent().add(new ExecutableWrapper(null));

		currentNode = null;
		isEnabled();
		separateThread = false;
		findExecutableGrouper().continueExecution(this, inSeparateThread);
	}

	private boolean separateThread = false;
	public boolean isSeparateThread() {
		return separateThread;
	}
	
	protected void loadProperties(Attributes props) {
		super.loadProperties(props);
		int childIndex = getIntValue(props, "currentChild", -1);
		if (childIndex >= 0)
			currentNode = (ActionNode)execChildren.get(childIndex);
	}
	
	protected void saveProperties(Properties props) {
		super.saveProperties(props);
		if (currentNode != null)
			saveProperty(props, "currentChild", execChildren.indexOf(currentNode));
	}
	
	/**
	 * Wrapper around each Executable. We use this when adding children to
	 * the UndoManager; then when resetExecute() is called we get notified.
	 * This is necessary because our enabled state depends on which is the 'current' child.
	 */
	private class ExecutableWrapper implements Executable {
		private Executable e;
		private ExecutableWrapper(ActionNode node) {
			this.e = (Executable)node;
		}
		
		public boolean execute(ExecutableGrouper grouper) {
			if (e == null)
				return true;
			return e.execute(grouper);
		}

		public void resetExecute() {
			int childIndex = execChildren.size();
			if (e != null)
				childIndex = execChildren.indexOf(e);

			if (childIndex > 0) {
				currentNode = null;
				for (int i = childIndex-1; i >= 0; i--)
					if (execChildren.get(i) instanceof ActionNode) {
						currentNode = (ActionNode)execChildren.get(i);
						break;
					}
				
				if (currentNode == null)
					System.err.println("GroupNode.ExecutableWrapper: couldn't find previous ActionNode");
			}

			System.out.println("GroupNode.ExecutableWrapper.resetExecute() called");

			if (e != null)
				e.resetExecute();
			
			setEnabled(true);
			// if this isn't right, GroupNode.resetExecute() will be called again
			// and we'll be disabled.
		}
	}
}
