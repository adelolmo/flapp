package flands;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;

/**
 * Complete copy of ComponentView, part of the JDK.
 * This seems to be the class causing problems in some instances; I'm hoping to solve
 * the problems with some judicious pruning.
 * 
 * @author Timothy Prinzing
 * @author Jonathan Mann
 */
public class ComponentView extends View {
    private Component c;
    
	public ComponentView(Element e) {
		super(e);
	}
	
    protected Component createComponent() {
    	AttributeSet attr = getElement().getAttributes();
    	Component comp = StyleConstants.getComponent(attr);
    	return comp;
    }
    
    public final Component getComponent() {
    	if (c == null)
    		c = createComponent();
    	return c;
    }

    public void paint(Graphics g, Shape a) {
    	if (c != null) {
    	    Rectangle alloc = (a instanceof Rectangle) ?
    	    		(Rectangle) a : a.getBounds();
    	    c.setBounds(alloc.x, alloc.y, alloc.width, alloc.height);
    	}
    }
    
    public float getPreferredSpan(int axis) {
    	if ((axis != X_AXIS) && (axis != Y_AXIS)) {
    	    throw new IllegalArgumentException("Invalid axis: " + axis);
    	}
    	if (c != null) {
    	    Dimension size = c.getPreferredSize();
    	    if (axis == View.X_AXIS) {
    	    	return size.width;
    	    } else {
    	    	return size.height;
    	    }
    	}
    	return 0;
    }
    
    public float getMinimumSpan(int axis) {
    	if ((axis != X_AXIS) && (axis != Y_AXIS)) {
    	    throw new IllegalArgumentException("Invalid axis: " + axis);
    	}
    	if (c != null) {
    	    Dimension size = c.getMinimumSize();
    	    if (axis == View.X_AXIS) {
    	    	return size.width;
    	    } else {
    	    	return size.height;
    	    }
    	}
    	return 0;
    }
    
    public float getMaximumSpan(int axis) {
    	return getPreferredSpan(axis);
    }
    
    public float getAlignment(int axis) {
    	if (c != null) {
    	    switch (axis) {
    	    case View.X_AXIS:
    	    	return c.getAlignmentX();
    	    case View.Y_AXIS:
    	    	return c.getAlignmentY();
    	    }
    	}
    	return super.getAlignment(axis);
    }
    
    public void setParent(View p) {
    	if (p == null)
    		System.out.println("ComponentView(null) called");
    	super.setParent(p);
    	setComponentParent();
    	/*
        if (SwingUtilities.isEventDispatchThread()) {
        	setComponentParent();
        } else {
            Runnable callSetComponentParent = new Runnable() {
                public void run() {
				    Document doc = getDocument();
				    try {
						if (doc instanceof AbstractDocument) {
						    ((AbstractDocument)doc).readLock();
						}
						setComponentParent();
						Container host = getContainer();
						if (host != null) {
						    preferenceChanged(null, true, true);
						    host.repaint();
						}
				    } finally {
						if (doc instanceof AbstractDocument) {
						    ((AbstractDocument)doc).readUnlock();
						}
				    }			
                }
            };
            SwingUtilities.invokeLater(callSetComponentParent);
        }
        */
    }
    
    void setComponentParent() {
    	View p = getParent();
    	if (p != null) {
    	    Container parent = getContainer();
    	    if (parent != null) {
	    		if (c == null)
	    		    // try to build a component
	    			c = createComponent();
	    		if (c != null) {
	    		    if (c.getParent() == null) {
		    			// components associated with the View tree are added
		    			// to the hosting container with the View as a constraint.
		    			parent.add(c, this);
	    		    }
	    		}
    	    }
    	} else {
            if (c != null) {
                Container parent = c.getParent();
                if (parent != null) {
                    // remove the component from its hosting container
                    parent.remove(c);
                }
            }
        }
    }
    
    public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
    	int p0 = getStartOffset();
    	int p1 = getEndOffset();
    	if ((pos >= p0) && (pos <= p1)) {
    	    Rectangle r = a.getBounds();
    	    if (pos == p1) {
    	    	r.x += r.width;
    	    }
    	    r.width = 0;
    	    return r;
    	}
    	throw new BadLocationException(pos + " not in range " + p0 + "," + p1, pos);
    }
    
    public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {
    	Rectangle alloc = (Rectangle) a;
    	if (x < alloc.x + (alloc.width / 2)) {
    	    bias[0] = Position.Bias.Forward;
    	    return getStartOffset();
    	}
    	bias[0] = Position.Bias.Backward;
    	return getEndOffset();
    }
}
