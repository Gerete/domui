package to.etc.domui.dom;

import to.etc.domui.component.misc.*;
import to.etc.domui.dom.header.*;
import to.etc.domui.dom.html.*;
import to.etc.domui.server.*;
import to.etc.util.*;

/**
 * Visits the node tree in such a way that a valid html document is generated.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Aug 17, 2007
 */
public class FullHtmlRenderer extends NodeVisitorBase {
	/** The thingy responsible for rendering the tags, */
	private HtmlRenderer m_tagRenderer;

	private IBrowserOutput m_o;

	private IRequestContext m_ctx;

	private Page m_page;

	private boolean m_xml;

	private StringBuilder m_createJS = new StringBuilder();

	public FullHtmlRenderer(HtmlRenderer tagRenderer, IBrowserOutput o) {
		m_tagRenderer = tagRenderer;
		m_o = o;
		// 20090701 jal was ADDS which is WRONG - by definition a FULL render IS a full renderer... This caused SELECT tags to be rendered with domui_selected attributes instead of selected attributes.
		setRenderMode(HtmlRenderMode.FULL);
	}

	private HtmlRenderer getTagRenderer() {
		// 20090701 jal was ADDS which is WRONG - by definition a FULL render IS a full renderer... This caused SELECT tags to be rendered with domui_selected attributes instead of selected attributes.
		// 20091002 jal removed, make callers specify render mode...
		//		m_tagRenderer.setRenderMode(HtmlRenderMode.FULL); // All nodes from the full renderer are NEW by definition.
		return m_tagRenderer;
	}

	public HtmlRenderMode getMode() {
		return m_tagRenderer.getMode();
	}

	public void setRenderMode(HtmlRenderMode m) {
		m_tagRenderer.setRenderMode(m);
	}

	public boolean isXml() {
		return m_xml;
	}

	public void setXml(boolean xml) {
		m_xml = xml;
	}

	public IBrowserOutput o() {
		return m_o;
	}

	public IRequestContext ctx() {
		return m_ctx;
	}

	public Page page() {
		return m_page;
	}

	@Override
	public void visitNodeBase(NodeBase n) throws Exception {
		n.build();
		n.visit(getTagRenderer());
		if(n.getCreateJS() != null)
			m_createJS.append(n.getCreateJS());
		if(!(n instanceof TextNode)) {
			if(m_xml)
				getTagRenderer().renderEndTag(n);
			else
				m_o.dec(); // 20080626 img et al does not dec()...
		}
		n.clearDelta();
		checkForFocus(n);
	}

	/**
	 * Overridden because this is a NodeBase node which MUST be terminated with a /div, always.
	 * @see to.etc.domui.dom.html.NodeVisitorBase#visitLiteralXhtml(to.etc.domui.component.misc.LiteralXhtml)
	 */
	@Override
	public void visitLiteralXhtml(LiteralXhtml n) throws Exception {
		visitNodeBase(n); // Handle most thingies we need to do,
		if(!m_xml) {
			//-- In HTML mode we MUST end this tag, and we need to inc() because the visitNodeBase() has decremented..
			m_o.inc();
			getTagRenderer().renderEndTag(n); // Force close the tag in HTML mode.
		}
	}

	/**
	 * Overridden to fix bug 627; this prevents embedding content in textarea and renders the value as
	 * an attribute.
	 *
	 * @see to.etc.domui.dom.html.NodeVisitorBase#visitTextArea(to.etc.domui.dom.html.TextArea)
	 */
	@Override
	public void visitTextArea(TextArea n) throws Exception {
		if(getMode() == HtmlRenderMode.FULL) { // In FULL mode render content inside textarea goddamnit
			visitNodeContainer(n);
			return;
		}

		visitNodeBase(n);
		o().setIndentEnabled(true); // jal 20091002 indent when rendering js attribute
		//		getTagRenderer().renderEndTag(n);
	}

	@Override
	public void visitNodeContainer(NodeContainer n) throws Exception {
		n.build();
		boolean indena = o().isIndentEnabled(); // jal 20090903 Save indenting request....
		n.visit(getTagRenderer()); // Ask base renderer to render tag
		if(n.getCreateJS() != null)
			m_createJS.append(n.getCreateJS());
		visitChildren(n);
		getTagRenderer().renderEndTag(n);
		o().setIndentEnabled(indena); // And restore indenting if tag handler caused it to be cleared.
		n.clearDelta();
		checkForFocus(n);
	}

	/*
	 * Handle default input focus: if no focus is set AND this is an input control -> set focus.
	 */
	private void checkForFocus(NodeBase n) {
		if(n.getPage().getFocusComponent() != null)
			return;
		if(n instanceof IHasChangeListener) { // FIXME Why this 'if'?
			if(n instanceof IInputNode< ? >) {
				IInputNode< ? > in = (IInputNode< ? >) n;
				if(!in.isDisabled() && !in.isReadOnly())
					n.getPage().setFocusComponent(n);
			} else
				n.getPage().setFocusComponent(n);
		}
	}

	protected void renderPageHeader() throws Exception {
		o().writeRaw(
			"<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n" + "<html>\n" + "<head>\n"
				+ "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n");
	}

	public void renderThemeCSS() throws Exception {
		o().writeRaw("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
		o().writeRaw(ctx().getRelativePath(ctx().getRelativeThemePath("style.theme.css")));
		o().writeRaw("\"></link>\n");
	}

	public void renderHeadContributors() throws Exception {
		for(HeaderContributor hc : page().getApplication().getHeaderContributorList())
			hc.contribute(this);

		for(HeaderContributor hc : page().getHeaderContributorList()) {
			hc.contribute(this);
		}
	}

	public void renderLoadCSS(String path) throws Exception {
		//-- render an app-relative url
		o().tag("link");
		o().attr("rel", "stylesheet");
		o().attr("type", "text/css");
		o().attr("href", ctx().getRelativePath(path));
		o().endtag();
		o().closetag("link");
	}

	public void renderLoadJavascript(String path) throws Exception {
		//-- render an app-relative url
		o().tag("script");
		o().attr("language", "javascript");
		o().attr("src", ctx().getRelativePath(path));
		o().endtag();
		o().closetag("script");
	}

	private void genVar(String name, String val) throws Exception {
		o().writeRaw("var " + name + "=" + val + ";\n");
	}

	public void render(IRequestContext ctx, Page page) throws Exception {
		m_ctx = ctx;
		m_page = page;
		page.build();
		renderPageHeader();
		//		o().writeRaw(
		//			"<script language=\"javascript\"><!--\n"
		//		+	"var DomUIpageTag="+page.getPageTag()+";\n"
		//		+	"var DomUIThemeURL="+StringTool.strToJavascriptString(ctx.getRelativePath( ctx.getRelativeThemePath("") ), true)+";\n"
		//		+	"--></script>\n"
		//		);
		o().writeRaw("<script language=\"javascript\"><!--\n");
		genVar("DomUIpageTag", Integer.toString(page.getPageTag()));
		genVar("DomUIThemeURL", StringTool.strToJavascriptString(ctx.getRelativePath(ctx.getRelativeThemePath("")), true));
		genVar("DomUICID", StringTool.strToJavascriptString(page.getConversation().getFullId(), true));
		o().writeRaw("--></script>\n");
		renderThemeCSS();
		renderHeadContributors();
		if(page.getTitle() != null) {
			o().tag("title");
			o().endtag();
			o().text(page.getTitle());
			o().closetag("title");
		}
		o().closetag("head");

		// Render rest ;-)
		page.getBody().visit(this);

		/*
		 * Render all attached Javascript in an onReady() function. This code will run
		 * as soon as the body load has completed.
		 */
		StringBuilder sq = page.internalGetAppendedJS();
		o().tag("script");
		o().attr("language", "javascript");
		o().endtag();
		o().text("$(document).ready(function() {");

		//-- If any component has a focus request issue that,
		NodeBase f = page.getFocusComponent();
		if(f != null) {
			o().text("WebUI.focus('" + f.getActualID() + "');");
			page.setFocusComponent(null);
		}
		if(m_createJS.length() > 0) {
			o().writeRaw(m_createJS.toString());
			//				o().text(m_createJS.toString());
		}
		if(sq != null) {
			o().writeRaw(sq.toString());
			//				o().text(sq.toString());
		}

		//-- If asynchronous actions are pending call WebUI.startPolling();
		if(page.getConversation().hasDelayedActions())
			o().writeRaw("WebUI.startPolling();");

		o().text("});");
		o().closetag("script");
		o().closetag("html");
	}

	public StringBuilder getCreateJS() {
		return m_createJS;
	}
}
