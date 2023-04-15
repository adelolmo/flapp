package flands;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;

import javax.swing.JOptionPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyleConstants;

import org.xml.sax.Attributes;

/**
 * The node that handles combat for the character. Rather complicated: players may flee
 * some fights at arbitrary moments, the enemy may do different sorts of damage,
 * there may be more than opponent (sequentially or simultaneously), and the
 * code also has to handle combat-related blessings.
 * 
 * @author Jonathan Mann
 */
public class FightNode extends Node implements Executable, ActionListener, Roller.Listener {
	public static final String ElementName = "fight";
	private static List<FightNode> groupFights = new LinkedList<FightNode>();

	private static class FightIterator implements Iterator<FightNode> {
		private String group;
		private Iterator<FightNode> i;
		private FightNode current;
		private FightIterator(Iterator<FightNode> i, String group) {
			this.i = i;
			this.group = group;
			findNext();
		}
		
		private void findNext() {
			while (i.hasNext()) {
				current = i.next();
				if (current.group.equals(group))
					return;
			}
			current = null;
		}
		
		public boolean hasNext() {
			return current != null;
		}

		public FightNode next() {
			if (current == null)
				throw new NoSuchElementException();
			FightNode temp = current;
			findNext();
			return temp;
		}

		public void remove() {
			i.remove();
			findNext();
		}
		
	}
	
	private static Iterator<FightNode> getFights(String group) {
		return new FightIterator(groupFights.iterator(), group);
	}
	
	private EnemyDetails detailsNode;
	private AttackNode attackNode;
	private DefendNode defendNode;
	private SkipNode skipNode = null;
	private RoundNode roundNode = null;
	private DamageNode damageNode = null;
	private FleeNode fleeNode = null;
	private ChoiceNode fleeChoiceNode = null;
	private GotoNode fleeGotoNode = null;

	private String enemy;
	private int combat;
	private int defence;
	private int stamina;
	private int fleeAt;
	private boolean playerFirst;
	private String group;
	private boolean usingAbilityBonus = false;
	private int attackBonus = 0;
	private int defenceBonus = 0;
	private String playerDefence;
	private int abilityDamaged = -1;
	private String preDamage;
	private String staminaLost;
	private boolean endedFight = false;
	private String useCache;
	private int attackDice;
	private boolean skipping = false;

	public FightNode(Node parent) {
		super(ElementName, parent);
		findExecutableGrouper().addExecutable(this);
	}

	public void init(Attributes atts) {
		enemy = atts.getValue("name");
		combat = getIntValue(atts, "combat", -1);
		defence = getIntValue(atts, "defence", -1);
		stamina = getIntValue(atts, "stamina", -1);
		fleeAt = getIntValue(atts, "flee", -1);
		playerFirst = getBooleanValue(atts, "playerFirst", true);
		playerDefence = atts.getValue("playerDefence");
		String val = atts.getValue("abilityDamaged");
		if (val != null)
			abilityDamaged = Adventurer.getAbilityType(val);
		preDamage = atts.getValue("preDamage");
		staminaLost = atts.getValue("staminaLost");
		useCache = atts.getValue("useCache");
		if (useCache != null) {
			ItemList cacheItems = CacheNode.getItemCache(useCache);
			if (cacheItems != null) {
				EffectSet effects = cacheItems.createTempEffects();
				System.out.println("Stats before=(" + combat + "," + defence + "," + stamina + ")");
				int combatRaise = effects.adjustAbility(Adventurer.ABILITY_COMBAT, combat) - combat;
				combat += combatRaise;
				defence = effects.adjustAbility(Adventurer.ABILITY_DEFENCE, defence) + combatRaise;
				stamina = effects.adjustAbility(Adventurer.ABILITY_STAMINA, stamina);
				System.out.println("Stats after=(" + combat + "," + defence + "," + stamina + ")");
				cacheItems.dumpTempEffects();
			}
		}
		group = atts.getValue("group");
		attackDice = getIntValue(atts, "attackDice", 2);
		if (group != null)
			groupFights.add(this);
		if (enemy == null || combat < 0 || defence < 0 || stamina < 0)
			System.err.println("<fight> element is missing some attributes: needs name, combat, defence, and stamina");

		super.init(atts);

		Node parentNode = this;
		//if (getParent() instanceof SectionNode) {
			TableNode detailsTable = new TableNode("FightTable", this);
			addChild(detailsTable);
			detailsTable.init(atts);
			parentNode = detailsTable;
		//}

		ParagraphNode row = new ParagraphNode(parentNode, StyleConstants.ALIGN_CENTER);
		parentNode.addChild(row);
		row.init(atts);
		row.setEnabled(true);

		detailsNode = new EnemyDetails(row);
		row.addChild(detailsNode);
		detailsNode.init(atts);
		detailsNode.handleEndTag();

		attackNode = new AttackNode(row);
		row.addChild(attackNode);
		attackNode.init(atts);

		defendNode = new DefendNode(row);
		row.addChild(defendNode);
		defendNode.init(atts);
		
		if (group == null) {
			// Too tricky getting it to work with a grouped fight
			skipNode = new SkipNode(row);
			row.addChild(skipNode);
			skipNode.init(atts);
		}
	}

	protected Element createElement() { return null; }

	public int getPlayerCombat() {
		if (cachedAttackBonus != 0) {
			attackBonus = cachedAttackBonus;
			cachedAttackBonus = 0;
		}
		return getAdventurer().getAbilityValue(Adventurer.ABILITY_COMBAT, Adventurer.MODIFIER_AFFECTED) + attackBonus;
	}
	public int getPlayerDefence() {
		if (playerDefence == null)
			return getAdventurer().getDefence().affected;
		else
			return getAttributeValue(playerDefence);
	}

	public boolean canWin() {
		return (getPlayerCombat() + attackDice*6 > defence);
	}
	private boolean canLoseThisFight() {
		return (combat + 12 > getPlayerDefence());
	}
	public boolean canLose() {
		if (canLoseThisFight())
			return true;
		
		if (group != null) {
			for (Iterator<FightNode> i = getFights(group); i.hasNext(); ) {
				FightNode otherFight = i.next();
				if (otherFight.canLoseThisFight())
					return true;
			}
		}
		return false;
	}
	
	void endFight() {
		endFight(true);
	}
	void endFight(boolean continueExecution) {
		endedFight = true;
		FLApp.getSingle().fireGameEvent(GameEvent.FIGHT_END);
		if (defenceBonus > 0)
			Blessing.removeDefenceBlessing();
		if (usingAbilityBonus)
			getAdventurer().getEffects().removeAbilityPotionBonus(Adventurer.ABILITY_COMBAT);
		if (continueExecution)
			findExecutableGrouper().continueExecution(this, true);
	}

	public void damageEnemy(int damage) {
		if (damage > 0) {
			if (damage > stamina)
				attackNode.damageDone = damage = stamina;
			stamina -= damage;
			detailsNode.updateStamina();
			if (staminaLost != null)
				getCodewords().adjustValue(staminaLost, damage);
		}
		
		if (stamina == 0 || stamina <= fleeAt) {
			// Player won fight
			boolean continueFight = false;
			if (group != null) {
				// Make sure other fights in this group aren't still going
				for (Iterator<FightNode> i = getFights(group); i.hasNext(); ) {
					FightNode otherFight = i.next();
					if (otherFight == this || otherFight.endedFight) continue;
					continueFight = true;
					break;
				}
			}
			
			if (!continueFight) {
				disableFlee();
				endFight();
				return;
			}
			else
				endedFight = true;
		}
		
		if (!endedFight)
			defendNode.execute();
		if (group != null) {
			for (Iterator<FightNode> i = getFights(group); i.hasNext(); ) {
				FightNode otherFight = i.next();
				if (otherFight == this || otherFight.endedFight) continue;
				otherFight.defendNode.execute();
			}
		}
		if (defendNode.isEnabled() && skipping)
			defendNode.actionPerformed(null);
	}
	/**
	 * Callback from DefendNode.
	 * @param damage the amount of damage rolled up;
	 * @param done whether this was the last attack now, or whether there's still more to come.
	 * @return <code>true</code> if the player is still alive.
	 * This is only relevant when <code>done</code> is false, so that the node knows whether to proceed.
	 */
	public boolean damagePlayer(int damage, boolean done) {
		System.out.println("damagePlayer(" + done + ") called");
		boolean death = false;
		if (damage > 0) {
			if (damageNode == null || !damageNode.isReplacement()) {
				if (abilityDamaged < 0) {
					Adventurer.StaminaStat stam = getAdventurer().getStamina();
					if (stam.current < damage)
						defendNode.damageDone = stam.current;
					death = stam.damage(damage);
				}
				else {
					int damageDone = getAdventurer().adjustAbility(abilityDamaged, -damage, true);
					defendNode.damageDone = -damageDone;
				}
			}
		}
		if (getAdventurer().isDead())
			death = true;
		
		if (death) {
			disableFlee();
			endFight();
			return false;			
		}
		
		if (damage > 0 && damageNode != null) {
			if (!damageNode.execute(null))
				return true;
		}

		if (done) {
			if (roundNode != null)
				if (!roundNode.execute(null))
					return true;
			defendDone();
			//attackNode.setEnabled(true);
		}
		return true;
	}

	public void roundNodePerformed(RoundNode round) {
		if (getAdventurer().isDead()) {
			disableFlee();
			endFight();
			return;
		}

		if (fleeNode != null && !fleeNode.getExecuted())
			fleeNode.execute();

		defendDone();
		//attackNode.setEnabled(true);
	}

	public void damageNodePerformed(DamageNode damage) {
		if (roundNode != null)
			if (!roundNode.execute(null))
				return;

		if (getAdventurer().isDead()) {
			disableFlee();
			endFight();
			return;
		}

		defendDone();
		//attackNode.setEnabled(true);
	}

	void fleeNodeActivated(FleeNode flee) {
		// User has chosen to flee - the fight is over
		resetExecute();
		enableFlee();
		endFight(false);
	}
	
	public void actionPerformed(ActionEvent e) {
		// Callback from fleeChoiceNode or fleeGotoNode
		resetExecute();
		endFight();
	}

	private void enableFlee() {
		System.out.println("Enabling Flee gotos");
		if (fleeChoiceNode != null) {
			fleeChoiceNode.execute(null);
			UndoManager.getCurrent().add(fleeChoiceNode);
		}
		if (fleeGotoNode != null) {
			fleeGotoNode.execute(null);
			UndoManager.getCurrent().add(fleeGotoNode);	
		}
	}
	
	private void disableFlee() {
		System.out.println("Disabling Flee gotos");
		if (fleeChoiceNode != null)
			fleeChoiceNode.setEnabled(false);
		if (fleeGotoNode != null)
			fleeGotoNode.setEnabled(false);
	}
	
	private void hookupNodes() {
		if (roundNodes.size() > 0) {
			roundNode = roundNodes.remove(0);
			roundNode.setOwner(this);
		}

		if (damageNodes.size() > 0) {
			damageNode = damageNodes.remove(0);
			damageNode.setOwner(this);
		}

		if (fleeNodes.size() > 0) {
			fleeNode = fleeNodes.remove(0);
			fleeNode.setOwner(this);
		}

		fleeChoiceNode = getRoot().getFleeChoice();
		if (fleeChoiceNode != null)
			fleeChoiceNode.addActionListener(this);
		else {
			fleeGotoNode = getRoot().getFleeGoto();
			if (fleeGotoNode != null)
				fleeGotoNode.addActionListener(this);
		}
				
	}
	
	public boolean execute(ExecutableGrouper grouper) {
		if (getAdventurer().isDead() || endedFight) // ie. either party is already dead
			return true; // it happens, especially with pre-fight hijinx

		detailsNode.setEnabled(true);

		hookupNodes();
		
		if (staminaLost != null)
			getCodewords().setValue(staminaLost, 0);
		
		if (preDamage != null) {
			// Do this damage now
			int damage = 0;
			if (getCodewords().hasCodeword(preDamage))
				damage = getCodewords().getValue(preDamage);
			else if (isVariableDefined(preDamage))
				damage = getVariableValue(preDamage);
			
			if (damage != 0) {
				if (damage > stamina)
					damage = stamina;
				stamina -= damage;
				detailsNode.updateStamina();
				if (staminaLost != null)
					getCodewords().adjustValue(staminaLost, damage);
			}
			
			if (stamina == 0 || stamina <= fleeAt) {
				// Player won fight
				boolean continueFight = false;
				if (group != null) {
					// Make sure other fights in this group aren't still going
					for (Iterator<FightNode> i = getFights(group); i.hasNext(); ) {
						FightNode otherFight = i.next();
						if (otherFight == this || otherFight.endedFight) continue;
						continueFight = true;
						break;
					}
				}
				
				if (!continueFight) {
					disableFlee();
					endFight(false);
				}
				else
					endedFight = true;

				return true;
			}
		}
		
		FLApp.getSingle().fireGameEvent(GameEvent.FIGHT_START);
		if (getBlessings().hasBlessing(Blessing.WRATH)) {
			int useBlessing = JOptionPane.showConfirmDialog(FLApp.getSingle(), "Do you want to use your\nblessing of Divine Wrath?", "Use Blessing?", JOptionPane.YES_NO_OPTION);
			if (useBlessing == JOptionPane.YES_OPTION) {
				setEnabled(false);
				getBlessings().removeBlessing(Blessing.WRATH);
				Roller r = new Roller(1, 0);
				r.addListener(this);
				r.startRolling();
				return false;
			}
		}

		if (roundNode != null && roundNode.isPreFight()) {
			if (!roundNode.execute(null))
				return false;
			if (getAdventurer().isDead()) {
				endFight();
				return true;
			}
		}

		finalFightPrep();
		if (skipNode != null && roundNode == null && damageNode == null)
			skipNode.execute();
		
		return false;
	}

	private void finalFightPrep() {
		if (fleeNode != null)
			fleeNode.execute();
		else
			enableFlee();
		
		if (playerFirst)
			attackNode.setEnabled(true);
		else
			defendNode.setEnabled(true);

		if (group != null) {
			for (Iterator<FightNode> i = getFights(group); i.hasNext(); ) {
				FightNode otherFight = i.next();
				if (otherFight == this || otherFight.endedFight) continue;
				
				// Prep each other grouped fight 
				otherFight.executeGroupedFight();
			}
		}		
	}
	
	/** Callback from Divine Wrath blessing. */
	public void rollerFinished(Roller r) {
		if (r != null) {
			int damage = r.getResult();
			if (damage > stamina)
				damage = stamina;
			stamina -= damage;
			detailsNode.updateStamina();
			if (staminaLost != null)
				getCodewords().adjustValue(staminaLost, damage);
		}
		
		setEnabled(true);
		
		if (stamina <= 0 || stamina <= fleeAt) {
			// Player won fight
			boolean continueFight = false;
			if (group != null) {
				// Make sure other fights in this group aren't still going
				for (Iterator<FightNode> i = getFights(group); i.hasNext(); ) {
					FightNode otherFight = i.next();
					if (otherFight == this || otherFight.endedFight) continue;
					continueFight = true;
					break;
				}
			}
			
			if (!continueFight) {
				disableFlee();
				endFight(false);
			}
			else
				endedFight = true;

			findExecutableGrouper().continueExecution(this, true);
			return;
		}
		
		if (roundNode != null && roundNode.isPreFight()) {
			if (!roundNode.execute(null))
				return;
			if (getAdventurer().isDead()) {
				endFight();
				findExecutableGrouper().continueExecution(this, true);
				return;
			}
		}
		
		finalFightPrep();
	}

	private void executeGroupedFight() {
		detailsNode.setEnabled(true);
		hookupNodes();
		
		if (playerFirst)
			attackNode.setEnabled(true);
		else
			defendNode.setEnabled(true);
		
		if (skipNode != null && roundNode == null && damageNode == null)
			skipNode.execute();
	}
	
	private void attackClicked() {
		if (group != null) {
			for (Iterator<FightNode> i = getFights(group); i.hasNext(); ) {
				FightNode otherFight = i.next();
				if (otherFight == this) continue;
				otherFight.attackNode.setEnabled(false);
			}
		}
	}

	private void defendDone() {
		if (group != null) {
			for (Iterator<FightNode> i = getFights(group); i.hasNext(); ) {
				FightNode otherFight = i.next();
				if (otherFight == this) continue;
				if (otherFight.defendNode.isEnabled())
					// Player can't attack quite yet
					return;
			}
		}
		
		attackNode.setEnabled(true);
		if (group != null)
			for (Iterator<FightNode> i = getFights(group); i.hasNext(); ) {
				FightNode fight = i.next();
				if (fight == this || fight.endedFight) continue;
				fight.attackNode.setEnabled(true);
			}
		if (skipping)
			attackNode.actionPerformed(null);
	}
	
	public void resetExecute() {
		// In what context would a fight be repeated?
		// Possibly the enemy's stamina should be returned to its initial value.
		// For now, just disable all the initial nodes
		attackNode.setEnabled(false);
		defendNode.setEnabled(false);
		if (roundNode != null)
			roundNode.setEnabled(false);
		if (damageNode != null)
			damageNode.setEnabled(false);
		if (fleeChoiceNode != null)
			fleeChoiceNode.resetExecute();
		if (fleeGotoNode != null)
			fleeGotoNode.resetExecute();
		if (skipNode != null)
			skipNode.setEnabled(false);
	}

	private class EnemyDetails extends Node {
		private int staminaOffset, staminaLength;

		public EnemyDetails(Node parent) {
			super("EnemyDetails", parent);
			setEnabled(false);
		}

		public void init(Attributes atts) {
			super.init(atts);

			// Add the content now
			String content = enemy + ", COMBAT " + combat + ", Defence " + defence + ", Stamina ";
			String staminaStr = Integer.toString(stamina);
			staminaOffset = getDocument().getLength() + content.length();
			staminaLength = staminaStr.length();
			content += staminaStr;
			addContent(content + "\n");
		}

		private void addContent(String text) {
			Element[] leaves = getDocument().addLeavesTo(getElement(), new String[] { text }, null);
			addEnableElements(leaves);
		}

		public void updateStamina() {
			String str = Integer.toString(stamina);
			while (str.length() < staminaLength)
				str += " ";
			getDocument().replaceContent(staminaOffset, staminaLength, str);
		}

		protected String getElementViewType() { return ParagraphViewType; }
	}

	private abstract class ActionCell extends ActionNode implements Roller.Listener, UndoManager.Creator {
		protected ActionCell(String name, Node parent) {
			super(name, parent);
		}

		protected MutableAttributeSet createStandardAttributes() {
			MutableAttributeSet atts = super.createStandardAttributes();
			setViewType(atts, ParagraphViewType);
			StyleConstants.setItalic(atts, true);
			return atts;
		}

		protected Element createElement() {
			Element parentElement = getParent().getElement();
			SectionDocument.Branch branch = getDocument().createBranchElement(parentElement, createStandardAttributes());
			if (branch != null) {
				if (parentElement != null)
					((SectionDocument.Branch)parentElement).addChild(branch);
			}
			return branch;
		}

		protected void addContent(String text) {
			Element[] leaves = getDocument().addLeavesTo(getElement(), new String[] { text }, null);
			addEnableElements(leaves);
			addHighlightElements(leaves);
		}

		protected int getDiceCount() { return 2; }
		
		protected abstract int getRollAdjustment();
		public void actionPerformed(ActionEvent evt) {
			setEnabled(false);
			Roller r = new Roller(getDiceCount(), getRollAdjustment());
			if (skipping)
				r.setInstant(true);
			r.addListener(this);
			r.startRolling();
		}

		public void rollerFinished(Roller r) {
			damageDone = calcDamageDone(r.getResult());
			if (!r.isInstant())
				r.appendTooltipText(getTipText(damageDone));
			UndoManager.createNew(this);
		}

		// The damage done may get modified by the parent FightNode, if it is truncated (ie. would set Stamina to < 0)
		protected int damageDone;
		protected abstract int calcDamageDone(int rollResult);
		protected abstract void undoDamage(int damage);
		protected String getTipText(int damage) {
			return (damage > 0 ? " - " + damage + " damage" : " - Miss");
		}

		public void undoOccurred(UndoManager undo) {
			undoDamage(damageDone);
			damageDone = 0;
			setEnabled(true);
			if (endedFight) {
				// Restore any bonuses that were in place
				endedFight = false;
				if (defenceBonus > 0)
					Blessing.readdDefenceBlessing();
				if (usingAbilityBonus)
					getAdventurer().getEffects().addAbilityPotionBonus(Adventurer.ABILITY_COMBAT);
				if (fleeNode == null)
					enableFlee();
			}
		}
	}

	private static boolean firstDefenceBlessingMessage = true;
	private class AttackNode extends ActionCell implements Executable {
		public AttackNode(Node parent) {
			super("AttackNode", parent);
			setEnabled(false);
		}

		public void init(Attributes atts) {
			super.init(atts);
			addContent("Attack\n");
		}

		/*
		 * AttackNode needs to implements Executable because it may be reached after
		 * DamageNode or RoundNode. If those nodes include a roll, then an undo should reset
		 * AttackNode and go back to that roll. This is not the case for DefendNode.
		 */

		public boolean execute(ExecutableGrouper grouper) {
			setEnabled(true);
			damageDone = 0;
			UndoManager.getCurrent().add(this); // so it can be undone
			return false;
		}

		public void resetExecute() {
			setEnabled(false);
		}

		protected int getDiceCount() { return attackDice; }
		protected int getRollAdjustment() { return getPlayerCombat(); }

		protected int calcDamageDone(int rollResult) { return rollResult - defence; }

		protected void undoDamage(int damage) {
			if (damage > 0) {
				stamina += damage;
				detailsNode.updateStamina();
			}
		}

		private boolean firstAttack = true;
		public void actionPerformed(ActionEvent evt) {
			if (firstAttack) {
				if (!usingAbilityBonus)
					// This bonus will have to be removed after the fight
					usingAbilityBonus = getAdventurer().getEffects().hasAbilityPotionBonus(Adventurer.ABILITY_COMBAT);

				if (playerDefence == null) {
					// Check whether there's already an active Defence blessing
					defenceBonus = Blessing.findActiveDefenceBlessing();
					
					// If not, check whether the player has a Defence blessing
					// they might want to use
					if (defenceBonus == 0 && getBlessings().hasBlessing(Blessing.DEFENCE) && firstDefenceBlessingMessage) {
						firstDefenceBlessingMessage = false; // so we don't show the dialog again
						int useBlessing = JOptionPane.showConfirmDialog(FLApp.getSingle(), "Do you want to use your\nDefence through Faith blessing?", "Use Blessing?", JOptionPane.YES_NO_OPTION);
						if (useBlessing == JOptionPane.YES_OPTION) {
							defenceBonus = getBlessings().getDefenceBlessingBonus();
							Blessing.addDefenceBlessing(defenceBonus);
							getBlessings().removeBlessing(Blessing.DEFENCE);
							firstDefenceBlessingMessage = true;
							// so the next blessing will have the dialog shown again
						}
					}
				}
			}
			firstAttack = false;
			attackClicked(); // which will disable Attack for other grouped fights

			super.actionPerformed(evt);
		}

		public void rollerFinished(Roller r) {
			super.rollerFinished(r);
			damageEnemy(damageDone);
		}

		public void undoOccurred(UndoManager undo) {
			defendNode.setEnabled(false);
			super.undoOccurred(undo);
		}
		
		protected String getTipText() {
			String text = "<p>Attack: roll ";
			text += getDiceText(getDiceCount());
			text += " and add your COMBAT score ("+getPlayerCombat()+").</p>";
			text += "<p>To hit, you must roll higher than your opponent's Defence (" + defence + ").</p>";
			return text;
		}
	}

	private class DefendNode extends ActionCell {
		/**
		 * The number of separate attacks the enemy makes each round.
		 */
		private int attacks;
		/**
		 * The current attack number; initialised to zero, incremented until it
		 * equals <code>attacks</code>.
		 */
		private int attackNumber = 0;

		public DefendNode(Node parent) {
			super("DefendNode", parent);
			setEnabled(false);
		}

		public void init(Attributes atts) {
			attacks = getIntValue(atts, "attacks", 1);
			super.init(atts);
			addContent("Defend\n");
		}

		public void execute() {
			System.out.println("FightNode.DefendNode.execute() called");
			attackNumber = 0;
			damageDone = 0;
			setEnabled(true);
		}

		protected int getRollAdjustment() { return combat; }

		protected int calcDamageDone(int rollResult) { return rollResult - getPlayerDefence(); }
		protected void undoDamage(int damage) {
			if (damage > 0) {
				if (abilityDamaged < 0)
					getAdventurer().getStamina().heal(damage);
				else
					getAdventurer().adjustAbility(abilityDamaged, damage);
			}
		}

		protected boolean isDone() { return (attackNumber == attacks); }
		public void rollerFinished(Roller r) {
			super.rollerFinished(r);
			attackNumber++;
			if (!damagePlayer(damageDone, attackNumber == attacks))
				return; // player is dead

			if (attackNumber < attacks)
				actionPerformed(null); // to immediately roll the new attack
		}

		public void undoOccurred(UndoManager undo) {
			attackNode.setEnabled(false); // it might be...
			if (getAdventurer().isDead())
				// Execution will have continued beyond the fight
				detailsNode.setEnabled(true);

			super.undoOccurred(undo);

			attackNumber = attacks - 1; // if there is more than 1 attack, can only undo the last
			setEnabled(true);
		}
		
		protected void saveProperties(Properties props) {
			super.saveProperties(props);
			saveProperty(props, "attackNumber", attackNumber);
		}

		protected void loadProperties(Attributes atts) {
			super.loadProperties(atts);
			attackNumber = getIntValue(atts, "attackNumber", attackNumber);
		}
		
		protected String getTipText() {
			String text = "<p>Defend: roll ";
			text += getDiceText(getDiceCount());
			text += " and add your enemy's COMBAT score ("+getRollAdjustment()+").</p>";
			text += "<p>If this exceeds your Defence ("+getPlayerDefence()+") you will be hit.</p>";
			return text;
		}
	}

	static List<RoundNode> roundNodes = new LinkedList<RoundNode>();
	public static RoundNode createRoundNode(Node parent) {
		RoundNode round = new RoundNode(parent);
		roundNodes.add(round);
		return round;
	}

	public static class RoundNode extends Node implements Executable, ExecutableGrouper {
		public static final String ElementName = "fightround";
		private FightNode owner;
		private boolean preFight;
		public RoundNode(Node parent) {
			super(ElementName, parent);
			setEnabled(false);
		}

		public void init(Attributes atts) {
			preFight = getBooleanValue(atts, "pre", false);
		}

		protected Element createElement() { return null; }

		public void handleContent(String text) {
			if (text.trim().length() == 0) return;

			Element[] leaves = getDocument().addLeavesTo(getElement(), new String[] { text }, null);
			addEnableElements(leaves);
		}

		public void setOwner(FightNode owner) {
			this.owner = owner;
		}

		public boolean isPreFight() { return preFight; }

		private ExecutableRunner runner = null;
		public ExecutableGrouper getExecutableGrouper() {
			if (runner == null)
				runner = new ExecutableRunner(ElementName, null);
			return runner;
		}

		/**
		 * @param grouper expect this to be null
		 */
		public boolean execute(ExecutableGrouper grouper) {
			setEnabled(true);
			Node parent = getParent();
			while (!isEnabled()) {
				System.out.println("RoundNode still not enabled: enabling parent nodes");
				parent.setEnabled(true);
				parent = parent.getParent();
			}

			UndoManager.getCurrent().add(this);

			if (runner != null) {
				if (!runner.execute(this))
					return false;
			}
			return true;
		}

		public void resetExecute() {
			if (runner != null)
				runner.resetExecute();
		}

		/* ExecutableGrouper methods - implemented so we can get a callback once Executable children are done */
		public void addExecutable(Executable e) {}
		public void addIntermediateNode(Node n) {}
		public boolean isSeparateThread() { return false; }
		public void continueExecution(Executable eDone, boolean inSeparateThread) {
			System.out.println("FightNode.RoundNode.continueExecution called");
			owner.roundNodePerformed(this);
		}
		
		public void saveProperties(Properties props) {
			super.saveProperties(props);
			if (runner != null && runner.willCallContinue())
				saveProperty(props, "continue", true);
		}
		
		public void loadProperties(Attributes atts) {
			super.loadProperties(atts);
			if (getBooleanValue(atts, "continue", false))
				((ExecutableRunner)getExecutableGrouper()).setCallback(this);
		}
	}

	static List<DamageNode> damageNodes = new LinkedList<DamageNode>();
	public static DamageNode createDamageNode(Node parent) {
		DamageNode damage = new DamageNode(parent);
		damageNodes.add(damage);
		return damage;
	}

	public static class DamageNode extends Node implements Executable, ExecutableGrouper {
		public static final String ElementName = "fightdamage";
		private FightNode owner;
		private boolean replace;
		public DamageNode(Node parent) {
			super(ElementName, parent);
			setEnabled(false);
		}

		public boolean isReplacement() { return replace; }

		public void init(Attributes atts) {
			String type = atts.getValue("type");
			if (type == null || type.startsWith("add"))
				replace = false;
			else if (type.startsWith("repl"))
				replace = true;
			else
				System.out.println("fightdamage: type is unrecognised (should be 'add' or 'replace': " + type);

			super.init(atts);
		}

		protected Element createElement() { return null; }

		public void handleContent(String text) {
			if (text.trim().length() == 0) return;

			Element[] leaves = getDocument().addLeavesTo(getElement(), new String[] { text }, null);
			addEnableElements(leaves);
		}

		public void setOwner(FightNode owner) {
			this.owner = owner;
		}

		private ExecutableRunner runner = null;
		public ExecutableGrouper getExecutableGrouper() {
			if (runner == null)
				runner = new ExecutableRunner(ElementName, null);
			return runner;
		}

		/**
		 * @param grouper expect this to be null
		 */
		public boolean execute(ExecutableGrouper grouper) {
			setEnabled(true);
			Node parent = getParent();
			while (!isEnabled()) {
				System.out.println("DamageNode still not enabled: enabling parent nodes");
				parent.setEnabled(true);
				parent = parent.getParent();
			}

			UndoManager.getCurrent().add(this);

			if (runner != null) {
				if (!runner.execute(this))
					return false;
			}
			return true;
		}

		public void resetExecute() {
			if (runner != null)
				runner.resetExecute();
		}

		/* ExecutableGrouper methods - implemented so we can get a callback once Executable children are done */
		public void addExecutable(Executable e) {}
		public void addIntermediateNode(Node n) {}
		public boolean isSeparateThread() { return false; }
		public void continueExecution(Executable eDone, boolean inSeparateThread) {
			System.out.println("FightNode.DamageNode.continueExecution called");
			owner.damageNodePerformed(this);
		}
		public void saveProperties(Properties props) {
			super.saveProperties(props);
			if (runner != null && runner.willCallContinue())
				saveProperty(props, "continue", true);
		}

		public void loadProperties(Attributes atts) {
			super.loadProperties(atts);
			if (getBooleanValue(atts, "continue", false))
				((ExecutableRunner)getExecutableGrouper()).setCallback(this);
		}
	}

	static List<FleeNode> fleeNodes = new LinkedList<FleeNode>();
	public static FleeNode createFleeNode(Node parent) {
		FleeNode flee = new FleeNode(parent);
		fleeNodes.add(flee);
		return flee;
	}

	public static class FleeNode extends Node implements ActionListener, ExecutableGrouper {
		public static final String ElementName = "flee";
		private FightNode owner;

		public FleeNode(Node parent) {
			super(ElementName, parent);
			setEnabled(false);
		}

		public void setOwner(FightNode owner) {
			this.owner = owner;
		}

		private ExecutableRunner runner = null;
		public ExecutableGrouper getExecutableGrouper() {
			if (runner == null)
				runner = new ExecutableRunner(ElementName, null);
			return runner;
		}

		public void handleContent(String text) {
			Element[] leaves = getDocument().addLeavesTo(getElement(), new String[] { text }, new AttributeSet[] { StyleNode.createActiveAttributes() });
			addEnableElements(leaves);
		}

		private boolean executed = false;
		public boolean getExecuted() { return executed; }
		public void execute() {
			// Called by FightNode when it is executed
			executed = true;
			Node n = this;
			while (!isEnabled()) {
				n.setEnabled(true);
				n = n.getParent();
			}
			if (runner != null) {
				if (runner.execute(this))
					//runner.startExecution(false);
					owner.fleeNodeActivated(this);
			}
		}

		protected Element createElement() { return null; }

		/**
		 * Previously activated when all child nodes had been activated.
		 * Now handled 
		 */
		public void actionPerformed(ActionEvent evt) {
			owner.fleeNodeActivated(this);
		}

		/* ******************************************************************
		 * ExecutableGrouper methods - added so we can get the right callback
		 * once all Executable children have been executed
		 ****************************************************************** */
		
		public void addExecutable(Executable e) {}
		public void addIntermediateNode(Node n) {}

		public void continueExecution(Executable done, boolean inSeparateThread) {
			if (done == runner)
				owner.fleeNodeActivated(this);
		}

		public boolean isSeparateThread() {
			return false;
		}
	}

	protected void saveProperties(Properties props) {
		super.saveProperties(props);
		props.setProperty("stamina", Integer.toString(stamina));
		saveProperty(props, "abilityBonus", usingAbilityBonus);
		saveProperty(props, "defenceBonus", defenceBonus);
	}

	protected void loadProperties(Attributes atts) {
		super.loadProperties(atts);
		stamina = getIntValue(atts, "stamina", stamina);
		detailsNode.updateStamina();
		usingAbilityBonus = getBooleanValue(atts, "abilityBonus", false);
		defenceBonus = getIntValue(atts, "defenceBonus", 0);
		
		// Connect us with all the other Nodes.
		// This is usually done in execute(); when loading the section dynamically
		// a fight may already be in progress.
		hookupNodes();
	}
	
	public void dispose() {
		if (!endedFight) {
			// Player skipped out somehow - dispose of any bonuses that may still be in play
			// TODO: This should happen in the very next section, if the player isn't coming
			// back to the fight. I'm thinking of items with goto effects...
			if (defenceBonus > 0)
				Blessing.removeDefenceBlessing();
			if (usingAbilityBonus)
				getAdventurer().getEffects().removeAbilityPotionBonus(Adventurer.ABILITY_COMBAT);
		}
		if (group != null)
			groupFights.remove(this);
	}
	
	private static int cachedAttackBonus = 0;
	/**
	 * Add an attack bonus. This will be 'grabbed' by the first AttackNode that occurs
	 * after this is set.
	 * @param bonus the amount to add to attack rolls; may be positive or negative.
	 */
	public static void setAttackBonus(int bonus) {
		cachedAttackBonus = bonus;
	}
	
	private class SkipNode extends ActionNode {
		public SkipNode(Node parent) {
			super("SkipNode", parent);
			setEnabled(false);
		}
		
		protected MutableAttributeSet createStandardAttributes() {
			MutableAttributeSet atts = super.createStandardAttributes();
			setViewType(atts, ParagraphViewType);
			StyleConstants.setItalic(atts, true);
			return atts;
		}

		protected Element createElement() {
			Element parentElement = getParent().getElement();
			SectionDocument.Branch branch = getDocument().createBranchElement(parentElement, createStandardAttributes());
			if (branch != null) {
				if (parentElement != null)
					((SectionDocument.Branch)parentElement).addChild(branch);
			}
			return branch;
		}
		
		public void init(Attributes atts) {
			super.init(atts);
			Element[] leaves = getDocument().addLeavesTo(getElement(), new String[] { "Skip\n" }, null);
			addEnableElements(leaves);
			addHighlightElements(leaves);
		}
		
		public void execute() { setEnabled(true); }
		
		public void actionPerformed(ActionEvent evt) {
			boolean killPlayer = false;
			boolean killEnemy = false;
			if (canWin() && canLose()) {
				int result = JOptionPane.showConfirmDialog(FLApp.getSingle(), new String[] {"This will determine the fight result immediately.", "Do you want to continue?"}, "Skip Fight?", JOptionPane.OK_CANCEL_OPTION);
				if (result != JOptionPane.OK_OPTION)
					return;
			}
			else if (canWin()) // !canLose()
				killEnemy = true;
			else if (canLose()) // !canWin()
				killPlayer = true;
			else { // !canWin() && !canLose()
				JOptionPane choosePane = new JOptionPane(new String[] {"Neither you nor your enemy can win this fight.", "If you want, pick a winner now."}, JOptionPane.QUESTION_MESSAGE);
				choosePane.setOptions(new String[] {"Player", enemy, "Cancel"});
				choosePane.createDialog(FLApp.getSingle(), "Pick Winner").setVisible(true);
				Object selection = choosePane.getValue();
				
				if (selection == null || selection.equals("Cancel"))
					return;
				else if (selection.equals(enemy))
					killPlayer = true;
				else
					killEnemy = true;
			}
			// Fallthrough - skipping action will eventually notice that one of
			// the combatants is now dead!
			
			skipping = true;
			setEnabled(false);
			if (killEnemy) {
				stamina = 0;
				detailsNode.updateStamina();
			}
			if (killPlayer) {
				Adventurer.StaminaStat stamina = getAdventurer().getStamina();
				stamina.damage(stamina.current);
			}
			if (attackNode.isEnabled())
				attackNode.actionPerformed(null);
			else
				defendNode.actionPerformed(null);
		}
		
		protected String getTipText() {
			String text;
			if (canWin() && canLose())
				text = "Automatically determine the result of this fight (Warning: your character may be killed).";
			else if (canWin())
				text = "Automatically determine the result of this fight.";
			else if (canLose())
				text = "Automatically determine the result of this fight (Warning: your character will be killed).";
			else
				text = "Determine the result of this fight (neither you nor your enemy can damage each other).";
			return text;
		}
	}
}
