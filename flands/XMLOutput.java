package flands;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Properties;

/**
 * Interface for objects wanting to store themselves in an XML format.
 * Used primarily to store Effects (as part of curses or items).
 * 
 * @author Jonathan Mann
 */
public interface XMLOutput {
	public static final int OUTPUT_PROPS_STATIC = 1 << 0;
	public static final int OUTPUT_PROPS_DYNAMIC = 1 << 1;
	public String getXMLTag();
	public void storeAttributes(Properties atts, int flags);
	public Iterator<XMLOutput> getOutputChildren();
	public void outputTo(PrintStream out, String indent, int flags) throws IOException;
}
