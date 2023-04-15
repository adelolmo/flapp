package flands;

/**
 * Once a page is fully loaded, execution begins: the Executable objects
 * (usually Nodes) within that page are executed, one by one.
 * In the case of most nodes, this will enable them; in the case of hidden nodes,
 * their action will be immediately executed as well. Execution may then pause
 * on that node, or continue to the next one in the list (really a tree).
 * 
 * @author Jonathan Mann
 */
public interface Executable {
	/**
	 * Execute the activity/ies associated with this object.
	 * If the activities are blocked, this Executable should call back the
	 * grouper later via the {@link #continueExecution} method once it has finished.
	 * @param grouper the object running this activity.  This can be called to
	 * find out other details, such as whether this is running on a separate thread.
	 * @return <code>true</code> when or if this object has finished its job;
	 * <code>false</code> if it has blocked, waiting for user input.
	 */
	public boolean execute(ExecutableGrouper grouper);
	/**
	 * Reset, so that we're back to the state we were in before being executed.
	 * This will get called before execute is called again.
	 * The context is a while loop, or if the player undo-es the most recent action.
	 */
	public void resetExecute();
}
