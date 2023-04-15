package flands;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Wrapper for SectionNodes, that lets us store and retrieve one from any filename.
 * Using this we can define a 'current' and 'previous' section and restore them
 * without needing to know the section numbers.
 * @author Jonathan Mann
 */
public class LoadableSection implements Loadable {
	private String filename;
	private SectionNode node;
	private SectionDocument document;
	private String book, section;

	public LoadableSection(String filename) {
		this(filename, null);
	}
	
	public LoadableSection(String filename, SectionNode node) {
		this.filename = filename;
		this.node = node;
	}
	
	public String getFilename() {
		return filename;
	}

	/** Get the book key of the section that has been read. */
	public String getBook() { return book; }
	/** Get the section name of the section that has been read. */
	public String getSection() { return section; }
	
	private static ParserHandler handler = null;
	private static ParserHandler getHandler() {
		if (handler == null) {
			// Create a handler that won't begin execution when it's done
			handler = new ParserHandler(false);
		}
		return handler;
	}
	
	public boolean loadFrom(InputStream in) throws IOException {
		// First, load the static details of the required section
		DataInputStream din = new DataInputStream(in);
		book = din.readUTF();
		section = din.readUTF();
		Address address = new Address(book, section);
		
		InputStream sectionStream = address.getStream();
		try {
			ParserHandler handler = getHandler();
			handler.setBook(book);
			XMLReader reader = XMLReaderFactory.createXMLReader();
			reader.setContentHandler(handler);
			reader.parse(new InputSource(new InputStreamReader(sectionStream)));
		}
		catch (SAXException e) {
			e.printStackTrace();
			throw new IOException("Couldn't load section " + filename);
		}
		
		node = (SectionNode)getHandler().getRootNode();
		((SectionNode)node).setSection(section);
		document = getHandler().getDocument();

		// Then load the dynamic details
		return node.loadFrom(in);
	}

	public boolean saveTo(OutputStream out) throws IOException {
		DataOutputStream dout = new DataOutputStream(out);
		dout.writeUTF(node.getBook());
		dout.writeUTF(node.getSection());
		return node.saveTo(out);
	}
	
	public SectionNode getNode() { return node; }
	public SectionDocument getDocument() { return document; }
}
