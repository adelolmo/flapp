package flands;

import java.io.InputStream;

/**
 * Convenient encapsulation of a unique address, being a book and a section.
 * 
 * @author Jonathan Mann
 */
public class Address {
	protected static String currentBookKey;

	public final String book;

	public final String section;

	public static Books.BookDetails getCurrentBook() {
		return Books.getCanon().getBook(getCurrentBookKey());
	}

	/** Returns the key of the current book. */
	public static String getCurrentBookKey() {
		return currentBookKey;
	}

	/**
	 * Update the key of the current book. This is used to resolve the address
	 * for any Addresses that don't specify a book.
	 * 
	 * @return <code>true</code> if the book has changed.
	 */
	public static boolean setCurrentBookKey(String book) {
		if (currentBookKey == null || !currentBookKey.equals(book)) {
			currentBookKey = book;
			return true;
		} else
			return false;
	}

	public Address(String section) {
		this(null, section);
	}

	public Address(String book, String section) {
		this.book = book;
		this.section = section;
	}

	public String getBook() {
		return (book == null ? currentBookKey : book);
	}

	public String toString() {
		return (book == null ? "" : book) + "," + section;
	}

	/*
	 * Currently unused
	 * public String toFilename() {
	 *     Books.BookDetails details = getCurrentBook();
	 *     if (details == null) {
	 *         System.out.println("Address.toFilename(): couldn't find book " + book);
	 *         return null;
	 *     }
	 * 	   return details.getPath() + "/" + section + ".xml";
	 * }
	 */

	public InputStream getStream() {
		Books.BookDetails details = Books.getCanon().getBook(getBook());
		if (details == null) {
			System.out.println("Address.getStream(): couldn't find book "
					+ book);
			return null;
		}

		return details.getInputStream(section + ".xml");
	}
}
