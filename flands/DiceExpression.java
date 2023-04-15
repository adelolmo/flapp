package flands;

/**
 * Encapsulates a dice expression, of the form 'Xd + y'.
 * @see Roller
 * @author Jonathan Mann
 */
public class DiceExpression {
	public int dice;
	public int adjustment;

	/**
	 * Handle an expression of the form 'xD+y',
	 * where x is the number of dice and
	 * y is the adjustment.
	 */
	public DiceExpression(String exp) {
		exp = exp.toUpperCase();
		dice = 0;
		adjustment = 0;

		int index = exp.indexOf("D");
		if (index >= 0) {
			try {
				dice = Integer.parseInt(exp.substring(0, index).trim());
			}
			catch (NumberFormatException nfe) {
				System.out.println("Expected number before D: " + exp);
			}
		}

		index = exp.indexOf("+");
		if (index < 0) {
			index = exp.indexOf("-");
			if (index >= 0)
				index--;
		}
		if (index >= 0 || dice == 0) {
			try {
				adjustment = Integer.parseInt(index < 0 ? exp : exp.substring(index+1));
			}
			catch (NumberFormatException nfe) {
				System.out.println("Expected adjustment number: " + exp);
			}
		}
	}

	public DiceExpression(DiceExpression exp) {
		dice = exp.dice;
		adjustment = exp.adjustment;
	}
	
	public void addAdjustment(int delta) {
		adjustment += delta;
	}

	public Roller createRoller() {
		return (dice == 0 ? null : new Roller(dice, adjustment));
	}

	public String toString() {
		return (dice > 0 ? dice + "D" : "") + (adjustment < 0 || dice == 0 ? Integer.toString(adjustment) : (adjustment == 0 ? "" : "+" + adjustment));
	}
}
