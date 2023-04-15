package flands;


import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToolTip;
import javax.swing.ToolTipManager;
import javax.swing.border.EmptyBorder;

/**
 * Displays all the character's attributes. The Documents being displayed will
 * automatically refresh when Adventurer updates their contents.
 * 
 * @see Adventurer
 * 
 * @author Jonathan Mann
 */
public class AdventurerFrame extends JDialog implements ItemListener, MouseListener, ActionListener, SectionDocument.FontUser {
	private Adventurer adv;

	private JPanel left;
	private JLabel godLabel;
	private JTextField nameField, professionField, godField, moneyField;
	private JTextField rankField, defenceField, staminaField; // Single Stats
	private JTextPane abilityPane, resurrectionPane;
	private JTextArea titlesArea;
	private JList itemList, curseList, blessingList;

	private JPanel godGenderPanel, genderPanel;
	private GBC genderPanelGBC;
	private GridBagLayout gbl;
	private JRadioButton maleButton, femaleButton;

	private static JTextField createTextField() {
		JTextField field = new JTextField();
		field.setEditable(false);
		setFont(field);
		field.setForeground(Color.black);
		return field;
	}

	private static void setFont(Component c) {
		c.setFont(SectionDocument.getPreferredFont());
	}

	private JRadioButton createGenderButton(String text, ButtonGroup group) {
		JRadioButton button = new JRadioButton(text);
		group.add(button);
		setFont(button);
		button.addItemListener(this);
		return button;
	}

	private class AbilityPane extends JTextPane {
		private AbilityPane() {
			setEditable(false);
			setEditorKit(new BookEditorKit());
			ToolTipManager.sharedInstance().registerComponent(this);
		}
		
		public String getToolTipText(MouseEvent evt) {
			int pos = viewToModel(evt.getPoint());
			int ability = adv.posToAbility(pos);
			if (ability >= 0)
				return adv.getAbilityTooltip(ability);
			else
				return null;
		}
		
		public JToolTip createToolTip() {
			System.out.println("AbilityPane.createToolTip called");
			JToolTip tip = super.createToolTip();
			tip.setFont(SectionDocument.getPreferredFont());
			return tip;
		}
	}
	
	public AdventurerFrame() {
		super(FLApp.getSingle(), "Adventure Sheet", false);

		nameField = createTextField();
		professionField = createTextField();
		godField = createTextField();
		rankField = createTextField();
		defenceField = createTextField();
		staminaField = createTextField();
		abilityPane = new AbilityPane();
		setFont(abilityPane);
		abilityPane.setBorder(nameField.getBorder());
		moneyField = createTextField();
		moneyField.addMouseListener(this);
		resurrectionPane = new JTextPane();
		resurrectionPane.setEditable(false);
		setFont(resurrectionPane);
		resurrectionPane.setBorder(nameField.getBorder());
		titlesArea = new JTextArea();
		titlesArea.setEditable(false);
		setFont(titlesArea);
		itemList = new JList();
		blessingList = new JList();
		curseList = new JList();

		gbl = new GridBagLayout();
		Container pane = getContentPane();
		pane.setLayout(new GridLayout(1, 2));
		left = new JPanel();
		left.setLayout(gbl);
		JPanel right = new JPanel();
		right.setLayout(gbl);

		GBC labelGBC = new GBC().setSpan(2, 1).setWeight(0.5f, 0).setAnchor(GBC.WEST).setInsets(0, 0, 3, 12);
		GBC fieldGBC = new GBC().setSpan(2, 1).setWeight(0.5f, 0).setBothFill().setInsets(0, 0, 5, 11);
		GBC halfLabelGBC = ((GBC)labelGBC.clone()).setWeight(0.25f, 0).setSpan(1, 1).setInsets(0, 0, 3, 6);
		GBC halfFieldGBC = ((GBC)fieldGBC.clone()).setWeight(0.25f, 0).setSpan(1, 1).setInsets(0, 0, 5, 5);

		// Handle the God/Gender panel - Gender is missing most of the time, so this is tricky
		godGenderPanel = new JPanel();
		godGenderPanel.setLayout(gbl);

		genderPanel = new JPanel();
		genderPanel.setLayout(gbl);
		ButtonGroup genderGroup = new ButtonGroup();
		maleButton = createGenderButton("Male", genderGroup);
		femaleButton = createGenderButton("Female", genderGroup);
		new GBC(0, 0)
			.setSpan(2, 1)
			.setWeight(1, 1)
			.setAnchor(GBC.WEST)
			.setInsets(0, 0, 3, 6)
			.addComp(genderPanel, new JLabel("Gender"), gbl);
		new GBC(0, 1)
			.setWeight(0.5, 1)
			.setAnchor(GBC.WEST)
			.setInsets(0, 0, 5, 5)
			.addComp(genderPanel, maleButton, gbl);
		new GBC(1, 1)
			.setWeight(0.5, 1)
			.setAnchor(GBC.WEST)
			.setInsets(0, 0, 5, 11)
			.addComp(genderPanel, femaleButton, gbl);

		godLabel = new JLabel("God");
		halfLabelGBC.addComp(godGenderPanel, godLabel, gbl, 0, 0);
		halfFieldGBC.addComp(godGenderPanel, godField, gbl, 0, 1);
		genderPanelGBC = new GBC(1, 0)
				.setSpan(1, 2)
				.setBothFill(); // will use this to add the genderPanel when necessary

		// Add in the rest of the fields
		int row = 0;
		labelGBC.addComp(left, new JLabel("Name"), gbl, 0, row++);
		fieldGBC.addComp(left, nameField, gbl, 0, row++);
		new GBC()
			.setSpan(2, 2)
			.setBothFill()
			.setInsets(0, 0, 0, 6)
			.addComp(left, godGenderPanel, gbl, 0, row);
		row += 2;
		halfLabelGBC.addComp(left, new JLabel("Ability"), gbl, 0, row);
		((GBC)halfLabelGBC.clone()).setAnchor(GBC.EAST).setInsets(0, 0, 3, 12).addComp(left, new JLabel("Score"), gbl, 1, row++);
		fieldGBC.addComp(left, abilityPane/*new JScrollPane(abilityPane)*/, gbl, 0, row++);
		labelGBC.addComp(left, new JLabel("Stamina"), gbl, 0, row++);
		fieldGBC.addComp(left, staminaField, gbl, 0, row++);
		labelGBC.addComp(left, new JLabel("Resurrection Arrangements"), gbl, 0, row++);
		fieldGBC.addComp(left, new JScrollPane(resurrectionPane), gbl, 0, row++);
		labelGBC.addComp(left, new JLabel("Titles and Honours"), gbl, 0, row++);
		((GBC)fieldGBC.clone()).setWeight(0.5f, 1.0f).addComp(left, new JScrollPane(titlesArea), gbl, 0, row++);
		labelGBC.addComp(left, new JLabel("Curses"), gbl, 0, row++);
		((GBC)fieldGBC.clone()).setWeight(0.5f, 1.0f).addComp(left, new JScrollPane(curseList), gbl, 0, row++);

		row = 0;
		labelGBC.addComp(right, new JLabel("Profession"), gbl, 0, row++);
		fieldGBC.addComp(right, professionField, gbl, 0, row++);
		halfLabelGBC.addComp(right, new JLabel("Rank"), gbl, 0, row);
		halfLabelGBC.addComp(right, new JLabel("Defence"), gbl, 1, row++);
		halfFieldGBC.addComp(right, rankField, gbl, 0, row);
		halfFieldGBC.addComp(right, defenceField, gbl, 1, row++);
		labelGBC.addComp(right, new JLabel("Possessions (maximum of 12)"), gbl, 0, row++);
		((GBC)fieldGBC.clone()).setWeight(0.5f, 1.0f).addComp(right, new JScrollPane(itemList), gbl, 0, row++);
		labelGBC.addComp(right, new JLabel("Money"), gbl, 0, row++);
		fieldGBC.addComp(right, moneyField, gbl, 0, row++);
		labelGBC.addComp(right, new JLabel("Blessings"), gbl, 0, row++);
		((GBC)fieldGBC.clone()).setWeight(0.5f, 0.5f).addComp(right, new JScrollPane(blessingList), gbl, 0, row++);
		
		pane.add(left);
		pane.add(right);

		getRootPane().setBorder(new EmptyBorder(12, 12, 6, 0));
		getRootPane().setOpaque(true);
		getRootPane().setBackground(godField.getBackground());
		
		SectionDocument.addFontUser(this);
	}

	public void init(Adventurer adv) {
		this.adv = adv;
		reset();
	}

	public boolean isEditable() {
		return nameField.isEditable();
	}
	
	public void setEditable(boolean b) {
		if (nameField.isEditable() != b) {
			nameField.setEditable(b);
			if (!b && adv != null)
				// Update the adventurer's name to whatever's there now
				adv.setName(nameField.getText());

			if (b)
				genderPanelGBC.addComp(godGenderPanel, genderPanel, gbl);
			else
				godGenderPanel.remove(genderPanel);
			godGenderPanel.validate();
		}
	}

	private javax.swing.text.StyledDocument abilityDocument;
	public void reset() {
		nameField.setText(adv.getName());
		professionField.setText(adv.getProfessionName());
		if (adv.isMale())
			maleButton.setSelected(true);
		else
			femaleButton.setSelected(true);

		rankField.setDocument(adv.getRank().getDocument());
		defenceField.setDocument(adv.getDefence().getDocument());
		staminaField.setDocument(adv.getStamina().getDocument());

		abilityDocument = adv.getAbilityDocument();
		abilityPane.setDocument(abilityDocument);

		godField.setDocument(adv.getGodDocument());
		godField.setVisible(!adv.isGodless());
		godLabel.setVisible(!adv.isGodless());
		moneyField.setDocument(adv.getMoneyDocument());
		resurrectionPane.setDocument(adv.getResurrectionDocument());

		titlesArea.setDocument(adv.getTitleDocument());

		adv.getItems().configureList(itemList);
		adv.getCurses().configureList(curseList);
		adv.getBlessings().configureList(blessingList);
	}

	public void itemStateChanged(ItemEvent evt) {
		if (evt.getStateChange() == ItemEvent.SELECTED)
			adv.setMale(maleButton.isSelected());
	}

	private static final String dropCommand = "D", transferCommand = "T";
	private void handlePopup(MouseEvent evt) {
		if (adv.getMoney() == 0) return;
		
		JPopupMenu popup = new JPopupMenu("Money Options");
		ItemList cache = adv.getItems().getItemCache();
		if (cache != null && !adv.getItems().getItemCacheNode().isFrozen()) {
			JMenuItem transferItem = new JMenuItem("Transfer...");
			transferItem.setActionCommand(transferCommand);
			transferItem.addActionListener(this);
			popup.add(transferItem);
		}
		JMenuItem dropItem = new JMenuItem("Drop");
		dropItem.setActionCommand(dropCommand);
		dropItem.addActionListener(this);
		popup.add(dropItem);
		
		popup.show(moneyField, evt.getX(), evt.getY());
	}
	
	private void doMoneyTransfer() {
		if (adv.getMoney() > 0) {
			ItemList cache = adv.getItems().getItemCache();
			if (cache != null) {
				// Choose money amount
				MoneyChooser chooser = new MoneyChooser(FLApp.getSingle(), "Transfer Money", "How much do you want to leave here:", 0, adv.getMoney());
				chooser.setVisible(true);

				if (chooser.getResult() == 0) return;
				FLApp.getSingle().actionTaken();
				adv.adjustMoney(-chooser.getResult());
				cache.addItem(Item.createMoneyItem(chooser.getResult()));
			}
		}
	}
	
	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount() == 2)
			doMoneyTransfer();
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

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals(transferCommand)) {
			doMoneyTransfer();
		}
		else if (e.getActionCommand().equals(dropCommand)) {
			if (adv.getMoney() > 50) {
				int result = JOptionPane.showConfirmDialog(this, new String[] {"Are you sure want to drop " + adv.getMoney() + " Shards?", "They will be gone for good!"}, "Discard Money?", JOptionPane.OK_CANCEL_OPTION);
				if (result != JOptionPane.OK_OPTION)
					return;
			}
			adv.setMoney(0);
			FLApp.getSingle().actionTaken();
		}
	}

	public void fontChanged(Font f, int smallerFontSize) {
		reset();
		setFont(nameField);
		setFont(professionField);
		setFont(godField);
		setFont(rankField);
		setFont(defenceField);
		setFont(staminaField);
		setFont(abilityPane);
		setFont(moneyField);
		setFont(resurrectionPane);
		setFont(titlesArea);
	}
}
