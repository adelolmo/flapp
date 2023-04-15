package flands;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Ship owned by the character, of a fixed type, with crew of variable quality,
 * 1-3 units of cargo, and a current location. Includes helper methods for display
 * in a table.
 * @author Jonathan Mann
 */
public class Ship {
	/******** Constants ********/
	public static final int BARQ_TYPE = 0;
	public static final int BRIG_TYPE = 1;
	public static final int GALL_TYPE = 2;
	public static final int MAX_TYPE = 2;
	private static final String[] TypeNames = {"Barque", "Brigantine", "Galleon"};
	public static String getTypeName(int type) { return TypeNames[type]; }
	public static int getType(String name) {
		name = name.toLowerCase();
		for (int i = 0; i < TypeNames.length; i++)
			if (TypeNames[i].toLowerCase().startsWith(name))
				return i;
		System.err.println("Failed to match ship type: " + name);
		return -1;
	}

	private static final int[] Capacities = {1, 2, 3};
	public static int getCapacity(int type) { return Capacities[type]; }
	public static String getCapacityString(int type) {
		int cap = getCapacity(type);
		return cap + (cap == 1 ? " Cargo Unit" : " Cargo Units");
	}

	public static final int NO_CREW = -1;
	public static final int POOR_CREW = 0;
	public static final int AVG_CREW = 1;
	public static final int GOOD_CREW = 2;
	public static final int EX_CREW = 3;
	public static final int MAX_CREW = 3;
	private static final String[] CrewNames = {"Poor", "Average", "Good", "Excellent"};
	public static final String getCrewName(int type) {
		if (type == NO_CREW)
			return "None";
		else
			return CrewNames[type];
	}
	public static int getCrew(String name) {
		name = name.toLowerCase();
		for (int i = 0; i < CrewNames.length; i++)
			if (CrewNames[i].toLowerCase().startsWith(name))
				return i;
		return NO_CREW;
	}

	public static final int MATCH_ALL_CARGO = -2;
	public static final int MATCH_SINGLE_CARGO = -1;
	public static final int NO_CARGO = 0;
	public static final int FUR_CARGO = 1;
	public static final int GRAIN_CARGO = 2;
	public static final int METAL_CARGO = 3;
	public static final int MINERAL_CARGO = 4;
	public static final int SPICE_CARGO = 5;
	public static final int TEXTILE_CARGO = 6;
	public static final int TIMBER_CARGO = 7;
	public static final int SLAVE_CARGO = 8;
	public static final int MAX_CARGO = 8;
	private static final String[] CargoNames = {"None", "Furs", "Grain", "Metals", "Minerals", "Spices", "Textiles", "Timber", "Slaves"};
	public static String getCargoName(int type) { return CargoNames[type]; }
	public static int getCargo(String name) {
		if (name.equals("?")) return MATCH_SINGLE_CARGO;
		else if (name.equals("*")) return MATCH_ALL_CARGO;
		
		name = name.toLowerCase();
		for (int i = 0; i <= MAX_CARGO; i++)
			if (CargoNames[i].toLowerCase().startsWith(name))
				return i;
		return -1;
	}

	/******** Instance ********/
	private int type;
	private String name;
	private int crewQuality;
	private int[] cargo;
	private String dock;

	public Ship(int type, String name, int crew) {
		this.type = type;
		this.name = name;
		this.crewQuality = crew;
		if (name == null)
			this.name = "New " + getTypeName(type);

		cargo = new int[Capacities[type]];
		Arrays.fill(cargo, NO_CARGO);
	}

	static Ship load(DataInputStream din) throws IOException {
		byte t      = din.readByte();
		String name = din.readUTF();
		byte crew   = din.readByte();
		Ship ship = new Ship(t, name, crew);
		ship.dock   = din.readUTF();
		if (ship.dock.length() == 0)
			ship.dock = null;

		byte cargoCount = din.readByte();
		for (int c = 0; c < cargoCount; c++)
			ship.addCargo(din.readByte());

		return ship;
	}

	boolean saveTo(DataOutputStream dout) throws IOException {
		dout.writeByte((byte)type);
		dout.writeUTF(name);
		dout.writeByte((byte)crewQuality);
		dout.writeUTF(dock == null ? "" : dock);

		byte cargoCount = 0;
		for (int c = 0; c < cargo.length; c++)
			if (cargo[c] != NO_CARGO)
				cargoCount++;
		dout.writeByte(cargoCount);
		for (byte c = 0; c < cargo.length; c++)
			if (cargo[c] != NO_CARGO)
				dout.writeByte(cargo[c]);

		return true;
	}

	public int getType() { return type; }

	public String getName() { return name; }
	public void setName(String s) { name = s; }

	public int getCrew() { return crewQuality; }
	public void setCrew(int crew) { crewQuality = crew; }
	
	/**
	 * Adjust the crew quality up or down.
	 * The crew quality cannot go above excellent, or below poor.
	 */
	public void adjustCrew(int change) {
		crewQuality += change;
		if (crewQuality > MAX_CREW)
			crewQuality = MAX_CREW;
		else if (crewQuality < POOR_CREW)
			crewQuality = POOR_CREW;
	}

	public int getCapacity() { return cargo.length; }

	public int getCargo(int i) { return cargo[i]; }
	private int findCargo(int type) {
		for (int i = 0; i < cargo.length; i++)
			if (cargo[i] == type)
				return i;
		return -1;
	}
	public boolean isFull() {
		return findCargo(NO_CARGO) < 0;
	}
	public int getFreeSpace() {
		int count = 0;
		for (int i = 0; i < cargo.length; i++)
			if (cargo[i] == NO_CARGO)
				count++;
		return count;
	}
	public boolean hasCargo(int type) {
		if (type == MATCH_SINGLE_CARGO || type == MATCH_ALL_CARGO) {
			for (int i = 0; i < cargo.length; i++)
				if (cargo[i] != NO_CARGO)
					return true;
			return false;
		}

		return findCargo(type) >= 0;
	}
	public int[] hasCargoTypes() {
		int[] counts = getCargoCounts();
		int length = 0;
		for (int i = 1; i < counts.length; i++)
			if (counts[i] > 0) length++;
		
		int types[] = new int[length];
		length = 0;
		for (int i = 1; i < counts.length; i++)
			if (counts[i] > 0)
				types[length++] = i;
		
		return types;
	}
	public boolean addCargo(int type) {
		int i = findCargo(NO_CARGO);
		if (i >= 0) {
			cargo[i] = type;
			return true;
		}
		else
			return false;
	}
	/**
	 * Remove an item of cargo.
	 * @param type the type of cargo to be removed. This may be {@link #MATCH_ALL_CARGO},
	 * in which case all cargo will be removed, but {@link #MATCH_SINGLE_CARGO} is not
	 * handled here.
	 * @return <code>true</code> if an item of cargo of the given type was removed.
	 */
	public boolean removeCargo(int type) {
		if (type == MATCH_ALL_CARGO) {
			boolean anyCargo = false;
			for (int i = 0; i < cargo.length; i++) {
				if (cargo[i] != NO_CARGO) {
					cargo[i] = NO_CARGO;
					anyCargo = true;
				}
			}
			return anyCargo;
		}
		
		int i = findCargo(type);
		if (i >= 0) {
			cargo[i] = NO_CARGO;
			return true;
		}
		else
			return false;
	}
	private int[] getCargoCounts() {
		int[] cargoCounts = new int[MAX_CARGO + 1];
		for (int i = 0; i < cargo.length; i++)
			cargoCounts[cargo[i]]++;
		return cargoCounts;
	}
	public String getCargoString() {
		int[] cargoCounts = getCargoCounts();
		String str = null;
		for (int c = 1; c < cargoCounts.length; c++) {
			if (cargoCounts[c] > 0) {
				String cargoName = getCargoName(c);
				if (cargoCounts[c] > 1)
					cargoName += " x" + cargoCounts[c];
				if (str == null)
					str = cargoName;
				else
					str += "/" + cargoName;
			}
		}

		return (str == null ? "None" : str);
	}

	public String getDocked() { return dock; }
	public boolean isDocked() { return dock != null; }
	public void setDocked(String str) { dock = str; }

	/******** TableModel helper methods ********/
	public static final int TYPE_COLUMN = 0;
	public static final int NAME_COLUMN = 1;
	public static final int CREW_COLUMN = 2;
	public static final int CAPACITY_COLUMN = 3;
	public static final int CARGO_COLUMN = 4;
	public static final int DOCKED_COLUMN = 5;
	public static final int COLUMN_COUNT = 6;

	public static int getColumnCount() { return COLUMN_COUNT; }
	public static String getColumnName(int column) {
		switch (column) {
			case TYPE_COLUMN:
				return "Ship type";
			case NAME_COLUMN:
				return "Name";
			case CREW_COLUMN:
				return "Crew quality";
			case CAPACITY_COLUMN:
				return "Cargo cap.";
			case CARGO_COLUMN:
				return "Current cargo";
			case DOCKED_COLUMN:
				return "Where docked";
		}
		return "Bad column #" + column;
	}
	public static Class getColumnClass(int column) {
		if (column == CAPACITY_COLUMN)
			return Integer.class;
		else
			return String.class;
	}
	public static boolean isCellEditable(int column) {
		return (column == NAME_COLUMN);
	}
	public Object getValueAt(int column) {
		switch (column) {
			case TYPE_COLUMN:
				return getTypeName(getType());
			case NAME_COLUMN:
				return getName();
			case CREW_COLUMN:
				return getCrewName(getCrew());
			case CAPACITY_COLUMN:
				return Integer.valueOf(getCapacity());
			case CARGO_COLUMN:
				return getCargoString();
			case DOCKED_COLUMN:
				return (dock == null ? "At large" : dock);
		}
		return "Bad column #" + column;
	}
	public boolean setValueAt(Object val, int column) {
		if (column == NAME_COLUMN) {
			name = val.toString();
			return true;
		}
		else
			return false;
	}
}
