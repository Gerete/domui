package to.etc.domui.component.layout;

import to.etc.domui.dom.html.*;

/**
 * Pop-in panel and link functionality. It must be extended to be used; when used it
 * registers itself as a popin, causing it to disappear when another popin is registered.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Dec 6, 2009
 */
public class PopInPanel extends Div {
	private final String			m_ttl;

	private ButtonBar m_buttonBar;

	private Div m_content;

	static public enum Bar {
		LEFT, TOP, RIGHT, BOTTOM
	}

	private Bar m_barLocation;

	public PopInPanel(final String ttl) {
		m_ttl = ttl;
		setCssClass("ui-popin");
	}

	public PopInPanel(final String ttl, Bar loc) {
		m_ttl = ttl;
		setCssClass("ui-popin");
		m_barLocation = loc;
	}

	@Override
	final public void createContent() throws Exception {
		m_content = null;
		Span	ttl = new Span();
		ttl.setCssClass("ui-popin-ttl");
		add(ttl);
		ttl.add(m_ttl);
		Div d = new Div();

		if(m_barLocation == null) {
			add(d);
		} else {
			m_buttonBar = new ButtonBar(m_barLocation == Bar.LEFT || m_barLocation == Bar.RIGHT);
			switch(m_barLocation){
				default:
					throw new IllegalStateException(m_barLocation + ": dunno?");
				case BOTTOM:
				case TOP:
					add(d);
					d.add(m_buttonBar);
					break;
				case LEFT:
				case RIGHT:
					Table t = new Table();
					add(t);
					TBody b = t.addBody();
					TD l = b.addRowAndCell();
					TD r = b.addCell();

					if(m_barLocation == Bar.LEFT) {
						l.add(m_buttonBar);
						l.setWidth("1%");
						l.setNowrap(true);
						l.setValign(TableVAlign.TOP);

						r.add(d);
						r.setWidth("99%");
						r.setNowrap(true);
						r.setValign(TableVAlign.TOP);
					} else {
						l.add(d);
						l.setWidth("99%");
						l.setNowrap(true);
						l.setValign(TableVAlign.TOP);

						r.add(m_buttonBar);
						r.setWidth("1%");
						r.setNowrap(true);
						r.setValign(TableVAlign.TOP);
					}
					break;
			}
		}
		delegateTo(d);
		createPopin();
	}

	public ButtonBar getButtonBar() {
		if(m_buttonBar == null) {
			m_buttonBar = new ButtonBar();
			add(m_buttonBar);
		}
		return m_buttonBar;
	}

	@Override
	public void onAddedToPage(Page p) {
		super.onAddedToPage(p);
		p.setPopIn(this);
	}

	@Override
	public void onRemoveFromPage(Page p) {
		super.onRemoveFromPage(p);
		p.setPopIn(null);
	}

	public void createPopin() throws Exception {
	}

	public void close() {
		getPage().clearPopIn();
	}

}
