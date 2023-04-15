package flands;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Enumeration;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

public class SectionDocumentViewer extends JDialog implements GameListener, TreeSelectionListener {
	private JTree elementTree;
	private JTable attTable;
	
	public SectionDocumentViewer(Frame parent) {
		super(parent, "Document Browser");
		init(parent);
	}
	
	public SectionDocumentViewer(Dialog parent) {
		super(parent, "Document Browser");
		init(parent);
	}
	
	private void init(Window parent) {
		FLApp.getSingle().addGameListener(this);
		getContentPane().setLayout(new BorderLayout());
		elementTree = new JTree();
		elementTree.addTreeSelectionListener(this);
		attTable = new JTable();
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				new JScrollPane(elementTree),
				new JScrollPane(attTable));
		getContentPane().add(splitPane);
		eventOccurred(new GameEvent(GameEvent.NEW_SECTION)); // immediately show current section
		pack();
		setSize(400, 400);
		setLocationRelativeTo(parent);
		splitPane.setDividerLocation(0.7);
		setVisible(true);
	}

	public void eventOccurred(GameEvent evt) {
		if (evt.getID() != GameEvent.NEW_SECTION) return;
		SectionDocument doc = FLApp.getSingle().getCurrentDocument();
		if (doc == null) return;
		setDocument(doc);
	}
	
	public void setDocument(Document doc) {
		Element[] rootElements = doc.getRootElements();
		if (rootElements.length > 1) {
			System.out.println("More than one root element in SectionDocument!");
			for (int e = 0; e < rootElements.length; e++)
				System.out.println("Root " + (e+1) + ": " + rootElements[e]);
		}
		if (rootElements.length > 0 && rootElements[0] instanceof TreeNode)
			elementTree.setModel(new DefaultTreeModel((TreeNode)rootElements[0]));
	}
	
	private TableModel makeTableModel(String content, AttributeSet atts) {
		DefaultTableModel model = new DefaultTableModel();
		model.addColumn("Attribute");
		model.addColumn("Value");
		if (content != null)
			model.addRow(new Object[] {"[Content]", convertString(content)});
		for (Enumeration<?> a = atts.getAttributeNames(); a.hasMoreElements(); ) {
			Object att = a.nextElement();
			Object val = atts.getAttribute(att);
			if (val instanceof String) val = convertString((String)val);
			model.addRow(new Object[] {att, val});
		}
		return model;
	}

	private static String convertString(String str) {
		StringBuffer sb = new StringBuffer();
		for (int c = 0; c < str.length(); c++) {
			switch (str.charAt(c)) {
			case '\t': sb.append("\\t"); break;
			case '\n': sb.append("\\n"); break;
			default: sb.append(str.charAt(c));
			}
		}
		return sb.toString();
	}
	
	public void valueChanged(TreeSelectionEvent evt) {
		Object selectedNode = evt.getPath().getLastPathComponent();
		if (selectedNode instanceof Element) {
			Element node = (Element)selectedNode;
			AttributeSet atts = node.getAttributes();
			String textContent = null;
			try {
				textContent = node.getDocument().getText(node.getStartOffset(),
						node.getEndOffset() - node.getStartOffset());
			}
			catch (BadLocationException ble) {
				System.out.println("Bad document position: " + ble.offsetRequested());
			}
			attTable.setModel(makeTableModel(textContent, atts));
		}
	}
	
	private static final boolean method = true;
	public static void main(String args[]) {
		String htmlText = "<html><body><p>Para 1</p>" +
			"<p><a href=\"http://www.abc.net.au\">This is <b>a link</b></a>.</p>" +
			"<table><tr><th>Header 1</th><th>Header 2</th></tr><tr><td>Cell 1</td><td>Cell 2</td></tr></table></body></html>";
		JFrame frame = new JFrame("HTML Doc");
		final JEditorPane tp;
		if (method) {
			tp = new JEditorPane("text/html", htmlText);
		}
		else {
			tp = new JEditorPane();
			DefaultStyledDocument doc = new DefaultStyledDocument();
			try {
				doc.insertString(0, "This is the first paragraph.\nAnd this is the second.", null);
			}
			catch (BadLocationException ble) {}
			tp.setDocument(doc);
		}
		frame.getContentPane().add(new JScrollPane(tp));
		JButton refreshButton = new JButton("Refresh");
		JPanel buttonPane = new JPanel();
		buttonPane.add(refreshButton);
		frame.getContentPane().add(buttonPane, BorderLayout.SOUTH);
		frame.pack();
		frame.setVisible(true);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				System.exit(0);
			}
		});
		
		final SectionDocumentViewer viewer = new SectionDocumentViewer(frame);
		viewer.setDocument(tp.getDocument());
		
		refreshButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				viewer.setDocument(tp.getDocument());
			}
		});
	}
}
