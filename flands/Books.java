package flands;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.AbstractListModel;

/*
 * Tracks the available books. The application goes looking for the appropriate
 * zips or directories (based on books.ini).
 * @author Jonathan Mann
 */
public class Books {
	public static class BookDetails implements Comparable {
		private static final int ERROR_TYPE = -2;
		private static final int MISSING_TYPE = -1;
		private static final int DIR_TYPE = 0;
		private static final int ZIP_TYPE = 1;
		
		private int[] pathTypes;
		private String[] paths;
		
		private String key;
		private String title;
		private boolean foundSectionRange = false;
		private int lowestSection, highestSection;
		private Properties bookProps = null;

		private BookDetails(String key, String title) {
			this.key = key;
			this.title = title;
			paths = new String[0];
		}
		
		public BookDetails(String key, String title, String path) {
			this(key, title);
			
			if (path != null)
				paths = path.split(",");
			else
				paths = new String[0];
			pathTypes = new int[paths.length];
			
			boolean gotAnyPath = false;
			for (int p = 0; p < paths.length; p++) {
				if (paths[p] == null) {
					pathTypes[p] = MISSING_TYPE;
					System.out.println("Book " + toString() + " has no path");
				}
				else {
					File f = new File(paths[p]);
					if (f.exists())
					{
						if (f.isDirectory())
							pathTypes[p] = DIR_TYPE;
						else
						{
							try {
								ZipFile zf = new ZipFile(paths[p]);
								pathTypes[p] = ZIP_TYPE;
								zf.close();
							}
							catch (IOException e) {
								System.out.println("Couldn't open zip-file " + paths[p]);
								pathTypes[p] = ERROR_TYPE;
							}
						}
					}
					else
						pathTypes[p] = MISSING_TYPE;
				}
				
				if (pathTypes[p] >= 0)
					gotAnyPath = true;
			}
			
			if (gotAnyPath) {
				if (!fileExists("book.ini"))
					System.out.println("Couldn't find book.ini file for book " + key);
			}

			/*
			if (path == null) {
				pathType = MISSING_TYPE;
				System.out.println("Book " + toString() + " has no path");
			}
			else {
				File f = new File(path);
				if (f.exists())
				{
					if (f.isDirectory())
						pathType = DIR_TYPE;
					else
					{
						try {
							new ZipFile(path);
							pathType = ZIP_TYPE;
						}
						catch (IOException e) {
							System.out.println("Couldn't open zip-file " + path);
							pathType = ERROR_TYPE;
						}
					}
				}
				else
					pathType = MISSING_TYPE;
			}
			if (pathType >= 0) {
				if (!fileExists("book.ini"))
					pathType = MISSING_TYPE;
			}
			*/
		}

		public boolean fileExists(String name) {
			for (int p = 0; p < paths.length; p++) {
				switch (pathTypes[p]) {
				case DIR_TYPE:
					if (new File(paths[p] + "/" + name).exists())
						return true;
					continue;
					//return new File(path + "/" + name).exists();
				case ZIP_TYPE:
					try {
						ZipFile zf = new ZipFile(paths[p]);
						ZipEntry entry = zf.getEntry(name);
						zf.close();
						if (entry != null)
							return true;
						//return (entry != null);
					}
					catch (IOException ioe) {
						System.out.println("Error reading zip-file " + paths[p]);
						//return false;
					}
				}
			}
			return false;
		}
		
		public InputStream getInputStream(String name) {
			for (int p = 0; p < paths.length; p++) {
				switch (pathTypes[p]) {
				case DIR_TYPE:
					try {
						return new FileInputStream(paths[p] + "/" + name);
					}
					catch (FileNotFoundException e) {
						//e.printStackTrace();
					}
					break;
				case ZIP_TYPE:
					try {
						ZipFile zf = new ZipFile(paths[p]);
						ZipEntry entry = zf.getEntry(name);
						if (entry != null)
							return zf.getInputStream(entry);
					}
					catch (IOException e) {
						//System.out.println("Error reading zip-file " + paths[p]);
					}
				}
			}
			System.out.println("Couldn't find file " + name);
			
			return null;
		}
		
		public boolean hasBook() {
			return (fileExists("book.ini"));
			//return (pathType >= DIR_TYPE);
		}

		public String getKey() { return key; }
		private int getKeyNumber() {
			try {
				return Integer.parseInt(key);
			}
			catch (NumberFormatException nfe) { return -1; }
		}
		public String getTitle() { return title; }
		public int getLowestSection() {
			findSectionRange();
			return lowestSection;
		}
		public int getHighestSection() {
			findSectionRange();
			return highestSection;
		}
		
		private String[] getAllFilenames() {
			Vector<String> allFiles = new Vector<String>();
			for (int p = 0; p < paths.length; p++) {
				switch (pathTypes[p]) {
				case DIR_TYPE:
					File dir = new File(paths[p]);
					String[] contents = dir.list();
					for (int f = 0; f < contents.length; f++)
						allFiles.add(contents[f]);
					break;
				case ZIP_TYPE:
					try {
						ZipFile zf = new ZipFile(paths[p]);
						//String[] names = new String[zf.size()];
						//int i = 0;
						for (Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements(); )
							allFiles.add(e.nextElement().getName());
							//names[i++] = e.nextElement().getName();
						//return names;
						zf.close();
					}
					catch (IOException e) {
						System.out.println("Error reading zip-file " + paths[p]);
					}
				}
			}
			return allFiles.toArray(new String[allFiles.size()]);
		}
		
		private void findSectionRange() {
			if (foundSectionRange) return;
			foundSectionRange = true;
			String[] files = getAllFilenames();
			if (files != null) {
				int min = Integer.MAX_VALUE;
				int max = Integer.MIN_VALUE;
				for (int i = 0; i < files.length; i++) {
					if (files[i].endsWith(".xml") && Character.isDigit(files[i].charAt(0))) {
						try {
							int section = Integer.parseInt(files[i].substring(0, files[i].length() - 4));
							if (section < min)
								min = section;
							else if (section > max)
								max = section;
						}
						catch (NumberFormatException nfe) {}
					}
				}
				System.out.println("Book " + key + ": lowest section=" + min + ",highest=" + max);
				lowestSection = min;
				highestSection = max;
			}
		}
		
		public String[] getOfficialCodewords() {
			String codewordsStr = getProps().getProperty("Codewords");
			if (codewordsStr == null)
				return new String[0];
			String[] codewords = codewordsStr.split(",");
			for (int c = 0; c < codewords.length; c++)
				codewords[c] = codewords[c].trim();
			Arrays.sort(codewords);
			return codewords;
		}
		
		private Properties getProps() {
			if (bookProps == null) {
				InputStream propStream = getInputStream("book.ini");
				if (propStream != null) {
					bookProps = new Properties();
					try {
						bookProps.load(propStream);
					}
					catch (IOException e) {
						System.out.println("Error in reading book definition file book.ini");
						e.printStackTrace();
					}
				}
				else
					return new Properties();
			}
			return bookProps;
		}
		
		public String getMapFilename() {
			return getProps().getProperty("Map");
		}
		public String getMapTitle() {
			return getProps().getProperty("Map.Title");
		}
		public String getDeathSection() {
			return getProps().getProperty("Death");
		}
		public String getIconFilename() {
			return getProps().getProperty("Icon");
		}
		public String getRequiredRules() {
			return getProps().getProperty("Rules");
		}

		public int compareTo(Object o) {
			try {
				BookDetails bd = (BookDetails)o;
				int bookNumber = getKeyNumber(), otherBookNumber = bd.getKeyNumber();
				if (bookNumber != -1 && otherBookNumber != -1)
					return bookNumber - otherBookNumber;
				else if (bookNumber != -1)
					return -1;
				else if (otherBookNumber != -1)
					return 1;
				else
					return getKey().compareTo(bd.getKey());
			}
			catch (ClassCastException cce) {
				return -1;
			}
		}
		
		public String toString() {
			return "Book[" + key + "," + paths[0] + "," + title + "]";
		}
	}

	/**
	 * List model of the available books.
	 */
	public static class BookListModel extends AbstractListModel {
		private ArrayList<BookDetails> books;
		public BookListModel() {
			SortedSet<BookDetails> sortedBooks = new TreeSet<BookDetails>();
			Books canon = getCanon();
			for (Iterator<BookDetails> i = canon.getAllBooks(); i.hasNext(); ) {
				BookDetails book = i.next();
				if (book.hasBook())
					sortedBooks.add(book);
			}
			
			books = new ArrayList<BookDetails>();
			for (Iterator<BookDetails> i = sortedBooks.iterator(); i.hasNext(); )
				books.add(i.next());
		}
		
		public int getSize() {
			return books.size();
		}
	
		public BookDetails getBook(int index) {
			return books.get(index);
		}
		
		public Object getElementAt(int index) {
			BookDetails book = getBook(index);
			return book.getKey() + ": " + book.getTitle();
		}
	}

	public static BookDetails MissingBook = new BookDetails("0", "MISSING") {
		public boolean hasBook() { return false; }
	};

	private Map<String,BookDetails> bookMap;
	public Books() {
		bookMap = new HashMap<String,BookDetails>();
	}

	public BookDetails getBook(String key) {
		BookDetails book = bookMap.get(key);
		if (book == null)
			return MissingBook;
		else
			return book;
	}
	public void addBook(BookDetails book) {
		bookMap.put(book.key, book);
	}

	public Iterator<BookDetails> getAllBooks() {
		return bookMap.values().iterator();
	}

	public String[] getAvailableKeys() {
		Set<String> availableBooks = new HashSet<String>();
		for (Iterator<BookDetails> i = getAllBooks(); i.hasNext(); ) {
			BookDetails b = i.next();
			if (b.hasBook())
				availableBooks.add(b.getKey());
		}
		return availableBooks.toArray(new String[availableBooks.size()]);
	}

	private static Books canon = null;
	public static Books getCanon() {
		if (canon == null) {
			String listingFile = "books.ini";
			try {
				Properties props = new Properties();
				props.load(new FileInputStream(listingFile));
				String[] keys = props.getProperty("Books").split(",");
				Books books = new Books();
				for (int k = 0; k < keys.length; k++) {
					String title = props.getProperty(keys[k] + ".Title");
					String path = props.getProperty(keys[k] + ".Path");
					books.addBook(new BookDetails(keys[k], title, path));
				}
				canon = books;
			} catch (FileNotFoundException e) {
				System.out.println("Couldn't find book listing file '" + listingFile + "'");
				e.printStackTrace();
			} catch (IOException e) {
				System.out.println("Error in reading book listing file '" + listingFile + "'");
				e.printStackTrace();
			}
			
			if (canon == null) {
				canon = new Books();
				canon.addBook(new BookDetails("1", "The War-Torn Kingdom", "book1"));
				canon.addBook(new BookDetails("2", "Cities of Gold and Glory", "book2"));
				canon.addBook(new BookDetails("3", "Over the Blood-Dark Sea", "book3"));
				canon.addBook(new BookDetails("4", "Devils & Howling Darkness", "book4"));
				canon.addBook(new BookDetails("5", "The Court of Hidden Faces", "book5"));
				canon.addBook(new BookDetails("6", "Lords of the Rising Sun", "book6"));
				canon.addBook(new BookDetails("7", "The Serpent-King\u2019s Domain", "book7"));
				canon.addBook(new BookDetails("8", "The Lone and Level Sands", "book8"));
				canon.addBook(new BookDetails("9", "The Isle of a Thousand Spires", "book9"));
				canon.addBook(new BookDetails("10", "Legions of the Labyrinth", "book10"));
				canon.addBook(new BookDetails("11", "The City in the Clouds", "book11"));
				canon.addBook(new BookDetails("12", "Into The Underworld", "book12"));
			}
		}
		return canon;
	}
	
	public static void main(String args[]) {
		Books books = getCanon();
		for (int a = 0; a < args.length; a++) {
			Books.BookDetails book = books.getBook(args[a]);
			if (book.hasBook()) {
				String[] codewords = book.getOfficialCodewords();
				System.out.println("Official codewords for book " + args[a] + ": " + codewords.length);
				for (int c = 0; c < codewords.length; c++)
					System.out.println((c+1) + ": " + codewords[c]);
			}
		}
	}
}