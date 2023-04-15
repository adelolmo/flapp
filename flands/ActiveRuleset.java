package flands;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Set of the rules that may be active within a game.
 * Each rule extension is defined by a String key.
 * A rule may be chosen at the start of the game, in which case it is 'fixed';
 * a rule may also be required within a book, in which case it is added as
 * temporary while the character is in that book.
 * 
 * @author Jonathan Mann
 */
public class ActiveRuleset {
	public static String SARVEN = "Sarven";
	
	private Set<String> fixedRules;
	private Set<String> tempRules;
	
	public ActiveRuleset() {
		fixedRules = new HashSet<String>();
		tempRules = new HashSet<String>();
	}
	
	public boolean hasRule(String rule) {
		rule = rule.trim().toLowerCase();
		return fixedRules.contains(rule) || tempRules.contains(rule);
	}
	
	public void addFixedRule(String rule) {
		fixedRules.add(rule.trim().toLowerCase());
	}
	
	public void addFixedRules(String rules) {
		if (rules == null) return;
		String[] rs = rules.split(",");
		for (int r = 0; r < rs.length; r++)
			addFixedRule(rs[r]);
	}
	
	/**
	 * Get a comma-separated list of the fixed rules (for output into a saved game).
	 */
	public String getFixedRules() {
		StringBuffer sb = new StringBuffer();
		for (Iterator<String> i = fixedRules.iterator(); i.hasNext(); ) {
			if (sb.length() > 0) sb.append(',');
			sb.append(i.next());
		}
		return sb.toString();
	}
	
	public void addTempRule(String rule) {
		tempRules.add(rule.trim().toLowerCase());
	}
	
	public void addTempRules(String rules) {
		if (rules == null) return;
		String[] rs = rules.split(",");
		for (int r = 0; r < rs.length; r++)
			addTempRule(rs[r]);
	}
	
	public void clearTempRules() {
		tempRules.clear();
	}
}
