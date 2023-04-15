package flands;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * Extension of Properties that allows easy use of integer and boolean values,
 * and arrays of these or Strings. Designed primarily for use by Loadable
 * implementors.
 * @author Jonathan Mann
 */
public final class ExtProperties extends Properties implements Loadable {
	private static final String defaultFilename = "props.ini";

	private String filename;

	public ExtProperties() {
		this(defaultFilename);
	}

	public ExtProperties(String filename) {
		super();
		this.filename = filename;
	}

	public ExtProperties(Properties props) {
		this(defaultFilename);
	}

	public ExtProperties(Properties props, String filename) {
		super(props);
		this.filename = filename;
	}

	public int getInt(String name, int defaultVal) {
		String val = getProperty(name);
		if (val != null) {
			try {
				return Integer.parseInt(val);
			} catch (NumberFormatException nfe) {
				System.err.println("getInt(): property '" + name
						+ "' value is not an int: " + val);
			}
		}
		return defaultVal;
	}

	public boolean getBoolean(String name, boolean defaultVal) {
		String val = getProperty(name);
		if (val != null) {
			char c = val.charAt(0);
			if (c == '0' || c == 'f' || c == 'F')
				return false;
			else if (c == 't' || c == 'T' || (c > '0' && c <= '9'))
				return true;
		}
		return defaultVal;
	}

	public String[] getArray(String name) {
		int count = getInt(name, 0);
		if (count <= 0)
			return new String[0];
		String[] arr = new String[count];
		for (int i = 0; i < count; i++)
			arr[i] = getProperty(name + i);
		return arr;
	}

	public int[] getIntArray(String name, int defaultVal) {
		int count = getInt(name, 0);
		if (count <= 0)
			return new int[0];
		int[] arr = new int[count];
		for (int i = 0; i < count; i++)
			arr[i] = getInt(name + i, defaultVal);
		return arr;
	}

	public boolean[] getBooleanArray(String name, boolean defaultVal) {
		int count = getInt(name, 0);
		if (count <= 0)
			return new boolean[0];
		boolean[] arr = new boolean[count];
		for (int i = 0; i < count; i++)
			arr[i] = getBoolean(name + i, defaultVal);
		return arr;
	}

	/** Overridden to call remove instead of setProperty if the value is <code>null</code>. */
	public Object setProperty(String key, String value) {
		if (value == null)
			return remove(key);
		else
			return super.setProperty(key, value);
	}
	
	public void set(String name, int val) {
		setProperty(name, Integer.toString(val));
	}

	public void set(String name, boolean val) {
		setProperty(name, val ? "T" : "F");
	}

	public void set(String name, String[] arr) {
		if (arr == null)
			setProperty(name, "0");
		else {
			set(name, arr.length);
			for (int i = 0; i < arr.length; i++)
				setProperty(name + i, arr[i]);
		}
	}

	public void set(String name, int[] arr) {
		if (arr == null)
			setProperty(name, "0");
		else {
			set(name, arr.length);
			for (int i = 0; i < arr.length; i++)
				set(name + i, arr[i]);
		}
	}

	public void set(String name, boolean[] arr) {
		if (arr == null)
			setProperty(name, "0");
		else {
			set(name, arr.length);
			for (int i = 0; i < arr.length; i++)
				set(name + i, arr[i]);
		}
	}

	/* ****************
	 * Loadable methods
	 **************** */
	public String getFilename() {
		return filename;
	}

	public boolean loadFrom(InputStream in) throws IOException {
		try {
			load(in);
			return true;
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return false;
		}
	}

	public boolean saveTo(OutputStream out) throws IOException {
		try {
			store(out, null);
			return true;
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return false;
		}
	}
}
