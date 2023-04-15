package flands;


import java.util.Properties;

import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/**
 * Encapsulates a character's resurrection deal: the description, end location,
 * and god providing the service.
 * 
 * @author Jonathan Mann
 */
public class Resurrection {
	private String text;
	private String book;
	private String section;
	private String god = null;
	private boolean supplemental = false;

	public Resurrection(String text, String book, String section) {
		this.text = text;
		this.book = book;
		this.section = section;
	}

	public String getBook() { return book; }
	public String getSection() { return section; }
	public String getText() { return text; }
	public String getGod() { return god; }
	public boolean isGod(String god) { return (this.god != null && this.god.equalsIgnoreCase(god)); }
	public void setGod(String god) { this.god = god; }
	public boolean isSupplemental() { return supplemental; }
	public void setSupplemental(boolean b) { this.supplemental = b; }
	
	/**
	 * Activate the resurrection.
	 * Heals the player entirely, and triggers a goto to the resurrection location.
	 */
	public void activate() {
		FLApp app = FLApp.getSingle();
		app.getAdventurer().getStamina().heal(-1);
		app.getAdventurer().removeResurrection(this);
		app.gotoAddress(new Address(getBook(), getSection()));
	}
	
	public StyledTextList getContent(AttributeSet atts) {
		StyledTextList list = new StyledTextList();
		if (text != null)
			list.add(text + " - ", atts);
		SimpleAttributeSet italicAtts = (atts == null ? new SimpleAttributeSet() : new SimpleAttributeSet(atts));
		StyleConstants.setItalic(italicAtts, true);
		String bookName = Books.getCanon().getBook(book).getTitle();
		list.add(bookName, italicAtts);
		list.add(" ", atts);
		SimpleAttributeSet boldAtts = (atts == null ? new SimpleAttributeSet() : new SimpleAttributeSet(atts));
		StyleConstants.setBold(boldAtts, true);
		list.add(section, boldAtts);
		return list;
	}
	
	static Resurrection loadResurrection(Properties props, int key) {
		String book = props.getProperty("ResBook" + key);
		String section = props.getProperty("ResSection" + key);
		if (book != null && section != null) {
			Resurrection r = new Resurrection(props.getProperty("ResText" + key), book, section);
			r.setGod(props.getProperty("ResGod" + key));
			r.setSupplemental(props.getProperty("ResSupp" + key) != null);
			return r;
		}
		else
			return null;
	}
	
	/**
	 * Internal output method - used when saving character stats.
	 */
	void saveTo(Properties props, int key) {
		props.setProperty("ResBook" + key, book);
		props.setProperty("ResSection" + key, section);
		if (text != null)
			props.setProperty("ResText" + key, text);
		if (god != null)
			props.setProperty("ResGod" + key, god);
		if (supplemental)
			props.setProperty("ResSupp" + key, "1");
	}
	
	/**
	 * Public output method - used when saving as part of a ResurrectionNode.
	 * @see ResurrectionNode#outit(Properties)
	 */
	void saveTo(Properties props) {
		props.setProperty("book", book);
		props.setProperty("section", section);
		if (text != null) props.setProperty("text", text);
		if (god != null)  props.setProperty("god", god);
		if (supplemental) props.setProperty("supplemental", "1");
	}
}
