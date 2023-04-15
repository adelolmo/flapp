package flands;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/**
 * Keeps track of all the things affecting the players abilities.
 * A sorted list of effects, or items, is kept for each ability: the natural ability
 * value is modified by each effect in turn.
 * @author Jonathan Mann
 */
public class EffectSet {
	public static class EffectRecord implements Comparable<EffectRecord> {
		public Object src;
		public Effect effect;

		public EffectRecord(Object src) {
			this(src, null);
		}
		public EffectRecord(Object src, Effect e) {
			this.src = src;
			this.effect = e;
		}

		public boolean isItem() { return (src instanceof Item); }
		public Item getItem() { return isItem() ? (Item)src : null; }
		public boolean isImplicit() { return (effect == null); }

		/**
		 * For once, sorting is important.
		 * Some types of AbilityEffects should be listed before others;
		 * it's also important to make sure that effects with different sources
		 * are never equal.
		 * <p>
		 * Records are sorted by:
		 * - records with Effects before records without (eg. intrinsic Item effects)
		 * - records without Effects are in no particular order (simple items only have +/- effect)
		 * - Effects are sorted by description if present
		 * - Effects are sorted by type (AURA, WIELD, USE, TOOL in order)
		 */
		public int compareTo(EffectRecord r) {
			if ((src == r.src || src.equals(r.src)) && effect == r.effect) return 0;

			if (effect == null) return 1;
			else if (r.effect == null) return -1;
			else {
				int effectcmp = effect.compareTo(r.effect);
				return (effectcmp != 0 ? effectcmp :
					src.hashCode() - r.src.hashCode());
			}
		}

		public boolean equals(Object o) {
			try {
				EffectRecord r = (EffectRecord)o;
				if (src != r.src && !src.equals(r.src)) return false;
				return (effect.compareTo(r.effect) == 0);
			}
			catch (ClassCastException cce) { return false; }
		}
		
		public StyledTextList getStyledSource(Adventurer owner) {
			StyledTextList stList = new StyledTextList();
			if (src == null)
				stList.add("Unknown source", null);
			else if (src instanceof Item) {
				SimpleAttributeSet atts = new SimpleAttributeSet();
				StyleConstants.setBold(atts, true);
				stList.add(((Item)src).getName(), atts);
			}
			else if (src instanceof Blessing)
				stList.add(((Blessing)src).getContentString(), null);
			else if (src instanceof Curse)
				stList.add(((Curse)src).getName(), null);
			else if (src instanceof Adventurer.GodEffectSrc)
				stList.add("Initiate of " + ((Adventurer.GodEffectSrc)src).getGod(), null);
			
			return stList;
		}
		
		public String toString() { return "[src=" + src + ",effect=" + effect + "]"; }
	}

	private Adventurer owner;
	private SortedSet[] statRelated;
	private boolean[] updatedAbilities;

	public EffectSet(Adventurer owner) {
		this.owner = owner;
		statRelated = new SortedSet[Adventurer.ABILITY_STAMINA + 1];
		for (int i = 0; i < statRelated.length; i++)
			statRelated[i] = new TreeSet<EffectRecord>();
		updatedAbilities = new boolean[statRelated.length];
	}

	public Adventurer getAdventurer() { return owner; }

	public void notifyOwner() {
		if (updatedAbilities[Adventurer.ABILITY_COMBAT] || updatedAbilities[Adventurer.ABILITY_RANK])
			updatedAbilities[Adventurer.ABILITY_DEFENCE] = true;
		if (owner == null) return;
		for (int a = 0; a < updatedAbilities.length; a++)
			if (updatedAbilities[a]) {
				owner.checkAbilityBonus(a);
				updatedAbilities[a] = false;
			}
	}

	private final void abilityUpdated(int a) {
		updatedAbilities[a] = true;
	}

	@SuppressWarnings("unchecked")
	private SortedSet<EffectRecord> getStatRelated(int ability) { return (SortedSet<EffectRecord>)statRelated[ability]; }
	public void addStatRelated(int ability, Object src) {
		addStatRelated(ability, new EffectRecord(src));
	}
	public void addStatRelated(int ability, Object src, Effect e) {
		addStatRelated(ability, new EffectRecord(src, e));
	}
	public void addAbilityPotionBonus(int ability) {
		addStatRelated(ability, Item.AbilityPotionSource, AbilityEffect.createAbilityBonus(ability, 1));
	}

	private void addStatRelated(int ability, EffectRecord r) {
		int a1 = ability, a2 = ability + 1;
		if (ability == Adventurer.ABILITY_ALL) {
			a1 = 0;
			a2 = Adventurer.ABILITY_COUNT;
		}
		
		for (int a = a1; a < a2; a++) {
			if (!getStatRelated(a).contains(r)) {
				System.out.println("Didn't already have this ability effect");
				getStatRelated(a).add(r);
			}
			else
				System.out.println("Already had this ability effect");
			
			abilityUpdated(a); // either way - the ability effect may have been modified
		}
	}

	/**
	 * Search for an occurrence of the given item/effect pair.
	 * The source must be identical (ie. pointer equality);
	 * the effect is compared on a field-by-field basis.
	 * This method is used to check for potion bonuses, which are added with a <code>null</code> source.
	 */
	public boolean hasStatRelated(int ability, Object src, Effect e) {
		for (Iterator<EffectRecord> i = getStatRelated(ability).iterator(); i.hasNext(); ) {
			EffectRecord er = i.next();
			if (er.src == src) {
				if (e.compareTo(er.effect) == 0)
					return true;
			}
		}
		return false;
	}

	/**
	 * Look for an 'adjust' ability effect related to the given source, and return the
	 * amount of that adjustment.
	 * @param ability the ability being affected;
	 * @param src the effect source.
	 * @return <code>0</code> if a related adjust effect can't be found.
	 */
	public int getStatRelatedBonus(int ability, Object src) {
		for (Iterator<EffectRecord> i = getStatRelated(ability).iterator(); i.hasNext(); ) {
			EffectRecord er = i.next();
			if (er.src == src) {
				if (er.effect instanceof AbilityEffect) {
					AbilityEffect ae = (AbilityEffect)er.effect;
					if (ae.getModifyType() == AbilityEffect.ADJUST_ABILITY)
						return ae.getValue();
				}
			}
		}
		return 0;
	}
	
	public boolean hasAbilityPotionBonus(int ability) {
		return hasStatRelated(ability, Item.AbilityPotionSource, AbilityEffect.createAbilityBonus(ability, 1));
	}

	public void notifyEffectsUpdated(Object src) {
		for (int a = 0; a < statRelated.length; a++) {
			for (Iterator<EffectRecord> i = getStatRelated(a).iterator(); i.hasNext(); ) {
				if (i.next().src == src) {
					abilityUpdated(a);
					break;
				}
			}
		}
		notifyOwner();
	}

	public void removeStatRelated(int ability, Object src) {
		removeStatRelated(ability, new EffectRecord(src));
	}
	public void removeStatRelated(int ability, Object src, Effect e) {
		removeStatRelated(ability, new EffectRecord(src, e));
	}
	public void removeAbilityPotionBonus(int ability) {
		removeStatRelated(ability, Item.AbilityPotionSource, AbilityEffect.createAbilityBonus(ability, 1));
	}
	public void removeAllItems() {
		for (int a = 0; a < statRelated.length; a++) {
			for (Iterator<EffectRecord> i = getStatRelated(a).iterator(); i.hasNext(); ) {
				if (i.next().isItem())
					i.remove();
				abilityUpdated(a);
			}
		}
	}
	private void removeStatRelated(int ability, EffectRecord r) {
		int a1 = ability, a2 = ability + 1;
		if (ability == Adventurer.ABILITY_ALL) {
			a1 = 0;
			a2 = Adventurer.ABILITY_COUNT;
		}
		for (int a = a1; a < a2; a++) 
			if (getStatRelated(a).remove(r))
				abilityUpdated(a);
	}

	public int adjustAbility(int ability, int value) {
		return adjustAbility(ability, value, Adventurer.MODIFIER_AFFECTED);
	}
	
	public int adjustAbility(int ability, int value, int modifier) {
		boolean applyTool = true, applyArmour = true, applyAura = true;
		switch (modifier) {
		case Adventurer.MODIFIER_NATURAL:
			applyTool = applyArmour = applyAura = false;
			break;
		case Adventurer.MODIFIER_NOARMOUR:
			applyArmour = false;
			break;
		case Adventurer.MODIFIER_NOTOOL:
			applyTool = false;
			break;
		}
		
		int bestToolBonus = 0;
		for (Iterator<EffectRecord> i = getStatRelated(ability).iterator(); i.hasNext(); ) {
			EffectRecord r = i.next();
			if (r.isImplicit()) {
				if (r.isItem()) {
					Item item = r.getItem();
					switch (item.getType()) {
						case Item.WEAPON_TYPE:
						case Item.TOOL_TYPE:
							if (((Item.Weapon)item).affectsAbility(ability) && applyTool)
								bestToolBonus = Math.max(bestToolBonus, item.getBonus());
								//value += item.getBonus();
							break;
						case Item.ARMOUR_TYPE:
							if (ability == Adventurer.ABILITY_DEFENCE && applyArmour)
								value += item.getBonus();
							break;
					}
				}
			}
			else {
				Effect e = r.effect;
				if ((e.getType() == Effect.TYPE_AURA && applyAura) ||
					(e.getType() == Effect.TYPE_TOOL && applyTool) ||
					(e.getType() == Effect.TYPE_WIELDED && r.isItem() && ((Item.Weapon)r.getItem()).isWielded()) && applyTool) {
					if (e instanceof AbilityEffect) {
						AbilityEffect ae = (AbilityEffect)e;
						if (ae.getAbility() == ability ||
							(ae.getAbility() == Adventurer.ABILITY_ALL && ability < Adventurer.ABILITY_COUNT)) {
							if (e.getType() == Effect.TYPE_TOOL) {
								if (ae.getModifyType() != AbilityEffect.ADJUST_ABILITY)
									System.err.println("Tool type Effect has unhandled adjustment type: " + ae.getModifyType());
								else
									bestToolBonus = Math.max(bestToolBonus, ae.getValue());
							}
							else
								value = ae.adjustAbility(value);
						}
					}
				}
			}
		}
		
		if (bestToolBonus > 0)
			// Apply it now
			value += bestToolBonus;

		// Peg the minimum value for an affected ability at 1.
		// This stops curses from lowering an early character's stats below 1.
		// TODO: Does this screw anything else up?
		return Math.max(1, value);
	}
	
	public String getAbilityAdjustments(int ability, int natural) {
		StringBuffer html = new StringBuffer("<html><table><tr><td>Natural Score</td><td align=right>" + natural + "</td></tr>");
		for (Iterator<EffectRecord> i = getStatRelated(ability).iterator(); i.hasNext(); ) {
			EffectRecord r = i.next();
			AbilityEffect ae = null;
			if (r.isImplicit()) {
				if (r.isItem()) {
					Item item = r.getItem();
					switch (item.getType()) {
						case Item.WEAPON_TYPE:
						case Item.TOOL_TYPE:
							if (((Item.Weapon)item).affectsAbility(ability))
								ae = AbilityEffect.createAbilityBonus(ability, item.getBonus());
							break;
						case Item.ARMOUR_TYPE:
							if (ability == Adventurer.ABILITY_DEFENCE)
								ae = AbilityEffect.createAbilityBonus(ability, item.getBonus());
							break;
					}
				}
			}
			else {
				Effect e = r.effect;
				if (e.getType() == Effect.TYPE_AURA ||
					(e.getType() == Effect.TYPE_WIELDED && r.isItem() && ((Item.Weapon)r.getItem()).isWielded())) {
					if (e instanceof AbilityEffect) {
						ae = (AbilityEffect)e;
						if (ae.getAbility() != ability &&
							(ae.getAbility() != Adventurer.ABILITY_ALL || ability >= Adventurer.ABILITY_COUNT))
							ae = null;
					}
				}
			}
			
			if (ae != null) {
				html.append("<tr><td>");
				html.append(r.getStyledSource(owner).toXML());
				html.append("</td><td align=right>");
				html.append(ae.getModStr());
				html.append("</td></tr>");
			}
		}
		
		//return doc;
		html.append("</table></html>");
		return html.toString();
	}
	
	public static AttributeSet createColumnAtts(int align) {
		SimpleAttributeSet atts = new SimpleAttributeSet();
		Node.setViewType(atts, Node.ParagraphViewType);
		StyleConstants.setAlignment(atts, align);
		StyleConstants.setLeftIndent(atts, 5.0f);
		StyleConstants.setRightIndent(atts, 5.0f);
		return atts;
	}

	public static void main(String args[]) {
		Item tool1 = new Item.Tool("compass", Adventurer.ABILITY_SCOUTING, 1);
		Item rod = new Item.Weapon("rod of woe", 4);
		AbilityEffect magicToolEffect = AbilityEffect.createAbilityBonus(Adventurer.ABILITY_SCOUTING, 4);
		magicToolEffect.type = Effect.TYPE_TOOL;
		rod.addEffect(magicToolEffect);
		Item tool2 = new Item.Tool("sextant", Adventurer.ABILITY_SCOUTING, 3);
		EffectSet effects = new EffectSet(new Adventurer());
		effects.addStatRelated(Adventurer.ABILITY_SCOUTING, tool1);
		effects.addStatRelated(Adventurer.ABILITY_SCOUTING, tool2);
		effects.addStatRelated(Adventurer.ABILITY_COMBAT, rod);
		effects.addStatRelated(Adventurer.ABILITY_SCOUTING, rod, magicToolEffect);
		System.out.println("3 -> " + effects.adjustAbility(Adventurer.ABILITY_SCOUTING, 3));
	}
}
