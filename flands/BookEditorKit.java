package flands;


import javax.swing.text.Element;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

/**
 * Simple wrapper of the standard editor kit, which creates appropriate views for
 * document elements; in our case, the elements can specify the view required.
 * @author Jonathan Mann
 */
public class BookEditorKit extends StyledEditorKit {
	public String getContentType() { return "text/xml"; }

	/**
	 * Our ViewFactory wraps the existing ViewFactory.
	 * Any Element with the 'viewType' attribute will have an appropriate View created by
	 * {@link Node#createNode}; otherwise, or if this is null,
	 * the wrapped factory is called to create the usual View.
	 * @see Node#createViewFor
	 */
	private static class BookViewFactory implements ViewFactory {
		private ViewFactory innerFactory;
		public BookViewFactory(ViewFactory inner) {
			innerFactory = inner;
		}

		public View create(Element e) {
			View nodeView = Node.createViewFor(e);
			if (nodeView != null)
				return nodeView;
			View defaultView = innerFactory.create(e);
			//System.out.println("Wrapped Factory created " + defaultView + " for " + e);
			/*
			if (defaultView.getClass().equals(javax.swing.text.BoxView.class)) {
				// This is currently happening for the overall view - ie. for the root node
				System.out.println("Wrapped Factory creating local BoxView for " + e);
				defaultView = new BoxView(e, ((javax.swing.text.BoxView)defaultView).getAxis());
			}
			*/
			return defaultView;
		}
	}

	public ViewFactory getViewFactory() {
		ViewFactory usualFactory = super.getViewFactory();
		return new BookViewFactory(usualFactory);
	}
}