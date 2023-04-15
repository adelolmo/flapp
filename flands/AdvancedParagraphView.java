package flands;


import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.SizeRequirements;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.FlowView;
import javax.swing.text.LabelView;
import javax.swing.text.ParagraphView;
import javax.swing.text.Position;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

/**
 * An extension of ParagraphView that actually implements text justification.
 * At this point the original may do so, although it didn't at the time.
 * Nabbed from
 * <a href="http://forums.sun.com/thread.jspa?forumID=57&threadID=289879">here</a>,
 * if memory serves, although there was an occasional boundary case that I fixed
 * along the way.
 * 
 * @author Jonathan Mann
 */
public class AdvancedParagraphView extends ParagraphView {
	public AdvancedParagraphView(Element elem) {
		super(elem);
		strategy = new AdvancedFlowStrategy();
	}

	protected View createRow() {
		return new AdvancedRow(getElement());
	}

	protected static int getSpaceCount(String content) {
		int result = 0;
		int index = content.indexOf(' ');
		while (index >= 0) {
			result++;
			index = content.indexOf(' ', index + 1);
		}
		return result;
	}

	protected static int[] getSpaceIndexes(String content, int shift) {
		int cnt = getSpaceCount(content);
		int[] result = new int[cnt];
		int counter = 0;
		int index = content.indexOf(' ');
		while (index >= 0) {
			result[counter] = index + shift;
			counter++;
			index = content.indexOf(' ', index + 1);
		}
		return result;
	}

	public int getFlowSpan(int index) {
		int span = super.getFlowSpan(index);
		if (index == 0) {
			int firstLineIdent = (int)
				StyleConstants.getFirstLineIndent(this.getAttributes());
			span -= firstLineIdent;
		}
		return span;
	}

	protected void layoutMinorAxis(int targetSpan, int axis, int[] offsets, int[] spans) {
		super.layoutMinorAxis(targetSpan, axis, offsets, spans);
		int firstLineIndent = (int)
			StyleConstants.getFirstLineIndent(this.getAttributes());
		offsets[0] += firstLineIndent;
	}

	protected static boolean isContainSpace(View v) {
		int startOffset = v.getStartOffset();
		int len = v.getEndOffset() - startOffset;
		try {
			String text = v.getDocument().getText(startOffset, len);
			return (text.indexOf(' ') >= 0);
		}
		catch (Exception ex) {
			return false;
		}
	}

	protected int getOffset(int axis, int childIndex) {
		try {
			return super.getOffset(axis, childIndex);
		}
		catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("AdvancedParagraphView.getOffset(" + axis + "," + childIndex + "): threw ArrayException");
			return 0;
		}
	}
	static private class AdvancedFlowStrategy extends FlowStrategy {
		public void layout(FlowView fv) {
			super.layout(fv);

			AttributeSet attr = fv.getElement().getAttributes();
			float lineSpacing = StyleConstants.getLineSpacing(attr);
			boolean justifiedAlignment = (StyleConstants.getAlignment(attr) == StyleConstants.ALIGN_JUSTIFIED);

			if (!(justifiedAlignment || (lineSpacing > 1))) return;

			int cnt = fv.getViewCount();
			for (int i = 0; i < (cnt - 1); i++) {
				AdvancedRow row = (AdvancedRow) fv.getView(i);

				if (lineSpacing > 1) {
					float height = row.getMinimumSpan(View.Y_AXIS);
					float addition = (height * lineSpacing) - height;

					if (addition > 0) {
						row.setInsets(row.getTopInset(), row.getLeftInset(), (short)addition, row.getRightInset());
					}
				}

				if (justifiedAlignment) {
					restructureRow(row, i);
					row.setRowNumber(i + 1);
				}
			}
		}

		protected void restructureRow(View row, int rowNum) {
			int rowStartOffset = row.getStartOffset();
			int rowEndOffset = row.getEndOffset();
			String rowContent = "";

			try {
				rowContent = row.getDocument().getText(rowStartOffset,
					rowEndOffset - rowStartOffset);

				if (rowNum == 0) {
					for (int index = 0; index < rowContent.length(); index++)
						if (rowContent.charAt(index) != ' ') {
							rowContent = rowContent.substring(index);
							break;
						}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			int rowSpaceCount = getSpaceCount(rowContent);
			if (rowSpaceCount < 1)
				return;

			int[] rowSpaceIndexes = getSpaceIndexes(rowContent, row.getStartOffset());
			int currentSpaceIndex = 0;

			for (int i = 0; i < row.getViewCount(); i++) {
				View child = row.getView(i);

				if ((child.getStartOffset() < rowSpaceIndexes[currentSpaceIndex]) &&
					(child.getEndOffset()   > rowSpaceIndexes[currentSpaceIndex])) {

					// split view
					View first = child.createFragment(child.getStartOffset(), rowSpaceIndexes[currentSpaceIndex]);
					View second = child.createFragment(rowSpaceIndexes[currentSpaceIndex], child.getEndOffset());
					View[] repl = new View[2];
					repl[0] = first;
					repl[1] = second;
					row.replace(i, 1, repl);
					currentSpaceIndex++;

					if (currentSpaceIndex >= rowSpaceIndexes.length)
						break;
				}
			}

			//int childCnt = row.getViewCount();
		}
	}

	private class AdvancedRow extends flands.BoxView {
		private int rowNumber = 0;

		AdvancedRow(Element elem) {
			super(elem, View.X_AXIS);
		}

		protected void loadChildren(ViewFactory f) {
		}

		public AttributeSet getAttributes() {
			View p = getParent();
			return (p != null) ? p.getAttributes() : null;
		}

		public float getAlignment(int axis) {
			if (axis == View.X_AXIS) {

				AttributeSet attr = getAttributes();
				int justification = StyleConstants.getAlignment(attr);

				switch (justification) {
					case StyleConstants.ALIGN_LEFT:
					case StyleConstants.ALIGN_JUSTIFIED:
						return 0;

					case StyleConstants.ALIGN_RIGHT:
						return 1;

					case StyleConstants.ALIGN_CENTER:
						return 0.5f;
				}
			}

			return super.getAlignment(axis);
		}

		public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
			Rectangle r = a.getBounds();
			View v = getViewAtPosition(pos, r);

			if ((v != null) && (!v.getElement().isLeaf())) { // Don't adjust the height if the view represents a branch.

				return super.modelToView(pos, a, b);
			}

			r = a.getBounds();

			int height = r.height;
			int y = r.y;
			Shape loc = super.modelToView(pos, a, b);
			r = loc.getBounds();
			r.height = height;
			r.y = y;

			return r;
		}

		public int getStartOffset() {
			int offs = Integer.MAX_VALUE;
			int n = getViewCount();

			for (int i = 0; i < n; i++) {
				View v = getView(i);
				offs = Math.min(offs, v.getStartOffset());
			}

			return offs;
		}

		public int getEndOffset() {
			int offs = 0;
			int n = getViewCount();

			for (int i = 0; i < n; i++) {
				View v = getView(i);
				offs = Math.max(offs, v.getEndOffset());
			}

			return offs;
		}

		protected void layoutMinorAxis(int targetSpan, int axis, int[] offsets, int[] spans) {
			baselineLayout(targetSpan, axis, offsets, spans);
		}

		protected SizeRequirements calculateMinorAxisRequirements(int axis, SizeRequirements r) {
			return baselineRequirements(axis, r);
		}

		protected int getViewIndexAtPosition(int pos) {
			// This is expensive, but are views are not necessarily laid out in model order.
			if ((pos < getStartOffset()) || (pos >= getEndOffset()))
				return -1;

			for (int counter = getViewCount() - 1; counter >= 0; counter--) {
				View v = getView(counter);
				if ((pos >= v.getStartOffset()) && (pos < v.getEndOffset()))
					return counter;
			}

			return -1;
		}

        /* Make these 4 protected methods public */
		public short getTopInset() {
			return super.getTopInset();
		}

		public short getLeftInset() {
			return super.getLeftInset();
		}

		public short getRightInset() {
			return super.getRightInset();
		}

		public void setInsets(short topInset, short leftInset, short bottomInset, short rightInset) {
			super.setInsets(topInset, leftInset, bottomInset, rightInset);
		}

		/**
		 * @param targetSpan the total span given to the view, which would be used to layout the children
		 * @param axis the axis being layed out
		 * @param offsets the offsets from the origin of the view for each of the child views; this is a return value and is filled in by the
         * implementation of this method
		 * @param spans the span of each child view; this is a return value and is filled in by the implementation of this method
		 */
		protected void layoutMajorAxis(int targetSpan, int axis, int[] offsets, int[] spans) {
			super.layoutMajorAxis(targetSpan, axis, offsets, spans);

			AttributeSet attr = getAttributes();

			if ((StyleConstants.getAlignment(attr) != StyleConstants.ALIGN_JUSTIFIED) &&
				(axis != View.X_AXIS))
				return;

			int cnt = offsets.length;
			int span = 0;

			for (int i = 0; i < cnt; i++)
				span += spans[i];
			//System.out.println("Total span=" + span);

			if (getRowNumber() == 0)
				return;

			//System.out.println("Number of child views=" + offsets.length);
			int startOffset = getStartOffset();
			int len = getEndOffset() - startOffset;
			String context = "";
			//System.out.println("Start offset=" + startOffset + ", len=" + len);

			try {
				context = getElement().getDocument().getText(startOffset, len);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			//System.out.println("Context='" + context + "'");

			int spaceCount = getSpaceCount(context);
			if (context.endsWith(" ")) spaceCount--;
			// This algorithm assumed that context would end with a space.
			// This is apparently not always the case, as I discovered by accident.
			// I need to find what breaks the line in the wrong place and change that!
			int pixelsToAdd = targetSpan - span;
			//System.out.println("Space count=" + spaceCount + ", pixels to add=" + pixelsToAdd);

			if (this.getRowNumber() == 1) {
				int firstLineIndent = (int)
					StyleConstants.getFirstLineIndent(getAttributes());
				pixelsToAdd -= firstLineIndent;
				//System.out.println("First line indent, pixels to add now=" + pixelsToAdd);
			}

			int[] spaces = getSpaces(pixelsToAdd, spaceCount);
			int j = 0;
			int shift = 0;

			for (int i = 1; i < cnt; i++) {
				LabelView v = (LabelView) getView(i);
				offsets[i] += shift;

				if ((isContainSpace(v)) && (i != (cnt - 1))) {
					//System.out.println("isContainsSpace(view(" + i + ")) is true, i=" + i);
					offsets[i] += spaces[j];
					spans[i - 1] += spaces[j];
					shift += spaces[j];
					//System.out.println("offsets[" + i + "] now=" + offsets[i] + ", spans[" + (i-1) + "] now =" + spans[i-1] + ",shift now=" + shift);
					j++;
					//System.out.println("j now=" + j);
				}
			}
		}

		protected int[] getSpaces(int space, int cnt) {
			int[] result = new int[cnt];

			if (cnt == 0)
				return result;

			int base = space / cnt;
			int rst = space % cnt;

			for (int i = 0; i < cnt; i++) {
				result[i] = base;

				if (rst > 0) {
					result[i]++;
					rst--;
				}
			}

			return result;
		}

		public float getMinimumSpan(int axis) {
			if (axis == View.X_AXIS) {
				AttributeSet attr = getAttributes();

				if (StyleConstants.getAlignment(attr) != StyleConstants.ALIGN_JUSTIFIED)
					return super.getMinimumSpan(axis);
				else
					return this.getParent().getMinimumSpan(axis);
			}
			else
				return super.getMinimumSpan(axis);
		}

		public float getMaximumSpan(int axis) {
			if (axis == View.X_AXIS) {
				AttributeSet attr = getAttributes();

				if (StyleConstants.getAlignment(attr) != StyleConstants.ALIGN_JUSTIFIED)
					return super.getMaximumSpan(axis);
				else
					return this.getParent().getMaximumSpan(axis);
			}
			else
				return super.getMaximumSpan(axis);
		}

		public float getPreferredSpan(int axis) {
			if (axis == View.X_AXIS) {
				AttributeSet attr = getAttributes();

				if (StyleConstants.getAlignment(attr) != StyleConstants.ALIGN_JUSTIFIED)
					return super.getPreferredSpan(axis);
				else
					return this.getParent().getPreferredSpan(axis);
			}
			else
				return super.getPreferredSpan(axis);
		}

		public void setRowNumber(int value) {
			rowNumber = value;
		}

		public int getRowNumber() {
			return rowNumber;
		}
	}
}