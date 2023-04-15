package flands;


import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Segment;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StringContent;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

/**
 * The main document for a section being displayed. This is built up by each Node during
 * XML parsing, using the various methods for adding content. Messy stuff.
 * 
 * @author Jonathan Mann
 */
public class SectionDocument extends AbstractDocument implements StyledDocument {
	private RootElement rootNode;

	private static Font preferredFont = null;
	private static float fontAlignment = 0.5f;
	private static int smallerFontSize = 0;
	public static Font getPreferredFont() {
		if (preferredFont == null)
			findFonts();
		return preferredFont;
	}
	public static float getFontVerticalAlignment() {
		if (preferredFont == null)
			findFonts();
		return fontAlignment;
	}
	public static int getSmallerCapsFontSize() {
		if (smallerFontSize == 0)
			findFonts();
		return smallerFontSize;
	}
	public static SimpleAttributeSet getSmallerAtts(AttributeSet parentAtts) {
		SimpleAttributeSet atts = (parentAtts == null ? new SimpleAttributeSet() : new SimpleAttributeSet(parentAtts));
		StyleConstants.setFontSize(atts, getSmallerCapsFontSize());
		return atts;
	}
	
	@SuppressWarnings("deprecation")
	private static void findFonts() {
		String[] availableFonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		try {
			Properties props = new Properties();
			props.load(new FileInputStream("user.ini"));
			for (int i = 0; true; i++) {
				String line = props.getProperty("font" + i);
				if (line == null) {
					if (i > 5)
						break;
					else
						continue;
				}

				int comma1 = line.indexOf(',');
				int comma2 = line.indexOf(',', comma1+1);
				if (comma1 < 0 || comma2 < 0) {
					System.out.println("Bad line in options file: expected 'font,size,smaller size'");
					continue;
				}

				String fontname = line.substring(0, comma1).trim();
				if (Arrays.binarySearch(availableFonts, fontname, String.CASE_INSENSITIVE_ORDER) >= 0) {
					int size = Integer.parseInt(line.substring(comma1+1,comma2).trim());
					int smallerSize = Integer.parseInt(line.substring(comma2+1).trim());
					preferredFont = new Font(fontname, Font.PLAIN, size);
					smallerFontSize = smallerSize;
					break;
				}
				System.err.println("Font '" + fontname + "' isn't available");
			}
		}
		catch (IOException ioe) {
			System.err.println("Error in reading options file");
			ioe.printStackTrace();
		}
		catch (NumberFormatException nfe) {
			System.err.println("Bad number in options file");
			nfe.printStackTrace();
		}

		if (preferredFont == null) {
			preferredFont = new Font("Palatino Linotype", Font.PLAIN, 15);
			smallerFontSize = 11;
		}
		
		// This call is deprecated. So sue me.
		FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(preferredFont);
		fontAlignment = ((float)fm.getLeading()/2 + fm.getAscent()) / fm.getHeight();
	}

	public SectionDocument() {
		super(new StringContent());

		// Define the root style
		if (context == null) {
			context = new StyleContext();
			Style rootStyle = addStyle(StyleContext.DEFAULT_STYLE, null);
			Font f = getPreferredFont();
			StyleConstants.setFontFamily(rootStyle, f.getFamily());
			StyleConstants.setFontSize(rootStyle, f.getSize());
			rootStyle.addChangeListener(new javax.swing.event.ChangeListener() 
			{
				public void stateChanged(javax.swing.event.ChangeEvent evt) 
				{
					System.out.println("Root style changed: " + evt);
				}
			});
		}
	}

	public Element getDefaultRootElement() { return rootNode; }

	/* ****************
	 * Document methods
	 **************** */
	/** Overridden to do nothing. */
	public void insertString(int offset, String str, AttributeSet a) {}

	/** Overridden to do nothing. */
	public void remove(int offs, int len) throws BadLocationException {}

	/**
	 * Adds some text to the content, without doing anything with the elements.
	 * This is our preferred (hidden) interface.
	 */
	void addContent(String str) {
		insertContent(getLength(), str);
	}
	void insertContent(int offset, String str) {
		try {
			getContent().insertString(offset, str);
		}
		catch (BadLocationException e) {
			System.out.println("insertContent(): " + e);
		}
	}

	/**
	 * Replace some of the content with the given text.
	 */
	public void replaceContent(int offset, int len, String str) {
		if (len != str.length())
			System.err.println("Error: replaceContent called with len != str.length(); this would result in bad errors");
		else {
			Element e = getCharacterElement(offset);
			try {
				getContent().remove(offset, len);
				getContent().insertString(offset, str);
			}
			catch (BadLocationException ble) {}
			fireChangeEvent(e);
		}
	}

	public boolean isNewSentence() { return isNewSentence(getLength()); }

	public boolean isNewSentence(int offset) {
		// Look for a previous end-of-sentence
		Segment txt = new Segment();
		try {
			while (--offset >= 0) {
				getContent().getChars(offset, 1, txt);
				char ch = txt.array[txt.offset];
				if (ch == '.' || ch == '!' || ch == '?' || Character.isDigit(ch)) {
					System.out.println("Found sentence ender: " + ch);
					return true;
				}
				else if (Character.isWhitespace(ch) || ch == '\'') {
					//System.out.println("Skipping whitespace");
				}
				else if (ch == '\'' || ch == '\u2019' || ch == ')') {
					System.out.println("Skipping possible end-quote - is this correct?");
				}
				else {
					System.out.println("Found non-sentence ender: " + ch);
					return false;
				}
			}
		}
		catch (BadLocationException ble) {
			System.err.println("Exception in isNewSentence(): " + ble);
		}
		return true;
	}

	/**
	 * Utility method: add a set of LeafElements to the given BranchElement.
	 * @param attrs this array may be null, rather than passing an array of nulls.
	 * @return an array of the leaves created
	 * @see {#addLeavesTo(Element,StyledText[])}
	 */
	Element[] addLeavesTo(Element parentElement, String[] contents, AttributeSet[] attrs) {
		StyledText[] styledText = new StyledText[contents.length];
		for (int i = 0; i < styledText.length; i++)
			styledText[i] = new StyledText(contents[i], attrs == null ? null : attrs[i]);
		return addLeavesTo(parentElement, styledText);
	}

	/**
	 * Utility method: add a set of LeafElements to the given BranchElement.
	 * @return an array of the leaves created
	 */
	Element[] addLeavesTo(Element parentElement, StyledText[] styledText) {
		if (!parentElement.isLeaf()) {
			BranchElement branch = (BranchElement)parentElement;
			if (branch.getElementCount() == 0)
				// The first leaf shouldn't have any leading spaces
				styledText[0].text = Node.trimStart(styledText[0].text);
			List<Element> leafList = new LinkedList<Element>();
			for (int i = 0; i < styledText.length; i++)
				addLeavesTo(branch, styledText[i], leafList);
			Element[] leaves = new Element[leafList.size()];
			leaves = leafList.toArray(leaves);
			branch.replace(branch.getElementCount(), 0, leaves);
			return leaves;
		}
		else {
			System.out.println("Can't add children to leaf: " + parentElement);
			return null;
		}
	}

	/** Immediately add content and a leaf to handle the content. */
	private final void addLeafTo(BranchElement parentElement, StyledText styledText, List<Element> leafList) {
		int startOffset = getLength();
		addContent(styledText.text);
		leafList.add(createLeafElement(parentElement, styledText.atts, startOffset, getLength()));
	}

	private static String[] capsWords = null;
	/** Get a list of words that should be 'capitalized'. */
	public static String[] getCapsWords() {
		if (capsWords == null) {
			capsWords = new String[Adventurer.ABILITY_COUNT];
			for (int a = 0; a < Adventurer.ABILITY_COUNT; a++)
				capsWords[a] = Adventurer.getAbilityName(a).toUpperCase();
		}
		return capsWords;
	}

	private static boolean timing = false;
	private static boolean method = true;
	private static int rootIterations = 0;
	private static long totalMs = 0;

	static void printTimingInfo() {
		System.out.println("addLeavesTo() timing info (method " + (method ? "1" : "2") + ")");
		System.out.println("Root iterations: " + rootIterations);
		System.out.println("Total time=" + totalMs + ", average=" + (totalMs/(double)rootIterations));
	}

	/**
	 * Add a child leaf.  If the text contains a word to be capitalized, this may create several leaves.
	 */
	private void addLeavesTo(BranchElement parentElement, StyledText styledText, List<Element> leafList) {
	    // Go looking for a string of all-uppercase letters
	    // Alternatively, we could reverse this by searching for each known capitalized word in the text
	    // Which way is faster?
	    // TODO: Attach timers to find out which method is fastest.
		boolean usTiming = false;
		if (!timing) {
			timing = true;
			usTiming = true;
			rootIterations++;
		}
		long start = System.currentTimeMillis();
		if (method) {
			String text = styledText.text;
			for (int i = 0; i < text.length(); i++)
				if (Character.isLetter(text.charAt(i)) && Character.isUpperCase(text.charAt(i))) {
					int j = i+1;
					for (; j < text.length(); j++)
						if (!Character.isLetter(text.charAt(j)) || !Character.isUpperCase(text.charAt(j)))
							break;
								
					// See if this matches any known words
					String capsString = text.substring(i, j);
					String[] words = getCapsWords();
					for (int w = 0; w < words.length; w++)
						if (words[w].equals(capsString)) {
							SimpleAttributeSet smallerFontAtts = getSmallerAtts(styledText.atts);
							addLeafTo(parentElement, new StyledText(text.substring(0, i+1), styledText.atts), leafList);
							addLeafTo(parentElement, new StyledText(text.substring(i+1, j), smallerFontAtts), leafList);
							if (j < text.length())
								// Recursively handle the rest of the text
								addLeavesTo(parentElement, new StyledText(text.substring(j), styledText.atts), leafList);
							return;
						}
				}

			// No caps - handle as usual
			addLeafTo(parentElement, styledText, leafList);
		}
		else {
			String text = styledText.text;
			String[] words = getCapsWords();
			for (int w = 0; w < words.length; w++) {
				int index = text.indexOf(words[w]);
				if (index >= 0) {
					SimpleAttributeSet smallerFontAtts = getSmallerAtts(styledText.atts);
					if (index > 0)
						addLeavesTo(parentElement, new StyledText(text.substring(0, index+1), styledText.atts), leafList);
					addLeafTo(parentElement, new StyledText(words[w].substring(1), smallerFontAtts), leafList);
					if (index + words[w].length() < text.length())
						addLeavesTo(parentElement, new StyledText(text.substring(index + words[w].length()), styledText.atts), leafList);
					return;
				}
			}

			// No caps - handle as usual
			addLeafTo(parentElement, styledText, leafList);
		}

		long end = System.currentTimeMillis();
		if (usTiming) {
			timing = false;
			totalMs += (end-start);
		}
	}

	/**
	 * A special case: add text that should contain a 'caps' word - one that is all uppercase, and should be
	 * displayed with all but the first character in a smaller font.
	 */
	Element[] addCapsText(Element parentElement, String text, String capsWord, AttributeSet textAtts) {
		int index = text.indexOf(capsWord);
		if (index < 0)
			return addLeavesTo(parentElement, new String[] { text }, new AttributeSet[] { textAtts });
		else {
			SimpleAttributeSet capsAtts = getSmallerAtts(textAtts);

			String[] contents;
			AttributeSet[] atts;
			if (text.length() == index + capsWord.length()) {
				contents = new String[] { text.substring(0, index+1), text.substring(index+1) };
				atts = new AttributeSet[] { textAtts, capsAtts };
			}
			else {
				contents = new String[] { text.substring(0, index+1), capsWord.substring(1), text.substring(index+capsWord.length()) };
				atts = new AttributeSet[] { textAtts, capsAtts, textAtts };
			}

			return addLeavesTo(parentElement, contents, atts);
		}
	}

	Element[] addStyledText(Element parentElement, String text, String special, AttributeSet textAtts, AttributeSet specialAtts) {
		String[] contents;
		AttributeSet[] atts;
		int index = text.indexOf(special);
		if (index < 0 || (text.length() == special.length())) {
			// Only one leaf here - special not found, or it occupies the whole text
			contents = new String[] { text };
			atts = new AttributeSet[] { (index < 0 ? textAtts : specialAtts) };
		}
		else if (index == 0) {
			// No text before special
			contents = new String[] { special, text.substring(special.length()) };
			atts = new AttributeSet[] { specialAtts, textAtts };
		}
		else if (text.length() == index + special.length()) {
			// No text after special
			contents = new String[] { text.substring(0, index), special };
			atts = new AttributeSet[] { textAtts, specialAtts };
		}
		else {
			contents = new String[] { text.substring(0, index), special, text.substring(index + special.length()) };
			atts = new AttributeSet[] { textAtts, specialAtts, textAtts };
		}

		return addLeavesTo(parentElement, contents, atts);
	}

	/** Copied from DefaultStyledDocument. */
	public Element getParagraphElement(int pos) {
		Element e = getDefaultRootElement();
		while (!e.isLeaf()) {
			int index = e.getElementIndex(pos);
			e = e.getElement(index);
		}
		
		return (e != null ? e.getParentElement() : null);
	}

	/** Copied from DefaultStyledDocument. */
	public Element getCharacterElement(int pos) {
		Element e = null;
		for (e = getDefaultRootElement(); !e.isLeaf(); ) {
			int index = e.getElementIndex(pos);
			e = e.getElement(index);
		}
		return e;
	}

	/** Copied from DefaultStyledDocument. */
	public Style getLogicalStyle(int pos) {
		Style s = null;
		Element paragraph = getParagraphElement(pos);
		if (paragraph != null) {
			AttributeSet a = paragraph.getAttributes();
			AttributeSet parent = a.getResolveParent();
			if (parent instanceof Style) {
				s = (Style) parent;
			}
		}
		return s;
	}
	public void setLogicalStyle(int pos, Style s) {}
	public void setParagraphAttributes(int offset, int length, AttributeSet s, boolean replace) {}
	public void setCharacterAttributes(int offset, int length, AttributeSet s, boolean replace) {}

	/* ********************
	 * StyleContext methods
	 ******************** */
	private static StyleContext context = null;
	public StyleContext getStyleContext() { return context; }

	public Style addStyle(String name, Style parent) {
		return getStyleContext().addStyle(name, parent);
	}
	public void removeStyle(String name) {
		getStyleContext().removeStyle(name);
	}
	public Style getStyle(String name) {
		System.out.println("SectionDocument.getStyle(" + name + ")");
		//Thread.currentThread().dumpStack();
		return getStyleContext().getStyle(name);
	}

	public Color getForeground(AttributeSet attr) {
		return getStyleContext().getForeground(attr);
	}
	public Color getBackground(AttributeSet attr) {
		return getStyleContext().getBackground(attr);
	}
	public Font getFont(AttributeSet attr) {
		return getStyleContext().getFont(attr);
	}

	/* ******************************************
	 * Inner classes (specialized node/elements).
	 ****************************************** */
	void grabWriteLock() { writeLock(); }
	void releaseWriteLock() { writeUnlock(); }

	/**
	 * Methods to create and retrieve common Styles.
	 */
	public Style getRootStyle() {
		return getStyle(StyleContext.DEFAULT_STYLE);
	}

	public static interface FontUser {
		public void fontChanged(Font f, int smallerFontSize);
	}
	
	private static List<FontUser> fontUsers = new LinkedList<FontUser>();
	private static List<Component> componentFontUsers = new LinkedList<Component>();
	public static void addFontUser(FontUser fu) {
		if (!fontUsers.contains(fu))
			fontUsers.add(fu);
	}
	public static void removeFontUser(FontUser fu) {
		fontUsers.remove(fu);
	}
	public static void addComponentFontUser(Component c) {
		if (!componentFontUsers.contains(c))
			componentFontUsers.add(c);
	}
	public static void removeComponentFontUser(Component c) {
		componentFontUsers.remove(c);
	}
	
	@SuppressWarnings("deprecation")
	public static void setPreferredFont(Font f, int size) {
		if (f.getFamily().equals(preferredFont.getFamily()) &&
			f.getSize() == preferredFont.getSize() &&
			smallerFontSize == size)
			return;
		preferredFont = new Font(f.getFamily(), Font.PLAIN, f.getSize());
		smallerFontSize = size;
		if (context != null) {
			Style rootStyle = context.getStyle(StyleContext.DEFAULT_STYLE);
			StyleConstants.setFontFamily(rootStyle, f.getFamily());
			StyleConstants.setFontSize(rootStyle, f.getSize());
		}
		
		// This call is deprecated. So sue me.
		FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(preferredFont);
		fontAlignment = ((float)fm.getLeading()/2 + fm.getAscent()) / fm.getHeight();
		
		for (Iterator<Component> i = componentFontUsers.iterator(); i.hasNext(); )
			i.next().setFont(preferredFont);
		for (Iterator<FontUser> i = fontUsers.iterator(); i.hasNext(); )
			i.next().fontChanged(preferredFont, smallerFontSize);
		
		// Save the chosen font
		Properties props = new Properties();
		try {
			props.load(new FileInputStream("user.ini"));
			props.setProperty("font0", f.getFamily() + "," + f.getSize() + "," + size);
			
			FileOutputStream out = new FileOutputStream("user.ini");
			
			// Add some initial comments
			// See Properties.store(OutputStream, String) for the original code copied here.
			BufferedWriter headerWriter = new BufferedWriter(new OutputStreamWriter(out, "8859_1"));
			writeln(headerWriter, "# To try a new font, add it in the form");
			writeln(headerWriter, "# fontN=<fontname>,<size>,<smaller size>");
			writeln(headerWriter, "# where N is a number, <fontname> is an available font family name");
			writeln(headerWriter, "# <size> is a size for this font, and <smallersize> is what to use when printing");
			writeln(headerWriter, "# the ability names (which contain smaller capitals).");
			writeln(headerWriter, "# The program will try each possible font in order,");
			writeln(headerWriter, "# using the first one that is recognised.");
			writeln(headerWriter, "#");
			headerWriter.flush();
			
			props.store(out, null);
			out.close();
		}
		catch (IOException e) {
			System.err.println("Error in saving preferred font");
			e.printStackTrace();
		}
	}
	private static void writeln(BufferedWriter writer, String line) throws IOException {
		writer.write(line);
		writer.newLine();
	}

	public Style getSectionTitleStyle() {
		Style s = getStyle("SectionTitle");
		if (s == null) {
			// It seems like assigning a parent to the style messes things up
			// So instead we're going to let the Styles resolve by looking at Element parents
			s = addStyle("SectionTitle", null);
			StyleConstants.setBold(s, true);
			StyleConstants.setAlignment(s, StyleConstants.ALIGN_CENTER);
		}
		return s;
	}

	public Style getTableStyle() {
		Style s = getStyle("Table");
		if (s == null) {
			s = addStyle("Table", null);
			TableView.setCellPadding(s, (short)5);
			// Except that to use this, we need to use TableCell, which looks for this setting...
			StyleConstants.setLeftIndent(s, 5.0f);
			StyleConstants.setRightIndent(s, 5.0f);
			StyleConstants.setAlignment(s, StyleConstants.ALIGN_CENTER);
		}
		return s;
	}

	public Style getRightColumnStyle() {
		Style s = getStyle("RightColumn");
		if (s == null) {
			s = addStyle("RightColumn", null);
			StyleConstants.setAlignment(s, StyleConstants.ALIGN_RIGHT);
		}
		return s;
	}

	public RootElement createRootElement() {
		if (rootNode == null) {
			rootNode = new RootElement();
			return rootNode;
		}
		else {
			System.out.println("Document already possesses a root node!");
			return null;
		}
	}

	public class RootElement extends Branch  {
		public RootElement() {
			super(null, getRootStyle());
			addAttribute(Node.ViewTypeAttribute, Node.BoxYViewType);
		}

		public String getName() {
			return AbstractDocument.SectionElementName;
		}
	}


	protected SectionDocument.Branch createBranchElement(Element parent, AttributeSet a) {
		return new Branch(parent, a);
	}

	protected Element createLeafElement(Element parent, AttributeSet a, int p1, int p2) {
		return new ContentElement(parent, a, p1, p2);
	}

	/**
	 * Our subclass of BranchElement.
	 */
	public class Branch extends BranchElement {
		public Branch(Element parent, AttributeSet a) {
			super(parent, a);
		}

		/**
		 * Add some purely textual content (a leaf) to this branch.
		 */
		public Element addLeaf(String text, AttributeSet a) {
			int startOffset = getLength();
			addContent(text);
			Element leaf = createLeafElement(this, a, startOffset, getLength());
			addChild(leaf);
			return leaf;
		}

		public void addChild(Element e) {
			replace(getElementCount(), 0, new Element[] { e });
		}

		void endWithNewline() {
			if (getElementCount() > 0) {
				Element lastChild = getElement(getElementCount() - 1);
				if (lastChild.isLeaf())
					((ContentElement)lastChild).endWithNewline();
			}
		}

		public Element[] getLeaves() {
			List<Element> leaves = new ArrayList<Element>(getElementCount());
			int count = getElementCount();
			for (int i = 0; i < count; i++) {
				Element child = getElement(i);
				if (child.isLeaf())
					leaves.add(child);
			}
			Element[] result = new Element[leaves.size()];
			return leaves.toArray(result);
		}
		
		public String toString() {
			String result = "Branch(" + getName() + ") " + getStartOffset() + "," +
			getEndOffset();
			try {
				result += "[" + getText(getStartOffset(), getEndOffset() - getStartOffset()) + "]";
			}
			catch (BadLocationException ble) {
				result += "[text unavailable]";
			}
			result += "\n";
			return result;
		}
	}

	/**
	 * Our implementation of LeafElement, with offsets that don't shift.
	 */
	public class ContentElement extends AbstractElement {
		private transient int start, end;

		public ContentElement(Element parent, AttributeSet a, int offs0, int offs1) {
			super(parent, a);
			start = offs0;
			end = offs1;
			/*
			 try {
			 // Check that the offsets are valid
			 Position p = createPosition(offs0);
			 p = createPosition(offs1);
			 start = offs0;
			 end = offs1;
			 } catch (BadLocationException e) {
			 throw new StateInvariantError("Can't create Position references");
			 }
			 */
		}

		void endWithNewline() {
			try {
				String text = getText(start, end - start);
				for (int i = text.length() - 1; i >= 0; i--) {
					char ch = text.charAt(i);
					if (ch == '\n')
						return;
					else if (!Character.isWhitespace(ch)) {
						// No newline here - add one now
						addContent("\n");
						end++;
						return;
					}
				}
			}
			catch (BadLocationException e) {
				System.err.println("Couldn't get text for content element, start=" + start + ",length=" + (end-start));
			}
		}

		public String toString() {
			return "ContentElement(" + getName() + ") " + start + "," + end + "\n";
		}

		public int getStartOffset() { return start; }
		public int getEndOffset() { return end; }

		public String getName() {
			String nm = super.getName();
			if (nm == null) {
				nm = ContentElementName;
			}
			return nm;
		}

		public int getElementIndex(int pos) { return -1; }
		public Element getElement(int index) { return null; }
		public int getElementCount()  { return 0; }
		public boolean isLeaf() { return true; }
		public boolean getAllowsChildren() { return false; }
		public java.util.Enumeration children() { return null; }

		// --- serialization (may as well copy these methods) ------------
		private void writeObject(java.io.ObjectOutputStream s) throws IOException {
			s.defaultWriteObject();
			s.writeInt(start);
			s.writeInt(end);
		}

		private void readObject(java.io.ObjectInputStream s) throws ClassNotFoundException, IOException {
			s.defaultReadObject();

			// set the range with positions that track change
			start = s.readInt();
			end = s.readInt();
		}
	}

	protected void fireChangeEvents(Element[] es) {
		for (int i = 0; i < es.length; i++) {
			Element e = es[i];
			fireChangeEvent(e);
		}
	}
	protected void fireChangeEvent(Element e) {
		if (e.isLeaf()) {
			int start = e.getStartOffset();
			DefaultDocumentEvent evt = new DefaultDocumentEvent(start, e.getEndOffset() - start, DefaultDocumentEvent.EventType.CHANGE);
			fireChangedUpdate(evt);
		}
		else {
			for (int i = 0; i < e.getElementCount(); i++)
				fireChangeEvent(e.getElement(i));
		}
	}
}
