package flands;


import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Insets;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

/**
 * Pop-up window to show an image, with scrollbars used as necessary.
 * Used to display the maps.
 * @author Jonathan Mann
 */
public class ImageWindow extends JDialog {
	private JLabel imageLabel;
	
	public ImageWindow(Frame f, Image i, String title) {
		super(f, title);
		init(i);
	}
	public ImageWindow(Dialog d, Image i, String title) {
		super(d, title);
		init(i);
	}

	private void init(Image i) {
		imageLabel = new JLabel(new ImageIcon(i));
		JScrollPane imagePane = new JScrollPane(imageLabel);
		imagePane.setWheelScrollingEnabled(true);
		imagePane.getHorizontalScrollBar().setUnitIncrement(5);
		imagePane.getVerticalScrollBar().setUnitIncrement(5);
		getContentPane().add(imagePane);
		pack();

		// but packing doesn't take into account the actual screen size
		// (minus borders such as the Windows Taskbar)
		Dimension screenSize = getToolkit().getScreenSize();
		Insets screenInsets = getToolkit().getScreenInsets(getGraphicsConfiguration());
		setSize(Math.min(getWidth(), screenSize.width - (screenInsets.left + screenInsets.right)),
			Math.min(getHeight(), screenSize.height - (screenInsets.top + screenInsets.bottom)));
		setLocationRelativeTo(getOwner());
	}

	public void setImage(Image i, String title) {
		imageLabel.setIcon(new ImageIcon(i));
		setTitle(title);
	}
	
	public static void main(String args[]) {
		Image i;
		if (args.length == 0) {
			System.out.println("Usage: ImageWindow <image-filename>");
			return;
		}

		javax.swing.JFrame jf = new javax.swing.JFrame("Test");
		jf.setSize(100, 100);
		jf.setVisible(true);
		jf.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);

		//i = jf.getToolkit().createImage(args[0]);
		i = jf.getToolkit().createImage(args[0]);
		ImageWindow window = new ImageWindow(jf, i, args[0]);
		window.setVisible(true);
	}
}
