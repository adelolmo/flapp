package flands;


import java.text.MessageFormat;

/**
 * One of the character's titles. This may be a simple text string, or a pattern with a value
 * that can be checked and modified (eg. the 'rank X of Bokh' title in book 5).
 * 
 * @author Jonathan Mann
 */
public class Title {
	private String title, pattern;
	private int value;
	public Title(String title) {
		this.title = title;
		pattern = null;
	}
	public Title(String key, String pattern, int value) {
		this.title = key;
		this.pattern = pattern;
		this.value = value;
	}

	public boolean matches(String lowerCaseTitle) {
		return lowerCaseTitle.equals(this.title.toLowerCase());
	}

	public String getTitle() {
		return title;
	}

	public int getValue() { return value; }
	public void adjustValue(int delta) {
		value += delta;
	}
	public void setValue(int value) {
		this.value = value;
	}

	public String toString() {
		if (pattern == null)
			return title;
		else
			return MessageFormat.format(pattern, Integer.valueOf(value));
	}
	
	public String toLoadableString() {
		if (pattern == null)
			return title;
		else
			return title + "," + value + "," + pattern;
	}
	
	public static Title createFromLoadableString(String str) {
		int comma1 = str.indexOf(',');
		if (comma1 >= 0) {
			int comma2 = str.indexOf(',', comma1+1);
			if (comma2 >= 0) {
				String valStr = str.substring(comma1+1, comma2);
				try {
					int val = Integer.parseInt(valStr);
					return new Title(str.substring(0, comma1), str.substring(comma2+1), val);
				}
				catch (NumberFormatException nfe) {
					System.err.println("Title.create(): couldn't parse value from: " + str);
				}
			}
		}
		
		return new Title(str);
	}
}
