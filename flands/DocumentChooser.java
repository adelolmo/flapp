package flands;


import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.text.StyledDocument;

/**
 * Pop-up to display a list of formatted items, of which the player must choose one or more.
 * @author Jonathan Mann
 */
public class DocumentChooser extends JDialog implements java.awt.event.ActionListener {
	private JList list;
	private int[] selectedIndices = null;

	public static int[] showChooser(Window w, String title, StyledDocument[] documents, boolean multipleSelection) {
		DocumentChooser chooser;
		if (w instanceof Frame)
			chooser = new DocumentChooser((Frame)w, title, documents, multipleSelection);
		else
			chooser = new DocumentChooser((Dialog)w, title, documents, multipleSelection);
		
		chooser.setVisible(true);
		return chooser.getSelectedIndices();
	}
	
	public DocumentChooser(Frame f, String title, StyledDocument[] documents, boolean multipleSelection) {
		super(f, title, true);
		init(f, documents, multipleSelection);
	}
	public DocumentChooser(Dialog d, String title, StyledDocument[] documents, boolean multipleSelection) {
		super(d, title, true);
		init(d, documents, multipleSelection);
	}
	
	private void init(Window parent, StyledDocument[] documents, boolean multipleSelection) {
		list = new JList(documents);
		list.setSelectionMode(multipleSelection ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
		list.setCellRenderer(new DocumentCellRenderer());

		GridBagLayout gbl = new GridBagLayout();
		Container content = getContentPane();
		content.setLayout(gbl);

		int row = 0;
		if (multipleSelection) {
			new GBC(0, row++)
			.setWeight(1, 0)
			.setBothFill()
			.setInsets(12, 12, 0, 11)
			.addComp(content, new JLabel("Select multiple items with CTRL + click."), gbl);
		}
		new GBC(0, row++)
			.setWeight(1, 1)
			.setBothFill()
			.setInsets(12, 12, 0, 11)
			.addComp(content, new JScrollPane(list), gbl);

		CommandButtons buttons = CommandButtons.createRow(CommandButtons.OK_CANCEL, this);
		new GBC(0, row++)
			.setWeight(1, 0)
			.setBothFill()
			.addComp(content, buttons, gbl);

		pack();
		setLocationRelativeTo(parent);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				close();
			}
		});
	}

	public int[] getSelectedIndices() { return selectedIndices; }

	public void close() {
		setVisible(false);
		dispose();
	}

	public void actionPerformed(java.awt.event.ActionEvent evt) {
		String command = evt.getActionCommand();
		if (command.equals(CommandButtons.okCommand)) {
			selectedIndices = list.getSelectedIndices();
			System.out.println("Selected indices has length " + selectedIndices.length);
		}
		else if (command.equals(CommandButtons.cancelCommand))
			;
		close();
	}
}
