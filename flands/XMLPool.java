package flands;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Object that holds other objects that will be saved in XML format. Currently, these
 * objects are the ItemLists (possessions and caches), money caches and current curses.
 * The objects are loaded again via LoadableNode.
 * @author Jonathan Mann
 */
public class XMLPool implements Loadable, XMLOutput {
	private static XMLPool single = null;
	public static XMLPool createPool(Adventurer adv) {
		single = new XMLPool(adv);
		return single;
	}
	public static XMLPool getPool() {
		return single;
	}
	
	private Adventurer adv;
	private XMLPool(Adventurer adv) {
		this.adv = adv;
	}
	
	public Adventurer getAdventurer() { return adv; }
	
	public String getFilename() {
		return "saved.xml";
	}

	public boolean loadFrom(InputStream in) throws IOException {
		try {
			XMLReader reader = XMLReaderFactory.createXMLReader();
			ParserHandler handler = new ParserHandler();
			reader.setContentHandler(handler);
			reader.parse(new InputSource(new InputStreamReader(in)));
			
			return true;
		}
		catch (SAXException sax) {
			sax.printStackTrace();
		}
		return false;
	}

	public boolean saveTo(OutputStream out) throws IOException {
		PrintStream pout = new PrintStream(out);
		outputTo(pout, "", XMLOutput.OUTPUT_PROPS_DYNAMIC | XMLOutput.OUTPUT_PROPS_STATIC);
		return true;
	}
	
	public String getXMLTag() {
		return "saved";
	}
	
	public void storeAttributes(Properties atts, int flags) {}
	
	public Iterator<XMLOutput> getOutputChildren() {
		LinkedList<XMLOutput> l = new LinkedList<XMLOutput>();
		l.add(getAdventurer().getCurses());
		l.add(getAdventurer().getItems());
		// do curses before items, because cursed items are included in the item list
		// (and we don't want their curses to get removed when curses are initialised).
		for (Iterator<ItemList> i = CacheNode.getItemCaches(); i.hasNext(); ) {
			ItemList il = i.next();
			if (il.getItemCount() > 0)
				l.add(il);
		}
		for (Iterator<Map.Entry<String,Integer>> i = CacheNode.getMoneyCaches(); i.hasNext(); )
			l.add(new MoneyCacheOutput(i.next()));
		return l.iterator();
	}
	
	public void outputTo(PrintStream out, String indent, int flags) throws IOException {
		Node.output(this, out, indent, flags);
	}
	
	private static class MoneyCacheOutput implements XMLOutput {
		private Map.Entry<String,Integer> cache;
		private MoneyCacheOutput(Map.Entry<String,Integer> cache) {
			this.cache = cache;
		}
		public String getXMLTag() {
			return "moneycache";
		}

		public void storeAttributes(Properties atts, int flags) {
			atts.setProperty("name", cache.getKey());
			atts.setProperty("shards", cache.getValue().toString());
		}

		public Iterator<XMLOutput> getOutputChildren() {
			return null;
		}

		public void outputTo(PrintStream out, String indent, int flags) throws IOException {
			if (cache.getValue().intValue() > 0)
				Node.output(this, out, indent, flags);
		}
		
	}
	}
