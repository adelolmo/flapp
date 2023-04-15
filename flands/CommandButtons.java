package flands;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;


/**
 * Encapsulation of the commonly used row of command buttons at the bottom
 * of every dialog.  There are constants for the standard configurations,
 * somewhat similarly to JOptionPane.  The layout is also done internally
 * (and turned out to be the most difficult part).
 * <p>
 * This class was designed with the <a href="http://java.sun.com/products/jlf/ed2/book/HIG.Dialogs2.html">Java L&F Design Guidelines</a> in mind.
 *
 * @author Jonathan Mann
 */
public class CommandButtons extends JComponent
implements ActionListener, LayoutManager {
    private List<AbstractButton> buttons = new ArrayList<AbstractButton>();
    private List<ActionListener> listeners = new LinkedList<ActionListener>();

    protected boolean includeTopMargin = true;
    protected boolean includeBorderMargin = true;

    /**
     * Code for the standard 'OK' button.  This button should close a
     * single-use dialog, applying any changes made.  It is usually the
     * default button.
     * @see #CANCEL_BUTTON
     */
    public static final int OK_BUTTON = 1 << 1;
    /**
     * Code for the standard 'Cancel' button.  This button should
     * close the dialog.  It is usually linked to the escape key.
     * @see #OK_BUTTON
     */
    public static final int CANCEL_BUTTON = 1 << 3;
    /**
     * Code for the standard 'Apply' button.  This button should
     * apply any changes made in the dialog without closing it.
     * It is usually the default button.
     * @see #CLOSE_BUTTON
     * @see #RESET_BUTTON
     */
    public static final int APPLY_BUTTON = 1 << 0;
    /**
     * Code for the standard 'Close' button.  This button should close
     * the dialog without saving any changes.  If changes have been made,
     * a warning dialog may be displayed.  It is usually linked to the
     * escape key.
     * @see #APPLY_BUTTON
     * @see #RESET_BUTTON
     */
    public static final int CLOSE_BUTTON = 1 << 2;
    /**
     * Code for the standard 'Reset' button.  This button should
     * reset any changes made since the last time the 'Apply' button
     * was pressed, or since the dialog was shown.
     * @see #APPLY_BUTTON
     * @see #CLOSE_BUTTON
     */
    public static final int RESET_BUTTON = 1 << 4;
    /**
     * Code for the standard 'Help' button.  Displays help for the
     * dialog.
     */
    public static final int HELP_BUTTON = 1 << 5;

    /* *******************************
     * Action Commands for the buttons
     ******************************* */
    public static final String applyCommand = "c.a";
    public static final String cancelCommand = "c.x";
    public static final String closeCommand = "c.c";
    public static final String helpCommand = "c.h";
    public static final String okCommand = "c.o";
    public static final String resetCommand = "c.r";

    /** The strings displayed for each of the standard buttons. */
    private static final String[] buttonText =
    {"Apply",
     "OK",
     "Close",
     "Cancel",
     "Reset",
     "Help"};

    /** The respective action commands for each of the standard buttons. */
    private static final String[] buttonCommands =
    {applyCommand, okCommand, closeCommand,
     cancelCommand, resetCommand, helpCommand};

    /**
     * This is the order in which buttons will be laid out, from left
     * to right.  Some of these buttons shouldn't coexist.
     * The other arrays all need to have the same order, as do the codes,
     * but this array can have any order.
     */
    private static final int[] DefaultOrder =
    {APPLY_BUTTON, OK_BUTTON, RESET_BUTTON,
     CANCEL_BUTTON, CLOSE_BUTTON, HELP_BUTTON};

    // -------------------------------------
    // Common combinations of dialog buttons
    // -------------------------------------

    /**
     * OK/Cancel work well in a single-use dialog box.
     * OK should be the default button.
     * If possible, provide the button with a command name that
     * describes the action, such as Print or Find, instead of OK.
     */
    public static final int OK_CANCEL = OK_BUTTON | CANCEL_BUTTON;
    public static final int OK_CANCEL_HELP = OK_CANCEL | HELP_BUTTON;

    /**
     * Apply/Close work well in multiple-use dialog boxes.
     * Apply carries out the changes specified without closing the window.
     * The apply button will normally be the default button.
     * Close should display a warning if changes have been made,
     * asking whether the changes should be applied before closing,
     * discarded, or cancelling the close operation.
     */
    public static final int APPLY_CLOSE = APPLY_BUTTON | CLOSE_BUTTON;
    public static final int APPLY_CLOSE_HELP = APPLY_CLOSE | HELP_BUTTON;
    /**
     * The reset button should restore the dialog to the settings held
     * the last time the Apply button was pushed, or when the window
     * was opened.  It should not close the dialog.
     * @see #APPLY_CLOSE
     */
    public static final int APPLY_RESET_CLOSE = APPLY_CLOSE | RESET_BUTTON;
    public static final int APPLY_RESET_CLOSE_HELP = APPLY_RESET_CLOSE | HELP_BUTTON;

    /** Create a new button row, initially empty. */
    public CommandButtons() {
		setLayout(this);
    }

    /**
     * Create a row with a set of buttons in place.
     * @param buttons a bitmask combination of the button codes,
     * either constructed or one of the
     * {@link #OK_CANCEL predefined combinations};
     * @param listener a listener to the buttons.
     */
    public static CommandButtons createRow(int buttons, ActionListener listener) {
		CommandButtons row = new CommandButtons();
		for (int b = 0; b < DefaultOrder.length; b++)
			if ((buttons & DefaultOrder[b]) != 0)
			row.addButton(DefaultOrder[b]);
		row.addActionListener(listener);
		return row;
    }

    private int codeToIndex(int code) {
		if (code == 0) return -1;
		int i = 0;
		while (code % 2 == 0) { code /= 2; i++; }
		return i;
    }

    /** Add one of the standard buttons to the end of the row. */
    public void addButton(int buttonCode) {
		insertButton(buttonCode, -1);
    }
    /**
     * Add one of the standard buttons to the row.
     * @param the index at which the button should be inserted.
     */
    public void insertButton(int buttonCode, int index) {
		int i = codeToIndex(buttonCode);
		if (i >= 0) {
			JButton button = new JButton(buttonText[i]);
			button.setActionCommand(buttonCommands[i]);
			button.addActionListener(this);
			doAddButton(button, index);
		}
    }

    /**
     * Add an externally-defined button to the end of the row.
     * The row will add itself as a listener to the button,
     * passing along action events to all listeners on this row.
     */
    public void addButton(AbstractButton button) {
		insertButton(button, -1);
    }

    /**
     * Add an externally-defined button to the row.
     * The row will add itself as a listener to the button,
     * passing along action events to all listeners on this row.
     * @param index the index at which the button will be added.
     */
    public void insertButton(AbstractButton button, int index) {
		button.addActionListener(this);
		doAddButton(button, index);
    }

    private void doAddButton(AbstractButton button, int index) {
		if (index < 0) {
			// Add as last button
			buttons.add(button);
			add(button);
		}
		else {
			buttons.add(index, button);
			add(button, index);
		}
		minButton = prefButton = null;
    }

    /** Get the nth button in the row. */
    public AbstractButton getButtonAt(int i) {
		return buttons.get(i);
    }

    /**
     * Get one of the standard buttons from the row.
     * @param buttonCode the code for this button.
     * @see #OK_CANCEL Button Codes
     */
    public JButton getButton(int buttonCode) {
		int i = codeToIndex(buttonCode);
		if (i < 0) return null;
		String command = buttonCommands[i];
		for (Iterator<AbstractButton> j = buttons.iterator(); j.hasNext(); ) {
			AbstractButton ab = j.next();
			if (command.equals(ab.getActionCommand()))
			return (JButton)ab;
		}
		return null;
    }

    /**
     * Returns either the OK or the Apply button, if one is in the row.
     * This looks at each button, returning the first one that has the
     * matching action command of one of these standard default buttons.
     * If neither of these is found, returns the first button in the
     * row.
     * @see javax.swing.JRootPane#setDefaultButton
     * @return <code>null</code> if no matching button can be found.
     */
    public JButton getDefaultButton() {
		// Look for OK / Apply buttons
		for (Iterator<AbstractButton> i = buttons.iterator(); i.hasNext(); ) {
			AbstractButton b = i.next();
			if (b.getActionCommand().equals(okCommand) || b.getActionCommand().equals(applyCommand))
				return (JButton)b;
		}

		if (buttons.size() > 0) {
			// See if the first button is a JButton
			AbstractButton ab = buttons.get(0);
			if (ab instanceof JButton)
				return (JButton)ab;
		}
		return null;
    }


    /** Remove one of the standard buttons from the row. */
    public void removeButton(int buttonCode) {
		JButton button = getButton(buttonCode);
		if (button != null)
			removeButton(button);
    }

    /** Remove a button from the row. */
    public void removeButton(AbstractButton button) {
		if (buttons.remove(button)) {
			remove(button);
			button.removeActionListener(this);
			minButton = prefButton = null;
		}
    }

    private static final String[] confirmOptions =
    {"Save", "Don't Save", "Cancel"};
    /**
     * Show a confirmation dialog with the options 'Save',
     * 'Don't Save' and 'Cancel'.
     * This is intended for use if the Close button is pressed and
     * there are unsaved changes made in the dialog; the message
     * should give an appropriate context so that the user can choose
     * to apply the changes, keep the data as is or cancel closing the
     * dialog.
     * @return <code>1</code> for 'Save', <code>0</code> for 'Don't Save',
     * <code>-1</code> for 'Cancel'.
     */
    public static int showConfirmDialog(String title, String[] message, Component context) {
		// TODO: message used to be wrapped by Standards
		JOptionPane pane =
			new JOptionPane(message, JOptionPane.QUESTION_MESSAGE);
		pane.setOptions(confirmOptions);
		pane.createDialog(context, title).setVisible(true);
		Object value = pane.getValue();
		if (value != null) {
			if (confirmOptions[0].equals(value)) return 1; // save
			else if (confirmOptions[1].equals(value)) return 0; // don't save
		}
		return -1; // cancel
    }

    /* **************
     * ActionListener
     ************** */
    /**
     * Add an action listener to the component.  The listeners
     * will be notified when any of the buttons are pushed.
     */
    public void addActionListener(ActionListener listener) {
		if (!listeners.contains(listener))
			listeners.add(listener);
    }

    /** Remove an action listener from the row. */
    public void removeActionListener(ActionListener listener) {
		while (listeners.remove(listener)) ;
    }

    /** Handle a button being pushed. */
    public void actionPerformed(ActionEvent e) {
		for (Iterator<ActionListener> i= listeners.iterator(); i.hasNext(); )
			i.next().actionPerformed(e);
    }

    /**
     * Layout needs to be recalculated, possibly due to a L&F change.
     * Clear the cached button sizes.
     */
    public void invalidate() {
		minButton = prefButton = null;
		super.invalidate();
    }

    /* *************
     * LayoutManager
     ************* */
    /**
     * Returns whether a margin above the buttons will be included
     * within the component.  The standard margin is <code>17</code> pixels.
     */
    public boolean getIncludeTopMargin() { return includeTopMargin; }
    /**
     * Sets whether a margin above the buttons will be included within
     * the component.
     */
    public void setIncludeTopMargin(boolean b) { includeTopMargin = b; }

    /**
     * Returns whether a margin around the 'outer' edge of the buttons will
     * be included within the component.  Since the command button row
     * should be at the bottom of a window and occupy the entire width,
     * this flag controls the margin on the 'outer' three sides.
     * The standard margin is <code>12</code> on the left and
     * <code>11</code> on the bottom and right.
     */
    public boolean getIncludeBorderMargin() { return includeBorderMargin; }
    /**
     * Sets whether a margin around the 'outer' edge of the buttons will
     * be included within this component.
     */
    public void setIncludeBorderMargin(boolean b) { includeBorderMargin = b; }

    protected int topMargin()    { return (includeTopMargin    ? 17 : 0); }
    protected int leftMargin()   { return (includeBorderMargin ? 12 : 0); }
    protected int rightMargin()  { return (includeBorderMargin ? 11 : 0); }
    protected int bottomMargin() { return (includeBorderMargin ? 11 : 0); }

    private Dimension minButton = null;
    private Dimension prefButton = null;

    /** Find the minimum and preferred button sizes, for use in layout. */
    private void calcButtonSizes() {
		if (buttons.size() == 0) {
			minButton = new Dimension(0, 0);
			prefButton = new Dimension(0, 0);
			return;
		}

		// Find the widest button
		minButton = new Dimension(0, 0);
		prefButton = new Dimension(0, 0);
		for (Iterator<AbstractButton> i = buttons.iterator(); i.hasNext(); ) {
			AbstractButton b = i.next();
			Dimension min = b.getMinimumSize();
			Dimension pref = b.getPreferredSize();
			if (minButton.height == 0) {
				// Get the height of only one button
				minButton.height = min.height;
				prefButton.height = pref.height;
			}
			if (minButton.width < min.width) minButton.width = min.width;
			if (prefButton.width < pref.width) prefButton.width = pref.width;
		}
    }

    /** Add a component to the layout.  Ignored. */
    public void addLayoutComponent(String name, Component comp) {}
    /** Remove a component from the layout.  Ignored. */
    public void removeLayoutComponent(Component comp) {}

    public Dimension minimumLayoutSize(Container parent) {
		if (parent != this) return null;

		if (minButton == null) 
			calcButtonSizes();
		return layoutSize(minButton);
   }

    public Dimension preferredLayoutSize(Container parent) {
		if (parent != this) return null;

		if (prefButton == null)
			calcButtonSizes();
		return layoutSize(prefButton);
    }

    /**
     * Calculate the size of a layout using a uniform button size.
     */
    private Dimension layoutSize(Dimension buttonSize) {
		if (buttonSize.width == 0) return new Dimension(0, 0);
		else return new Dimension(leftMargin() + buttons.size() * (buttonSize.width + 5) - 5 + rightMargin(),
					              topMargin() + buttonSize.height + bottomMargin());
    }

    /**
     * Layout the buttons in the space available.  Buttons will have 5 pixels
     * between each other, and will be anchored to the right.
     * Extra margins may be included around the buttons, depending
     * on the settings of the margin flags.
     * @see #getIncludeTopMargin
     * @see #getIncludeBorderMargin
     */
    public void layoutContainer(Container parent) {
		if (parent != this) return;

		int nb = buttons.size();
		Dimension size = getSize();
		Dimension minSize = minimumLayoutSize(this);
		Dimension prefSize = preferredLayoutSize(this);

		// Figure out y-offset and button height
		int atY = topMargin(), buttonHeight;
		if (size.height >= prefSize.height) {
			buttonHeight = prefButton.height;
			atY = size.height - (buttonHeight + bottomMargin());
		}
		else if (size.height >= minSize.height)
			buttonHeight = size.height - (atY + bottomMargin());
		else {
			buttonHeight = (size.height >= minButton.height ? minButton.height : size.height);
			if (atY > 0) // may need to be reduced
			atY = (atY * (size.height - buttonHeight)) / (atY + bottomMargin());
		}

		if (size.width < minSize.width) {
			// Start reducing the space allocated
			// - at minimum, gap of 1 pixel between each button
			int xgaps = Math.max(nb - 1, size.width - nb * minButton.width);
			// - at minimum, each button 1 pixel wide
			int totalButtonWidth = Math.max(size.width - xgaps, nb);
			if (totalButtonWidth == nb)
			// may need to reduce gaps to zero
			xgaps = Math.max(0, size.width - totalButtonWidth);

			int atX = 0;
			if (includeBorderMargin && xgaps > nb) {
				// enough space for >= 1 pixel per gap (incl right margin)
				atX = (12 * xgaps) / (12 + 6 + nb * 5);
				xgaps -= atX;
			}
			// adjust xgaps to include the right margin
			if (includeBorderMargin && xgaps > nb - 1) {
				int rightMargin = (11 * xgaps) / (6 + nb * 5);
				xgaps -= rightMargin;
			}

			int i = nb-1; // divisor for allocating space
			for (Iterator<AbstractButton> j = buttons.iterator(); j.hasNext(); i--) {
				int buttonWidth = totalButtonWidth / (i+1);
				j.next().setBounds(atX, atY, buttonWidth, buttonHeight);
				totalButtonWidth -= buttonWidth;
				if (i > 0) {
					atX += buttonWidth + xgaps/i;
					xgaps -= xgaps/i;
				}
			}
		}
		else {
			// More space than the minimum
			// - buttons will go up to preferred width
			int buttonWidth = (size.width >= prefSize.width ?
					prefButton.width :
					(size.width+5 - (leftMargin()+rightMargin()))/nb
					- 5);

			int atX = size.width + 5 - (nb * (5+buttonWidth) + rightMargin());
			for (Iterator<AbstractButton> b = buttons.iterator(); b.hasNext(); ) {
				b.next().setBounds(atX, atY, buttonWidth, buttonHeight);
				atX += buttonWidth + 5;
			}
		}
    }

    /* ***********
     * Test method
     *********** */
    public static void main(String args[]) {
		CommandButtons row =
			CommandButtons.createRow(APPLY_RESET_CLOSE_HELP,
						new ActionListener() {
							public void actionPerformed(ActionEvent evt) {
							System.out.println("Action command=" + evt.getActionCommand());
							}
						});
		row.addButton(new javax.swing.JButton("Bogus"));

		javax.swing.JFrame f = new javax.swing.JFrame("Test");
		f.getContentPane().add(row);
		f.pack();
		f.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
		f.setVisible(true);
    }
}
