package flands;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Loads or saves a set of Loadable objects from/to a zip-file. 
 * @author Jonathan Mann
 */
public class LoadableHandler {
	private ArrayList<Loadable> elements;

	private String filename;

	public LoadableHandler(String filename) {
		this.filename = filename;
		elements = new ArrayList<Loadable>();
	}

	public void add(Loadable l) {
		elements.add(l);
	}

	public boolean load() {
		ZipFile file;
		try {
			file = new ZipFile(filename);
		} catch (IOException ioe) {
			System.err.println("Couldn't open zip-file " + filename);
			ioe.printStackTrace();
			return false;
		}

		boolean success = true;
		for (int i = 0; i < elements.size(); i++) {
			Loadable l = elements.get(i);
			String entryFilename = l.getFilename();
			System.err.println("Looking for zip entry " + entryFilename);
			if (entryFilename == null || entryFilename.length() == 0)
				continue;
			try {
				ZipEntry entry = file.getEntry(entryFilename);
				if (entry == null)
					success = false;
				else {
					InputStream in = file.getInputStream(entry);
					if (!l.loadFrom(new BufferedInputStream(in)))
						success = false;
				}
			} catch (IOException ioe) {
				System.err.println("Couldn't open stream for sub-file "
						+ entryFilename);
				ioe.printStackTrace();
				success = false;
			}
		}

		try {
			file.close();
		} catch (IOException ioe) {
			System.err.println("Error in closing zip-file " + filename);
			ioe.printStackTrace();
		}

		return success;
	}

	public boolean save() {
		ZipOutputStream out;
		try {
			FileOutputStream fout = new FileOutputStream(filename);
			out = new ZipOutputStream(fout);
		} catch (FileNotFoundException ioe) {
			System.err.println("Couldn't open output zipfile " + filename);
			ioe.printStackTrace();
			return false;
		}

		boolean success = true;
		for (int i = elements.size() - 1; i >= 0; i--) {
			Loadable l = elements.get(i);
			ZipEntry e = new ZipEntry(l.getFilename());
			try {
				out.putNextEntry(e);
				if (!l.saveTo(out))
					success = false;
				out.closeEntry();
			} catch (IOException ioe) {
				System.err
						.println("Couldn't write sub-file " + l.getFilename());
				success = false;
			}
		}

		try {
			out.close();
		} catch (IOException ioe) {
			System.err.println("Couldn't close output zipfile " + filename);
			ioe.printStackTrace();
			success = false;
		}

		return success;
	}
}
