package flands;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * A codeword, as used by FLApp, is a keyword with an integer value, shared
 * across all sections (unlike variables, which are local to a section).
 * There are known (named) codewords, checkbox values (usually named by
 * book and section number), and hidden codewords so that we can track
 * certain events that don't have codewords attached.
 * 
 * @author Jonathan Mann
 */
public class Codewords implements Loadable {
	// The adventurer that has this set of codewords
	private Adventurer owner;

	/*
	 * A mapping from codeword->"0" or "1"
	 * Also from section number->"integer"
	 * A better representation would be a set, but this has built in load and save
	 * and a simple (editable) format.
	 */
	private Properties props;

	public Codewords(Adventurer adv) {
		props = new Properties();
		owner = adv;
	}

	/**
	 * Returns <code>true</code> if the player has the given codeword.
	 */
	public boolean hasCodeword(String word) {
		refresh();
		return !props.getProperty(word, "0").equals("0");
	}
	/**
	 * Add a codeword.
	 */
	public void addCodeword(String word) {
		props.setProperty(word, "1");
		update(word);
	}
	/**
	 * Remove the given codeword.
	 * @return <code>true</code> if it was there to be removed.
	 */
	public boolean removeCodeword(String word) {
		refresh();
		Object val = props.remove(word);
		if (val != null) {
			update(word);
			return (val.equals("1"));
		}
		else
			return false;
	}

	public int getTickCount(String section) {
		return getTickCount(Address.getCurrentBookKey(), section);
	}
	public int getTickCount(String book, String section) {
		return getValue(book + "/" + section);
	}
	public final void addTick(String section) {
		addTicks(Address.getCurrentBookKey() + "/" + section, 1);
	}
	public void addTicks(String section, int ticks) {
		adjustValue(Address.getCurrentBookKey() + "/" + section, ticks);
	}

	public int getValue(String name) {
		refresh();
		String val = props.getProperty(name, null);
		if (val != null) {
			try {
				return Integer.parseInt(val);
			}
			catch (NumberFormatException nfe) {
				System.err.println("Error parsing integer val for codeword " + name + ": " + nfe);
			}
		}
		return 0;
	}

	public void adjustValue(String name, int delta) {
		int val = getValue(name);
		setValue(name, val + delta);
	}

	public void setValue(String name, int value) {
		props.setProperty(name, Integer.toString(value));
		update(name);
	}

	public void clear() {
		props.clear();
		update(null);
	}

	private static final String propFilename = "codewords.ini";
	private long lastSynch = 0;
	/**
	 * Called to reload the codewords from file.
	 * This is called before most hasCodeword() calls.
	 */
	public void refresh() {
		if (FLApp.debugging) {
			long time = System.currentTimeMillis();
			if (time - lastSynch > 2000)
				load(owner.getFolderName());
			lastSynch = time;
		}
	}

	public void update(String key) {
		if (FLApp.debugging) {
			if (save(owner.getFolderName()))
				lastSynch = System.currentTimeMillis();
		}
		fireChangeEvent(key);
	}

	private void load(String path) {
		if (path == null) return;
		if (path.length() > 0 && !path.endsWith(File.separator))
			path = path + File.separator;
		path += propFilename;

		try {
			FileInputStream in = new FileInputStream(path);
			props.load(in);
		}
		catch (FileNotFoundException fnfe) {
			// OK, default to no codewords
			props.clear();
		}
		catch (IOException ioe) {
			System.err.println("Error in loading codewords from: " + path);
			props.clear();
		}
	}

	private boolean save(String path) {
		if (path.length() > 0 && !path.endsWith(File.separator))
			path = path + File.separator;

		// Make sure the adventurer directory exists
		File f = new File(path);
		if (!f.exists())
			f.mkdir();

		path += propFilename;
		try {
			FileOutputStream out = new FileOutputStream(path);
			props.store(out, null);
			return true;
		}
		catch (FileNotFoundException fnfe) {
			System.err.println("Error in creating output file: " + path);
		}
		catch (IOException ioe) {
			System.err.println("Error in saving codewards to: " + path);
		}
		return false;
	}
	
	private List<Object> listeners;
	public void addChangeListener(String key, ChangeListener l) {
		if (listeners == null)
			listeners = new ArrayList<Object>();
		listeners.add(key);
		listeners.add(l);
	}
	public void addChangeListener(ChangeListener l) { addChangeListener(null, l); }
	public void removeChangeListener(ChangeListener l) {
		if (listeners == null) return;
		for (int i = 1; i < listeners.size(); i += 2) {
			if (listeners.get(i) == l) {
				listeners.remove(i);
				listeners.remove(i-1);
				i -= 2;
			}
		}
	}
	public void fireChangeEvent(String key) {
		if (listeners != null && key != null)
			for (int i = 0; i < listeners.size(); i += 2)
				if (listeners.get(i) == null || listeners.get(i).equals(key))
					((ChangeListener)listeners.get(i+1)).stateChanged(new ChangeEvent(key));
	}

	public String getFilename() {
		return "codewords.ini";
	}

	public boolean loadFrom(InputStream in) throws IOException {
		// TODO: Notify any listeners?
		props.clear();
		props.load(in);
		update(null);
		props.list(System.out);
		return true;
	}

	public boolean saveTo(OutputStream out) throws IOException {
		props.store(out, null);
		return true;
	}
	
	private static final String NotesKey = "*AdventurerNotes*";
	public String getNotes() {
		refresh();
		return props.getProperty(NotesKey);
	}
	
	public void setNotes(String text) {
		props.setProperty(NotesKey, text);
		update(NotesKey);
	}
}
