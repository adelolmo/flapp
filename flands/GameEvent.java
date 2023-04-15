package flands;

/**
 * A 'game'-level event that may be listened for.
 * 
 * @see GameListener
 * @see FLApp#addGameListener(GameListener)
 * @see FLApp#fireGameEvent(int)
 * 
 * @author Jonathan Mann
 */
public class GameEvent {
	public static final int NEW_SECTION = 0;
	public static final int NEW_BOOK = 1;
	public static final int FIGHT_START = 2;
	public static final int FIGHT_END = 3;
	public static final int DIFFICULTY_SUCCESS = 4;
	public static final int DIFFICULTY_FAILURE = 5;
	public static final int ROLL_OCCURRED = 6;
	public static final int PLAYER_INJURY = 7;
	public static final int OTHER_EVENT = 8; // punctuates other events that are cared about

	private int id;
	public GameEvent(int id) {
		this.id = id;
	}

	public int getID() {
		return id;
	}
}
