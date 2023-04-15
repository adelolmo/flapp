package flands;


import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Interface for classes that can be transferred via input or output streams.
 * Used for saved games.
 * @author Jonathan Mann
 */
public interface Loadable {
	public String getFilename();
	public boolean loadFrom(InputStream in) throws IOException;
	public boolean saveTo(OutputStream out) throws IOException;
}
