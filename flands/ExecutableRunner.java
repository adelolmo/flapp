package flands;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

/**
 * The standard implementation of ExecutableGrouper. Generally used by Nodes
 * (and with support for this).
 * @author Jonathan Mann
 */
public class ExecutableRunner extends ArrayList<Executable> implements ExecutableGrouper, Executable, Runnable {
	/**
	 * Wrapper for Node that will let us treat it as an Executable.
	 * When called on to execute, it enables the node and returns <code>true</code>.
	 */
	private static class NodeExecutable implements Executable {
		private Node node;
		private boolean wasEnabled;

		private NodeExecutable(Node n) { this.node = n; }

		public boolean execute(ExecutableGrouper eg) {
			wasEnabled = node.isEnabled();
			node.setEnabled(true);
			return true;
		}

		public void resetExecute() {
			if (!wasEnabled)
				node.setEnabled(false);
		}
	}

	private String debugName;
	// The Executable node that should be passed as an argument to parent groupers (in continueExecution())
	private Executable owner;
	// The separate thread that Executables are being called on
	private Thread thread = null;
	// The grouper running this Executable
	private ExecutableGrouper grouper = null;
	private boolean autoAction = false;

	public ExecutableRunner() { super(); }
	public ExecutableRunner(String name, Executable owner) {
		this();
		debugName = name;
		this.owner = owner;
	}

	public void setAutoAction(boolean b) { autoAction = b; }
	
	/**
	 * Add an Executable to be run later.  This is not thread-safe with the other methods;
	 * add all the Executables first, then run this later!
	 */
	public void addExecutable(Executable e) {
		add(e);
	}

	/**
	 * Add an intermediate Node to the list of Executables.
	 * When it is reached, it will be enabled.
	 */
	public void addIntermediateNode(Node n) {
		addExecutable(new NodeExecutable(n));
	}

	/**
	 * Callback from an Executable child.  Continue execution from the next child.
	 */
	public void continueExecution(Executable eDone, boolean inSeparateThread) {
		System.out.println("continueExecution callback from child " + eDone);
		int startAtIndex = 0;
		if (eDone != null)
			startAtIndex = indexOf(eDone) + 1;

		if (inSeparateThread)
			thread = Thread.currentThread();

		boolean finished = startExecution(startAtIndex);
		thread = null;

		if (finished) {
			UndoManager.getCurrent().ignoreCalls(false);
			if (grouper != null) {
				// Tell the parent who called us (some time ago) that we're finished
				ExecutableGrouper parent = grouper;
				//grouper = null;
				parent.continueExecution((owner == null) ? this : owner, inSeparateThread);
			}
		}
	}

	/**
	 * Execute by running each sub-method.  Called by <code>grouper</code>.
	 */
	public boolean execute(ExecutableGrouper grouper) {
		this.grouper = grouper;
		if (grouper.isSeparateThread())
			thread = Thread.currentThread();

		UndoManager.getCurrent().ignoreCalls(true);
		boolean finished = startExecution(0);
		thread = null;
		if (finished) {
			grouper = null;
			UndoManager.getCurrent().ignoreCalls(false);
		}
		return finished;
	}

	public boolean willCallContinue() { return (grouper != null); }
	public void setCallback(ExecutableGrouper grouper) { this.grouper = grouper; }
	
	/**
	 * Step through the list of Executables, executing each in turn.
	 * @return <code>true</code> if each one returns <code>true</code>;
	 * <code>false</code> when one is blocked.
	 */
	protected boolean startExecution(int eIndex) {
		for (int e = eIndex; e < size(); e++) {
			//System.out.println("Will execute child " + e + ": " + get(e));
			UndoManager.getCurrent().add(get(e));
			boolean canContinue = get(e).execute(this);
			if (autoAction && get(e) instanceof ActionNode) {
				ActionNode n = (ActionNode)get(e);
				if (n.isEnabled())
					n.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, debugName));
			}
			if (!canContinue)
				return false;
		}
		return true;
	}

	/** Return whether this is running on a separate thread. */
	public boolean isSeparateThread() {
		return Thread.currentThread() == thread;
	}

	/**
	 * Start executing each child in turn.
	 * This can be called on the root to start the whole process.
	 * @param separateThread whether a separate thread should be created to do it.
	 */
	public void startExecution(boolean separateThread) {
		if (separateThread) {
			thread = new Thread(this);
			thread.start();
		}
		else
			run();
	}

	/** Runnable method. */
	public void run() {
		UndoManager.getCurrent().ignoreCalls(true);
		if (startExecution(0))
			UndoManager.getCurrent().ignoreCalls(false);
	}

	public void resetChildren() {
		for (int e = 0; e < size(); e++)
			get(e).resetExecute();
	}

	public void resetExecute() {
		resetChildren();
	}

	public String toString() {
		return "ExecutableRunner("
			+ (debugName == null ? "" : debugName + ",")
			+ "childCount=" + size() + ")";
	}
}
