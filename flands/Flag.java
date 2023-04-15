package flands;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;


/**
 * Each section may use one or more named flags, each of which is either 'clear'
 * or 'set'. Various node type may register as listeners, so that they are enabled
 * or disabled depending on the flag's state.
 * @author Jonathan Mann
 */
public class Flag {
	/**
	 * A set of flags, arranged by name.
	 * Moves the formerly static flagMap into a class that can be held by Adventurer,
	 * letting it be saved with the rest of the game data.
	 */
	public static class Set {
		private Map<String,Flag> flagMap = new HashMap<String,Flag>();
		
		public Flag getFlag(String name) {
			Flag f = flagMap.get(name);
			if (f == null) {
				f = new Flag(name);
				flagMap.put(name, f);
			}
			return f;
		}
		
		// Two shortcut methods
		public boolean getState(String name) {
			return getFlag(name).getState();
		}
		public void setState(String name, boolean b) {
			getFlag(name).setState(b);
		}
		
		private void removeFlag(String name) {
			flagMap.remove(name);
		}
		
		public void addListener(String name, Listener l) {
			getFlag(name).addListener(l);
		}
		
		public void removeListener(String name, Listener l) {
			Flag f = flagMap.get(name);
			if (f != null) {
				if (f.removeListener(l))
					removeFlag(name);
			}
		}
		
		// TODO: Complete the following methods, make them part of the Flag.Map inner class
		// which is held by Adventurer, change all static refs to Flag to refs to this
		// Adventurer.getFlags() instance.
		public void saveTo(Properties props) {
			int flagCount = flagMap.size();
			props.setProperty("FlagCount", Integer.toString(flagCount));
			flagCount = 0;
			for (Iterator<Entry<String,Flag> > i = flagMap.entrySet().iterator(); i.hasNext(); ) {
				Entry<String,Flag> e = i.next();
				props.setProperty("FlagName" + flagCount, e.getKey());
				props.setProperty("FlagValue" + flagCount, e.getValue().state ? "1" : "0");
				flagCount++;
			}
		}
		
		public void loadFrom(Properties props) {
			int flagCount = Integer.parseInt(props.getProperty("FlagCount", "0"));
			flagMap.clear();
			for (int i = 0; i < flagCount; i++) {
				String name = props.getProperty("FlagName" + i);
				String value = props.getProperty("FlagValue" + i, "0");
				getFlag(name).setState(value.equals("1"));
			}
		}
	}
	
	public static interface Listener {
		public void flagChanged(String name, boolean state);
	}
	
	private final String name;
	private boolean state;
	private LinkedList<Listener> listeners;

	public Flag(String name) {
		this.name = name;
		this.state = false;
		listeners = new LinkedList<Listener>();
	}

	public String getName() { return name; }
	public boolean getState() { return state; }
	public void setState(boolean b) {
		if (state != b) {
			state = b;
			for (Iterator<Listener> i = listeners.iterator(); i.hasNext(); )
				i.next().flagChanged(name, b);
		}
	}

	private void addListener(Listener l) {
		if (!listeners.contains(l))
			listeners.add(l);
	}
	/** Return <code>true</code> if the number of listeners is now zero. */
	private boolean removeListener(Listener l) {
		listeners.remove(l);
		if (listeners.size() == 0) {
			System.out.println("Flag '" + name + "' removing itself");
			return true;
		}
		return false;
	}
}