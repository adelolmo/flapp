package flands;


import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Designed after the <a href="http://www.horstmann.com/articles/Taming_the_GridBagLayout.html">examples</a>
 * given by Cay Horstmann at his site.
 * @author Jonathan Mann
 */
public final class GBC extends GridBagConstraints implements Cloneable {
    /**
     * Create a new GBC object.  Initially it has the following settings:
     * <ul>
     * <li>gridx = gridy = 0
     * <li>gridwidth = gridheight = 1
     * <li>weightx = weighty = 0
     * <li>insets = [0,0,0,0]
     * <li>fill = NONE
     * <li>anchor = CENTER
     * <li>ipadx = ipady = 0
     * </ul>
     */
    public GBC() {
		gridx = gridy = 0;
		gridwidth = gridheight = 1;
		weightx = weighty = 0;
		resetInsets();
		fill = NONE;
		anchor = CENTER;
		ipadx = ipady = 0;
    }

    public GBC(int x, int y) {
		this();
		setLocation(x, y);
    }

    public GBC setLocation(int x, int y) {
		gridx = x;
		gridy = y;
		return this;
    }

    public GBC setSpan(int w, int h) {
		gridwidth = w;
		gridheight = h;
		return this;
    }
    public GBC setUnitSpan() {
		gridwidth = gridheight = 1;
		return this;
    }

    public GBC setWeight(double x, double y) {
		weightx = x;
		weighty = y;
		return this;
    }
    public GBC setNoWeight() {
		weightx = weighty = 0;
		return this;
    }

    public GBC setFill(int i) {
		fill = i;
		return this;
    }

    public GBC setNoFill() {
		return setFill(NONE);
    }
    public GBC setHorizFill() {
		return setFill(HORIZONTAL);
    }
    public GBC setVertFill() {
		return setFill(VERTICAL);
    }
    public GBC setBothFill() {
		return setFill(BOTH);
    }

    public GBC setAnchor(int a) {
		anchor = a;
		return this;
    }
    public GBC resetAnchor() {
		return setAnchor(CENTER);
    }

    public GBC setInsets(int top, int left, int bottom, int right) {
		insets.top = top;
		insets.left = left;
		insets.bottom = bottom;
		insets.right = right;
		return this;
    }
    public GBC setInsets(Insets ins) {
		insets = ins;
		return this;
    }
    public GBC resetInsets() {
		return setInsets(0, 0, 0, 0);
    }

    public GBC setPad(int x, int y) {
		ipadx = x;
		ipady = y;
		return this;
    }
    public GBC setNoPad() {
		return setPad(0, 0);
    }

    public void addComp(Container co, Component comp, GridBagLayout gbl) {
		gbl.setConstraints(comp, this);
		co.add(comp);
    }

    public void addComp(Container co, Component comp, GridBagLayout gbl, int x, int y) {
		setLocation(x, y).addComp(co, comp, gbl);
    }

    public void setValues(String strVal) {
		synchronized (this) {
			str = strVal.trim().toLowerCase();
			parseIndex = 0;
			len = str.length();
			while (parse()) ;
		}
    }

    private String str;
    private int len;
    private int parseIndex;
    private boolean parse() {
		while (parseIndex < len && !Character.isLetter(str.charAt(parseIndex)))
			parseIndex++;
		if (parseIndex == len)
			return false;

		String s = str.substring(parseIndex);
		if (s.startsWith("x") || s.startsWith("gridx")) {
			gridx = parseInt(gridx);
		}
		else if (s.startsWith("y") || s.startsWith("gridy")) {
			gridy = parseInt(gridy);
		}
		else if (s.startsWith("width") || s.startsWith("gridwidth")) {
			gridwidth = parseInt(gridwidth);
		}
		else if (s.startsWith("height") || s.startsWith("gridheight")) {
			gridheight = parseInt(gridheight);
		}
		else if (s.startsWith("ipadx")) {
			ipadx = parseInt(ipadx);
		}
		else if (s.startsWith("ipady")) {
			ipady = parseInt(ipady);
		}
		else if (s.startsWith("weightx")) {
			weightx = parseDouble(weightx);
		}
		else if (s.startsWith("weighty")) {
			weighty = parseDouble(weighty);
		}
		else if (s.startsWith("fill")) {
			parseFill();
		}
		else if (s.startsWith("anchor")) {
			parseAnchor();
		}
		else if (s.startsWith("inset")) {
			parseInsets();
		}
		else {
			int badToken = parseIndex++;
			while (parseIndex < len && Character.isLetter(str.charAt(parseIndex)))
			parseIndex++;
			System.out.println("Unrecognised token: " + str.substring(badToken, parseIndex));
		}

		return (parseIndex < len);
    }

    private int parseInt(int defaultVal) {
		int val = defaultVal;
		while (parseIndex < len && !Character.isDigit(str.charAt(parseIndex)) && str.charAt(parseIndex) != '-')
			parseIndex++;
		if (parseIndex == len)
			return val;

		int nextIndex = parseIndex + 1;
		while (nextIndex < len && Character.isDigit(str.charAt(nextIndex)))
			nextIndex++;

		try {
			val = Integer.parseInt(str.substring(parseIndex, nextIndex));
		}
		catch (NumberFormatException nfe) {
			System.out.println("Bad integer in substring: " + str.substring(parseIndex, nextIndex));
		}
		parseIndex = nextIndex;
		return val;
    }

    private double parseDouble(double defaultVal) {
		double val = defaultVal;
		while (parseIndex < len && !Character.isDigit(str.charAt(parseIndex)) && str.charAt(parseIndex) != '-')
			parseIndex++;
		if (parseIndex == len)
			return val;

		int nextIndex = parseIndex + 1;
		while (nextIndex < len && (Character.isDigit(str.charAt(nextIndex)) || str.charAt(nextIndex) == '.'))
			nextIndex++;
		try {
			val = Double.parseDouble(str.substring(parseIndex, nextIndex));
		}
		catch (NumberFormatException nfe) {
			System.out.println("Bad double in substring: " + str.substring(parseIndex, nextIndex));
		}
		parseIndex = nextIndex;
		return val;
    }

    private void parseFill() {
		parseIndex += 4; // skip the "fill"
		while (parseIndex < len && !Character.isLetter(str.charAt(parseIndex)))
			parseIndex++;
		switch (str.charAt(parseIndex)) {
		case 'b':
			fill = BOTH;
			break;
		case 'h':
			fill = HORIZONTAL;
			break;
		case 'v':
			fill = VERTICAL;
			break;
		case 'n':
			fill = NONE;
			break;
		default:
			System.out.println("Unrecognised fill type: " + str.substring(parseIndex));
		}
		while (parseIndex < len && Character.isLetter(str.charAt(parseIndex)))
			parseIndex++;
    }

    private void parseAnchor() {
		parseIndex += 6; // skip "anchor"
		while (parseIndex < len && !Character.isLetter(str.charAt(parseIndex)))
			parseIndex++;
		if (parseIndex == len)
			return;
		String s = str.substring(parseIndex);
		if (s.startsWith("c"))
			anchor = CENTER;
		else if (s.startsWith("nw") || s.startsWith("northwest"))
			anchor = NORTHWEST;
		else if (s.startsWith("ne") || s.startsWith("northeast"))
			anchor = NORTHEAST;
		else if (s.startsWith("sw") || s.startsWith("southwest"))
			anchor = SOUTHWEST;
		else if (s.startsWith("se") || s.startsWith("southeast"))
			anchor = SOUTHEAST;
		else if (s.startsWith("n"))
			anchor = NORTH;
		else if (s.startsWith("e"))
			anchor = EAST;
		else if (s.startsWith("s"))
			anchor = SOUTH;
		else if (s.startsWith("w"))
			anchor = WEST;
		else
			System.out.println("Unrecognised anchor type: " + s);
		while (parseIndex < len && Character.isLetter(str.charAt(parseIndex)))
			parseIndex++;
    }

    private void parseInsets() {
		if (len < parseIndex + 6) {
			parseIndex = len;
			return;
		}

		parseIndex += 6;
		switch (str.charAt(parseIndex-1)) { // character after "inset"
			// We allow "inset[tlbr]" for setting one value
			// or "insets" for all four
		case 't':
			insets.top = parseInt(insets.top);
			break;
		case 'l':
			insets.left = parseInt(insets.left);
			break;
		case 'b':
			insets.bottom = parseInt(insets.bottom);
			break;
		case 'r':
			insets.right = parseInt(insets.right);
			break;
		case 's':
			// "insets" - all four values given
			insets.top = parseInt(insets.top);
			insets.left = parseInt(insets.left);
			insets.bottom = parseInt(insets.bottom);
			insets.right = parseInt(insets.right);
			break;
		default:
			System.out.println("Bad token: " + str.substring(parseIndex - 6));
			while (parseIndex < len && Character.isLetter(str.charAt(parseIndex)))
			parseIndex++;
		}
    }

    public String toString() {
		return "["
			+"gridx=" + gridx
			+",gridy=" + gridy
			+",gridwidth=" + gridwidth
			+",gridheight=" + gridheight
			+",weightx=" + weightx
			+",weighty=" + weighty
			+",anchor=" + anchorToString(anchor)
			+",fill=" + fillToString(fill)
			+",ipadx=" + ipadx
			+",ipady=" + ipady
			+",insets="+insets
			+"]";
    }

    private static String anchorToString(int anchor) {
		switch (anchor) {
		case CENTER:
			return "C";
		case NORTH:
			return "N";
		case NORTHEAST:
			return "NE";
		case EAST:
			return "E";
		case SOUTHEAST:
			return "SE";
		case SOUTH:
			return "S";
		case SOUTHWEST:
			return "SW";
		case WEST:
			return "W";
		case NORTHWEST:
			return "NW";
		default:
			return "?";
		}
    }

    private static String fillToString(int fill) {
		switch (fill) {
		case BOTH:
			return "BOTH";
		case HORIZONTAL:
			return "HORIZONTAL";
		case VERTICAL:
			return "VERTICAL";
		case NONE:
			return "NONE";
		default:
			return "?";
		}
    }

	public Object clone() {
		return super.clone();
	}

    public static void main(String args[]) {
		for (int a = 0; a < args.length; a++) {
			if (a > 0)
			System.out.println();
			System.out.println("Params=" + args[a]);
			GBC gbc = new GBC();
			gbc.setValues(args[a]);
			System.out.println("GBC=" + gbc);

			if (args.length == 1) {
				long startms = System.currentTimeMillis();
				for (int i = 0; i < 100; i++)
					gbc.setValues(args[0]);
				long endms = System.currentTimeMillis();
				System.out.println("Parse time=" + (endms-startms/(double)100));
			}
		}
    }
}
