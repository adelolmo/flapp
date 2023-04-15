package flands;


import java.awt.Dimension;
import java.awt.Point;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import javax.swing.JComponent;
import javax.swing.JToolTip;
import javax.swing.Popup;
import javax.swing.PopupFactory;

/**
 * Handles a dice roll via tool-tip display. Unless instant is set to true, notification
 * of the result is necessarily delayed.
 * 
 * @author Jonathan Mann
 */
public class Roller implements Runnable {
	public static interface Listener {
		public void rollerFinished(Roller r);
	}

	private static Random randomGen = new Random();
	private static long[] resultCount = new long[6];

	private int dice;
	private int adjustment;
	private int result = -1;
	private boolean instant = false;

	private JToolTip toolTip;
	private Popup toolTipPopup;
	private int popupX, popupY;
	private List<Listener> listeners = new LinkedList<Listener>();

	public Roller(int dice, int adjustment) {
		this.dice = dice;
		this.adjustment = adjustment;
	}

	public boolean isInstant() { return instant; }
	public void setInstant(boolean b) {
		this.instant = b;
	}
	
	public String toString() {
		return dice + "D" + (adjustment > 0 ? "+" + adjustment :
			(adjustment < 0 ? "" + adjustment : ""));
	}
	
	public void addListener(Listener l) {
		listeners.add(l);
	}

	public void startRolling() {
		if (running) return;

		JComponent toolTipContext = FLApp.getSingle().getToolTipContext();
		//System.out.println("ToolTip context component is at location " + toolTipContext.getLocationOnScreen());
		toolTip = new JToolTip();
		toolTip.setComponent(toolTipContext);
		int maxResult = 6*dice + adjustment;
		if (maxResult >= 100) // unlikely
			toolTip.setTipText("666");
		else if (maxResult >= 10 || dice + adjustment < 0) // ie. -X
			toolTip.setTipText("66");
		else
			toolTip.setTipText("6");
		toolTip.setFont(SectionDocument.getPreferredFont());
		Dimension tipSize = toolTip.getPreferredSize();
		toolTip.setPreferredSize(tipSize);
		toolTip.setTipText("");
		int x = FLApp.getSingle().getMouseAtX();
		int y = FLApp.getSingle().getMouseAtY();
		try {
			Point contextLoc = toolTipContext.getLocationOnScreen();
			x += contextLoc.x;
			y += contextLoc.y;
		}
		catch (java.awt.IllegalComponentStateException e) {}
		if (y > 20)
			y -= (tipSize.height + 10);
		else
			y += 10;
		toolTipPopup = PopupFactory.getSharedInstance().getPopup(toolTipContext, toolTip, x, y);
		popupX = x;
		popupY = y;

		new Thread(this).start();
	}

	public int getResult() { return result; }

	private static void addResult(int result) {
		resultCount[result]++;
	}
	
	public static void printResultCount() {
		System.out.println("Dice result count:");
		for (int i = 0; i < resultCount.length; i++)
			System.out.println((i+1) + ": " + resultCount[i]);
	}
	
	protected void doRoll() {
		int min = 0, max = 0;
		boolean exceptionPrinted = false;
		for (int i = 0; i < 20; i++) {
			if (i > 0) {
				try {
					Thread.sleep(50);
				}
				catch (InterruptedException ie) {}
			}

			int sum = dice + adjustment; // minimum = number of dice * [1]
			for (int d = 0; d < dice; d++) {
				int result = (int)(6 * randomGen.nextDouble());
				sum += result; // number of dice * [0-5]
				addResult(result);
			}
			String sumStr = (sum > 0 ? Integer.toString(sum) : "0");
			toolTip.setTipText(sumStr);
			result = sum;
			if (instant) break;
			if (i == 0)
				toolTipPopup.show();
			else {
				try {
					toolTip.paintImmediately(toolTip.getBounds());
				}
				catch (NullPointerException npe) {
					// Bug reported 6/11/14
					// NPE in javax.swing.plaf.metal.MetalToolTipUI.getAcceleratorString
					if (!exceptionPrinted) {
						exceptionPrinted = true;
						npe.printStackTrace();
					}
				}
			}

			if (i == 0)
				min = max = result;
			else {
				if (result < min) min = result;
				else if (result > max) max = result;
			}
		}
		
		if (!instant)
			System.out.println("Min=" + min + ", max=" + max);
		else
			System.out.println("Result=" + result);
	}

	private boolean running = false;
	private boolean linger = false;
	private long lingerTill = -1;
	public void run() {
		if (!linger) {
			running = true;
			doRoll();

			if (!instant) {
				// Start another thread that will clean up after a while
				linger = true;
				new Thread(this).start();
			}

			for (Iterator<Listener> i = listeners.iterator(); i.hasNext(); )
				i.next().rollerFinished(this);
		}
		else {
			long timeNow = System.currentTimeMillis();
			if (lingerTill < 0 || lingerTill < timeNow)
				lingerTill = timeNow + 1000;
			while (timeNow < lingerTill) {
				try {
					Thread.sleep(lingerTill - timeNow);
				}
				catch (InterruptedException ie) {}
				timeNow = System.currentTimeMillis();
			}
			linger = false;
			lingerTill = -1;
			synchronized (this) {
				toolTipPopup.hide();
				toolTipPopup = null;
				toolTip = null;
			}
			running = false;
		}
	}

	/**
	 * Get the current tooltip text.
	 */
	public String getTooltipText() {
		return (toolTip == null ? null : toolTip.getTipText());
	}
	/**
	 * Set the tooltip text. This will make the tooltip hang around for at least another second.
	 */
	public void setTooltipText(String text) {
		synchronized (this) {
			if (toolTip != null) {
				lingerTill = System.currentTimeMillis() + 1000;
				toolTip.setTipText(text);
				toolTip.setPreferredSize(null); // we fixed it earlier

				// I was trying to resize the popup, but it didn't work, so instead I create a new one.
				// Then I found this in the API:
				//   The general contract is that if you need to change the size of the Component,
				//   or location of the Popup, you should obtain a new Popup
				// So this is the correct course of action!
				Popup newPopup = PopupFactory.getSharedInstance().getPopup(FLApp.getSingle().getToolTipContext(), toolTip, popupX, popupY);
				newPopup.show();
				toolTipPopup.hide();
				toolTipPopup = newPopup;
			}
		}
	}
	public void appendTooltipText(String text) {
		setTooltipText(getTooltipText() + text);
	}
}
