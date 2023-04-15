package flands;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import javax.swing.event.DocumentEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

/**
 * Extension of standard TableView to fix some bugs.
 * I'm not sure what the issues were now, or if I nabbed some of this code from
 * another forum.
 * 
 * @author Jonathan Mann
 */
public class TableView extends javax.swing.text.TableView {
	// Style keys of our own making
	public final static String BorderWidth = "borderWidth";
	public final static String CellPadding = "cellPadding";
	public final static String CellAlign   = "cellAlign";
	public final static String CellWidth   = "cellWidth";
	public final static String FillWidth   = "fillWidth";

	private static float DefaultCellAlign = 0.5f;

	private View _parent;

	private BasicStroke _stroke;
	private short       _cellPadding;
	private float       _cellAlign;
	private short       _borderWidth;
	private boolean     _fillWidth;

	TableView (Element elem) {
		super(elem);
		setPropertiesFromAttributes();
	}
	    
	// Override to protect our children from having their
	// parent view set to null as well.  See View.setParent()
	public void setParent(View parent) {
		_parent = parent;
		        
		if (parent != null)
			super.setParent(parent);
	}
		
	public View getParent() {
		return _parent;
	}
   
	public TableRow createTableRow(Element e) {
		return new TableRow(e);
	}

	public void setSize(float width, float height) {
		super.setSize(width, height);
		//System.out.println("TableView.setSize          " + hashCode() + " " + width + " " + height);
	}

	public float getAlignment(int axis) {
		return (axis == X_AXIS ? 0.5f : super.getAlignment(axis));
	}

	public float getMinimumSpan(int axis) {
		float f = super.getMinimumSpan(axis);
		//if (axis == View.Y_AXIS)
		//System.out.println("TableView.getMinimumSpan   " + hashCode() + " " + axis + " " + f);
		return f;
	}

	public float getPreferredSpan(int axis) {
		float f = super.getPreferredSpan(axis);
		//if (axis == View.Y_AXIS)
			//System.out.println("TableView.getPreferredSpan " + hashCode() + " " + axis + " " + f);
		return f;
	}

	// Does super.getPreferredSpan(X) and super.getMaximumSpan(Y)
	// which has the effect of preventing the table growing into
	// the available space in the X direction.
	// The Y direction is not constrained by this method.  See
	// instead TableRow.calculateMinorAxisRequirements for Y axis
	// handling.
	public float getMaximumSpan(int axis) {
		//float f = super.getMaximumSpan(axis);
		float f ;
		if (axis == View.X_AXIS) {
			if (_fillWidth)
				f = 10000f; // super.getMaximumSpan returns -2 !?
			else
				f = super.getPreferredSpan(axis);  // No stretch.  Could make configurable...
		}
		else 
			f = super.getMaximumSpan(axis);
		//if (axis == View.Y_AXIS)
			//System.out.println("TableView.getMaximumSpan   " + hashCode() + " " + axis + " " + f);
		return f;
	}

	    
	protected void loadChildren(ViewFactory f) {
		if (f == null) {
			// No factory. This most likely indicates the parent view
			// has changed out from under us, bail!
			System.out.println("No factory!!");
			return;
		}
	      
		//System.out.println("TableView.loadChildren()");
		Element e = getElement();
		int n = e.getElementCount();
		if (n > 0) {
			View[] added = new View[n];
			for (int i = 0; i < n; i++) {
				TableRow r = createTableRow(e.getElement(i));
				r.setCellPadding(_cellPadding);
				r.setBorderWidth(_borderWidth);
				r.setCellAlign(_cellAlign);
				added[i] = r;
				//System.out.println("row");
			}
			replace(0, getViewCount(), added);
		}
	}
	    
	// This is the same as the base class implementation
	// with the exception that it does not use the ViewFactory.
	// TableRow objects can only be created in the context of
	// a TableView.  See TableView.loadChildren() also.
	protected boolean updateChildren(DocumentEvent.ElementChange ec, DocumentEvent e, ViewFactory f) {
		//System.out.println("updateChildren " + e);
		//System.out.println("updateChildren " + ec);
	      
		//System.out.println("added " + dumpElemArray(ec.getChildrenAdded()));
		//System.out.println("removed " + dumpElemArray(ec.getChildrenRemoved()));
		//System.out.println("element " + ec.getElement() + ec.getElement().hashCode());
		//System.out.println("index " + ec.getIndex());

		Element[] removedElems = ec.getChildrenRemoved();
		Element[] addedElems   = ec.getChildrenAdded();
		View[] added = null;
		if (addedElems != null) {
			added = new View[addedElems.length];
			for (int i = 0; i < addedElems.length; i++) {
				TableRow r = createTableRow(addedElems[i]);
				r.setCellPadding(_cellPadding);
				r.setBorderWidth(_borderWidth);
				r.setCellAlign(_cellAlign);
				added[i] = r;
			}
		}
		int nremoved = 0;
		int index = ec.getIndex();
		if (removedElems != null)
			nremoved = removedElems.length;
		replace(index, nremoved, added);
		return true;
	}
	    
	@SuppressWarnings("unused")
	private String dumpElemArray(Element e[]) {
		StringBuffer sb = new StringBuffer();
		
		for (int i = 0; i < e.length; i++) {
			sb.append(e[i].toString());
			sb.append(" " + e[i].hashCode() + "\n");
		}
		return sb.toString();
	}
	    
	protected int getOffset(int axis, int childIndex) {
		try {
			return super.getOffset(axis, childIndex);
		}
		catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("TableView.getOffset(" + axis + "," + childIndex + "): threw ArrayException");
			return 0;
		}
	}
   /**
	* Convenience setter methods, ala StyleConstants.
	*/
	public static void setBorderWidth(MutableAttributeSet attr, float f) {
		attr.addAttribute(BorderWidth, Float.valueOf(f));
	}
	public static void setCellPadding(MutableAttributeSet attr, short s) {
		attr.addAttribute(CellPadding, Short.valueOf(s));
	}
	public static void setCellAlign(MutableAttributeSet attr, float f) {
		attr.addAttribute(CellAlign, Float.valueOf(f));
	}
	public static void setFillWidth(MutableAttributeSet attr, boolean b) {
		attr.addAttribute(FillWidth, Boolean.valueOf(b));
	}

	protected void setPropertiesFromAttributes() {
		AttributeSet attr = getAttributes();
		if (attr != null) {
			retrieveBorderWidth(attr);
			retrieveCellPadding(attr);
			retrieveFillWidth(attr);
		}
		retrieveCellAlign(attr);
	}

	private void retrieveBorderWidth(AttributeSet attr) {
		Object o = null;
		float f = 0f; // default - no border

		if ((o = attr.getAttribute(BorderWidth)) != null) {
			Float F = (Float)o;
			f = F.floatValue();
		}
		 
		if (f != 0)
			_stroke = new BasicStroke(f);
		      
		_borderWidth = (short)f;
	}

	private void retrieveCellPadding(AttributeSet attr) {
		Object o = null;
		short  s = 1;
		      
		if ((o = attr.getAttribute(CellPadding)) != null) {
			Short S = (Short)o;
			s = S.shortValue();
		}
		      
		_cellPadding = s;
	}

	private void retrieveCellAlign(AttributeSet attr) {
		Object o = null;
		float  f = DefaultCellAlign;
		      
		if (attr != null &&
			(o = attr.getAttribute(CellAlign)) != null) {
			Float F = (Float)o;
			f = F.shortValue();
		}

		_cellAlign = f;
	}

	private void retrieveFillWidth(AttributeSet attr) {
		Object o = null;
		boolean b = false;

		if (attr != null && (o = attr.getAttribute(FillWidth)) != null) {
			Boolean B = (Boolean)o;
			b = B.booleanValue();
		}

		_fillWidth = b;
	}

	public class TableRow extends javax.swing.text.TableView.TableRow {
		private short       cellPadding_;
		private short       borderWidth_;
		private float       cellAlign_;
		private BasicStroke stroke_;
	      
		TableRow(Element elem) {
			super(elem);
		}

		public void paint(Graphics g,
			Shape    allocation) {
			super.paint(g, allocation);
			drawCellBorder(g, allocation);
			//System.out.println("TableRow.paint");
		}
	    
		public void setSize(float width, float height) {
			super.setSize(width, height);
			//System.out.println("TableRow.setSize           " + hashCode() + " " + width + " " + height);
		}
	      
		public float getPreferredSpan(int axis) {
			float f = super.getPreferredSpan(axis);
			//if (axis == View.Y_AXIS)
			//System.out.println("TableRow.getPreferredSpan  " + hashCode() + " " + axis + " " + f);
			return f;
		}
	      
		public float getMinimumSpan(int axis) {
			float f = super.getMinimumSpan(axis);
			//if (axis == View.Y_AXIS)
			//System.out.println("TableRow.getMinimumSpan    " + hashCode() + " " + axis + " " + f);
			return f;
		}
	      
		public float getMaximumSpan(int axis) {
			float f;
			if (axis == View.Y_AXIS)
				f = super.getPreferredSpan(axis);
			else
				f = super.getMaximumSpan(axis);
			//System.out.println("TableRow.getMaximumSpan    " + hashCode() + " " + axis + " " + f);
			return f;
		}

	      
		void setCellAlign(float cellAlign) {
			cellAlign_ = cellAlign;
		}

		void setCellPadding(short padding) {
			cellPadding_ = padding;
		}
	      
		void setBorderWidth(short borderWidth) {
			borderWidth_ = borderWidth;
			if ((borderWidth_ != TableView.this._borderWidth) &&
				(borderWidth_ != 0)) {
				// per row bordering style is different to parent
				// table.  Create local stroke object
				float f = borderWidth_;
				stroke_ = new BasicStroke(f);
			}
		}
	      
		short getCellPadding() {
			return cellPadding_;
		}
	      
		short getBorderWidth() {
			return borderWidth_;
		}
	      
		float getCellAlign() {
			return cellAlign_;
		}
	      
		protected void drawCellBorder(Graphics g, Shape allocation) {
			if (borderWidth_ == 0)
				return;
	          
			// Get the overall bounding rectangle of the row
			Rectangle2D r = allocation.getBounds2D();
	        
			// Set up the drawing
			Graphics2D g2 = (Graphics2D)g;
			BasicStroke stroke = (this.stroke_ != null) ?
				this.stroke_ : TableView.this._stroke;
			g2.setStroke(stroke);
			g2.setColor(Color.BLACK);
	        
			int children = getViewCount();
	        
			// draw each cell border
			for (int i = 0; i < children; i++) {
				int width = getSpan(View.X_AXIS, i);
				r.setRect(r.getX(), r.getY(), width, r.getHeight());
				//System.out.println("draw " + r);
				g2.draw(r);
	          
				// Move the rectangle origin on to the next cell starting point
				r.setRect(r.getX() + width, r.getY(), width, r.getHeight());
			}
		}
	}
}