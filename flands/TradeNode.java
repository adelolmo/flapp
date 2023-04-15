package flands;


import java.awt.event.ActionEvent;
import java.util.Properties;

import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.xml.sax.Attributes;

/**
 * Specialised node to be part of a MarketNode - defines one row of the table. Contains
 * an item, ship, crew or cargo to be bought or sold.
 * 
 * @author Jonathan Mann
 */
public class TradeNode extends Node implements Executable {
	public static final String ElementName = "trade";
	private int shipType = -1;
	//private int initialCrew = Ship.NO_CREW;
	private int cargoType = Ship.NO_CARGO;
	private int buy, sell;
	private ParagraphNode paraNode = null;
	private Attributes atts = null;
	private BuyNode buyNode = null;
	private SellNode sellNode = null;
	private Item item;
	private TradeEventNode tradeEvent = null;

	public TradeNode(Node parent) {
		super(ElementName, parent);
		setEnabled(false);
	}

	public TradeNode(Node parent, Item item) {
		this(parent);
		this.item = item;
	}

	private boolean isBuyingMarket() {
		if (getParent() instanceof MarketNode)
			return ((MarketNode)getParent()).isBuyingMarket();
		return buy >= 0;
	}
	private boolean isSellingMarket() {
		if (getParent() instanceof MarketNode)
			return ((MarketNode)getParent()).isSellingMarket();
		return sell >= 0;
	}
	private String getCurrency() {
		if (getParent() instanceof MarketNode)
			return ((MarketNode)getParent()).getCurrency();
		return null;
	}

	public void init(Attributes atts) {
		this.atts = atts; // we'll need them later
		buy = getIntValue(atts, "buy", -1);
		sell = getIntValue(atts, "sell", -1);
		if (buy < 0 && sell < 0)
			System.out.println("Error: trade:buy and :sell both appear to be missing!");

		// Grab only the necessary attributes to figure out the text cells
		String val = atts.getValue("ship");
		if (val != null)
			shipType = Ship.getType(val);

		val = atts.getValue("cargo");
		if (val != null)
			cargoType = Ship.getCargo(val);

		if (item != null)
			item.init(atts);

		super.init(atts);

		findExecutableGrouper().addExecutable(this);
	}

	/**
	 * Create a BuyNode as a child of this row.
	 * The 'buy' attribute value will be passed in as the price.
	 */
	private void createBuyNode(Attributes atts) {
		buyNode = new BuyNode(this, buy);
		addChild(buyNode);
		if (item != null)
			buyNode.setItem(item);
		buyNode.setCurrency(getCurrency());
		buyNode.init(atts);
		buyNode.handleContent("");
		buyNode.handleEndTag();
	}

	/**
	 * Create a SellNode as a child of this row.
	 * The 'sell' attribute value will be passed in as the price.
	 */
	private void createSellNode(Attributes atts) {
		sellNode = new SellNode(this, sell);
		addChild(sellNode);
		if (item != null)
			sellNode.setItem(item);
		sellNode.setCurrency(getCurrency());
		sellNode.init(atts);
		sellNode.handleContent("");
		sellNode.handleEndTag();
	}

	private static String formatPrice(int money, String currency) {
		return (money == 0 ? "free!" : // see 3.318
			(money + " "
			 + (currency == null ? "Shard" : currency)
			 + (money == 1 ? "" : "s")));
	}
	
	/**
	 * Create a textual cell of this row.
	 * @param text the text in the cell;
	 * @param align the alignment of this cell.
	 */
	private void createParagraphNode(String text, int align) {
		ParagraphNode paraNode = new ParagraphNode(this, align);
		addChild(paraNode);
		paraNode.init(null);
		paraNode.handleContent(text);
		paraNode.handleEndTag();
	}

	/** Attribute style (right-aligned) to be used by both subclasses. */
	private static SimpleAttributeSet RightAlignStyle;
	static {
		RightAlignStyle = new SimpleAttributeSet();
		StyleConstants.setAlignment(RightAlignStyle, StyleConstants.ALIGN_RIGHT);
	}

	protected Node createChild(String name) {
		if (name.equals(TradeEventNode.BoughtElementName) ||
			name.equals(TradeEventNode.SoldElementName)) {
			tradeEvent = new TradeEventNode(name, this);
			addChild(tradeEvent);
			if (item != null)
				tradeEvent.setItem(item);
			System.out.println("TradeNode: created TradeEvent child");
			return tradeEvent;
		}
		else
			return super.createChild(name);
	}
	
	public void itemTraded(boolean bought, Item traded) {
		if (tradeEvent == null) {
			Node parent = getParent();
			if (parent instanceof MarketNode)
				((MarketNode)parent).itemTraded(bought, traded);
		}
		else
			tradeEvent.itemTraded(bought, traded);
	}

	public void handleContent(String text) {
		if (paraNode == null && text.trim().length() == 0) return;
		
		if (paraNode == null) {
			// Create this first cell now
			paraNode = new ParagraphNode(this, StyleConstants.ALIGN_LEFT);
			addChild(paraNode);
			paraNode.init(null);
		}
		
		paraNode.handleContent(text);
	}
	
	public boolean handleEndTag() {
		if (paraNode == null) {
			// Create a default first cell
			if (shipType >= 0)
				// Ship type / Cost / Capacity
				createParagraphNode(Ship.getTypeName(shipType), StyleConstants.ALIGN_LEFT);
			else if (cargoType > Ship.NO_CARGO)
				createParagraphNode(Ship.getCargoName(cargoType), StyleConstants.ALIGN_LEFT);			
			else if (item != null) {
				// Item description / Buy / Sell
				paraNode = new ParagraphNode(this, StyleConstants.ALIGN_LEFT);
				addChild(paraNode);
				paraNode.init(null);
				item.addTo(getDocument(), paraNode.getElement(), null, true);
			}
		}
		
		if (paraNode != null) {
			paraNode.handleEndTag();
			paraNode = null;
		}
		
		// Add the rest of the cells to this row
		if (shipType >= 0) {
			// Ship type / Cost / Capacity
			if (buy >= 0)
				createBuyNode(atts);
			if (sell >= 0)
				createSellNode(atts);
			if (buy >= 0)
				createParagraphNode(Ship.getCapacityString(shipType), StyleConstants.ALIGN_RIGHT);
		}
		else if (cargoType > Ship.NO_CARGO) {
			if (buy >= 0)
				createBuyNode(atts);
			else
				createParagraphNode(" - ", StyleConstants.ALIGN_RIGHT);
			if (sell >= 0)
				createSellNode(atts);
			else
				createParagraphNode(" - ", StyleConstants.ALIGN_RIGHT);
		}
		else if (item != null) {
			if (buy >= 0)
				createBuyNode(atts);
			else if (isBuyingMarket())
				createParagraphNode(" - ", StyleConstants.ALIGN_RIGHT);

			if (sell >= 0)
				createSellNode(atts);
			else if (isSellingMarket())
				createParagraphNode(" - ", StyleConstants.ALIGN_RIGHT);
		}
		this.atts = null;
		return true;
	}

	public boolean execute(ExecutableGrouper grouper) {
		setEnabled(true);
		return true;
	}

	public void resetExecute() {
		setEnabled(false);
	}
	
	public static class BuyNode extends ActionNode implements Executable, ChangeListener, Flag.Listener {
		public static final String ElementName = "buy";
		private boolean inTradeNode;
		private int shards;
		private int shipType = -1;
		private String initialCrew;
		private int cargoType = Ship.NO_CARGO;
		private int toCrew = Ship.NO_CREW;
		private String name;
		private Item item = null;
		private int quantity = -1;
		private boolean forced = false;
		private String currency;
		private String buyTags;
		private String flag;

		public BuyNode(Node parent) { this(parent, -1); }

		public BuyNode(Node parent, int shards) {
			super(ElementName, parent);
			this.shards = shards;
			inTradeNode = (parent instanceof TradeNode);//shards > 0;
			setEnabled(false);
		}

		public void setItem(Item i) {
			this.item = i;
			getItems().addChangeListener(this);
		}

		public void setCurrency(String currency) {
			this.currency = currency;
		}
		
		public void init(Attributes atts) {
			if (shards < 0)
				shards = getIntValue(atts, "shards", -1);
			if (shards > 0) {
				if (currency == null)
					getAdventurer().addMoneyListener(this);
				else
					getItems().addCurrencyListener(currency, this);
			}

			String val = atts.getValue("ship");
			if (val != null)
				shipType = Ship.getType(val);
			initialCrew = atts.getValue("initialCrew");
			name = atts.getValue("name"); // could have multiple applications

			val = atts.getValue("cargo");
			if (val != null) {
				cargoType = Ship.getCargo(val);
				if (cargoType > Ship.NO_CARGO) {
					System.out.println("Ready to take cargo " + val);
					getShips().addCargoListener(this);
				}
			}

			val = atts.getValue("crew");
			if (val != null) {
				toCrew = Ship.getCrew(val);
				if (toCrew > Ship.NO_CREW)
					getShips().addCrewListener(this);
			}

			if (item == null)
				item = Item.createItem(atts);
			if (item != null)
				getItems().addChangeListener(this);

			quantity = getIntValue(atts, "quantity", -1); // -1 means infinite amount
			buyTags = atts.getValue("buytags");
			forced = getBooleanValue(atts, "force", false);
			flag = atts.getValue("flag");
			if (flag != null)
				getFlags().addListener(flag, this);

			super.init(atts);

			findExecutableGrouper().addExecutable(this);
		}

		protected Node createChild(String name) {
			Node n = null;
			if (name.equals(EffectNode.ElementName))
				n = new EffectNode(this, item);

			if (n == null)
				n = super.createChild(name);
			else
				addChild(n);

			return n;
		}

		protected MutableAttributeSet getElementStyle() {
			return RightAlignStyle;
		}

		private boolean addedContent = false;
		public void handleContent(String text) {
			MutableAttributeSet atts = createStandardAttributes();
			Element[] leaves = null;
			if (text == null) {
				if (!addedContent && !getParent().hideChildContent()) {
					if (inTradeNode || item == null)
						text = formatPrice(shards, currency);
					else
						leaves = item.addTo(getDocument(), getElement(), atts, getDocument().isNewSentence());
				}
				else
					return;
			}
			else if (text.trim().length() == 0)
				return;
			addedContent = true;
			if (leaves == null)
				leaves = getDocument().addLeavesTo(getElement(), new StyledText[] { new StyledText(text, atts) });
			addHighlightElements(leaves);
			addEnableElements(leaves);
		}

		public boolean handleEndTag() {
			if (!addedContent)
				handleContent(null);
			return super.handleEndTag();
		}

		private boolean callContinue = false;
		public boolean execute(ExecutableGrouper eg) {
			stateChanged(null); // to set up initial enabled state
			if (forced && isEnabled()) {
				callContinue = true;
				return false;
			}
			return true;
		}

		public void resetExecute() { setEnabled(false); }

		public void stateChanged(ChangeEvent evt) {
			setEnabled(canBuyNow());
		}

		private int getMoney() {
			if (currency == null)
				return getAdventurer().getMoney();
			else {
				int moneyIndex = getItems().getMoneyItem(currency);
				return (moneyIndex < 0 ? 0 : getItems().getItem(moneyIndex).getMoney());
			}
		}
		
		protected boolean canBuyNow() {
			if (!(getParent() instanceof GroupNode) && !getParent().enabled)
				return false;
			if (getMoney() < shards)
				return false;
			if (cargoType > Ship.NO_CARGO && getShips().findShipsWithSpace().length == 0)
				return false;
			if (toCrew > Ship.NO_CREW && getShips().findShipsWithCrew(toCrew - 1).length == 0)
				return false;
			if (item != null && getItems().getFreeSpace() == 0)
				return false;
			if (quantity == 0)
				return false;
			if (flag != null && !getFlags().getState(flag))
				return false;

			return true;
		}

		public void actionPerformed(ActionEvent evt) {
			boolean success = true;
			if (shipType >= 0) {
				int crew;
				if (isVariableDefined(initialCrew))
					crew = getVariableValue(initialCrew);
				else
					crew = Ship.getCrew(initialCrew);
				Ship s = new Ship(shipType, name, crew);
				s.setDocked(getDockLocation());
				getShips().addShip(s);
			}
			else if (cargoType > Ship.NO_CARGO) {
				int[] ships = getShips().findShipsWithSpace();
				if (ships.length > 1) {
					JOptionPane.showMessageDialog(FLApp.getSingle(), new String[] {"You have multiple ships with free space", "docked here. Select one."}, "Multiple Ships", JOptionPane.INFORMATION_MESSAGE);
					success = false;
				}
				else
					getShips().addCargoTo(ships[0], cargoType);
			}
			else if (toCrew > Ship.NO_CREW) {
				int[] ships = getShips().findShipsWithCrew(toCrew-1);
				if (ships.length > 1) {
					String oldCrewStr = Ship.getCrewName(toCrew-1).toLowerCase();
					JOptionPane.showMessageDialog(FLApp.getSingle(), new String[] {"You have multiple ships with " + oldCrewStr + " crew", "docked here. Select one."}, "Multiple Ships", JOptionPane.INFORMATION_MESSAGE);
					success = false;
				}
				else
					getShips().setCrew(ships[0], toCrew);
			}
			else if (item != null) {
				Item specificItem = item.makeSpecific();
				if (specificItem == null)
					success = false;
				else {
					if ((quantity > 1 || quantity < 0) && specificItem == item)
						// Make sure item is a clone, rather than the original
						specificItem = specificItem.copy();
					if (buyTags != null)
						specificItem.addTags(buyTags);
					// Clear the sale price; otherwise this item could be sold at _any_
					// market, regardless of whether that market has a slot for that item.
					specificItem.clearSalePrice();
					getItems().addItem(specificItem);
					
					if (inTradeNode)
						((TradeNode)getParent()).itemTraded(true, specificItem);
				}
			}

			if (success) {
				if (quantity > 0) {
					quantity--;
					if (quantity == 0)
						setEnabled(false);
				}
				if (shards > 0) {
					if (currency == null)
						getAdventurer().adjustMoney(-shards);
					else
						getItems().adjustMoney(-shards, currency);
				}
				if (flag != null)
					getFlags().setState(flag, false);
				if (callContinue) {
					callContinue = false;
					findExecutableGrouper().continueExecution(this, false);
				}
				else
					callsContinue = false;
			}
		}

		public void dispose() {
			getAdventurer().removeMoneyListener(this);
			if (cargoType > Ship.NO_CARGO)
				getShips().removeCargoListener(this);
			if (toCrew > Ship.NO_CREW)
				getShips().removeCrewListener(this);
			if (item != null)
				getItems().removeChangeListener(this);
			if (flag != null)
				getFlags().removeListener(flag, this);
		}
		
		protected void loadProperties(Attributes atts) {
			super.loadProperties(atts);
			callContinue = getBooleanValue(atts, "continue", false);
		}
		
		protected void saveProperties(Properties props) {
			super.saveProperties(props);
			saveProperty(props, "continue", callContinue);
		}

		public void flagChanged(String name, boolean state) {
			setEnabled(canBuyNow());
		}
		
		protected String getTipText() {
			String suffix = null;
			if (shards != 0) {
				suffix = " for " + shards;
				if (currency == null)
					suffix += " Shards";
				else
					suffix += " " + currency;
			}
			if (shipType >= 0) {
				int crew;
				if (isVariableDefined(initialCrew))
					crew = getVariableValue(initialCrew);
				else
					crew = Ship.getCrew(initialCrew);
				return "Buy a " + Ship.getTypeName(shipType) + " with a crew of quality " + Ship.getCrewName(crew) + suffix;
			}
			else if (cargoType > Ship.NO_CARGO)
				return "Buy a cargo unit of " + Ship.getCargoName(cargoType) + suffix;
			else if (toCrew > Ship.NO_CREW)
				return "Upgrade a ship's crew from " + Ship.getCrewName(toCrew-1) + " to " + Ship.getCrewName(toCrew) + suffix;
			else if (item != null) {
				StyledTextList itemText = item.createItemText(null, false);
				return "Buy a " + itemText.toXML() + suffix;
			}
			return null;
		}
	}

	public static class SellNode extends ActionNode implements Executable, ChangeListener, Flag.Listener {
		public static final String ElementName = "sell";
		private boolean inTradeNode;
		private int shards;
		private int shipType = -1;
		private int cargoType = Ship.NO_CARGO;
		private Item item = null;
		private String currency;
		private int quantity = -1;
		private String price;

		public SellNode(Node parent) { this(parent, -1); }

		public SellNode(Node parent, int shards) {
			super(ElementName, parent);
			this.shards = shards;
			inTradeNode = (parent instanceof TradeNode); //shards > 0;
			setEnabled(false);
		}

		public void setItem(Item i) { this.item = i; }

		public void setCurrency(String currency) { this.currency = currency; }
		
		public int getPrice() { return shards; }

		public void init(Attributes atts) {
			if (shards < 0)
				shards = getIntValue(atts, "shards", -1);

			String val = atts.getValue("ship");
			if (val != null) {
				shipType = Ship.getType(val);
				if (shipType >= 0)
					getShips().addShipListener(this);
			}

			val = atts.getValue("cargo");
			if (val != null) {
				cargoType = Ship.getCargo(val);
				if (cargoType > Ship.NO_CARGO)
					getShips().addCargoListener(this);
			}

			quantity = getIntValue(atts, "quantity", -1);
			price = atts.getValue("price");
			if (price != null)
				getFlags().addListener(price, this);

			if (item == null)
				item = Item.createItem(atts);
			if (item != null)
				getItems().addChangeListener(this);

			super.init(atts);

			findExecutableGrouper().addExecutable(this);
		}

		protected MutableAttributeSet getElementStyle() {
			return RightAlignStyle;
		}

		private boolean addedContent = false;
		public void handleContent(String text) {
			MutableAttributeSet atts = createStandardAttributes();
			Element[] leaves = null;
			if (text.length() == 0 && !addedContent) {
				if (inTradeNode || item == null)
					// Show the price of the item
					text = formatPrice(shards, currency);
				else
					// Show the item description
					leaves = item.addTo(getDocument(), getElement(), atts, getDocument().isNewSentence());
			}

			addedContent = true;
			if (leaves == null)
				leaves = getDocument().addLeavesTo(getElement(), new StyledText[] { new StyledText(text, createStandardAttributes()) });
			addHighlightElements(leaves);
			addEnableElements(leaves);
		}

		public boolean execute(ExecutableGrouper eg) {
			stateChanged(null); // to set up initial enabled state
			return true;
		}

		public void stateChanged(ChangeEvent evt) {
			setEnabled(canSellNow());
		}

		public void resetExecute() { setEnabled(false); }

		protected boolean canSellNow() {
			if (!getParent().enabled)
				return false;
			if (quantity == 0) { System.out.println("quantity=0"); return false; }
			if (price != null && getFlags().getState(price)) { System.out.println(price + " is set"); return false; }
			if (shipType >= 0) {
				if (getShips().findShipsOfType(shipType).length > 0)
					return true;
			}
			else if (cargoType != Ship.NO_CARGO) {
				if (getShips().findShipsWithCargo(cargoType).length > 0)
					return true;
			}
			else if (item != null) {
				int[] matches = getItems().findMatches(item);
				if (item.getNextItem() != null)
					System.out.println("Matches found for chained items: " + matches.length);
				if (matches.length > 0) {
					// Attach this node to all matched items
					for (int i = 0; i < matches.length; i++)
						getItems().getItem(matches[i]).setSellNode(this);
				}
				return (matches.length > 0);
			}
			return false;
		}
		
		public void actionPerformed(ActionEvent evt) {
			callsContinue = false;
			if (shipType >= 0) {
				int[] ships = getShips().findShipsOfType(shipType);
				if (ships.length > 1) {
					JOptionPane.showMessageDialog(FLApp.getSingle(), new String[] {"You have multiple ships of this type. Select one and try again!"}, "Multiple Ships", JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				else
					getShips().removeShip(ships[0]);
			}
			else if (cargoType != Ship.NO_CARGO) {
				int[] ships = getShips().findShipsWithCargo(cargoType);
				int chosenType = cargoType;
				if (ships.length > 1) {
					JOptionPane.showMessageDialog(FLApp.getSingle(), new String[] {"You have multiple ships with this cargo", "docked here. Select one."}, "Multiple Ships", JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				else if (cargoType == Ship.MATCH_SINGLE_CARGO) {
					Ship s = getShips().getShip(ships[0]);
					int[] cargoTypes = s.hasCargoTypes();
					chosenType = cargoTypes[0];
					if (cargoTypes.length > 1) {
						DefaultStyledDocument[] docs = new DefaultStyledDocument[cargoTypes.length];
						for (int i = 0; i < cargoTypes.length; i++) {
							DefaultStyledDocument doc = new DefaultStyledDocument();
							try {
								doc.insertString(0, Ship.getCargoName(cargoTypes[i]), null);
							}
							catch (BadLocationException ble) {}
							docs[i] = doc;
						}
						
						DocumentChooser chooser = new DocumentChooser(FLApp.getSingle(), "Choose Cargo Type", docs, false);
						chooser.setVisible(true);
						
						if (chooser.getSelectedIndices() == null) {
							setEnabled(true);
							return;
						}
						chosenType = cargoTypes[chooser.getSelectedIndices()[0]];
					}
				}
				
				getShips().removeCargoFrom(ships[0], chosenType);
			}
			else if (item != null) {
				int[] matches = getItems().findMatches(item);
				if (matches.length > 1) {
					if (!getItems().areItemsSame(matches)) {
						JOptionPane.showMessageDialog(FLApp.getSingle(), new String[] {"You have multiple items of this type!", "Please select which one you want to sell."}, "Multiple Items", JOptionPane.INFORMATION_MESSAGE);
						return;
					}
				}

				Item i = getItems().getItem(matches[0]);
				getItems().removeItem(matches[0]);
				soldItem(i);
			}
			
			// Fallthrough - sale occurrred, so add the money
			if (shards > 0) {
				if (currency == null)
					getAdventurer().adjustMoney(shards);
				else
					getItems().adjustMoney(shards, currency);
			}
			
			if (quantity > 0) quantity--;
			if (price != null)
				getFlags().setState(price, true);
		}

		void soldItem(Item i) {
			if (inTradeNode)
				((TradeNode)getParent()).itemTraded(false, i);
		}
		
		public void flagChanged(String name, boolean state) {
			setEnabled(canSellNow());
		}

		public void dispose() {
			if (shipType >= 0)
				getShips().removeShipListener(this);
			if (cargoType > Ship.NO_CARGO)
				getShips().removeCargoListener(this);
			if (price != null)
				getFlags().removeListener(price, this);
		}
		
		protected String getTipText() {
			String suffix = " for " + shards;
			if (shards >= 0) {
				suffix = " for " + shards;
				if (currency == null)
					suffix += " Shards";
				else
					suffix += " " + currency;
			}
			if (shipType >= 0)
				return "Sell a " + Ship.getTypeName(shipType) + suffix;
			else if (cargoType == Ship.MATCH_SINGLE_CARGO)
				return "Sell a cargo unit " + suffix;
			else if (cargoType != Ship.NO_CARGO)
				return "Sell a cargo unit of " + Ship.getCargoName(cargoType) + suffix;
			else if (item != null) {
				StyledTextList itemText = item.createItemText(null, false);
				return "Sell a " + itemText.toXML() + suffix;
			}
			return null;
		}
	}
}
