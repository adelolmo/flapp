package flands;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;

/**
 * Handles the set of all ships owned by the character. Includes methods for modifying one
 * or more ships, plus mouse event handlers for the list view.
 * 
 * @author Jonathan Mann
 */
public class ShipList extends AbstractTableModel implements Loadable, MouseListener, ActionListener {
	private List<Ship> ships = new ArrayList<Ship>();
	private static JMenuItem transferItem;
	static {
		transferItem = new JMenuItem("Ship Transfer...");
		transferItem.addActionListener(new TransferListener());
		transferItem.setEnabled(false);
	}
	
	public int getShipCount() { return ships.size(); }
	public Ship getShip(int i) { return ships.get(i); }

	public void addShip(Ship s) {
		ships.add(s);
		fireTableRowsInserted(ships.size() - 1, ships.size() - 1);
		if (s.getName() == null && table != null)
			table.editCellAt(ships.size() - 1, Ship.NAME_COLUMN);
		notifyShipListeners();

		if (getShipCount() == 1)
			FLApp.getSingle().showShipWindow();
		checkTransferItem(this);
	}
	public void removeShip(int index) {
		ships.remove(index);
		fireTableRowsDeleted(index, index);
		notifyShipListeners();
		checkTransferItem(this);
	}

	private String dock = null;
	public void setAtDock(String dock) {
		this.dock = dock;
		System.out.println("Current dock location=" + dock);
		for (int s = 0; s < getShipCount(); s++) {
			if (getShip(s).getDocked() == null)
				// We'll presume the user just sailed this ship into dock
				getShip(s).setDocked(dock);
		}
		fireTableDataChanged();
		notifyShipListeners();
		checkTransferItem(this);
	}
	public void setOnLand() {
		setAtDock("*land*");
	}
	public void setAtSea() {
		setAtDock(null);
	}
	public boolean isOnLand() { return dock.equals("*land*"); }
	public boolean isAtSea() { return dock == null; }
	public void refresh() {
		fireTableDataChanged();
		checkTransferItem(this);
	}

	public boolean isHere(Ship s) {
		String shipDock = s.getDocked();
		if (dock == shipDock ||
			(dock != null && shipDock != null && dock.equalsIgnoreCase(shipDock)))
			return true;
		else
			return false;
	}

	private int[] listToArray(List<Integer> l) {
		if (l.size() > 1)
			// If the user has selected one of these possible ships, return only that one
			if (table != null && l.contains(Integer.valueOf(table.getSelectedRow())))
				return new int[] { table.getSelectedRow() };
		
		int[] result = new int[l.size()];
		for (int i = 0; i < l.size(); i++)
			result[i] = l.get(i).intValue();
		return result;		
	}
	
	public int[] findShipsOfType(int shipType) {
		List<Integer> l = new ArrayList<Integer>();
		for (int s = 0; s < ships.size(); s++) {
			Ship ship = getShip(s);
			if (isHere(ship) && ship.getType() == shipType)
				l.add(Integer.valueOf(s));
		}
		
		return listToArray(l);
	}
	
	public int[] findShipsWithSpace() { return findShipsWithCargo(Ship.NO_CARGO); }
	public int[] findShipsWithCargo(int cargoType) {
		List<Integer> l = new ArrayList<Integer>();
		for (int s = 0; s < ships.size(); s++) {
			Ship ship = getShip(s);
			if (isHere(ship) && ship.hasCargo(cargoType))
				l.add(Integer.valueOf(s));
		}

		return listToArray(l);
	}

	public void addCargoTo(int shipIndex, int cargoType) {
		Ship s = getShip(shipIndex);
		if (s.addCargo(cargoType)) {
			fireTableCellUpdated(shipIndex, Ship.CARGO_COLUMN);
			notifyCargoListeners();
		}
	}
	/*
	public boolean addCargo(int cargoType) {
		int[] possibleShips = findShipsWithSpace();
		if (possibleShips.length == 1) {
			getShip(possibleShips[0]).addCargo(cargoType);
			fireTableCellUpdated(possibleShips[0], Ship.CARGO_COLUMN);
			return true;
		}
		else
			return false;
	}
	*/

	public void removeCargoFrom(int shipIndex, int cargoType) {
		Ship s = getShip(shipIndex);
		if (s.removeCargo(cargoType)) {
			fireTableCellUpdated(shipIndex, Ship.CARGO_COLUMN);
			notifyCargoListeners();
		}
	}
/*
	public boolean removeCargo(int cargoType) {
		int[] possibleShips = findShipsWithCargo(cargoType);
		if (possibleShips.length == 1) {
			getShip(possibleShips[0]).removeCargo(cargoType);
			fireTableCellUpdated(possibleShips[0], Ship.CARGO_COLUMN);
			return true;
		}
		else
			return false;
	}
*/
	public int[] findShipsWithCrew(int crewType) {
		List<Integer> l = new ArrayList<Integer>();
		for (int s = 0; s < ships.size(); s++) {
			Ship ship = getShip(s);
			if (isHere(ship) && ship.getCrew() == crewType)
				l.add(Integer.valueOf(s));
		}

		return listToArray(l);
	}

	public void setCrew(int shipIndex, int toCrewType) {
		Ship s = getShip(shipIndex);
		s.setCrew(toCrewType);
		fireTableCellUpdated(shipIndex, Ship.CREW_COLUMN);
		notifyCrewListeners();
	}
	
	/**
	 * Return the index of the only ship that is here, or the one that is selected.
	 * @return <code>-1</code> if no single ship could be picked.
	 */
	public int getSingleShip() {
		int singleIndex = -1;
		for (int i = 0; i < ships.size(); i++) {
			if (isHere(getShip(i))) {
				if (singleIndex < 0)
					singleIndex = i;
				else {
					singleIndex = -1;
					break;
				}
			}
		}

		if (singleIndex < 0 && table != null) {
			singleIndex = table.getSelectedRow();
			if (singleIndex >= 0 && !isHere(getShip(singleIndex)))
				singleIndex = -1;
		}

		return singleIndex;
	}

	public int[] findShipsHere() {
		List<Integer> l = new ArrayList<Integer>();
		for (int s = 0; s < ships.size(); s++) {
			Ship ship = getShip(s);
			if (isHere(ship))
				l.add(Integer.valueOf(s));
		}

		return listToArray(l);
	}

	private static List<ChangeListener> addListenerTo(List<ChangeListener> listeners, ChangeListener l) {
		if (listeners == null)
			listeners = new LinkedList<ChangeListener>();
		listeners.add(l);
		return listeners;
	}
	private static void removeListenerFrom(List<ChangeListener> listeners, ChangeListener l) {
		if (listeners != null)
			listeners.remove(l);
		else {
			System.out.println("ShipList.removeListenerFrom called unnecessarily");
			Thread.dumpStack();
		}
	}
	private void notifyListeners(List<ChangeListener> listeners) {
		if (listeners != null && listeners.size() > 0) {
			ChangeEvent evt = new ChangeEvent(this);
			for (Iterator<ChangeListener> i = listeners.iterator(); i.hasNext(); )
				i.next().stateChanged(evt);
		}
	}

	/******** Listeners ********/
	private List<ChangeListener> crewListeners = null;
	private List<ChangeListener> cargoListeners = null;
	private List<ChangeListener> shipListeners = null;
	public void addShipListener(ChangeListener l) { shipListeners = addListenerTo(shipListeners, l); }
	public void removeShipListener(ChangeListener l) { removeListenerFrom(shipListeners, l); }
	private void notifyShipListeners() {
		notifyListeners(shipListeners);
		notifyListeners(crewListeners);
		notifyListeners(cargoListeners);
	}
	public void addCrewListener(ChangeListener l) { crewListeners = addListenerTo(crewListeners, l); }
	public void removeCrewListener(ChangeListener l) { removeListenerFrom(crewListeners, l); }
	private void notifyCrewListeners() { notifyListeners(crewListeners); }
	public void addCargoListener(ChangeListener l) { cargoListeners = addListenerTo(cargoListeners, l); }
	public void removeCargoListener(ChangeListener l) { removeListenerFrom(cargoListeners, l); }
	private void notifyCargoListeners() { notifyListeners(cargoListeners); }

	/******** TableModel ********/
	public int getRowCount() { return getShipCount(); }
	public int getColumnCount() { return Ship.getColumnCount(); }
	public String getColumnName(int col) { return Ship.getColumnName(col); }
	public Class<?> getColumnClass(int col) { return Ship.getColumnClass(col); }
	public boolean isCellEditable(int row, int col) {
		return Ship.isCellEditable(col) && isHere(getShip(row));
	}
	public Object getValueAt(int row, int col) {
		return getShip(row).getValueAt(col);
	}
	public void setValueAt(Object val, int row, int col) {
		if (getShip(row).setValueAt(val, col))
			fireTableCellUpdated(row, col);
	}

	/******** Table methods ********/
	private JTable table = null;
	public JTable getTable() {
		if (table == null) {
			table = new JTable(this) {
				public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
					Component c = super.prepareRenderer(renderer, row, col);
					c.setEnabled(isHere(getShip(row)));
					if (getColumnModel().getColumn(col).getModelIndex() == Ship.NAME_COLUMN) {
						Font f = c.getFont();
						c.setFont(new Font(f.getName(), f.getStyle() | Font.ITALIC, f.getSize()));
					}

					return c;
				}
			};
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			//table.getSelectionModel().addListSelectionListener(this);
			table.setShowHorizontalLines(false);
			table.setShowVerticalLines(true);
			table.setPreferredScrollableViewportSize(new Dimension(300, 50));
			table.addMouseListener(this);
		}
		return table;
	}

	/******** Loadable methods ********/
	public String getFilename() { return "ships.dat"; }
	public boolean loadFrom(InputStream in) throws IOException {
		DataInputStream din = new DataInputStream(in);
		dock = din.readUTF();
		if (dock.length() == 0)
			dock = null;
		int count = din.readInt();
		ships.clear();
		for (int s = 0; s < count; s++) {
			Ship ship = Ship.load(din);
			addShip(ship);
		}
		return true;
	}

	public boolean saveTo(OutputStream out) throws IOException {
		DataOutputStream dout = new DataOutputStream(out);
		dout.writeUTF(dock == null ? "" : dock);
		dout.writeInt(getShipCount());
		for (int s = 0; s < getShipCount(); s++)
			getShip(s).saveTo(dout);
		return true;
	}

	public static void main(String args[]) {
		final ShipList sl = new ShipList();
		String[] docks = new String[]{"Aku", "Kunrir", null};
		for (int s = 0; s < 10; s++) {
			Ship ship = new Ship((int)(Math.random() * (Ship.MAX_TYPE+1)), null,
				(int)(Math.random() * (Ship.MAX_CREW+2)) - 1);
			ship.setDocked(docks[(int)(Math.random() * docks.length)]);
			for (int c = 0; c < ship.getCapacity(); c++) {
				int cargoType = (int)(Math.random() * (Ship.MAX_CARGO+1));
				if (cargoType != Ship.NO_CARGO)
					ship.addCargo(cargoType);
			}
			sl.addShip(ship);
		}
		String dock = docks[(int)(Math.random() * docks.length)];
		sl.setAtDock(dock);

		final javax.swing.JComboBox cargoBox = new javax.swing.JComboBox();
		for (int c = 0; c <= Ship.MAX_CARGO; c++)
			cargoBox.addItem(Ship.getCargoName(c));
		cargoBox.setEditable(false);
		javax.swing.JButton buyCargoButton = new javax.swing.JButton("Buy");
		buyCargoButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				int[] ships = sl.findShipsWithSpace();
				if (ships.length == 0)
					JOptionPane.showMessageDialog(cargoBox, "No ships here with space!", "No space", JOptionPane.INFORMATION_MESSAGE);
				else if (ships.length > 1)
					JOptionPane.showMessageDialog(cargoBox, new Object[] {"Which ship should take the cargo?", "Please select one."}, "Can't decide", JOptionPane.INFORMATION_MESSAGE);
				else
					sl.addCargoTo(ships[0], cargoBox.getSelectedIndex());
			}
		});
		javax.swing.JButton sellCargoButton = new javax.swing.JButton("Sell");
		sellCargoButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				int[] ships = sl.findShipsWithCargo(cargoBox.getSelectedIndex());
				if (ships.length == 0)
					JOptionPane.showMessageDialog(cargoBox, "No ships here with this cargo!", "No cargo", JOptionPane.INFORMATION_MESSAGE);
				else if (ships.length > 1)
					JOptionPane.showMessageDialog(cargoBox, new Object[] {"Multiple ships have this cargo!", "Please select one."}, "Can't decide", JOptionPane.INFORMATION_MESSAGE);
				else
					sl.removeCargoFrom(ships[0], cargoBox.getSelectedIndex());
			}
		});
		JTable table = sl.getTable();
		javax.swing.JFrame jf = new javax.swing.JFrame("Ship list - " + dock);
		jf.getContentPane().add(new javax.swing.JScrollPane(table));
		javax.swing.JPanel lowerPanel = new javax.swing.JPanel();
		lowerPanel.add(cargoBox);
		lowerPanel.add(buyCargoButton);
		lowerPanel.add(sellCargoButton);
		jf.getContentPane().add(lowerPanel, java.awt.BorderLayout.SOUTH);
		jf.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
		jf.pack();
		jf.setVisible(true);
	}

	public static JMenuItem getTransferMenuItem() { return transferItem; }
	
	private static void checkTransferItem(ShipList ships) {
		transferItem.setEnabled(ships != null && ships.findShipsHere().length > 1);
	}
	
	private static class TransferListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			ShipFrame window = FLApp.getSingle().showShipWindow();
			new ShipSwapDialog(window, FLApp.getSingle().getAdventurer().getShips()).setVisible(true);
		}
	}

	private int selectedShip = -1;
	private void handlePopup(MouseEvent evt) {
		if (table == null) return;
		int row = table.rowAtPoint(evt.getPoint());
		if (row < 0) return;
		selectedShip = row;
		Ship s = getShip(row);
		JPopupMenu popup = new JPopupMenu(s.getName());
		JMenuItem dumpItem = new JMenuItem("Dump Cargo...");
		if (!isHere(s) || !s.hasCargo(Ship.MATCH_SINGLE_CARGO))
			dumpItem.setEnabled(false);
		dumpItem.addActionListener(this);
		popup.add(dumpItem);
		popup.show(table, evt.getX(), evt.getY());
	}
	
	public void actionPerformed(ActionEvent e) {
		// Dump Cargo selected
		if (selectedShip >= 0) {
			Ship s = getShip(selectedShip);
			int[] cargoTypes = new int[s.getCapacity() - s.getFreeSpace()];
			int len = 0;
			for (int i = 0; i < s.getCapacity(); i++) {
				int type = s.getCargo(i);
				if (type != Ship.NO_CARGO)
					cargoTypes[len++] = type;
			}
			
			DefaultStyledDocument[] cargoDocs = new DefaultStyledDocument[len];
			for (int i = 0; i < len; i++) {
				cargoDocs[i] = new DefaultStyledDocument();
				try {
					cargoDocs[i].insertString(0, Ship.getCargoName(cargoTypes[i]), null);
				}
				catch (BadLocationException ble) {}
			}
			
			int[] selected = DocumentChooser.showChooser(SwingUtilities.getWindowAncestor(table), "Dump Cargo", cargoDocs, true);
			if (selected != null && selected.length > 0) {
				for (int i = selected.length - 1; i >= 0; i--)
					removeCargoFrom(selectedShip, cargoTypes[selected[i]]);
			}
		}
		selectedShip = -1;
	}
	
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		if (e.isPopupTrigger())
			handlePopup(e);
	}
	
	public void mousePressed(MouseEvent e) {
		if (e.isPopupTrigger())
			handlePopup(e);		
	}
	
	public void mouseReleased(MouseEvent e) {
		if (e.isPopupTrigger())
			handlePopup(e);
	}
	
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
}
