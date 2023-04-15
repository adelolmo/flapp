package flands;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JToolTip;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.Document;
import javax.swing.text.Element;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import flands.resources.Resources;

/**
 * The main window. This should coordinate other classes and watch for user
 * events, but as far as possible delegate to other classes.
 * 
 * @author Jonathan Mann
 */
public class FLApp extends JFrame implements MouseListener,
		MouseMotionListener, ActionListener {
	private static FLApp single = null;

	/**
	 * If true, removes the end game confirm dialogs, as well as saving and retrieving
	 * codewords from a subdirectory (see Codewords.refresh() and update()).
	 */
	static boolean debugging = false;

	public static FLApp getSingle() {
		if (single == null)
			single = new FLApp();
		return single;
	}

	private CardLayout cardLayout;
	private static final String StartWindow = "start";
	private static final String TextWindow = "text";

	private StartPanel startPanel;
	private JTextPane textPane;
	private boolean newHardcore = false;

	private SectionDocument document, lastDocument = null;

	private Node rootNode, lastRootNode = null;

	/** The list of starting characcters to choose from. */
	private Adventurer[] starting = null;

	/** Record of the adventurer (and everything associated with this character). */
	private Adventurer adventurer = null;

	/** GUI showing the player's abilities and possessions. */
	private AdventurerFrame adventureSheet = null;

	/** The rules in place for this session. */
	private ActiveRuleset rules = new ActiveRuleset();
	
	/** Local/global map display */
	private ImageWindow mapWindow = null;

	/** Ship's manifest */
	private ShipFrame shipWindow = null;

	/** Display of official codewords. */
	private CodewordWindow codewordWindow = null;
	
	private Properties userProps;

	private WindowProperties windowProps;

	private String currentSection;

	private List<GameListener> gameListeners = new LinkedList<GameListener>();

	private FLApp() {
		super(Resources.GuiText("AppTitle"));

		textPane = new JTextPane() {
			public void setDocument(Document doc) {
				// Remove any components in place
				while (getComponentCount() > 0)
					remove(0);
				super.setDocument(doc);
			}
		};
		textPane.setFont(SectionDocument.getPreferredFont());
		textPane.setForeground(Color.black);
		textPane.setEditable(false);
		textPane.setEditorKit(new BookEditorKit()); // which will make our own
													// ViewFactory
		textPane.addMouseListener(this);
		textPane.addMouseMotionListener(this);
		SectionDocument.addComponentFontUser(textPane);
		
		startPanel = new StartPanel(this);
		cardLayout = new CardLayout();
		getContentPane().setLayout(cardLayout);
		getContentPane().add(startPanel, StartWindow);
		getContentPane().add(new JScrollPane(textPane), TextWindow);

		setIconImage(loadImage("icon.jpg"));
		
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				FLApp.this.quitGame();
			}
		});
	}

	private void showTextWindow() {
		cardLayout.show(getContentPane(), TextWindow);
	}
	private void showStartWindow() {
		cardLayout.show(getContentPane(), StartWindow);
	}
	
	void init(String section) {
		userProps = new Properties();
		boolean firstRun = false;
		try {
			FileInputStream propsFile = new FileInputStream("user.ini");
			userProps.load(propsFile);
		}
		catch (FileNotFoundException fnfe) {
			firstRun = true;
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
		
		createMenuBar();

		Adventurer[] advs = Adventurer.loadStarting(Address.getCurrentBook());
		starting = advs;
		if (section == null) {
			section = "New";
		} else {
			/*
			 * Testing
			 * When testing a particular section, I'll usually mess around with
			 * the character's setup here
			 */
			
			//adventurer = advs[Adventurer.PROF_MAGE];
			adventurer = advs[(int)(Math.random() * Adventurer.PROF_COUNT)];
			//adventurer.adjustAbility(Adventurer.ABILITY_COMBAT, 3);
			adventurer.adjustMoney(2000);
			//adventurer.addTitle("Paladin of Ravayne");
			adventurer.setGod("Molhern");
			//adventurer.setGod("Sig");
			//adventurer.setGodEffect("Sig", AbilityEffect.createAbilityBonus(Adventurer.ABILITY_THIEVERY, 1));
			//adventurer.setGodless(true);
			adventurer.addTitle("Chosen One of Nagil");
			//CacheNode.setMoneyCache("MerchantBank", 500);
			adventurer.getItems().addItem(Item.createMoneyItem(500, "Mithral"));
			adventurer.getItems().addItem(new Item("faery mead"));
			Blessing luckBlessing = new Blessing(Blessing.LUCK_TYPE);
			luckBlessing.setPermanent(true);
			adventurer.getBlessings().addBlessing(luckBlessing);
			//adventurer.getBlessings().addBlessing(Blessing.STORM);
			adventurer.getBlessings().addBlessing(Blessing.WRATH);
			Curse c = new Curse(Curse.CURSE_TYPE, "Blight of Nagil");
			c.addEffect(AbilityEffect.createAbilityBonus(
					Adventurer.ABILITY_COMBAT, -1));
			c.addEffect(AbilityEffect.createAbilityBonus(
					Adventurer.ABILITY_CHARISMA, -1));
			//adventurer.getCurses().addCurse(c);
			c = new Curse(Curse.CURSE_TYPE, "Curse of the Shadar");
			c.addEffect(AbilityEffect.createAbilityBonus(Adventurer.ABILITY_MAGIC, -1));
			c.addEffect(AbilityEffect.createAbilityBonus(Adventurer.ABILITY_COMBAT, -1));
			//adventurer.getCurses().addCurse(c);
			Item spear = new Item.Weapon("spear of retribution", 8);
			spear.addTag("Molherned");
			spear.addTag(Item.TagKeep);
			adventurer.getItems().addItem(spear);
			adventurer.getItems().addItem(new Item("paper sword"));
			adventurer.getItems().addItem(new Item.Armour("splint mail", 4));
			//adventurer.getItems().addItem(new Item("ink sac"));
			adventurer.getItems().addItem(new Item("rope"));
			Item lantern = new Item("lantern");
			lantern.addTag("light");
			//adventurer.getItems().addItem(lantern);
			Item candle = new Item("candle");
			candle.addTags("light,useonce");
			adventurer.getItems().addItem(candle);
			adventurer.getItems().addItem(new Item("verdigris key"));
			//adventurer.adjustAbility(Adventurer.ABILITY_CHARISMA, 10);
			//adventurer.adjustAbility(Adventurer.ABILITY_RANK, 12);
			//for (int i = 0; i < 4; i++)
			//	adventurer.getItems().addItem(new Item("smoulder fish"));
			//adventurer.getItems().addItem(new Item("four-leaf clover"));
			//adventurer.getItems().addItem(new Item("rabbit's foot charm"));
			//adventurer.getItems().addItem(new Item("pirate captain's head"));
			//adventurer.getItems().addItem(new Item("witch's hand"));
			//adventurer.getBlessings().addBlessing(Blessing.DISEASE);
			//adventurer.getBlessings().addBlessing(Blessing.STORM);
			//adventurer.getBlessings().addBlessing(Blessing.getAbilityBlessing(Adventurer.ABILITY_SANCTITY));
			//Resurrection resurrect = new Resurrection("Temple of Tyrnai", "5", "500");
			Resurrection resurrect = new Resurrection("Temple of Nagil", "2", "339");
			resurrect.setGod("Nagil");
			adventurer.addResurrection(resurrect);
			adventurer.getStamina().damage(2);
			// Start at an arbitrary rank
			//adventurer.adjustAbility(Adventurer.ABILITY_RANK, 11-adventurer.getAbilityValue(Adventurer.ABILITY_RANK, Adventurer.MODIFIER_AFFECTED));
			Ship oneShip = new Ship(Ship.BRIG_TYPE, "A ship", Ship.AVG_CREW);
			//oneShip.addCargo(Ship.METAL_CARGO);
			oneShip.addCargo(Ship.FUR_CARGO);
			oneShip.addCargo(Ship.FUR_CARGO);
			//oneShip.addCargo(Ship.GRAIN_CARGO);
			oneShip.setDocked("Elsweyr");
			//adventurer.getShips().addShip(oneShip);
			adventurer.getShips().setAtSea();
			adventurer.getExtraChoices().setMenu(extraChoiceMenu);
			//adventurer.getCodewords().addCodeword("Evade");
			adventurer.getCodewords().addCodeword("Barnacle");
			
			saveItem.setEnabled(true);
			quicksaveItem.setEnabled(true);
		}
		
		if (Address.getCurrentBookKey() == null)
			showStartWindow();
		else {
			updateActiveRules(Address.getCurrentBook().getRequiredRules());
			showTextWindow();
			gotoSection(section);
		}

		windowProps = new WindowProperties("MainWindow", userProps);
		if (firstRun || adventurer == null || windowProps == null)
			pack();
		else {
			windowProps.applyTo(this);
			validate();
		}

		if (adventurer == null)
			centerWindow(false);
		else {
			showProfession(-1);
			// showShipWindow();
			showMapWindow();
		}
		setVisible(true);

		requestFocus();
	}

	public boolean hasPickedAdventurer() {
		return (adventurer != null && adventureSheet != null && !adventureSheet.isEditable());
	}
	public void refreshAdventureSheet() {
		if (adventureSheet != null)
			adventureSheet.reset();
	}

	public void setNameEditable(boolean b, boolean setHardcore) {
		if (adventureSheet != null) {
			adventureSheet.setEditable(b);
			saveItem.setEnabled(hasPickedAdventurer());
			quicksaveItem.setEnabled(hasPickedAdventurer());
			if (!b) {
				adventurer.getExtraChoices().setMenu(extraChoiceMenu);
				if (setHardcore)
					adventurer.setHardcore(newHardcore);
			}
		}
	}

	public boolean isRuleActive(String rule) {
		return rules.hasRule(rule);
	}
	public void updateActiveRules(String bookRules) {
		rules.clearTempRules();
		rules.addTempRules(bookRules);
		// TODO: Any other rules may affect more than Defence
		if (getAdventurer() != null)
			getAdventurer().updatedActiveRules();
	}
	
	private void centerWindow(boolean pack) {
		boolean wasVisible = false;
		if (isVisible()) {
			wasVisible = true;
			setVisible(false);
		}
		
		if (pack)
			pack();
		
		Dimension screenSize = getActualScreenSize();
		setLocation((screenSize.width - getWidth()) / 2,
				(screenSize.height - getHeight()) / 2);
		
		if (wasVisible)
			setVisible(true);
	}
	public void showProfession(int type) {
		boolean firstAdventurer = false;
		if (starting != null && type >= 0) {
			if (adventurer == null)
				firstAdventurer = true;
			adventurer = starting[type];
		}

		if (adventurer != null) {
			if (adventureSheet == null) {
				adventureSheet = new AdventurerFrame();
				adventureSheet.init(adventurer);
				adventureSheet.pack();
				
				WindowProperties props = WindowProperties.create("AdventureSheet", userProps);
				if (props == null) {
					Dimension screenSize = getActualScreenSize();
					if (adventureSheet.getWidth() > screenSize.width / 2)
						adventureSheet.setSize(screenSize.width / 2, adventureSheet
								.getHeight());
	
					// Resize the main window as well
					int appWidth = screenSize.width - adventureSheet.getWidth();
					setBounds(0, 0, appWidth, 500);
					validate();

					adventureSheet.setLocation(appWidth, 0);
				}
				else {
					if (firstAdventurer)
						restoreMainWindowBounds();
					props.applyTo(adventureSheet);
				}
			}
			else
				adventureSheet.init(adventurer);
		}

		if (adventureSheet != null)
			adventureSheet.setVisible(true);

		if (adventurer != null && shipWindow != null)
			shipWindow.show(adventurer.getShips());
	}

	private boolean restoreMainWindowBounds() {
		WindowProperties props = WindowProperties.create("MainWindow", userProps);
		if (props != null) {
			props.applyTo(this);
			return true;
		}
		return false;
	}
	
	public void showMapWindow() {
		if (mapWindow == null) {
			Books.BookDetails book = Address.getCurrentBook();
			String mapFile = book.getMapFilename();
			String mapTitle = book.getMapTitle();
			if (mapFile != null) {
				mapWindow = createImageWindow(book.getInputStream(mapFile), mapTitle, false);
				WindowProperties windowProps = WindowProperties.create("LocalMap", userProps);
				if (windowProps == null) {
					Dimension screenSize = getActualScreenSize();
					if (adventureSheet == null)
						mapWindow.setBounds(getWidth(), screenSize.height / 2,
								screenSize.width - getWidth(), screenSize.height / 2);
					else
						mapWindow.setBounds(getWidth(), adventureSheet.getHeight(),
								adventureSheet.getWidth(), screenSize.height
										- adventureSheet.getHeight());
				}
				else
					windowProps.applyTo(mapWindow);
			}
		}
		if (mapWindow != null)
			mapWindow.setVisible(true);
	}

	public ShipFrame showShipWindow() {
		if (shipWindow == null) {
			if (adventurer == null)
				return null;
			shipWindow = new ShipFrame(adventurer.getShips());
			WindowProperties windowProps = WindowProperties.create("ShipManifest", userProps);
			if (windowProps == null) {
				int heightLeft = getActualScreenSize().height - getHeight();
				if (heightLeft >= 100)
					shipWindow.setBounds(0, getHeight(), getWidth(), Math.max(100,
							getActualScreenSize().height - getHeight()));
				else
					shipWindow.setLocation(0, getActualScreenSize().height - shipWindow.getHeight());
			}
			else
				windowProps.applyTo(shipWindow);
		}
		shipWindow.setVisible(true);
		return shipWindow;
	}

	public void showCodewordWindow() {
		if (adventurer == null) return;
		
		if (codewordWindow == null)
			codewordWindow = new CodewordWindow(this);
		else
			codewordWindow.refresh();
		codewordWindow.showCodewords();
		
		if (!codewordWindow.isVisible())
			codewordWindow.setVisible(true);
	}
	
	public void showNotesWindow() {
		if (adventurer == null) return;
		
		if (codewordWindow == null)
			codewordWindow = new CodewordWindow(this);
		else
			codewordWindow.refresh();
		codewordWindow.showNotes();
		
		if (!codewordWindow.isVisible())
			codewordWindow.setVisible(true);
	}
	
	public String getCurrentSection() {
		return currentSection;
	}

	private void saveUserProperties() {
		if (hasPickedAdventurer()) {
			WindowProperties wp = new WindowProperties("MainWindow", userProps);
			wp.getFrom(this);
			wp = new WindowProperties("AdventureSheet", userProps);
			wp.getFrom(adventureSheet);
			wp = new WindowProperties("ShipManifest", userProps);
			wp.getFrom(shipWindow);
			wp = new WindowProperties("LocalMap", userProps);
			wp.getFrom(mapWindow);
		}
		try {
			FileOutputStream propsFile = new FileOutputStream("user.ini");
			userProps.store(propsFile, Resources.GuiText().getString("UserIniHeader"));
		} catch (IOException ioe) {
			System.err.println(Resources.GuiTextFormat("UserIniWriteException", ioe));
		}
	}

	private ParserHandler handler = null;

	private ParserHandler getHandler() {
		if (handler == null)
			handler = new ParserHandler();
		return handler;
	}

	public Adventurer getAdventurer() {
		return adventurer;
	}

	public Node getRootNode() {
		return rootNode;
	}

	private JComponent toolTipContext = null;
	/**
	 * Get the context relative to which tooltips should be displayed.
	 * FLApp acts as a singleton here, collecting the current context and coordinates
	 * for the whole application; this allows Roller (displaying the dice roll in a
	 * tooltip) to find the right location for its tooltips.
	 * @see #getMouseAtX
	 * @see #getMouseAtY
	 */
	public JComponent getToolTipContext() { return toolTipContext; }
	void setToolTipContext(JComponent context) {
		toolTipContext = context;
	}

	public boolean gotoSection(String section) {
		if (gotoFile(Address.getCurrentBook().getInputStream(section + ".xml"),
				     Address.getCurrentBookKey())) {
			currentSection = section;
			((SectionNode)rootNode).setSection(currentSection);
			if (adventurer != null)
				adventurer.getExtraChoices().checkMenu();
			return true;
		}
		else
			return false;
	}
	
	public boolean gotoAddress(Address address) {
		if (address.getBook().equals(Address.getCurrentBookKey()) &&
			address.section.equals(currentSection))
			// Same section!
			// If we allow this, a <return> might break - so ignore.
			return true;
		
		if (gotoFile(address.getStream(), address.getBook())) {
			if (Address.setCurrentBookKey(address.getBook())) {
				updateLocalMap();
				updateActiveRules(Address.getCurrentBook().getRequiredRules());
			}
			currentSection = address.section;
			((SectionNode)rootNode).setSection(currentSection);
			if (adventurer != null)
				adventurer.getExtraChoices().checkMenu();
			else if (starting == null) {
				Adventurer[] advs = Adventurer.loadStarting(Address.getCurrentBook());
				starting = advs;
				
				// Resize and center the window again
				//centerWindow(true);
			}
			return true;
		}
		else
			return false;
	}
	
	private void updateLocalMap() {
		Books.BookDetails book = Address.getCurrentBook();
		if (mapWindow != null) {
			String mapFile = book.getMapFilename();
			if (mapFile != null)
				updateImage(mapWindow, loadImage(book.getInputStream(mapFile)), book.getMapTitle());
		}
		String iconFilename = book.getIconFilename();
		if (iconFilename != null)
			setIconImage(loadImage(iconFilename));		
	}
	
	private boolean gotoFile(InputStream in, String book)  {
		try {
			ParserHandler handler = getHandler();
			handler.setBook(book);
			XMLReader reader = XMLReaderFactory.createXMLReader();
			reader.setContentHandler(handler);
			reader.parse(new InputSource(new InputStreamReader(in)));

			// Parsing is now finished!
			closeSectionWindows();
			unhighlight();
			if (lastRootNode != null)
				lastRootNode.dispose();
			lastRootNode = rootNode;
			lastDocument = document;
			document = getHandler().getDocument();
			rootNode = getHandler().getRootNode();
			textPane.setDocument(document);
			textPane.setCursor(Cursor.getDefaultCursor());
			fireGameEvent(GameEvent.NEW_SECTION);
			((SectionNode)rootNode).startExecution();
			return true;
		}
		catch (Exception e) {
			e.printStackTrace();
			handler = null;
		}
		return false;
	}

	public SectionDocument getCurrentDocument() { return document; }
	
	public void returnFromSection() {
		if (lastDocument != null) {
			closeSectionWindows();
			unhighlight();
			SectionDocument tempDoc = document;
			Node tempRootNode = rootNode;
			document = lastDocument;
			rootNode = lastRootNode;
			lastDocument = tempDoc;
			lastRootNode = tempRootNode;
			textPane.setDocument(document);
			textPane.setCursor(Cursor.getDefaultCursor());
			currentSection = rootNode.getSectionName();
			fireGameEvent(GameEvent.NEW_SECTION);
			if (Address.setCurrentBookKey(((SectionNode)rootNode).getBook())) {
				// Switched books - probably an item with goto effect
				updateLocalMap();
				updateActiveRules(Address.getCurrentBook().getRequiredRules());
			}
			if (adventurer != null)
				adventurer.getExtraChoices().checkMenu();
		}
	}

	/**
	 * Add a listener for GameEvents.
	 * In practice, more events are sent than are listened for.
	 * @param l
	 */
	public void addGameListener(GameListener l) {
		if (!gameListeners.contains(l))
			gameListeners.add(l);
	}

	public void removeGameListener(GameListener l) {
		gameListeners.remove(l);
	}

	/**
	 * Send a game event.
	 * @param id one of the codes defined in {@link GameEvent}.
	 */
	public void fireGameEvent(int id) {
		if (gameListeners.size() > 0) {
			GameEvent evt = new GameEvent(id);
			for (Iterator<GameListener> i = gameListeners.iterator(); i
					.hasNext();)
				i.next().eventOccurred(evt);
		}
		
		if (id == GameEvent.NEW_SECTION)
			reportedBadLocation = false;	
	}

	private int mouseAtX;

	private int mouseAtY;

	public int getMouseAtX() { return mouseAtX; }
	public int getMouseAtY() { return mouseAtY; }
	void setMouseAtX(int x) { mouseAtX = x; }
	void setMouseAtY(int y) { mouseAtY = y; }

	private boolean popupEvent;
	public void mousePressed(MouseEvent evt) {
		popupEvent = false;
		if (evt.isPopupTrigger())
			popupEvent = true;
	}
	public void mouseReleased(MouseEvent evt) {
		if (evt.isPopupTrigger())
			popupEvent = true;
	}
	public void mouseEntered(MouseEvent evt) {}
	public void mouseDragged(MouseEvent evt) {}

	private ActionNode actionNode = null;

	private Element actionElement = null;

	private void unhighlight() {
		if (actionNode != null) {
			actionNode.setHighlighted(false);
			if (actionNode.isEnabled())
				textPane.setCursor(Cursor.getDefaultCursor());
			actionNode = null;
			actionElement = null;
		}
	}

	private boolean reportedBadLocation = false;
	public void mouseMoved(MouseEvent evt) {
		Point pt = evt.getPoint();
		mouseAtX = pt.x;
		mouseAtY = pt.y;
		toolTipContext = textPane;

		int pos = textPane.viewToModel(pt);
		Element currentElement = null;
		if (pos >= 0) {
			try {
				java.awt.Rectangle r = textPane.modelToView(pos);
				//System.out.println("Mouse at " + pt + "; rectangle of reported position: " + r);
				if (r.x - pt.x < 21 && pt.x - r.x < r.width + 21 &&
					pt.y >= r.y	&& pt.y < r.y + r.height)
					// The hotzone gets ridiculously big, especially at the end
					// of the document
					currentElement = document.getCharacterElement(pos);
			} catch (javax.swing.text.BadLocationException e) {
				// Often happens around ComponentViews
				if (!reportedBadLocation) {
					System.out.println(Resources.GuiTextFormat("BadLocationException", e));
					reportedBadLocation = true;
				}
			}
		}
		ActionNode currentAction = ActionNode.getActionNode(currentElement);
		if (currentAction == actionNode)
			return;

		if (actionNode != null)
			unhighlight();

		if (currentAction != null) {
			currentAction.setHighlighted(true);
			/*
			 * try {
			 *   int start = currentElement.getStartOffset(),
			 *       end = currentElement.getEndOffset();
			 *   System.out.println("Highlight new * element, offsets " +
			 *                      start + "-" + end + ", text=" +
			 *                      document.getText(start, end-start));
			 * }
			 * catch (javax.swing.text.BadLocationException e) {}
			 */
			actionNode = currentAction;
			actionElement = currentElement;
			if (actionNode.isEnabled())
				textPane.setCursor(Cursor
						.getPredefinedCursor(Cursor.HAND_CURSOR));
		}
	}

	public void mouseExited(MouseEvent evt) {
		mouseMoved(evt);
	}

	private Popup toolTipPopup = null;
	public void mouseClicked(MouseEvent evt) {
		if (evt.isPopupTrigger())
			popupEvent = true;
		if (toolTipPopup != null) {
			toolTipPopup.hide();
			toolTipPopup = null;
		}
		
		Point pt = evt.getPoint();
		int pos = textPane.viewToModel(pt);
		if (pos < 0)
			return;

		if (actionNode != null && actionNode.isEnabled()) {
			if (!popupEvent) {
				actionNode.fireActionEvent(actionElement);
				if (actionNode == null || !actionNode.isEnabled())
					textPane.setCursor(Cursor.getDefaultCursor());
			}
			else {
				// Display help for this action
				String tipText = actionNode.getToolTip();
				if (tipText != null) {
					JToolTip tip = new JToolTip();
					tip.setTipText(tipText);
					tip.setComponent(getToolTipContext());
					Font tipFont = SectionDocument.getPreferredFont();
					tipFont = new Font(tipFont.getName(), tipFont.getStyle(), tipFont.getSize()-2);
					tip.setFont(tipFont);
					int x = evt.getX();
					int y = evt.getY();
					try {
						Point contextLoc = getToolTipContext().getLocationOnScreen();
						x += contextLoc.x;
						y += contextLoc.y;
					}
					catch (java.awt.IllegalComponentStateException e) {}
					/*
					if (y > 20)
						y -= (tipSize.height + 10);
					else
						y += 10;
					*/
					toolTipPopup = PopupFactory.getSharedInstance().getPopup(getToolTipContext(), tip, x, y);
					toolTipPopup.show();
					
					//new Thread(new ToolTipHider(toolTipPopup)).start();
				}
			}
		}
	}
	
	public Dimension getActualScreenSize() {
		Dimension screenSize = getToolkit().getScreenSize();
		Insets screenInsets = getToolkit().getScreenInsets(
				getGraphicsConfiguration());
		return new Dimension(screenSize.width
				- (screenInsets.left + screenInsets.right), screenSize.height
				- (screenInsets.top + screenInsets.bottom));
	}

	private List<ImageWindow> sectionWindows;

	private ImageWindow createImageWindow(String imageFilename, String title,
			boolean sectionOnly) {
		Image i = loadImage(imageFilename);
		return createImageWindow(i, title, sectionOnly);
	}

	public ImageWindow createImageWindow(InputStream in, String title, boolean sectionOnly) {
		Image i = loadImage(in);
		return createImageWindow(i, title, sectionOnly);
	}
	
	private ImageWindow createImageWindow(Image i, String title, boolean sectionOnly) {
		if (i == null) return null;
		ImageWindow iw = new ImageWindow(this, i, title);
		
		if (sectionOnly) {
			if (sectionWindows == null)
				sectionWindows = new LinkedList<ImageWindow>();
			sectionWindows.add(iw);
		}
		return iw;
	}
	private void closeSectionWindows() {
		if (sectionWindows != null) {
			while (!sectionWindows.isEmpty()) {
				ImageWindow iw = sectionWindows.remove(0);
				iw.setVisible(false);
				iw.dispose();
			}
		}
	}
	
	/* Currently unused
	public ImageWindow showImage(String imageFilename, String title,
			boolean sectionOnly) {
		ImageWindow iw = createImageWindow(imageFilename, title, sectionOnly);
		iw.setVisible(true);
		return iw;
	}
	*/

	public void updateImage(ImageWindow iw, Image i, String title) {
		iw.setImage(i, title);
	}
	
	// Notification that an action has taken place - something affecting the game state.
	// This flag results in the user being asked for save confirmation before exiting.
	void actionTaken() { actionTaken = true; }
	
	boolean actionTaken = true;
	
	void quitGame() {
		if (actionTaken || (hasPickedAdventurer() && adventurer.isHardcore())) {
			if (!endGame(Resources.GuiText().getString("SaveBeforeQuitQuery")))
				return;
		}

		saveUserProperties();
		setVisible(false);
		dispose();
		System.exit(0);
	}
	
	/**
	 * End the game, allowing the player to save, not save, or cancel.
	 * @param messageSecondLine the second line of a dialog to be displayed to the user
	 * if quitting this game would lose progress.
	 * @return <code>true</code> if we should proceed to exit the game; <code>false</code>
	 * otherwise.
	 */
	private boolean endGame(String messageSecondLine) {
		if (debugging)
			return true;
		if (!hasPickedAdventurer())
			return true;
		boolean isDead = (adventurer.isDead() || getCurrentSection().equals(Address.getCurrentBook().getDeathSection()));
		if (!isDead || adventurer.hasResurrection()) {
			String firstLine = Resources.GuiText().getString(adventurer.isHardcore() ?
					"EndGameHardcore" : "EndGameNormal");
			int result =
				JOptionPane.showConfirmDialog(this, new String[] {firstLine, messageSecondLine}, Resources.GuiText("SaveQueryTitle"), JOptionPane.YES_NO_CANCEL_OPTION);
			if (result == JOptionPane.YES_OPTION) {
				String file = chooseSavedGame(false);
				if (file == null)
					return false; // Player cancelled or chose no file
				doLoadSave(false, file);
			}
			else if (result == JOptionPane.NO_OPTION)
				;
			else
				return false;
		}
		return true;
	}
	
	private static final String showRulesCommand = "rules",
			showQuickRulesCommand = "quick",
			showShipListCommand = "ships",
			showAdventureSheetCommand = "adventurer",
			localMapCommand = "local", globalMapCommand = "global",
			loadCommand = "load", saveCommand = "save",
			quickloadCommand = "qload", quicksaveCommand = "qsave", newCommand = "new",
			fontCommand = "font", codewordCommand = "codewords", notesCommand = "notes",
			exitCommand = "exit", aboutCommand = "about", docViewerCommand = "viewer", nodeDumpCommand = "dump";
	private static final String
			NormalSaveItemKey = "NormalSaveItem",
			HardcoreSaveItemKey = "HardcoreSaveItem",
			NormalQuicksaveItemKey = "NormalQuicksaveItem",
			HardcoreQuicksaveItemKey = "HardcoreQuicksaveItem";
	
	private JMenuItem saveItem = null, quicksaveItem = null;
	private JMenu extraChoiceMenu = null;

	private JMenuItem createMenuItem(String key, String command) {
		JMenuItem item = new JMenuItem(Resources.GuiText(key));
		item.setActionCommand(command);
		item.addActionListener(this);
		return item;
	}

	private void createMenuBar() {
		JMenuBar bar = new JMenuBar();
		JMenu fileMenu = new JMenu(Resources.GuiText("FileMenu"));
		fileMenu.add(createMenuItem("NewGameItem", newCommand));
		fileMenu.add(createMenuItem("LoadGameItem", loadCommand));
		saveItem = createMenuItem(NormalSaveItemKey, saveCommand);
		saveItem.setEnabled(hasPickedAdventurer());
		fileMenu.add(saveItem);
		fileMenu.add(createMenuItem("QuickloadGameItem", quickloadCommand));
		quicksaveItem = createMenuItem(NormalQuicksaveItemKey, quicksaveCommand);
		quicksaveItem.setEnabled(hasPickedAdventurer());
		fileMenu.add(quicksaveItem);
		fileMenu.addSeparator();
		fileMenu.add(createMenuItem("ExitItem", exitCommand));
		bar.add(fileMenu);
		
		JMenu windowMenu = new JMenu(Resources.GuiText("WindowsMenu"));
		windowMenu.add(createMenuItem("AdventureSheetItem",
				showAdventureSheetCommand));
		windowMenu.add(createMenuItem("CodewordsItem", codewordCommand));
		windowMenu.add(createMenuItem("NotesItem", notesCommand));
		windowMenu.add(createMenuItem("ShipManifestItem", showShipListCommand));
		windowMenu.add(ShipList.getTransferMenuItem());
		windowMenu.add(createMenuItem("LocalMapItem", localMapCommand));
		windowMenu.add(createMenuItem("GlobalMapItem", globalMapCommand));
		windowMenu.addSeparator();
		windowMenu.add(createMenuItem("FontChoiceItem", fontCommand));
		if (debugging) {
			windowMenu.add(createMenuItem("DocViewerItem", docViewerCommand));
			windowMenu.add(createMenuItem("NodeDumpItem", nodeDumpCommand));
		}
		bar.add(windowMenu);
		
		extraChoiceMenu = new JMenu(Resources.GuiText("ExtraChoiceMenu"));
		extraChoiceMenu.setVisible(false); // made visible when an adventurer has been chosen
		bar.add(extraChoiceMenu);
		
		JMenu helpMenu = new JMenu(Resources.GuiText("HelpMenu"));
		helpMenu.add(createMenuItem("QuickRulesItem", showQuickRulesCommand));
		helpMenu.add(createMenuItem("FullRulesItem", showRulesCommand));
		helpMenu.add(createMenuItem("AboutItem", aboutCommand));
		bar.add(helpMenu);
		
		getRootPane().setJMenuBar(bar);
	}

	private Window globalMapWindow = null;

	static final String saveFilename = "savegame.dat";
	public void actionPerformed(ActionEvent evt) {
		String command = evt.getActionCommand();
		if (command.equals(showRulesCommand)) {
			SectionBrowser rulesPanel = new SectionBrowser("Rules.xml");
			rulesPanel.createFrame(Resources.GuiText("FullRulesTitle")).setVisible(true);
		}
		else if (command.equals(showQuickRulesCommand)) {
			SectionBrowser rulesPanel = new SectionBrowser("QuickRules.xml");
			rulesPanel.createFrame(Resources.GuiText("QuickRulesTitle")).setVisible(true);
		}
		else if (command.equals(aboutCommand)) {
			new AboutDialog(this).setVisible(true);
		}
		else if (command.equals(localMapCommand)) {
			showMapWindow();
		}
		else if (command.equals(globalMapCommand)) {
			if (globalMapWindow == null)
				globalMapWindow = createImageWindow("global.jpg",
						Resources.GuiText("GlobalMapTitle"), false);
			globalMapWindow.setVisible(true);
		}
		else if (command.equals(showShipListCommand)) {
			showShipWindow();
		}
		else if (command.equals(showAdventureSheetCommand)) {
			if (adventurer != null)
				showProfession(-1);
		}
		else if (command.equals(codewordCommand)) {
			showCodewordWindow();
		}
		else if (command.equals(notesCommand)) {
			showNotesWindow();
		}
		else if (command.equals(newCommand)) {
			if (!endGame(Resources.GuiText("SaveBeforeNewGameQuery")))
				return;
			
			closeCurrentGame();
		}
		else if (command.equals(loadCommand)) {
			String file = chooseSavedGame(true);
			if (file != null)
				doLoadSave(true, file);
		}
		else if (command.equals(saveCommand)) {
			String file = chooseSavedGame(false);
			if (file != null) {
				doLoadSave(false, file);
				if (adventurer.isHardcore())
					closeCurrentGame();
			}
		}
		else if (command.equals(quickloadCommand)) {
			doLoadSave(true, null);
		}
		else if (command.equals(quicksaveCommand)) {
			doLoadSave(false, null);
			if (adventurer.isHardcore())
				closeCurrentGame();
		}
		else if (command.equals(exitCommand)) {
			quitGame();
		}
		else if (command.equals(fontCommand)) {
			FontChooser chooser = new FontChooser(this,
					SectionDocument.getPreferredFont(),
					SectionDocument.getSmallerCapsFontSize());
			chooser.setVisible(true);
			
			if (chooser.getChosenFont() != null)
				SectionDocument.setPreferredFont(chooser.getChosenFont(),
						chooser.getSmallerCapsFontSize());
		}
		else if (command.equals(docViewerCommand)) {
			new SectionDocumentViewer(this);
		}
		else if (command.equals(nodeDumpCommand)) {
			try {
				((SectionNode)this.rootNode).outputTo(System.out, "", XMLOutput.OUTPUT_PROPS_DYNAMIC);
			}
			catch (IOException ioe) {}
		}
	}

	void closeCurrentGame() {
		saveUserProperties();
		boolean recenter = (adventurer != null);
		if (adventureSheet != null) {
			adventureSheet.setVisible(false);
			adventureSheet.dispose();
			adventureSheet = null;
		}
		if (shipWindow != null) {
			shipWindow.setVisible(false);
			shipWindow.dispose();
			shipWindow = null;
		}
		if (mapWindow != null) {
			mapWindow.setVisible(false);
			mapWindow.dispose();
			mapWindow = null;
		}
		CacheNode.clearCaches();
		
		showStartWindow();
		if (recenter)
			gotoSection("New");
		adventurer = null;
		starting = null;
		Address.setCurrentBookKey(null);
		if (recenter)
			centerWindow(true);		
	}
	
	void doBeginBook(String book, boolean hardcore) {
		if (gotoAddress(new Address(book, "New"))) {
			newHardcore = hardcore;
			saveItem.setText(Resources.GuiText(hardcore ? HardcoreSaveItemKey : NormalSaveItemKey));
			quicksaveItem.setText(Resources.GuiText(hardcore ?  HardcoreQuicksaveItemKey : NormalQuicksaveItemKey));
			showTextWindow();
		}
	}
	
	String chooseSavedGame(boolean load) {
		JFileChooser chooser;
		try {
			chooser = new JFileChooser();
		}
		catch (Exception e) {
			chooser = new JFileChooser(new RestrictedFileSystemView());
		}
		chooser.setFileFilter(new FileFilter() {
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase().endsWith(".dat");
			}

			public String getDescription() {
				return Resources.GuiText("SaveGameFileDescription");
			}
		});
		new SavedGamePreview(chooser);
		String lastDir = userProps.getProperty("SaveDir");
		if (lastDir == null)
			lastDir = System.getProperty("user.dir");
		chooser.setCurrentDirectory(lastDir == null ? null : new File(lastDir));
		chooser.setDialogTitle(Resources.GuiText(load ? "LoadGameTitle" : "SaveGameTitle"));
		int result;
		if (load)
			result = chooser.showOpenDialog(this);
		else
			result = chooser.showSaveDialog(this);
		
		if (result == JFileChooser.APPROVE_OPTION) {
			File file = chooser.getSelectedFile();
			userProps.setProperty("SaveDir", file.getParent());
			String path = file.getPath();
			if (!path.endsWith(".dat"))
				path += ".dat";
			System.out.println("Chosen path " + path);
			return path;
		}
		else
			return null;
	}
	
	void doLoadSave(boolean load, String filename) {
		/*
		if (debugging) {
			JOptionPane.showMessageDialog(this, "You can't load or save\nin debugging mode!", "Load/Save Unavailable", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		*/
		
		Adventurer oldAdv = adventurer;
		if (filename == null)
			filename = saveFilename;
		File loadFile = null;
		if (load) {
			loadFile = new File(filename);
			if (!loadFile.exists())
				return;
			System.out.println("File last modified: " + loadFile.lastModified());
			
			if (!endGame(Resources.GuiText("SaveBeforeLoadGameQuery")))
				return;
			
			adventurer = new Adventurer();
		}
		else if (!hasPickedAdventurer())
			return; // can't save before we've started
		
		if (!load && codewordWindow != null)
			codewordWindow.applyNotes();
		
		LoadableHandler handler = new LoadableHandler(filename);
		handler.add(adventurer);
		handler.add(adventurer.getCodewords());
		handler.add(adventurer.getShips());
		handler.add(adventurer.getBlessings());
		handler.add(adventurer.getExtraChoices());
		handler.add(XMLPool.createPool(adventurer));
		LoadableSection previous = new LoadableSection("previous", (SectionNode)lastRootNode);
		if (lastRootNode != null || load)
			handler.add(previous);
		LoadableSection current = new LoadableSection("current", (SectionNode)rootNode);
		handler.add(current);
		
		if (load) {
			if (handler.load()) {
				if (oldAdv == null)
					restoreMainWindowBounds();
				
				if (adventureSheet == null) {
					adventureSheet = new AdventurerFrame();
					adventureSheet.init(adventurer);
					adventureSheet.pack();
					WindowProperties wp = WindowProperties.create("AdventureSheet", userProps);
					if (wp == null) {
						Dimension screenSize = getActualScreenSize();
						if (adventureSheet.getWidth() > screenSize.width / 2)
							adventureSheet.setSize(screenSize.width / 2, adventureSheet
									.getHeight());
	
						// Resize the main window as well
						int appWidth = screenSize.width - adventureSheet.getWidth();
						setBounds(0, 0, appWidth, 500);
						validate();
						
						adventureSheet.setLocation(appWidth, 0);
					}
					else
						wp.applyTo(adventureSheet);

					adventureSheet.setVisible(true);
				}
				else
					adventureSheet.init(adventurer);
				setNameEditable(false, false);

				if (lastRootNode != null)
					lastRootNode.dispose();
				lastRootNode = rootNode; // which we'll dispose of in a second
				rootNode = current.getNode();
				document = current.getDocument();
				unhighlight();
				textPane.setDocument(document);
				if (lastRootNode != null)
					lastRootNode.dispose();
				lastRootNode = previous.getNode();
				lastDocument = previous.getDocument();
				showTextWindow();
				
				if (adventurer.getShips().getShipCount() > 0) {
					if (shipWindow != null)
						shipWindow.show(adventurer.getShips());
					else
						showShipWindow();
				}
				else if (shipWindow != null)
					shipWindow.show(adventurer.getShips());
				
				if (Address.setCurrentBookKey(current.getBook())) {
					updateLocalMap();
					updateActiveRules(Address.getCurrentBook().getRequiredRules());
				}
				if (mapWindow == null)
					showMapWindow();
				if (codewordWindow != null) {
					codewordWindow.refresh();
					codewordWindow.resetNotes();
				}
				
				currentSection = rootNode.getSectionName();
				//fireGameEvent(GameEvent.NEW_SECTION);
				
				adventurer.getExtraChoices().setMenu(extraChoiceMenu);
				adventurer.getExtraChoices().checkMenu();
				
				if (!adventurer.validateHardcore(loadFile.lastModified()))
					JOptionPane.showMessageDialog(this, new String[] { Resources.GuiText("HardcoreFailValidateMessage1"), Resources.GuiText("HardcoreFailValidateMessage2") }, Resources.GuiText("HardcoreFailTitle"), JOptionPane.ERROR_MESSAGE);
				
				if (adventurer.isHardcore()) {
					// Remove the file now
					if (!loadFile.delete()) {
						adventurer.setHardcore(false);
						JOptionPane.showMessageDialog(this, new String[] { Resources.GuiText("HardcoreFailDeleteMessage1"), Resources.GuiText("HardcoreFailDeleteMessage2") }, Resources.GuiText("HardcoreFailTitle"), JOptionPane.ERROR_MESSAGE);
					}
				}
				
				saveItem.setText(Resources.GuiText(adventurer.isHardcore() ? HardcoreSaveItemKey : NormalSaveItemKey));
				saveItem.setEnabled(true);
				quicksaveItem.setText(Resources.GuiText(adventurer.isHardcore() ? HardcoreQuicksaveItemKey : NormalQuicksaveItemKey));
				quicksaveItem.setEnabled(true);
				
				actionTaken = false;
			}
			else {
				JOptionPane.showMessageDialog(this, new String[] { Resources.GuiText("LoadGameFailMessage1"), Resources.GuiText("LoadGameFailMessage2") }, Resources.GuiText("LoadGameFailTitle"), JOptionPane.ERROR_MESSAGE);
				adventurer = oldAdv;
			}
		}
		else {
			if (handler.save()) {
				JOptionPane.showMessageDialog(this, Resources.GuiText("SaveGameSuccessMessage"), Resources.GuiText("SaveGameSuccessTitle"), JOptionPane.INFORMATION_MESSAGE);
				actionTaken = false;
			}
			else
				JOptionPane.showMessageDialog(this, Resources.GuiText("SaveGameFailMessage"), Resources.GuiText("SaveGameFailTitle"), JOptionPane.ERROR_MESSAGE);
		}		
	}
	
	/**
	 * Load an image from a file.
	 */
	private Image loadImage(String filename) {
		return getToolkit().createImage(filename);
	}
	
	/**
	 * Load an image from an InputStream.
	 */
	Image loadImage(InputStream in) {
		try {
			byte[] bs = new byte[Math.max(8192, in.available() + 8)];
			int len = 0;
			int count;
			while ((count = in.read(bs, len, bs.length - len)) >= 0) {
				//System.out.println("Read " + count + " bytes");
				len += count;
				if (len + in.available() >= bs.length) {
					byte[] temp = bs;
					bs = new byte[bs.length * 2];
					System.out.println("Extended byte array to " + bs.length);
					System.arraycopy(temp, 0, bs, 0, len);
				}
			}
			return getToolkit().createImage(bs, 0, len);
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return null;
	}

	/**
	 * Convert a simple wildcarded string (that uses ? or * marks)
	 * into a regex pattern for pattern-matching.
	 */
	public static Pattern createNamePattern(String name) {
		StringBuffer patternStr = new StringBuffer("^");
		for (int i = 0; i < name.length(); i++) {
			char ch = name.charAt(i);
			if (ch == '*')
				patternStr.append(".*");
			else if (ch == '?')
				patternStr.append('.');
			else
				patternStr.append(ch);
		}
		patternStr.append('$');
		return Pattern.compile(patternStr.toString());
	}
	
	private static ByteArrayOutputStream standardOut;
	private static PrintStream defaultOut;
	public static void blockOutput() {
		if (defaultOut == null)
			defaultOut = System.out;
		
		if (defaultOut == System.out) {
			standardOut = new ByteArrayOutputStream();
			System.setOut(new PrintStream(standardOut));
		}
	}
	
	public static void unblockOutput() {
		if (defaultOut != System.out)
			System.setOut(defaultOut);
	}
	
	public static void main(String args[]) {
		/*
		// Trace the character encoding being used
		// This may be important for translation
		try {
			InputStreamReader read = new InputStreamReader(new FileInputStream("books.ini"));
			System.out.println("Standard text encoding: " + read.getEncoding());
			read.close();
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		*/
		FLApp app = FLApp.getSingle();
		debugging = false;
		if (args.length > 0)
			debugging = true;
		else
			blockOutput();

		String section = null;
		if (args.length > 1) {
			section = args[0];
			Address.setCurrentBookKey(args[1]);
		}
		else if (args.length > 0)
			Address.setCurrentBookKey(args[0]);
		//else
		//	Address.setCurrentBookKey("4");

		app.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		app.init(section);
	}
}
