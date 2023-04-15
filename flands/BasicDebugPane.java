package flands;

import javax.swing.AbstractSpinnerModel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * The beginnings of a class to let someone test a section,
 * by setting up the character beforehand.
 * It was too boring to complete, so I stopped :)
 *
 * @author Jonathan Mann
 */
public class BasicDebugPane extends JPanel implements ChangeListener {
	private Adventurer adv;
	private ProfessionModel profession;
	private AbilityModel ability;
	private JSpinner abilitySpinner, abilityValueSpinner;
	
	private static class ProfessionModel extends AbstractSpinnerModel {
		private int profession;
		
		private ProfessionModel(int profession) {
			this.profession = profession;
		}
		
		private static Object getValue(int prof) {
			return Adventurer.getProfessionName(prof);
		}
		
		private int getProfession() { return profession; }
		
		public Object getValue() {
			return getValue(profession);
		}

		public void setValue(Object value) {
			int profType = Adventurer.getProfessionType(value.toString());
			if (profType >= 0) {
				profession = profType;
				fireStateChanged();
			}
		}

		public Object getNextValue() {
			return getValue((profession + 1) % Adventurer.PROF_COUNT);
		}

		public Object getPreviousValue() {
			return getValue((profession + Adventurer.PROF_COUNT - 1) % Adventurer.PROF_COUNT);
		}
	}
	
	private static class AbilityModel extends AbstractSpinnerModel {
		private static final int[] AbilityTypes =
		{
			Adventurer.ABILITY_CHARISMA,
			Adventurer.ABILITY_COMBAT,
			Adventurer.ABILITY_MAGIC,
			Adventurer.ABILITY_SANCTITY,
			Adventurer.ABILITY_SCOUTING,
			Adventurer.ABILITY_THIEVERY,
			Adventurer.ABILITY_RANK,
			Adventurer.ABILITY_STAMINA
		};
		
		private int ability;
		public Object getValue() {
			// TODO Auto-generated method stub
			return null;
		}

		public void setValue(Object value) {
			// TODO Auto-generated method stub
			
		}

		public Object getNextValue() {
			// TODO Auto-generated method stub
			return null;
		}

		public Object getPreviousValue() {
			// TODO Auto-generated method stub
			return null;
		}
	}
	
	public BasicDebugPane(Adventurer adv) {
		this.adv = adv;
		
		profession = new ProfessionModel(adv.getProfession());
		profession.addChangeListener(this);
		JSpinner professionSpinner = new JSpinner(profession);
	}

	
	public void stateChanged(ChangeEvent e) {
		Object src = e.getSource();
		if (src.equals(profession))
			adv.setProfession(profession.getProfession());
	}
}
