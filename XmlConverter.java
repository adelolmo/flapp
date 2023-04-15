import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.swing.JFileChooser;


public class XmlConverter {
	public boolean anyConversion = false;
	public String convertXml(Reader reader) throws IOException {
	    // TODO: bug: code only works for Basic Multilingual Plane
		anyConversion = false;
	    StringBuilder out = new StringBuilder();
	    int ch;
	    while ((ch = reader.read()) != -1) {
	    	process((char)ch, out);
	    }
	    if (ampersand) {
	    	ampersand = false;
	    	out.append("&amp;");
	    	anyConversion = true;
	    }
	    
	    String result = out.toString();
	    if (!result.startsWith("<?")) {
	    	// Output is UTF-8 encoding
	    	result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + result;
	    	anyConversion = true;
	    }
	    return result;
	}
	
	private boolean ampersand = false;
	private void process(char codePoint, StringBuilder out) {
		if (ampersand) {
			ampersand = false;
			if (codePoint == '#' ||
				(codePoint >= 'a' && codePoint <= 'z') ||
				(codePoint >= 'A' && codePoint <= 'Z')) {
				out.append('&');
			}
			else {
				anyConversion = true;
				out.append("&amp;");
			}
		}
		
		switch (codePoint) {
		case '&':
			// Delay processing until we see the next char
			ampersand = true;
			break;
			/*
		case '"':
			out.append("&quot;");
			break;
		case '\'':
			out.append("&apos;");
			anyConversion = true;
			break;
			*/
			/*
		case '<':
			out.append("&lt;");
			anyConversion = true;
			break;
		case '>':
			out.append("&gt;");
			anyConversion = true;
			break;
			*/
		default:
			if (codePoint > 127) {
				// Special character - encode as number
				out.append("&#");
				out.append(Integer.toString(codePoint));
				out.append(";");
				anyConversion = true;
			}
			else
				out.append(codePoint);
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Safe XML Converter");
		chooser.setMultiSelectionEnabled(true);
		if (chooser.showDialog(null, "Convert") == JFileChooser.APPROVE_OPTION) {
			File[] files = chooser.getSelectedFiles();
			for (int f = 0; f < files.length; f++) {
				try {
					Reader read = new InputStreamReader(new FileInputStream(files[f]));
					// InputStreamReader constructor could take charset name
					// as additional parameter - but we're using default
					read = new BufferedReader(read);
					XmlConverter converter = new XmlConverter();
					String converted = converter.convertXml(read);
					read.close();
					if (converter.anyConversion) {
						if (files[f].delete()) {
							FileWriter write = new FileWriter(files[f]);
							write.write(converted);
							write.close();
							System.out.println(files[f] + ": successfully converted");
						}
						else {
							System.err.println(files[f] + ": couldn't delete old file");
						}
					}
					else {
						System.out.println(files[f] + ": no conversion required");
					}
				}
				catch (IOException ioe) {
					System.err.println(files[f] + ": " + ioe);
				}
			}
		}
	}
}
