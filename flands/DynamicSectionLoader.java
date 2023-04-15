package flands;


import java.util.LinkedList;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Handles loading a saved section (and its working state) from XML.
 * @author Jonathan Mann
 */
public class DynamicSectionLoader implements ContentHandler {
	private static class StackEntry {
		private Node n;
		private int childIndex;
		private StackEntry(Node n) {
			this.n = n;
			childIndex = 0;
		}
	}

	private LinkedList<StackEntry> stack = new LinkedList<StackEntry>();
	private SectionNode rootNode;
	public DynamicSectionLoader(SectionNode node) {
		rootNode = node;
	}

	/* *******************************
	 * Relevant ContentHandler methods
	 ******************************* */
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		Node childNode;
		if (stack.isEmpty()) {
			childNode = rootNode;
		}
		else {
			StackEntry entry = stack.getLast();
			childNode = entry.n.getChild(entry.childIndex++);
		}
		childNode.loadProperties(atts);
		stack.addLast(new StackEntry(childNode));
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		stack.removeLast();
	}

	/* *********************************
	 * Irrelevant ContentHandler methods
	 ********************************* */
	public void setDocumentLocator(org.xml.sax.Locator l) {
		//System.out.println("setDocumentLocator(" + l + ")");
	}
	public void startDocument() throws SAXException {
		//System.out.println("startDocument()");
	}
	public void endDocument() throws SAXException {
		//System.out.println("endDocument()");
	}
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		//System.out.println("startPrefixMapping(" + prefix + "," + uri + ")");
	}
	public void endPrefixMapping(String prefix) throws SAXException {
		//System.out.println("endPrefixMapping(" + prefix + ")");
	}
	public void characters(char[] ch, int start, int length) throws SAXException {
		//System.out.println("characters(" + length + ")");
	}
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
		//System.out.println("ignorableWhitespace('" + new String(ch) + "'," + start + "," + length + ")");
	}
	public void processingInstruction(String target, String data) throws SAXException {
		//System.out.println("processingInstruction(" + target + "," + data + ")");
	}
	public void skippedEntity(String name) throws SAXException {
		//System.out.println("skippedEntity(" + name + ")");
	}
}
