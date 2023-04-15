package flands;

import java.awt.Container;
import java.awt.Dialog;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

/**
 * Helper dialog, allowing the character to swap crew and cargo between two ships in
 * the same location. Not explicitly mentioned in the rules, but it seemed like a
 * sensible house rule to include.
 * 
 * @author Jonathan Mann
 */
public class ShipSwapDialog extends JDialog implements ActionListener {
	private ShipList ships;
	private ShipNameModel ship1, ship2;
	private JComboBox ship1Choice, ship2Choice;
	private JLabel ship1Type, ship2Type, ship1Location, ship2Location;
	private JTextField ship1Name, ship2Name, ship1Crew, ship2Crew;
	private JList ship1Cargo, ship2Cargo;
	private ShipCargoModel cargo1, cargo2;
	private JButton swapCrewButton, swapCargoButton, closeButton;
	
	public ShipSwapDialog(Dialog parent, ShipList ships) {
		super(parent, "Ship Swap", true);
		this.ships = ships;

		ship1 = new ShipNameModel();
		ship2 = new ShipNameModel();
		ship1Choice = new JComboBox(ship1);
		ship1Choice.setEditable(false);
		ship1Choice.addActionListener(this);
		ship2Choice = new JComboBox(ship2);
		ship2Choice.setEditable(false);
		ship2Choice.addActionListener(this);
		
		ship1Name = new JTextField();
		ship1Name.addActionListener(this);
		ship2Name = new JTextField();
		ship2Name.addActionListener(this);
		
		ship1Type = new JLabel();
		ship2Type = new JLabel();
		
		ship1Location = new JLabel();
		ship2Location = new JLabel();
		
		ship1Crew = new JTextField();
		ship1Crew.setEditable(false);
		ship2Crew = new JTextField();
		ship2Crew.setEditable(false);
		swapCrewButton = new JButton("<->");
		swapCrewButton.addActionListener(this);
		
		cargo1 = new ShipCargoModel();
		ship1Cargo = new JList(cargo1);
		ship1Cargo.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		cargo2 = new ShipCargoModel();
		ship2Cargo = new JList(cargo2);
		ship2Cargo.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		swapCargoButton = new JButton("<->");
		swapCargoButton.addActionListener(this);
		
		if (ships.getShipCount() > 1)
			ship2Choice.setSelectedIndex(1);
		shipChosen(true, true);
		
		closeButton = new JButton("Close");
		closeButton.addActionListener(this);
		
		GridBagLayout gbl = new GridBagLayout();
		Container content = getContentPane();
		content.setLayout(gbl);
		
		new GBC(0, 0)
			.setAnchor(GBC.WEST)
			.setInsets(12, 12, 0, 0)
			.addComp(content, new JLabel("Ship 1:"), gbl);
		new GBC(1, 0)
			.setHorizFill()
			.setInsets(12, 5, 0, 0)
			.addComp(content, ship1Choice, gbl);
		new GBC(3, 0)
			.setAnchor(GBC.WEST)
			.setInsets(12, 0, 0, 0)
			.addComp(content, new JLabel("Ship 2:"), gbl);
		new GBC(4, 0)
			.setHorizFill()
			.setInsets(12, 5, 0, 11)
			.addComp(content, ship2Choice, gbl);
		
		new GBC(0, 1)
			.setAnchor(GBC.WEST)
			.setInsets(5, 12, 0, 0)
			.addComp(content, new JLabel("Name:"), gbl);
		new GBC(1, 1)
			.setHorizFill()
			.setInsets(5, 5, 0, 0)
			.addComp(content, ship1Name, gbl);
		new GBC(3, 1)
			.setAnchor(GBC.WEST)
			.setInsets(5, 0, 0, 0)
			.addComp(content, new JLabel("Name:"), gbl);
		new GBC(4, 1)
			.setHorizFill()
			.setInsets(5, 5, 0, 11)
			.addComp(content, ship2Name, gbl);
		
		new GBC(0, 2)
			.setAnchor(GBC.WEST)
			.setInsets(5, 12, 0, 0)
			.addComp(content, new JLabel("Type:"), gbl);
		new GBC(1, 2)
			.setHorizFill()
			.setInsets(5, 5, 0, 0)
			.addComp(content, ship1Type, gbl);
		new GBC(3, 2)
			.setAnchor(GBC.WEST)
			.setInsets(5, 0, 0, 0)
			.addComp(content, new JLabel("Type:"), gbl);
		new GBC(4, 2)
			.setHorizFill()
			.setInsets(5, 5, 0, 11)
			.addComp(content, ship2Type, gbl);
		
		new GBC(0, 3)
			.setAnchor(GBC.WEST)
			.setInsets(5, 12, 0, 0)
			.addComp(content, new JLabel("Location:"), gbl);
		new GBC(1, 3)
			.setHorizFill()
			.setInsets(5, 5, 0, 0)
			.addComp(content, ship1Location, gbl);
		new GBC(3, 3)
			.setAnchor(GBC.WEST)
			.setInsets(5, 0, 0, 0)
			.addComp(content, new JLabel("Location:"), gbl);
		new GBC(4, 3)
			.setHorizFill()
			.setInsets(5, 5, 0, 11)
			.addComp(content, ship2Location, gbl);
		
		new GBC(0, 4)
			.setAnchor(GBC.WEST)
			.setInsets(5, 12, 0, 0)
			.addComp(content, new JLabel("Crew:"), gbl);
		new GBC(1, 4)
			.setHorizFill()
			.setInsets(5, 5, 0, 0)
			.addComp(content, ship1Crew, gbl);
		new GBC(2, 4)
			.setInsets(5, 5, 0, 5)
			.addComp(content, swapCrewButton, gbl);
		new GBC(3, 4)
			.setAnchor(GBC.WEST)
			.setInsets(5, 0, 0, 0)
			.addComp(content, new JLabel("Crew:"), gbl);
		new GBC(4, 4)
			.setHorizFill()
			.setInsets(5, 5, 0, 11)
			.addComp(content, ship2Crew, gbl);
		
		new GBC(0, 5)
			.setSpan(2, 1)
			.setAnchor(GBC.WEST)
			.setInsets(5, 12, 0, 0)
			.addComp(content, new JLabel("Cargo"), gbl);
		new GBC(3, 5)
			.setSpan(2, 1)
			.setAnchor(GBC.WEST)
			.setInsets(5, 0, 0, 11)
			.addComp(content, new JLabel("Cargo"), gbl);
		
		new GBC(0, 6)
			.setSpan(2, 1)
			.setBothFill()
			.setInsets(5, 12, 11, 0)
			.addComp(content, new JScrollPane(ship1Cargo), gbl);
		new GBC(2, 6)
			.setInsets(5, 5, 11, 5)
			.addComp(content, swapCargoButton, gbl);
		new GBC(3, 6)
			.setSpan(2, 1)
			.setBothFill()
			.setInsets(5, 0, 11, 11)
			.addComp(content, new JScrollPane(ship2Cargo), gbl);
		
		new GBC(0, 7)
			.setSpan(5, 1)
			.setAnchor(GBC.EAST)
			.setInsets(5, 12, 11, 11)
			.addComp(content, closeButton, gbl);
		
		pack();
		setResizable(false);
		setLocationRelativeTo(parent);
	}
	
	private void shipChosen(boolean b1, boolean b2) {
		if (b1) {
			ship1Name.setText(ship1.getShip().getName());
			ship1Type.setText(Ship.getTypeName(ship1.getShip().getType()));
			ship1Location.setText(ship1.getShip().getValueAt(Ship.DOCKED_COLUMN).toString());
			ship1Crew.setText(Ship.getCrewName(ship1.getShip().getCrew()));
			cargo1.setShip(ship1.getShip());
		}
		if (b2) {
			ship2Name.setText(ship2.getShip().getName());
			ship2Type.setText(Ship.getTypeName(ship2.getShip().getType()));
			ship2Location.setText(ship2.getShip().getValueAt(Ship.DOCKED_COLUMN).toString());
			ship2Crew.setText(Ship.getCrewName(ship2.getShip().getCrew()));
			cargo2.setShip(ship2.getShip());
		}
		
		String dock1 = ship1.getShip().getDocked();
		String dock2 = ship2.getShip().getDocked();
		boolean enableSwap = (ship1.getIndex() != ship2.getIndex() &&
				(dock1 == dock2 || (dock1 != null && dock2 != null && dock1.equalsIgnoreCase(dock2))));
		swapCrewButton.setEnabled(enableSwap);
		swapCargoButton.setEnabled(enableSwap);
	}
	
	private class ShipNameModel extends AbstractListModel implements ComboBoxModel {
		private int selectedIndex;
		public int getIndex() { return selectedIndex; }
		public Ship getShip() { return ships.getShip(getIndex()); }
		public void nameChanged(int index) {
			fireContentsChanged(this, index, index);
		}
		
		public int getSize() {
			return ships.getShipCount();
		}

		public Object getElementAt(int index) {
			return ships.getShip(index).getName();
		}

		public void setSelectedItem(Object anItem) {
			for (int i = 0; i < getSize(); i++)
				if (getElementAt(i).equals(anItem)) {
					selectedIndex = i;
					break;
				}
		}

		public Object getSelectedItem() {
			return getElementAt(selectedIndex);
		}
	}

	private class ShipCargoModel extends AbstractListModel {
		private Ship ship;
		public void setShip(Ship s) {
			if (s != ship) {
				if (ship != null)
					fireIntervalRemoved(this, 0, getSize()-1);
				
				this.ship = s;
				
				fireIntervalAdded(this, 0, getSize() - 1);
			}
		}
		
		public void refresh() {
			fireContentsChanged(this, 0, getSize() - 1);
		}
		
		public int getSize() {
			return (ship != null ? ship.getCapacity() : 0);
		}

		public Object getElementAt(int index) {
			int cargoType = ship.getCargo(index);
			if (cargoType >= 0)
				return Ship.getCargoName(cargoType);
			else
				return "Empty";
		}
	}
	
	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		if (src == ship1Choice)
			shipChosen(true, false);
		else if (src == ship2Choice)
			shipChosen(false, true);
		else if (src == ship1Name)
			setShipName(true);
		else if (src == ship2Name)
			setShipName(false);
		else if (src == swapCrewButton) {
			int crew1 = ship1.getShip().getCrew();
			int crew2 = ship2.getShip().getCrew();
			ship1.getShip().setCrew(crew2);
			ship2.getShip().setCrew(crew1);
			ship1Crew.setText(Ship.getCrewName(crew2));
			ship2Crew.setText(Ship.getCrewName(crew1));
		}
		else if (src == swapCargoButton) {
			int cargo1Count = 0, cargo2Count = 0;
			int[] selection1 = ship1Cargo.getSelectedIndices();
			int[] selection2 = ship2Cargo.getSelectedIndices();
			for (int i = 0; i < selection1.length; i++)
				if (ship1.getShip().getCargo(selection1[i]) != Ship.NO_CARGO)
					cargo1Count++;
			for (int i = 0; i < selection2.length; i++)
				if (ship2.getShip().getCargo(selection2[i]) != Ship.NO_CARGO)
					cargo2Count++;
			
			if (ship1.getShip().getFreeSpace() - cargo2Count + cargo1Count < 0 ||
				ship2.getShip().getFreeSpace() - cargo1Count + cargo2Count < 0) {
				getToolkit().beep();
				return;
			}
			
			int[] moveCargo1 = new int[cargo1Count];
			int[] moveCargo2 = new int[cargo2Count];
			int j = 0;
			for (int i = 0; i < selection1.length; i++) {
				int cargo = ship1.getShip().getCargo(selection1[i]);
				if (cargo != Ship.NO_CARGO) {
					moveCargo1[j++] = cargo;
					ship1.getShip().removeCargo(cargo);
				}
			}
			j = 0;
			for (int i = 0; i < selection2.length; i++) {
				int cargo = ship2.getShip().getCargo(selection2[i]);
				if (cargo != Ship.NO_CARGO) {
					moveCargo2[j++] = cargo;
					ship2.getShip().removeCargo(cargo);
				}
			}
			
			for (int i = 0; i < cargo1Count; i++)
				ship2.getShip().addCargo(moveCargo1[i]);
			for (int i = 0; i < cargo2Count; i++)
				ship1.getShip().addCargo(moveCargo2[i]);
			
			cargo1.refresh();
			cargo2.refresh();
		}
		else if (src == closeButton) {
			close();
		}
	}
	
	public void close() {
		setVisible(false);
		ships.refresh();
		dispose();
	}
	
	private void setShipName(boolean isShip1) {
		int index = (isShip1 ? ship1 : ship2).getIndex();
		JTextField sourceField = (isShip1 ? ship1Name : ship2Name);
		String name = sourceField.getText();
		if (name.length() == 0) {
			// Ignore
			sourceField.setText(ships.getShip(index).getName());
			sourceField.selectAll();
			getToolkit().beep();
			return;
		}
		
		// Check that no other ship is using this name
		for (int s = 0; s < ships.getShipCount(); s++) {
			if (s == index)
				continue;
			if (ships.getShip(s).getName().equals(name)) {
				JOptionPane.showMessageDialog(this, new String[] {"There is already another ship called", name }, "Same Name", JOptionPane.INFORMATION_MESSAGE);
				sourceField.selectAll();
				return;
			}
		}
		
		ships.getShip(index).setName(name);
		ship1.nameChanged(index);
		ship2.nameChanged(index);
		pack();
	}
	
	public static void main(String args[]) {
		ShipList ships = new ShipList();
		Ship ship1 = new Ship(Ship.BARQ_TYPE, "Tubby", Ship.POOR_CREW);
		ship1.addCargo(Ship.TIMBER_CARGO);
		ships.addShip(ship1);
		Ship ship2 = new Ship(Ship.BRIG_TYPE, "Cruiser", Ship.GOOD_CREW);
		ship2.addCargo(Ship.METAL_CARGO);
		ships.addShip(ship2);
		Ship ship3 = new Ship(Ship.GALL_TYPE, "Brutus", Ship.EX_CREW);
		ship3.addCargo(Ship.MINERAL_CARGO);
		ship3.addCargo(Ship.SPICE_CARGO);
		ships.addShip(ship3);
		
		JFrame jf = new JFrame("Test");
		jf.setBounds(0, 0, 200, 200);
		jf.setVisible(true);
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JDialog jd = new JDialog(jf, "Test2");
		jd.setBounds(10, 10, 200, 200);
		jd.setVisible(true);
		
		ShipSwapDialog ssd = new ShipSwapDialog(jd, ships);
		ssd.setVisible(true);
	}
}
