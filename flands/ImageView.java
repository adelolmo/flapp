package flands;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.View;

/**
 * Icon decorator that implements the view interface.  The
 * entire element is used to represent the icon.  This acts
 * as a gateway from the display-only View implementations to
 * interactive lightweight icons (that is, it allows icons
 * to be embedded into the View hierarchy.  The parent of the icon
 * is the container that is handed out by the associated view
 * factory.
 * <p>
 * Modified to scale the image as required.
 *
 * @author Timothy Prinzing
 * @author Jonathan Mann
 * @version 1.28 12/19/03
 */
public class ImageView extends View {

    /**
     * Creates a new icon view that represents an element.
     *
     * @param elem the element to create a view for
     */
    public ImageView(Element elem) {
		super(elem);
		AttributeSet attr = elem.getAttributes();
		i = Node.getImage(attr);
		drawer = new BufferDrawer();
    }

    // --- View methods ---------------------------------------------
    private Rectangle alloc;
    /**
     * Paints the icon.
     * The real paint behavior occurs naturally from the association
     * that the icon has with its parent container (the same
     * container hosting this view), so this simply allows us to
     * position the icon properly relative to the view.  Since
     * the coordinate system for the view is simply the parent
     * containers, positioning the child icon is easy.
     *
     * @param g the rendering surface to use
     * @param a the allocated region to render into
     * @see View#paint
     */
    public void paint(Graphics g, Shape a) {
		alloc = a.getBounds();
		if (alloc.getWidth() >= i.getWidth(null) && alloc.getHeight() >= i.getHeight(null)) {
			// Draw the image at the regular size
			int offX = (alloc.width - i.getWidth(null))/2;
			int offY = (alloc.height - i.getHeight(null))/2;
			g.drawImage(i, alloc.x + offX, alloc.y + offY, alloc.width, alloc.height, null);
		}
		else {
			// Scale the image
			double scale1 = (double)alloc.width/i.getWidth(null);
			double scale2 = (double)alloc.height/i.getHeight(null);
			double scale = Math.min(scale1, scale2);
			int scaledWidth = (int)(i.getWidth(null)*scale);
			int scaledHeight = (int)(i.getHeight(null)*scale);
			int offX = (alloc.width - scaledWidth)/2;
			int offY = (alloc.height - scaledHeight)/2;
			if (drawer.draw(scaledWidth, scaledHeight)) {
				// Same as last scale - copy the prerendered image
				g.clipRect(alloc.x+offX, alloc.y+offY, scaledWidth, scaledHeight);
				g.drawImage(drawer.buffer, alloc.x+offX, alloc.y+offY, null);
			}
			else
				// Draw a quickly scaled version for now
				g.drawImage(i, alloc.x+offX, alloc.y+offY, scaledWidth, scaledHeight, null);
		}
    }
    
    private float viewWidth = -1;
    /**
     * Sets the size available for this View.
     * Remember the width so that we can an appropriately scaled height
     * in {@link #getMinimumSpan(int)}.
     */
    public void setSize(float width, float height) {
    	viewWidth = width;
    }
    
    /**
     * Get the minimum space required by this View.
     * Defaults to returning 0 for the width;
     * returns a scaled height, based on how much width has been allocated.
     */
    public float getMinimumSpan(int axis) {
    	switch (axis) {
    	case View.X_AXIS:
    		return 0f;
    	case View.Y_AXIS:
    		if (i.getWidth(null) > viewWidth) {
    			double scale = (double)viewWidth/i.getWidth(null);
    			return (float)(scale*i.getHeight(null));
    		}
    		return i.getHeight(null);
    	default:
		    throw new IllegalArgumentException("Invalid axis: " + axis);
		}
    }
    
    /**
     * Determines the preferred span for this view along an
     * axis.
     *
     * @param axis may be either View.X_AXIS or View.Y_AXIS
     * @return  the span the view would like to be rendered into
     *           Typically the view is told to render into the span
     *           that is returned, although there is no guarantee.
     *           The parent may choose to resize or break the view.
     * @exception IllegalArgumentException for an invalid axis
     */
    public float getPreferredSpan(int axis) {
    	switch (axis) {
		case View.X_AXIS:
			return i.getWidth(null);
		case View.Y_AXIS:
			return i.getHeight(null);
		default:
		    throw new IllegalArgumentException("Invalid axis: " + axis);
		}
    }

    /**
     * Determines the desired alignment for this view along an
     * axis.  This is implemented to give the alignment to the
     * bottom of the icon along the y axis, and the default
     * along the x axis.
     *
     * @param axis may be either View.X_AXIS or View.Y_AXIS
     * @return the desired alignment >= 0.0f && <= 1.0f.  This should be
     *   a value between 0.0 and 1.0 where 0 indicates alignment at the
     *   origin and 1.0 indicates alignment to the full span
     *   away from the origin.  An alignment of 0.5 would be the
     *   center of the view.
     */
    public float getAlignment(int axis) {
	switch (axis) {
	case View.Y_AXIS:
	    return 1;
	default:
	    return super.getAlignment(axis);
	}
    }

    /**
     * Provides a mapping from the document model coordinate space
     * to the coordinate space of the view mapped to it.
     *
     * @param pos the position to convert >= 0
     * @param a the allocated region to render into
     * @return the bounding box of the given position
     * @exception BadLocationException  if the given position does not
     *   represent a valid location in the associated document
     * @see View#modelToView
     */
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

    /**
     * Provides a mapping from the view coordinate space to the logical
     * coordinate space of the model.
     *
     * @param x the X coordinate >= 0
     * @param y the Y coordinate >= 0
     * @param a the allocated region to render into
     * @return the location within the model that best represents the
     *  given point of view >= 0
     * @see View#viewToModel
     */
    public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {
	Rectangle alloc = (Rectangle) a;
	if (x < alloc.x + (alloc.width / 2)) {
	    bias[0] = Position.Bias.Forward;
	    return getStartOffset();
	}
	bias[0] = Position.Bias.Backward;
	return getEndOffset();
    }

    // --- member variables ------------------------------------------------

    private Image i;
    private BufferDrawer drawer;
    
    private class BufferDrawer implements Runnable {
    	private Image buffer;
    	private Graphics2D bufferG;
    	private boolean drawn = false;
    	private int scaledWidth, scaledHeight;
    	private int drawingWidth, drawingHeight;
    	
    	public BufferDrawer() {
    		drawn = false;
    		scaledWidth = -1;
    		scaledHeight = -1;
    	}

    	public boolean draw(int width, int height) {
    		if (width == scaledWidth && height == scaledHeight)
    			return drawn;
    		
    		drawn = false;
    		scaledWidth = width;
    		scaledHeight = height;
    		new Thread(this).start();
    		
    		return false;
    	}
    	
		public void run() {
			drawn = false;
			drawingWidth = scaledWidth;
			drawingHeight = scaledHeight;
			
			if (buffer == null)
				buffer = getContainer().createImage(i.getWidth(null), i.getHeight(null));
			if (bufferG != null)
				bufferG.dispose();
			bufferG = (Graphics2D)buffer.getGraphics();
			bufferG.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			bufferG.drawImage(i, 0, 0, drawingWidth, drawingHeight, null);
			
			if (drawingWidth == scaledWidth && drawingHeight == scaledHeight) {
				drawn = true;
				if (bufferG != null) {
					bufferG.dispose();
					bufferG = null;
				}
				
				int offX = (alloc.width - drawingWidth)/2;
				int offY = (alloc.height - drawingHeight)/2;
				Graphics g = getGraphics();
				g.clipRect(alloc.x+offX, alloc.y+offY, drawingWidth, drawingHeight);
				g.drawImage(buffer, alloc.x+offX, alloc.y+offY, null);
			}
		}
    }
}
