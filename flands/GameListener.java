package flands;

/**
 * An interface for classes interested in game-level events.
 * 
 * @see GameEvent
 * @see FLApp#addGameListener(GameListener)
 * @see FLApp#fireGameEvent(int)
 * 
 * @author Jonathan Mann
 */
public interface GameListener {
	public void eventOccurred(GameEvent evt);
}
