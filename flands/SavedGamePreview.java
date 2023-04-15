package flands;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.DateFormat;
import java.util.Date;

import javax.swing.JFileChooser;
import javax.swing.JLabel;

/**
 * Component to display relevant details from a saved game. Used when the player is
 * browsing for saved game files.
 * 
 * @author Jonathan Mann
 */
public class SavedGamePreview extends JLabel implements PropertyChangeListener {
	private DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG);
	
	public SavedGamePreview(JFileChooser chooser) {
		// Start with a likely message, so we can fix the label size
		setText("<html><table><tr>Ignatius the Devout, 12th Rank Troubadour</tr><tr><i>Over the Blood-Dark Sea</i> <b>400</b></tr><tr>Some date or other</tr></table></html>");
		setPreferredSize(getPreferredSize());
		
		// Reset the text to an initial value
		setText("<html><i>Select a file</i></html>");
		setHorizontalAlignment(CENTER);
		chooser.setAccessory(this);
		chooser.addPropertyChangeListener(this);
	}
	
	public void propertyChange(PropertyChangeEvent evt) {
		File f = null;
		boolean update = false;
		if (evt.getPropertyName().equals(JFileChooser.DIRECTORY_CHANGED_PROPERTY)) {
			update = true;
			f = null;
		}
		else if (evt.getPropertyName().equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
			update = true;
			f = (File)evt.getNewValue();
		}
		
		if (update)
			loadDetails(f);
	}

	protected void loadDetails(File f) {
		boolean loaded = false;
		if (f != null) {
			Adventurer adv = new Adventurer();
			SectionNode root = new SectionNode();
			LoadableHandler handler = new LoadableHandler(f.getPath());
			handler.add(adv);
			LoadableSection current = new LoadableSection("current", root);
			handler.add(current);
			
			System.out.println("About to load saved game details...");
			if (handler.load()) {
				// Display something like the following:
				// Ryft, 3rd Rank Wayfarer
				// Over the Blood-Dark Sea 172
				// 15/2/2007 3:42 PM
				loaded = true;
				StringBuffer text = new StringBuffer("<html><table>");
				text.append("<tr>").append(adv.getName()).append(", ");
				int rank = adv.getAbilityValue(Adventurer.ABILITY_RANK, Adventurer.MODIFIER_AFFECTED);
				text.append(rank);
				if (rank > 10 && rank < 21)
					text.append("th");
				else if (rank % 10 == 1)
					text.append("st");
				else if (rank % 10 == 2)
					text.append("nd");
				else if (rank % 10 == 3)
					text.append("rd");
				else
					text.append("th");
				text.append(" Rank ").append(adv.getProfessionName()).append("</tr>");
				text.append("<tr><i>").append(Books.getCanon().getBook(current.getBook()).getTitle()).append("</i>");
				text.append(" <b>").append(current.getSection()).append("</b></tr>");
				Date saveDate = new Date(f.lastModified());
				text.append("<tr>").append(df.format(saveDate)).append("</tr>");
				if (adv.isHardcore())
					text.append("<tr><b>Hardcore</b></tr>");
				text.append("</table></html>");

				setHorizontalAlignment(LEFT);
				setText(text.toString());
			}
			else
				setText("<html><i>Couldn't preview saved game</i></html>");
		}
		else
			setText("<html><i>Select a file</i></html>");
		
		if (!loaded)
			setHorizontalAlignment(CENTER);
	}
}
