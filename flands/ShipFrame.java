package flands;


import java.awt.GridBagLayout;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTable;

/**
 * Window to display the list of owned ships.
 * 
 * @see Ship
 * @see ShipList
 * @author Jonathan Mann
 */
public class ShipFrame extends JDialog {
	private JTable shipTable;
	private JScrollPane tablePane;

	public ShipFrame(ShipList ships) {
		super(FLApp.getSingle(), "Ship's Manifest", false);
		shipTable = ships.getTable();
		tablePane = new JScrollPane(shipTable);

		GridBagLayout gbl = new GridBagLayout();
		getContentPane().setLayout(gbl);

		new GBC(0, 0)
			.setWeight(1, 1)
			.setBothFill()
			.setInsets(12, 12, 11, 11)
			.addComp(getContentPane(), tablePane, gbl);

		pack();
		setDefaultCloseOperation(HIDE_ON_CLOSE);
	}

	public void show(ShipList ships) {
		shipTable = ships.getTable();
		tablePane.setViewportView(shipTable);
		validate();
	}
}
