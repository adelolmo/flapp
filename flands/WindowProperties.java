package flands;


import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import java.util.Properties;

/**
 * Encapsulates a window's dimensions; used for remembering the user's setup.
 * @author Jonathan Mann
 */
public class WindowProperties extends ComponentAdapter {
	private String name;
	private Properties props;
	private Window window;

	public static WindowProperties create(String name, Properties props) {
		WindowProperties wp = new WindowProperties(name, props);
		return (wp.getBounds() == null ? null : wp);
	}
	
	public WindowProperties(String name, Properties props) {
		this.name = name;
		this.props = props;
	}

	public Point getLocation() {
		String value = props.getProperty(name + ".loc");
		if (value != null) {
			int index = value.indexOf(',');
			if (index >= 0) {
				try {
					return new Point(Integer.parseInt(value.substring(0, index)),
						Integer.parseInt(value.substring(index+1)));
				}
				catch (NumberFormatException nfe) {}
			}
		}
		return null;
	}
	public void setLocation(Point p) {
		props.setProperty(name + ".loc", p.x + "," + p.y);
	}

	public Dimension getSize() {
		String value = props.getProperty(name + ".size");
		if (value != null) {
			int index = value.indexOf('x');
			if (index >= 0) {
				try {
					return new Dimension(Integer.parseInt(value.substring(0, index)),
						Integer.parseInt(value.substring(index+1)));
				}
				catch (NumberFormatException nfe) {}
			}
		}
		return null;
	}
	public void setSize(Dimension d) {
		props.setProperty(name + ".size", d.width + "x" + d.height);
	}

	public Rectangle getBounds() {
		Point loc = getLocation();
		Dimension size = getSize();
		if (loc == null || size == null)
			return null;
		else
			return new Rectangle(loc, size);
	}
	public void setBounds(Rectangle r) {
		setLocation(r.getLocation());
		setSize(r.getSize());
	}

	public boolean isShowing() {
		String val = props.getProperty(name + ".visible");
		return (val == null || val.equals("1"));
	}
	public void setShowing(boolean b) {
		props.setProperty(name + ".visible", b ? "1" : "0");
	}

	public void applyTo(Window w) {
		Rectangle r = getBounds();
		if (r != null)
			w.setBounds(r);
		else {
			Point p = getLocation();
			if (p != null)
				w.setLocation(p);
			Dimension d = getSize();
			if (d != null)
				w.setSize(d);
		}

		w.setVisible(isShowing());
	}

	public void getFrom(Window w) {
		if (w != null) {
			setBounds(w.getBounds());
			setShowing(w.isVisible());
		}
	}

	public void componentHidden(ComponentEvent e) {
		setShowing(window.isVisible());
	}
	public void componentMoved(ComponentEvent e) {
		setLocation(window.getLocation());
	}
	public void componentResized(ComponentEvent e) {
		setSize(window.getSize());
	}
	public void componentShown(ComponentEvent e) {
		setShowing(window.isVisible());
	}
}
