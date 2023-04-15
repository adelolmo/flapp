package flands;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.text.Element;

import org.xml.sax.Attributes;

/**
 * An extra choice is one which will be enabled in particular sections,
 * eg. the entrance to the sewers in Yellowport. They are first seen as ActionNodes,
 * that when clicked add the choice to the 'Extra Choices' menu. When the
 * appropriate section(s) are entered, the menu item will become available.
 * Selecting the menu item behaves like a GotoNode. Any choice
 * can later be removed with the 'remove' attribute, matching the 'key' of
 * a previously added choice.
 * 
 * @author Jonathan Mann
 */
public class ExtraChoice extends ActionNode implements Executable {
	public static final String ElementName = "extrachoice";
	private StyledTextList styledText = null;
	protected String text = null;
	private String book, section;
	private String atbook, atsection;
	private String tag;
	protected String key;
	private String remove;
	private int flashes = 3;
	
	public ExtraChoice(Node parent) {
		super(ElementName, parent);
		setEnabled(false);
	}

	public String getText() {
		if (text == null && styledText != null) {
			text = "";
			for (Iterator<StyledText> i = styledText.iterator(); i.hasNext(); )
				text += i.next().text;
		}
		return text;
	}
	public StyledTextList getStyledText() { return styledText; }
	public String getBook() { return book; }
	public String getSection() { return section; }
	
	/** Loading from section XML. */
	public void init(Attributes atts) {
		book      = atts.getValue("book");
		section   = atts.getValue("section");
		text      = atts.getValue("text");
		atbook    = atts.getValue("atbook");
		atsection = atts.getValue("atsection");
		tag       = atts.getValue("tag");
		key       = atts.getValue("key");
		remove    = atts.getValue("remove");
		
		super.init(atts);
	}
	
	/** Loading from saved game file. */
	public void loadFrom(ExtProperties props, int i) {
		book      = props.getProperty(i + ".book");
		section   = props.getProperty(i + ".section");
		text      = props.getProperty(i + ".text");
		atbook    = props.getProperty(i + ".atbook");
		atsection = props.getProperty(i + ".atsection");
		tag       = props.getProperty(i + ".tag");
		key       = props.getProperty(i + ".key");
	}
	
	/** Saving to saved game file. */
	public void saveTo(ExtProperties props, int i) {
		// TODO: Doesn't save styledText to file - no code for this (yet)
		props.setProperty(i + ".book",      book);
		props.setProperty(i + ".section",   section);
		props.setProperty(i + ".text",      getText());
		props.setProperty(i + ".atbook",    atbook);
		props.setProperty(i + ".atsection", atsection);
		props.setProperty(i + ".tag",       tag);
		props.setProperty(i + ".key",       key);
	}
	
	private static boolean equal(String s1, String s2) {
		return (s1 != null && s2 != null ? s1.equals(s2) : s1 == s2);
	}
	public boolean equals(Object o) {
		try {
			ExtraChoice choice = (ExtraChoice)o;
			return (equal(book, choice.book) &&
					equal(section, choice.section) &&
					equal(atbook, choice.atbook) &&
					equal(atsection, choice.atsection) &&
					equal(tag, choice.tag) &&
					equal(key, choice.key));
		}
		catch (ClassCastException cce) { return false; }
	}
	public void handleContent(String text) {
		if (text.length() == 0) return;
		if (styledText == null)
			styledText = new StyledTextList();
		styledText.add(text, StyleNode.createActiveAttributes());
		Element[] leaves = getDocument().addLeavesTo(getElement(), new StyledText[] { new StyledText(text, createStandardAttributes()) });
		addHighlightElements(leaves);
		addEnableElements(leaves);
	}
	
	public boolean handleEndTag() {
		if (styledText == null) {
			if (text == null)
				hidden = true;
			else {
				Element[] leaves = getDocument().addLeavesTo(getElement(), new StyledText[] { new StyledText(text, createStandardAttributes()) });
				setHighlightElements(leaves);
				addEnableElements(leaves);
			}
		}
		findExecutableGrouper().addExecutable(this);
		
		return super.handleEndTag();
	}
	
	public boolean execute(ExecutableGrouper grouper) {
		if (hidden) {
			actionPerformed(null);
			return true;
		}

		// New system - only enable and block if this exact choice isn't present
		if (getAdventurer().getExtraChoices().contains(this))
			return true;
		else {
			setEnabled(true);
			return false;
		}
		
		// Old system - non-blocking action
		//setEnabled(true);
		//return (getAdventurer().getExtraChoices().contains(this));
	}
	
	public void actionPerformed(ActionEvent evt) {
		if (menuItem != null && evt.getSource() == menuItem)
			activate();
		else {
			setEnabled(false);
			if (remove == null)
				getAdventurer().getExtraChoices().add(this);
			else
				getAdventurer().getExtraChoices().remove(remove);
			
			if (!hidden)
				findExecutableGrouper().continueExecution(this, false);
		}
	}
	
	public void resetExecute() {
		setEnabled(false);
	}
	
	public boolean isChoiceEnabled() {
		if (atbook != null && atsection != null) {
			if (atbook.equals(Address.getCurrentBookKey()) && atsection.equals(FLApp.getSingle().getCurrentSection()))
				return true;
		}
		if (tag != null) {
			if (((SectionNode)FLApp.getSingle().getRootNode()).hasTag(tag))
				return true;
		}
		
		return false;
	}
	
	public void activate() {
		FLApp.getSingle().gotoAddress(new Address(getBook(), getSection()));
	}
	
	protected String getTipText() {
		if (remove == null) {
			StringBuffer sb = new StringBuffer("Gain extra choice [");
			sb.append(key);
			sb.append("]");
			if (atbook != null && atsection != null)
				sb.append(", activated in book ").append(atbook).append(", section ").append(atsection);
			else if (tag != null)
				sb.append(", active in sections with tag '").append(tag).append("'");
			sb.append(", destination ");
			if (getBook() != null)
				sb.append("book ").append(getBook()).append(", ");
			sb.append("section ").append(getSection());
			return sb.toString();
		}
		else
			return "Remove extra choice [" + key + "]";
	}
	
	private JMenuItem menuItem = null;
	protected JMenuItem getMenuItem() {
		if (menuItem == null) {
			menuItem = new JMenuItem(getText());
			menuItem.setEnabled(isChoiceEnabled());
			menuItem.addActionListener(this);
		}
		return menuItem;
	}
	
	protected boolean showInMenu() {
		return (isChoiceEnabled() || (atbook != null && atbook.equals(Address.getCurrentBookKey())));
	}
	
	public static Death DeathChoice = new Death();
	public final static class Death extends ExtraChoice {
		private Death() {
			super(null);
			key = text = "Death";
		}
		public String getBook() { return Address.getCurrentBookKey(); }
		public String getSection() {
			return Address.getCurrentBook().getDeathSection();
		}
		public boolean isChoiceEnabled() {
			return getAdventurer().getStamina().current <= 0;
		}
		protected boolean showInMenu() { return true; }
	}
	
	public static class List extends LinkedList<ExtraChoice> implements Loadable {
		private JMenu choiceMenu = null;
		public void setMenu(JMenu menu) { choiceMenu = menu; }
		
		public boolean add(ExtraChoice choice) {
			if (choice.key != null)
				// Only one choice with a particular key can be present
				remove(choice.key);
			super.add(choice);
			return true;
		}
		
		public void remove(String key) {
			for (Iterator<ExtraChoice> i = iterator(); i.hasNext(); ) {
				ExtraChoice choice = i.next();
				if (choice.key != null && choice.key.equals(key)) {
					i.remove();
					return;
				}
			}
		}
		
		public void checkMenu() {
			if (choiceMenu == null) {
				System.out.println("ExtraChoice.List.checkMenu() called, menu has not yet been set");
				return;
			}

			choiceMenu.removeAll();
			boolean enabledItem = false;
			int flashes = 1;
			for (Iterator<ExtraChoice> i = iterator(); i.hasNext(); ) {
				ExtraChoice choice = i.next();
				if (choice.showInMenu()) {
					JMenuItem item = choice.getMenuItem();
					boolean enabled = choice.isChoiceEnabled();
					item.setEnabled(enabled);
					choiceMenu.add(item);
					if (enabled) {
						enabledItem = true;
						if (choice.flashes > flashes)
							flashes = choice.flashes--;
					}
				}
			}
			
			if (choiceMenu.getItemCount() > 0)
				choiceMenu.addSeparator();
			JMenuItem deathItem = DeathChoice.getMenuItem();
			deathItem.setEnabled(DeathChoice.isChoiceEnabled());
			choiceMenu.add(deathItem);
			
			if (enabledItem)
				// 'Flash' the menu
				new Thread(new MenuFlasher(choiceMenu, flashes)).start();
			
			if (!choiceMenu.isVisible())
				choiceMenu.setVisible(true);
		}
		
		public String getFilename() {
			return "extrachoices.ini";
		}

		public boolean loadFrom(InputStream in) throws IOException {
			ExtProperties props = new ExtProperties();
			props.load(in);
			int size = props.getInt("size", 0);
			clear();
			for (int i = 0; i < size; i++) {
				ExtraChoice choice = new ExtraChoice(null);
				choice.loadFrom(props, i);
				add(choice);
			}
			return true;
		}

		public boolean saveTo(OutputStream out) throws IOException {
			ExtProperties props = new ExtProperties();
			props.set("size", size());
			int j = 0;
			for (Iterator<ExtraChoice> i = iterator(); i.hasNext(); j++)
				i.next().saveTo(props, j);
			props.store(out, "Extra Choices");
			return true;
		}
	}
	
	private static class MenuFlasher implements Runnable, MenuListener {
		private JMenu menu;
		private boolean cancelled = false;
		private int flashes;
		
		private MenuFlasher(JMenu menu, int flashes) {
			this.menu = menu;
			menu.addMenuListener(this);
			this.flashes = flashes;
		}
		
		public void run() {
			for (int i = 0; i < (flashes*2)-1; i++) {
				if (cancelled)
					break;
				menu.setArmed(i % 2 == 0);
				try {
					Thread.sleep(500);
				}
				catch (InterruptedException e) {}
			}
			menu.setArmed(false);
		}

		public void menuSelected(MenuEvent e) { cancelled = true; }
		public void menuDeselected(MenuEvent e) { cancelled = true; }
		public void menuCanceled(MenuEvent e) { cancelled = true; }
	}
}
