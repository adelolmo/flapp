package flands;

/**
 * An object that can collect a sequence of Executable objects, and handles calling
 * them one by one.
 * 
 * @see Node#getExecutableGrouper()
 * @see Node#findExecutableGrouper()
 * @author Jonathan Mann
 */
public interface ExecutableGrouper {
	/**
	 * Add an Executable to the list of objects that will be triggered, in order.
	 */
	public void addExecutable(Executable e);
	/**
	 * Add a Node to the list of Executables.
	 * It will have setEnabled() called when all preceding Executables have been executed.
	 */
	public void addIntermediateNode(Node n);
	/**
	 * Notification from an Executable that it has finished its activity/ies.
	 * The next Executable can now be triggered.
	 * @param eDone the Executable that has finished;
	 * may be <code>null</code> to indicate that execution should commence from the first Executable.
	 * @param inSeparateThread whether this thread is already separate.
	 */
	public void continueExecution(Executable eDone, boolean inSeparateThread);
	/** Query whether we're currently running Executables on a separate thread. */
	public boolean isSeparateThread();
}
