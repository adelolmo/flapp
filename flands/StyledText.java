package flands;


import java.util.List;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/**
 * Convenient encapsulation of a piece of text and the associated style.
 * Currently the included static methods are unused.
 * @author Jonathan Mann
 */
public class StyledText {
	public String text;
	public AttributeSet atts;

	public StyledText(String text, AttributeSet atts) {
		this.text = text;
		this.atts = atts;
	}

	private static void appendStyledText(List<StyledText> list, StyledText[] resultArr, int index, StyledText st) {
		if (list != null)
			list.add(st);
		if (resultArr != null)
			resultArr[index] = st;
	}

	public static StyledText[] breakupCapsText(String text, String capsWord, AttributeSet textAtts, int smallerFontSize, List<StyledText> textList) {
		int index = text.indexOf(capsWord);
		if (index < 0) {
			if (textList == null)
				return new StyledText[] { new StyledText(text, textAtts) };
			else {
				textList.add(new StyledText(text, textAtts));
				return null;
			}
		}

		boolean thirdSegment = (index + capsWord.length()) < text.length();
		StyledText[] result = (textList == null) ? new StyledText[thirdSegment ? 3 : 2] : null;
		int offset = 0;
		appendStyledText(textList, result, offset++, new StyledText(text.substring(0, index+1), textAtts));
		SimpleAttributeSet capsAtts = new SimpleAttributeSet(textAtts);
		StyleConstants.setFontSize(capsAtts, smallerFontSize);
		appendStyledText(textList, result, offset++, new StyledText(capsWord.substring(1), capsAtts));
		if (thirdSegment)
			appendStyledText(textList, result, offset++, new StyledText(text.substring(index + capsWord.length()), textAtts));
		return result;
	}

	public static StyledText[] breakupStyledText(String text, String special, AttributeSet textAtts, AttributeSet specialAtts,
		List<StyledText> textList) {
		int index = text.indexOf(special);
		int end = (index < 0 ? 0 : index + special.length());
		StyledText[] result = (textList == null ? new StyledText[3] : null);
		int offset = 0;
		if (index > 0)
			// Text before special
			appendStyledText(textList, result, offset++, new StyledText(text.substring(0, index), textAtts));
		if (index >= 0)
			// Special text
			appendStyledText(textList, result, offset++, new StyledText(special, specialAtts));
		if (end < text.length())
			// Text after special
			appendStyledText(textList, result, offset++, new StyledText(text.substring(end), textAtts));

		if (result != null && offset < result.length) {
			StyledText[] temp = result;
			result = new StyledText[offset];
			System.arraycopy(temp, 0, result, 0, offset);
		}

		return result;
	}
}
