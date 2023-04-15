package flands;

import javax.swing.text.Element;
import javax.swing.text.View;

/**
 * I guess I added this while working the bugs out - BoxNode uses this view type,
 * which is used in turn by OutcomeNode when the outcome is more involved.
 * Not sure if it's still necessary.
 * @author Jonathan Mann
 */
public class BoxView extends javax.swing.text.BoxView {
	public BoxView(Element elem, int axis) {
		super(elem, axis);
	}
	
	protected int getOffset(int axis, int childIndex) {
		try {
			return super.getOffset(axis, childIndex);
		}
		catch (ArrayIndexOutOfBoundsException e) {
			//System.err.println("BoxView.getOffset(" + axis + "," + childIndex + "): threw ArrayException");
			return 0;
		}
	}
	
    protected int getSpan(int axis, int childIndex) {
    	try {
    		return super.getSpan(axis, childIndex);
    	}
		catch (ArrayIndexOutOfBoundsException e) {
			//System.err.println("BoxView.getSpan(" + axis + "," + childIndex + "): threw ArrayException");
			return 0;
		}
    }
    
    public View getView(int n) {
    	try {
    		return super.getView(n);
    	}
    	catch (ArrayIndexOutOfBoundsException e) {
       		if (FLApp.debugging) {
       			System.err.println("BoxView.getView(" + n + "): threw ArrayException");
       			e.printStackTrace();
       		}
    		return null;
    	}
    }
}
