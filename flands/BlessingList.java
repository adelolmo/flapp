package flands;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import javax.swing.AbstractListModel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

/**
 * List of all blessings on the character. Contains extra methods to deal with
 * the set of blessings, like matching a blessing type for removal, or activating
 * blessings when double-clicked.
 * @author Jonathan Mann
 */
public class BlessingList extends AbstractListModel implements Loadable,
		MouseListener, ActionListener {
	private ArrayList<Blessing> list = new ArrayList<Blessing>();

	public int getSize() {
		return list.size();
	}

	public Blessing getBlessing(int i) {
		return list.get(i);
	}

	public Object getElementAt(int i) {
		return getBlessing(i).getDocument();
	}

	public void addBlessing(Blessing b) {
		int index = list.indexOf(b);
		if (index >= 0 && b.isPermanent() && !getBlessing(index).isPermanent()) {
			// Permanent blessing overrides non-permanent one
			removeBlessing(getBlessing(index));
			index = -1;
		}
		if (index < 0) {
			System.out.println("Adding Blessing " + b.getContentString());
			list.add(b);
			fireIntervalAdded(this, getSize() - 1, getSize() - 1);
		}
	}

	public boolean hasBlessing(Blessing b) {
		if (b.getType() == Blessing.MATCHALL_TYPE || b.getType() == Blessing.MATCHANY_TYPE)
			return (getSize() > 0);
		else
			return list.contains(b);
	}

	public boolean removeBlessing(Blessing b) {
		System.out.println("Removing Blessing " + b.getContentString());
		if (getSize() == 0) return true; // TODO: there may be cases where we should complain here
		if (b.getType() == Blessing.MATCHALL_TYPE) {
			removeAllBlessings(b.isPermanent());
			return true;
		}
		
		int index;
		if (b.getType() == Blessing.MATCHANY_TYPE) {
			if (getSize() == 1)
				index = 0;
			else if (configuredList != null) {
				int[] selected = configuredList.getSelectedIndices();
				if (selected.length != 1) {
					JOptionPane.showMessageDialog(FLApp.getSingle(), new String[] {"Which blessing do you least want to lose?", "Select that one and try again."}, "Multiple Blessings", JOptionPane.INFORMATION_MESSAGE);
					return false;
				}
				index = selected[0];
			}
			else {
				System.out.println("Error: no blessing list configured");
				return true; // fail-safe
			}
		}
		else
			index = list.indexOf(b);
		
		if (index >= 0) {
			if (!list.get(index).isPermanent() || b.isPermanent()) {
				list.remove(index);
				fireIntervalRemoved(this, index, index);
			}
			return true;
		}
		return false;
	}

	public void removeAllBlessings(boolean removePermanent) {
		if (removePermanent) {
			int size = list.size();
			if (size > 0) {
				list.clear();
				fireIntervalRemoved(this, 0, size - 1);
			}
		}
		else {
			for (int i = list.size() - 1; i >= 0; i--) {
				if (!list.get(i).isPermanent()) {
					list.remove(i);
					fireIntervalRemoved(this, i, i);
				}
			}
		}
	}
	
	public boolean hasAbilityBlessing(int ability) {
		Blessing b = Blessing.getAbilityBlessing(ability);
		return hasBlessing(b);
	}

	public void removeAbilityBlessing(int ability) {
		Blessing b = Blessing.getAbilityBlessing(ability);
		removeBlessing(b);
	}

	public int getDefenceBlessingBonus() {
		int index = list.indexOf(Blessing.DEFENCE);
		if (index >= 0)
			return list.get(index).getBonus();
		else {
			System.err
					.println("getDefenceBlessingBonus(): no defence blessing found!");
			return 0;
		}
	}

	private JList configuredList = null;

	public void configureList(JList list) {
		if (list.getModel() instanceof BlessingList) {
			BlessingList oldModel = (BlessingList) list.getModel();
			list.removeMouseListener(oldModel);
			oldModel.configuredList = null;
		}

		configuredList = list;
		list.setFont(SectionDocument.getPreferredFont());
		list.setCellRenderer(new DocumentCellRenderer());
		list.setModel(this);
		list.addMouseListener(this);
	}

	private int currentIndex = -1;

	private Blessing currentBlessing = null;

	public void mouseEntered(MouseEvent evt) {}
	public void mouseExited(MouseEvent evt) {}

	public void mousePressed(MouseEvent evt) {
		if (evt.isPopupTrigger())
			handlePopup(evt);
	}

	public void mouseReleased(MouseEvent evt) {
		if (evt.isPopupTrigger())
			handlePopup(evt);
	}

	public void mouseClicked(MouseEvent evt) {
		if (evt.getClickCount() == 2 && currentBlessing == null) {
			int index = getBlessingIndex(evt.getPoint());
			if (index < 0)
				return;
			Blessing b = getBlessing(index);
			if (canUseBlessing(b))
				useBlessing(index);
		}
	}

	private int getBlessingIndex(Point p) {
		int index = configuredList.locationToIndex(p);
		if (index >= 0
				&& configuredList.getCellBounds(index, index).contains(p))
			return index;
		return -1;
	}

	private void handlePopup(MouseEvent evt) {
		int index = getBlessingIndex(evt.getPoint());
		if (index < 0)
			return;
		Blessing b = getBlessing(index);

		switch (b.getType()) {
		case Blessing.ABILITY_TYPE:
			// Can apply after a difficulty roll
			break;
		case Blessing.LUCK_TYPE:
			// Can apply after any roll
			break;
		case Blessing.TRAVEL_TYPE:
			// Can apply after encounter roll
			break;
		case Blessing.DEFENCE_TYPE:
			// Can apply at the beginning of a fight
			break;
		default:
			return;
		}

		currentIndex = index;
		currentBlessing = b;

		JPopupMenu menu = new JPopupMenu();
		JMenuItem useItem = new JMenuItem("Use");
		useItem.addActionListener(this);
		if (!canUseBlessing(b))
			useItem.setEnabled(false);
		menu.add(useItem);

		menu.show(configuredList, evt.getX(), evt.getY());
	}

	public void actionPerformed(ActionEvent evt) {
		useBlessing(currentIndex);
		currentIndex = -1;
		currentBlessing = null;
	}

	public static boolean canUseBlessing(Blessing b) {
		UndoManager.Creator currentUndoCreator = UndoManager.getCurrent().getCreator();
		switch (b.getType()) {
		case Blessing.ABILITY_TYPE:
			// Can apply after a difficulty roll
			if (currentUndoCreator instanceof DifficultyNode &&
				((DifficultyNode)currentUndoCreator).getAbilityChosen() == b.getAbility())
				return true;
			break;
		case Blessing.LUCK_TYPE:
			// Can apply after any roll
			if (!(currentUndoCreator instanceof UndoManager.NullCreator))
				return true;
			break;
		case Blessing.TRAVEL_TYPE:
			// Can apply after a random roll of type "encounter"
			if ((currentUndoCreator instanceof RandomNode) && ((RandomNode)currentUndoCreator).isTravel())
				return true;
			break;
		case Blessing.DEFENCE_TYPE:
			// Can apply at the beginning of a fight
			return true;
		}
		return false;
	}

	static boolean useBlessing(Blessing b) {
		switch (b.getType()) {
		case Blessing.ABILITY_TYPE:
		case Blessing.LUCK_TYPE:
		case Blessing.TRAVEL_TYPE:
			UndoManager.getCurrent().undo();
			//if (FLApp.debugging && b.getType() == Blessing.LUCK_TYPE)
			//	return;
			return true;
		case Blessing.DEFENCE_TYPE:
			Blessing.addDefenceBlessing(b.getBonus());
			return true;
		}
		return false;
	}
	
	private void useBlessing(int index) {
		Blessing b = getBlessing(index);
		if (useBlessing(b)) {
			if (!b.isPermanent())
				removeBlessing(b);
		}
	}

	public String getFilename() {
		return "blessings.dat";
	}

	public boolean loadFrom(InputStream in) throws IOException {
		DataInputStream din = new DataInputStream(in);
		int count = din.readByte();
		removeAllBlessings(true);
		for (int b = 0; b < count; b++)
			addBlessing(Blessing.createFromLoadableString(din.readUTF()));
		return true;
	}

	public boolean saveTo(OutputStream out) throws IOException {
		DataOutputStream dout = new DataOutputStream(out);
		dout.writeByte(getSize());
		for (int b = 0; b < getSize(); b++)
			dout.writeUTF(getBlessing(b).toLoadableString());
		return true;
	}
}
