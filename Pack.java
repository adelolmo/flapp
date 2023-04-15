import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Custom class that will automatically collect all resources for a distribution.
 * See Pack.bat and PackSource.bat for use.
 * @author Jonathan Mann
 */
public class Pack implements FilenameFilter {
	private static String zipName = "JaFL.zip";
	private static final String JarName = "flands.jar";
	private static final String LocalFiles[] =
		{
			"README.txt",
			"JaFL.bat",
			JarName,
			"user.ini",
			"icon.jpg",
			"global.jpg",
			"Rules.xml",
			"QuickRules.xml",
			"books.ini",
			"jafl.ico"
		};
	
	// An array of each book directory to include, along with the images that must
	// be included (all other non-JPGs will be included regardless)
	private static final String BookDetails[][] =
		{
			{"book1", "Forest of the Forsaken.JPG", "Sokara.JPG"},
			{"book2", "Golnir.JPG"},
			{"book3", "Map of Bazalek Isle.JPG", "Violet Ocean.JPG"},
			{"book4", "Great Steppes.JPG"},
			{"book5", "The Black Diptych.JPG", "Uttaku.JPG"},
			{"book6", "Akatsurai.JPG"}
		};

	private String extension;
	public boolean accept(File dir, String name) {
		return name.endsWith(extension);
	}

	static byte[] buffer = new byte[4096];
	public static void addFile(ZipOutputStream zout, String filepath, String entrypath)
	throws FileNotFoundException, IOException {
		ZipEntry entry = new ZipEntry(entrypath);
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(filepath));
		zout.putNextEntry(entry);
		int read;
		while ((read = in.read(buffer, 0, buffer.length)) >= 0)
			zout.write(buffer, 0, read);
		zout.closeEntry();
		in.close();		
	}
	
	public static void main(String args[]) {
		boolean zipbooks = true;
		if (args.length > 0) {
			if (args[0].equals("d"))
				zipbooks = false;
			else if (args[0].equals("z"))
				zipbooks = true;
			else
				System.out.println("Usage: Pack [{d|z} [zip-name]]");
			
			if (args.length > 1)
				zipName = args[1];
		}
		
		try {
			FileOutputStream fout = new FileOutputStream(zipName);
			ZipOutputStream zout = new ZipOutputStream(new BufferedOutputStream(fout));
			zout.setLevel(9);
			for (int i = 0; i < LocalFiles.length; i++) {
				System.out.println("File " + i + ": " + LocalFiles[i]);
				addFile(zout, LocalFiles[i], LocalFiles[i]);
			}
			
			for (int d = 0; d < BookDetails.length; d++) {
				File bookDir = new File(BookDetails[d][0]);
				if (bookDir.exists() && bookDir.isDirectory()) {
					String[] xmlFiles = bookDir.list(/*new Pack(".xml")*/);
					
					String bookZip = BookDetails[d][0] + ".zip";
					ZipOutputStream bookOut;
					String zipPathPrefix;
					if (zipbooks) {
						bookOut = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(bookZip)));
						//bookOut.setLevel(9);
						zipPathPrefix = "";
					}
					else {
						bookOut = zout;
						zipPathPrefix = BookDetails[d][0] + "/";
					}

					int count = 0;
					for (int i = 0; i < xmlFiles.length; i++) {
						String lowercase = xmlFiles[i].toLowerCase();
						if (xmlFiles[i].endsWith(".jpg")) {
							// Check if it's one of the files we're meant to include
							boolean matched = false;
							for (int f = 1; f < BookDetails[d].length; f++)
								if (lowercase.equals(BookDetails[d][f].toLowerCase())) {
									matched = true;
									break;
								}
							if (!matched)
								continue;
						}
						else if (xmlFiles[i].startsWith("."))
							continue; // hidden info, probably SVN-related
						count++;
						addFile(bookOut, BookDetails[d][0] + "/" + xmlFiles[i], zipPathPrefix + xmlFiles[i]);
					}
					System.out.println("Written " + count + " files from " + BookDetails[d][0]);
					
					if (zipbooks) {
						// Close the book.zip
						bookOut.close();
						
						// Write this book.zip file into the main zip
						addFile(zout, bookZip, bookZip);
						
						// and delete the book-zip
						new File(bookZip).delete();
					}
				}
			}
			zout.close();
			System.out.println("Closed ZIP-file");

			System.exit(0);
		}
		catch (FileNotFoundException fnfe) {
			System.out.println("Error in creating file: " + fnfe);
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}

		System.exit(1);
	}
}
