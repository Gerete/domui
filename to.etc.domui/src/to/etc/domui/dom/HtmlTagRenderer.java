/*
 * DomUI Java User Interface library
 * Copyright (c) 2010 by Frits Jalvingh, Itris B.V.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * See the "sponsors" file for a list of supporters.
 *
 * The latest version of DomUI and related code, support and documentation
 * can be found at http://www.domui.org/
 * The contact for the project is Frits Jalvingh <jal@etc.to>.
 */
package to.etc.domui.dom;

import java.io.*;

import to.etc.domui.component.misc.*;
import to.etc.domui.dom.css.*;
import to.etc.domui.dom.html.*;
import to.etc.domui.server.*;
import to.etc.domui.util.*;
import to.etc.util.*;

/**
 * Basic, mostly standard-compliant handler for rendering HTML tags.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Aug 17, 2007
 */
public class HtmlTagRenderer implements INodeVisitor {
	/** Scratch stringbuffer. */
	private StringBuilder m_sb;

	private BrowserVersion m_browserVersion;

	private final IBrowserOutput m_o;

	private boolean m_tagless;

	//	private boolean				m_updating;

	private HtmlRenderMode m_mode;

	//	private boolean				m_isNewNode;

	protected HtmlTagRenderer(BrowserVersion bv, final IBrowserOutput o) {
		m_o = o;
		m_browserVersion = bv;
	}

	protected BrowserVersion getBrowser() {
		return m_browserVersion;
	}

	/**
	 * When T this only renders attributes but no tags and tag-ends.
	 * @return
	 */
	public boolean isTagless() {
		return m_tagless;
	}

	//	public boolean isNewNode() {
	//		return m_isNewNode;
	//	}
	//	public void setNewNode(final boolean newNode) {
	//		m_isNewNode = newNode;
	//	}

	/**
	 * When T this only renders attributes but no tags and tag-ends.
	 */
	public void setTagless(final boolean tagless) {
		m_tagless = tagless;
	}

	//	public boolean isUpdating() {
	//		return m_updating;
	//	}

	public void setRenderMode(final HtmlRenderMode rm) {
		m_mode = rm;
	}

	public HtmlRenderMode getMode() {
		return m_mode;
	}

	//	public void setUpdating(final boolean updating) {
	//		m_updating = updating;
	//	}

	protected boolean isFullRender() {
		return m_mode == HtmlRenderMode.FULL;
	}

	private boolean isAttrRender() {
		return m_mode == HtmlRenderMode.ATTR;
	}

	//	private boolean	isAddsRender() {
	//		return m_mode == HtmlRenderMode.ADDS;
	//	}
	//	private boolean	isReplaceRender() {
	//		return m_mode == HtmlRenderMode.REPL;
	//	}

	/**
	 * Return the cleared scratchbuffer.
	 * @return
	 */
	protected StringBuilder sb() {
		if(m_sb == null)
			m_sb = new StringBuilder(128);
		else
			m_sb.setLength(0);
		return m_sb;
	}

	/**
	 * For browsers that have trouble with attribute updates (Microsoft's sinking flagship of course) this
	 * can be used to postphone setting node attributes until after the delta has been applied to the DOM; it
	 * executes attribute updates using Javascript at the end of a delta update.
	 *
	 * @param nodeID
	 * @param pairs
	 */
	protected void addDelayedAttrs(NodeBase n, String... pairs) {
		if(pairs.length == 0)
			return;
		if(0 != (pairs.length & 0x1))
			throw new IllegalArgumentException("Odd number of attribute/value strings.");

		StringBuilder sb = sb();
		sb.append("WebUI.delayedSetAttributes(\"");
		sb.append(n.getActualID());
		sb.append("\"");
		boolean isattr = false;
		for(String s : pairs) {
			sb.append(',');
			if(isattr) {
				//-- copy verbatim
				sb.append(s);
			} else {
				//-- Attribute name: enclose in string
				sb.append('"');
				sb.append(s);
				sb.append('"');
			}
			isattr = !isattr;
		}
		sb.append(");\n");
		n.getPage().appendJS(sb);
	}

	/**
	 * Render the "disabled" attribute. Override for shitware.
	 * @param n
	 * @param disabled
	 * @throws IOException
	 */
	protected void renderDisabled(NodeBase n, boolean disabled) throws IOException {
		if(isFullRender() && ! disabled)
			return;
		o().attr("disabled", disabled ? "disabled" : "");
	}

	/**
	 * Render the "checked" attribute. Override for shitware.
	 * @param n
	 * @param checked
	 * @throws IOException
	 */
	protected void renderChecked(NodeBase n, boolean checked) throws IOException {
		if(isFullRender() && !checked)
			return;
		o().attr("checked", checked ? "checked" : "");
	}

	/**
	 * Render the 'selected' attribute. Override for shitware.
	 * @param n
	 * @param checked
	 * @throws IOException
	 */
	protected void renderSelected(NodeBase n, boolean checked) throws IOException {
		if(isFullRender() && !checked)
			return;
		o().attr("selected", checked ? "selected" : "");
	}

	/**
	 * Render the 'readonly' attribute. Override for shitware.
	 * @param n
	 * @param readonly
	 * @throws IOException
	 */
	protected void renderReadOnly(NodeBase n, boolean readonly) throws IOException {
		if(isFullRender() && ! readonly)
			return;
		o().attr("readonly", readonly ? "readonly" : "");
	}

	protected void renderDiRo(NodeBase n, boolean disabled, boolean readonly) throws IOException {
		renderDisabled(n, disabled);
		renderReadOnly(n, readonly);
	}

	static public String fixColor(final String s) {
		if(s.startsWith("#"))
			return s;
		for(int i = s.length(); --i >= 0;) {
			char c = s.charAt(i);
			if(!Character.isDigit(c) && !(c >= 'a' && c <= 'f') && !(c >= 'A' && c <= 'F'))
				return s;
		}
		return "#" + s;
	}

	public void appendStyle(final CssBase c, final Appendable a) throws IOException { // Bloody idiot.
		if(c.getBackgroundAttachment() != null) {
			a.append("background-attachment: ");
			switch(c.getBackgroundAttachment()){
				default:
					throw new IllegalStateException("Unknown " + c.getBackgroundAttachment());
				case FIXED:
					a.append("fixed; ");
					break;
				case SCROLL:
					a.append("scroll;");
					break;
			}
		}
		if(c.getBackgroundColor() != null) {
			a.append("background-color:");
			a.append(fixColor(c.getBackgroundColor()));
			a.append(';');
		}
		if(c.getBackgroundImage() != null) {
			a.append("background-image:");
			if(c.getBackgroundImage().equalsIgnoreCase("none"))
				a.append("none");
			else {
				a.append("url(");
				a.append(c.getBackgroundImage());
				a.append(");");
			}
		}
		if(c.getBackgroundPosition() != null) {
			a.append("background-position:");
			a.append(c.getBackgroundPosition());
			a.append(';');
		}
		if(c.getBackgroundRepeat() != null) {
			a.append("background-repeat:");
			a.append(c.getBackgroundRepeat());
			a.append(';');
		}

		//-- Borders
		StringBuilder bb = new StringBuilder(20);
		String left = border(bb, c.getBorderLeftWidth(), c.getBorderLeftStyle(), c.getBorderLeftColor());
		String right = border(bb, c.getBorderRightWidth(), c.getBorderRightStyle(), c.getBorderRightColor());
		String top = border(bb, c.getBorderTopWidth(), c.getBorderTopStyle(), c.getBorderTopColor());
		String bottom = border(bb, c.getBorderBottomWidth(), c.getBorderBottomStyle(), c.getBorderBottomColor());

		if(DomUtil.isEqual(left, right, top, bottom)) {
			if(left.length() != 0) {
				a.append("border:");
				a.append(left);
				a.append(';');
			}
		} else if(DomUtil.isEqual(left, right, top)) {
			a.append("border:");
			a.append(left);
			a.append(';');
			a.append("border-bottom:");
			a.append(bottom);
			a.append(';');
		} else if(DomUtil.isEqual(left, right, bottom)) {
			a.append("border:");
			a.append(left);
			a.append(';');
			a.append("border-top:");
			a.append(top);
			a.append(';');
		} else if(DomUtil.isEqual(right, top, bottom)) {
			a.append("border:");
			a.append(right);
			a.append(';');
			a.append("border-left:");
			a.append(left);
			a.append(';');
		} else if(DomUtil.isEqual(left, top, bottom)) {
			a.append("border:");
			a.append(left);
			a.append(';');
			a.append("border-right:");
			a.append(right);
			a.append(';');
		} else {
			//-- All different.
			a.append("border-bottom:");
			a.append(bottom);
			a.append(';');
			a.append("border-top:");
			a.append(top);
			a.append(';');
			a.append("border-left:");
			a.append(left);
			a.append(';');
			a.append("border-right:");
			a.append(right);
			a.append(';');
		}

		//-- Clear
		if(c.getClear() != null) {
			a.append("clear:");
			a.append(c.getClear().toString());
			a.append(';');
		}

		//-- Display
		if(c.getDisplay() != null) {
			a.append("display:");
			a.append(c.getDisplay().toString());
			a.append(';');
		}
		if(c.getFloat() != null) {
			a.append("float:");
			a.append(c.getFloat().getCode());
			a.append(';');
		}
		if(c.getVisibility() != null) {
			a.append("visibility:");
			a.append(c.getVisibility().name().toLowerCase());
			a.append(';');
		}

		/******* Dimension *********/
		if(c.getHeight() != null) {
			a.append("height:");
			a.append(c.getHeight());
			a.append(";");
		}
		if(c.getWidth() != null) {
			a.append("width:");
			a.append(c.getWidth());
			a.append(";");
		}
		if(c.getLineHeight() != null) {
			a.append("line-height:");
			a.append(c.getLineHeight());
			a.append(";");
		}
		if(c.getMaxHeight() != null) {
			a.append("max-height:");
			a.append(c.getMaxHeight());
			a.append(";");
		}
		if(c.getMaxWidth() != null) {
			a.append("max-width:");
			a.append(c.getMaxWidth());
			a.append(";");
		}
		if(c.getMinHeight() != null) {
			a.append("min-height:");
			a.append(c.getMinHeight());
			a.append(";");
		}
		if(c.getMinWidth() != null) {
			a.append("min-width:");
			a.append(c.getMinWidth());
			a.append(";");
		}

		/********* Positioning **********/
		if(c.getOverflow() != null) {
			a.append("overflow:");
			a.append(c.getOverflow().toString());
			a.append(';');
		}
		if(c.getZIndex() != Integer.MIN_VALUE) {
			a.append("z-index:");
			a.append(Integer.toString(c.getZIndex()));
			a.append(';');
		}
		if(c.getPosition() != null) {
			a.append("position:");
			a.append(c.getPosition().getTxt());
			a.append(';');
		}
		if(c.getTop() != null) {
			a.append("top:");
			a.append(c.getTop()); //allows percentage values also (50%)
			a.append(";");
		}
		if(c.getBottom() != null) {
			a.append("bottom:");
			a.append(c.getBottom());
			a.append(";");
		}
		if(c.getLeft() != null) {
			a.append("left:");
			a.append(c.getLeft());
			a.append(";");
		}
		if(c.getRight() != null) {
			a.append("right:");
			a.append(c.getRight());
			a.append(";");
		}

		/***** Font properties ******/
		if(c.getColor() != null) {
			a.append("color:");
			a.append(fixColor(c.getColor()));
			a.append(";");
		}

		if(c.getFontSize() != null) {
			a.append("font-size:");
			a.append(c.getFontSize());
			a.append(';');
		}
		if(c.getFontFamily() != null) {
			a.append("font-family:");
			a.append(c.getFontFamily());
			a.append(';');
		}
		if(c.getFontSizeAdjust() != null) {
			a.append("font-size-adjust:");
			a.append(c.getFontSizeAdjust());
			a.append(';');
		}
		if(c.getFontStyle() != null) {
			a.append("font-style:");
			a.append(c.getFontStyle().name().toLowerCase());
			a.append(';');
		}
		if(c.getFontVariant() != null) {
			a.append("font-variant:");
			a.append(c.getFontVariant().name().toLowerCase());
			a.append(';');
		}
		if(c.getFontWeight() != null) {
			a.append("font-weight:");
			a.append(c.getFontWeight());
			a.append(';');
		}
		if(c.getTextAlign() != null) {
			a.append("text-align:");
			a.append(c.getTextAlign().name().toLowerCase());
			a.append(';');
		}
		if(c.getVerticalAlign() != null) {
			a.append("vertical-align:");
			a.append(c.getVerticalAlign().toString());
			a.append(';');
		}

		//Margins
		if(c.getMarginBottom() != null || c.getMarginLeft() != null || c.getMarginRight() != null || c.getMarginTop() != null) {
			if(DomUtil.isEqual(c.getMarginBottom(), c.getMarginTop(), c.getMarginLeft(), c.getMarginRight())) {
				a.append("margin:");
				a.append(c.getMarginLeft());
				a.append(';');
			} else {
				boolean topbot = DomUtil.isEqual(c.getMarginBottom(), c.getMarginTop()) && c.getMarginBottom() != null;
				boolean leri = DomUtil.isEqual(c.getMarginLeft(), c.getMarginRight()) && c.getMarginLeft() != null;
				if(topbot && leri) {
					a.append("margin:");
					a.append(c.getMarginTop());
					a.append(' ');
					a.append(c.getMarginLeft());
					a.append(';');
				} else {
					if(c.getMarginLeft() != null) {
						a.append("margin-left:");
						a.append(c.getMarginLeft());
						a.append(';');
					}

					if(c.getMarginRight() != null) {
						a.append("margin-right:");
						a.append(c.getMarginRight());
						a.append(';');
					}

					if(c.getMarginBottom() != null) {
						a.append("margin-bottom:");
						a.append(c.getMarginBottom());
						a.append(';');
					}

					if(c.getMarginTop() != null) {
						a.append("margin-top:");
						a.append(c.getMarginTop());
						a.append(';');
					}
				}
			}
		}

		if(c.getTransform() != null) {
			a.append("text-transform:");
			a.append(c.getTransform().name().toLowerCase());
			a.append(';');
		}
	}

	static private String border(final StringBuilder a, final int w, final String s, final String c) {
		a.setLength(0);
		if(w >= 0) {
			a.append(w);
			a.append("px");
		}
		if(s != null) {
			if(s.length() > 0)
				a.append(' ');
			a.append(s.trim().toLowerCase());
		}
		if(c != null) {
			if(c.length() > 0)
				a.append(' ');
			a.append(c.trim().toLowerCase());
		}
		return a.toString();
	}

	final protected IBrowserOutput o() {
		return m_o;
	}

	/**
	 * Returns the style for the node. It uses the cached style and recreates it when it has changed.
	 * @param b
	 * @return
	 */
	protected String getStyleFor(final NodeBase b) throws IOException {
		String s = b.getCachedStyle();
		if(s != null)
			return s;
		StringBuilder sb = sb(); // Get work buffer,
		appendStyle(b, sb); // Create style string
		s = sb.toString();
		b.setCachedStyle(s);
		return s;
	}

	protected void renderTag(final NodeBase b, final IBrowserOutput o) throws Exception {
		if(!m_tagless)
			o.tag(b.getTag()); // Open the tag
	}

	protected void renderTagend(final NodeBase b, final IBrowserOutput o) throws Exception {
		if(!m_tagless)
			o.endtag();
	}


	/*--------------------------------------------------------------*/
	/*	CODING:	Core node rendering.								*/
	/*--------------------------------------------------------------*/
	public void basicNodeRender(final NodeBase b, final IBrowserOutput o) throws Exception {
		basicNodeRender(b, o, false);
	}

	/**
	 * Basic rendering of a node. This renders the tag and all shared attributes.
	 * @param o
	 * @throws Exception
	 */
	public void basicNodeRender(final NodeBase b, final IBrowserOutput o, boolean inhibitevents) throws Exception {
		renderTag(b, o);
		if(m_tagless)
			o.attr("select", "#" + b.getActualID()); // Always has an ID
		else
			o.attr("id", b.getActualID()); // Always has an ID

		//-- Handle DRAGGABLE nodes.
		if(b instanceof IDraggable) {
			IDragHandler dh = ((IDraggable) b).getDragHandler();
			UIDragDropUtil.exposeDraggable(b, dh);
		}
		if(b instanceof IDropTargetable) {
			IDropHandler dh = ((IDropTargetable) b).getDropHandler();
			UIDragDropUtil.exposeDroppable(b, dh);
		}

		String s = getStyleFor(b); // Get/recalculate style
		if(s.length() > 0)
			o.attr("style", s); // Append style
		if(b.getTestID() != null)
			o.attr("testid", b.getTestID());
		if(b.isStretchHeight())
			o.attr("stretch", "true");
		if(b.getCssClass() != null)
			o.attr("class", b.getCssClass());
		String ttl = b.getTitle();
		if(ttl != null && !(b instanceof UrlPage)) // Do NOT render title on the thing representing the BODY.
			o().attr("title", ttl);

		if(b.getSpecialAttributeList() != null) {
			for(int i = 0; i < b.getSpecialAttributeList().size(); i += 2) {
				o().attr(b.getSpecialAttributeList().get(i), b.getSpecialAttributeList().get(i + 1));
			}
		}

		//-- Javascriptish
		if(inhibitevents)
			return;

		if(b.internalNeedClickHandler()) {
			o.attr("onclick", sb().append("return WebUI.clicked(this, '").append(b.getActualID()).append("', event)").toString());
		} else if(b.getOnClickJS() != null) {
			o.attr("onclick", b.getOnClickJS());
		}
		if(b instanceof IHasChangeListener) {
			IHasChangeListener inb = (IHasChangeListener) b;
			if(null != inb.getOnValueChanged()) {
				o.attr("onchange", sb().append("WebUI.valuechanged(this, '").append(b.getActualID()).append("', event)").toString());
			}
		}

		if(b.getOnMouseDownJS() != null) {
			o.attr("onmousedown", b.getOnMouseDownJS());
		}
	}

	/**
	 * Special thingy; this can actually be a BODY instead of a DIV; in that case we render some extra
	 * arguments...
	 * @see to.etc.domui.dom.html.INodeVisitor#visitDiv(to.etc.domui.dom.html.Div)
	 */
	@Override
	public void visitDiv(final Div n) throws Exception {
		basicNodeRender(n, m_o);
		if(n.getTag().equals("body")) {
			o().attr("onunload", "WebUI.unloaded()");
		}

		if(n.getReturnPressed() != null) {
			o().attr("onkeypress", "return WebUI.returnKeyPress(event, this)");
		}

		//-- Drop crud
		if(n.getDropBody() != null) {
			o().attr("uidropbody", n.getDropBody().getActualID());
		}
		if(n.getDropMode() != null) {
			o().attr("uidropmode", n.getDropMode().name());
		}
		renderTagend(n, m_o);
	}

	@Override
	public void visitSpan(final Span n) throws Exception {
		basicNodeRender(n, m_o);
		renderTagend(n, m_o);
	}

	@Override
	public void visitTable(final Table n) throws Exception {
		basicNodeRender(n, m_o);
		if(n.getTableBorder() != -1)
			o().attr("border", n.getTableBorder());
		if(n.getCellPadding() != null)
			o().attr("cellpadding", n.getCellPadding());
		if(n.getCellSpacing() != null)
			o().attr("cellspacing", n.getCellSpacing());
		if(n.getTableWidth() != null)
			o().attr("width", n.getTableWidth());
		if(n.getTableHeight() != null)
			o().attr("height", n.getTableHeight());
		if(n.getAlign() != null)
			o().attr("align", n.getAlign().getCode());
		renderTagend(n, m_o);
	}

	@Override
	public void visitTHead(final THead n) throws Exception {
		basicNodeRender(n, m_o);
		renderTagend(n, m_o);
	}

	@Override
	public void visitTBody(final TBody n) throws Exception {
		basicNodeRender(n, m_o);
		renderTagend(n, m_o);
	}

	@Override
	public void visitTD(final TD n) throws Exception {
		o().setIndentEnabled(false); // 20100222 jal
		basicNodeRender(n, m_o);
		if(n.getValign() != null) {
			switch(n.getValign()){
				default:
					throw new IllegalStateException("Unknown valign: " + n.getValign());
				case BOTTOM:
					o().attr("valign", "bottom");
					break;
				case TOP:
					o().attr("valign", "top");
					break;
				case MIDDLE:
					o().attr("valign", "middle");
					break;
			}
		}
		if(n.getColspan() > 0)
			o().attr("colspan", n.getColspan());
		if(n.getRowspan() > 0)
			o().attr("rowspan", n.getRowspan());
		if(n.isNowrap())
			o().attr("nowrap", "nowrap");
		if(n.getCellHeight() != null)
			o().attr("height", n.getCellHeight());
		if(n.getCellWidth() != null)
			o().attr("width", n.getCellWidth());
		if(n.getAlign() != null)
			o().attr("align", n.getAlign().getCode());
		renderTagend(n, m_o);
	}

	@Override
	public void visitTH(final TH n) throws Exception {
		basicNodeRender(n, m_o);
		if(n.getValign() != null) {
			switch(n.getValign()){
				default:
					throw new IllegalStateException("Unknown valign: " + n.getValign());
				case BOTTOM:
					o().attr("valign", "bottom");
					break;
				case TOP:
					o().attr("valign", "top");
					break;
				case MIDDLE:
					o().attr("valign", "middle");
					break;
			}
		}
		if(n.getColspan() > 0)
			o().attr("colspan", n.getColspan());
		if(n.getRowspan() > 0)
			o().attr("rowspan", n.getRowspan());
		if(n.isNowrap())
			o().attr("nowrap", "nowrap");
		if(n.getCellHeight() != null)
			o().attr("height", n.getCellHeight());
		if(n.getCellWidth() != null)
			o().attr("width", n.getCellWidth());
		if(n.getAlign() != null)
			o().attr("align", n.getAlign().getCode());

		if(n.getScope() != null)
			o().attr("scope", n.getScope());
		renderTagend(n, m_o);
	}

	@Override
	public void visitTR(final TR n) throws Exception {
		basicNodeRender(n, m_o);
		renderTagend(n, m_o);
	}

	@Override
	public void visitTextNode(final TextNode n) throws Exception {
		String lit = n.getText();
		if(lit != null && lit.length() > 0)
			m_o.text(lit);
	}

	/**
	 * This is a TextNode with the special characteristic that it contains
	 * not normal text but XML to be rendered verbatim. The XML may not be
	 * indented in any way and may not be escaped (that should already have
	 * been done by the one creating it).
	 *
	 * @see to.etc.domui.dom.html.INodeVisitor#visitXmlNode(to.etc.domui.dom.html.XmlTextNode)
	 */
	@Override
	public void visitXmlNode(XmlTextNode n) throws Exception {
		String lit = n.getText(); // Get tilde-replaced text
		if(lit != null && lit.length() > 0) {
			m_o.text(""); // 20100222 jal Force previous tag to end with >.
			m_o.writeRaw(lit);
		}
	}

	public void renderEndTag(final NodeBase b) throws IOException {
		if(!m_tagless)
			m_o.closetag(b.getTag());
	}

	@Override
	public void visitA(final ATag a) throws Exception {
		basicNodeRender(a, m_o);
		if(a.getHref() == null || a.getHref().trim().length() == 0) {
			o().attr("href", "javascript: void(0);");
		} else
			o().attr("href", a.getHref());
		if(a.getTarget() != null)
			o().attr("target", a.getTarget());
		renderTagend(a, m_o);
	}

	@Override
	public void visitLi(final Li n) throws Exception {
		basicNodeRender(n, m_o);
		renderTagend(n, m_o);
	}

	@Override
	public void visitUl(final Ul n) throws Exception {
		basicNodeRender(n, m_o);
		renderTagend(n, m_o);
	}

	private void renderType(final String t) throws Exception {
		if(isAttrRender()) // Cannot replace on existing node.
			return;
		o().attr("type", t);
	}

	/**
	 * Render the basic input tag.
	 * @see to.etc.domui.dom.html.INodeVisitor#visitInput(to.etc.domui.dom.html.Input)
	 */
	@Override
	public void visitInput(final Input n) throws Exception {
		basicNodeRender(n, m_o);
		o().attr("name", n.getActualID());
		renderType(n.getInputType());
		renderDiRo(n, n.isDisabled(), n.isReadOnly());
		if(n.getMaxLength() > 0)
			o().attr("maxlength", n.getMaxLength());
		if(n.getSize() > 0)
			o().attr("size", n.getSize());
		if(n.getRawValue() != null)
			o().attr("value", n.getRawValue());
		if(n.getOnKeyPressJS() != null) {
			o().attr("onkeypress", n.getOnKeyPressJS());
		}
		String transformScript = "";
		if(n.getTransform() != null) {
			switch(n.getTransform()){
				case LOWERCASE:
					transformScript = "javascript:this.value=this.value.toLowerCase();";
					break;
				case UPPERCASE:
					transformScript = "javascript:this.value=this.value.toUpperCase();";
					break;
				default://do nothing
			}
		}

		if(n.getOnLookupTyping() != null) {
			//20110304 vmijic: must be done using onkeypress (I tried onkeydown in combination with setReturnPressed, but that fails since onkeydown change model, so setReturnPressed is fired for dead node that results with exception)
			o().attr("onkeypress", sb().append("WebUI.onLookupTypingReturnKeyHandler('").append(n.getActualID()).append("', event)").toString());
			o().attr("onkeyup", sb().append("WebUI.scheduleOnLookupTypingEvent('").append(n.getActualID()).append("', event)").toString());
			o().attr("onblur", sb().append(transformScript).append("WebUI.hideLookupTypingPopup('").append(n.getActualID()).append("')").toString());
		} else {
			if(!DomUtil.isBlank(transformScript)) {
				o().attr("onblur", sb().append(transformScript).toString());
			}
		}
		renderTagend(n, m_o);
	}

	@Override
	public void visitFileInput(final FileInput n) throws Exception {
		basicNodeRender(n, m_o);
		//		if(! isUpdating())
		o().attr("type", "file");
		o().attr("name", n.getActualID());
		//		if(n.isDisabled())
		//			o().attr("disabled", "disabled");
		//		if(n.getMaxLength() > 0)
		//			o().attr("maxlength", n.getMaxLength());
		//		if(n.isReadOnly())
		//			o().attr("readonly", "readonly");
		//		if(n.getSize() > 0)
		//			o().attr("size", n.getSize());
		//		if(n.getRawValue()!= null)
		//			o().attr("value", n.getRawValue());
		//		if(n.getOnKeyPressJS() != null)
		//			o().attr("onkeypress", n.getOnKeyPressJS());
		renderTagend(n, m_o);
	}

	/**
	 * Render the basic input tag.
	 * @see to.etc.domui.dom.html.INodeVisitor#visitInput(to.etc.domui.dom.html.Input)
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void visitCheckbox(final Checkbox n) throws Exception {
		basicNodeRender(n, m_o, true);
		renderType("checkbox");
		o().attr("name", n.getActualID());
		renderDisabled(n, n.isDisabled()); // 20091110 jal Checkboxes do not have a readonly attribute.
		renderChecked(n, n.isChecked());

		//-- jal 20110125 Start fixing bug# 917: the idiots in the room (IE 7, 8) do not properly handle onchange on checkbox, sigh.
		if(m_browserVersion.isIE()) {
			//-- To fix IE's 26385652791725917435'th bug use the clicked listener to handle change events too.
			if(n.internalNeedClickHandler() || n.getOnValueChanged() != null) {
				m_o.attr("onclick", sb().append("WebUI.clickandchange(this, '").append(n.getActualID()).append("', event); return true;").toString());
			} else if(n.getOnClickJS() != null) {
				m_o.attr("onclick", n.getOnClickJS());
			}
		} else {
			if(n.internalNeedClickHandler()) {
				m_o.attr("onclick", sb().append("WebUI.clicked(this, '").append(n.getActualID()).append("', event); return true;").toString());
			} else if(n.getOnClickJS() != null) {
				m_o.attr("onclick", n.getOnClickJS());
			}
			if(null != n.getOnValueChanged()) {
				m_o.attr("onchange", sb().append("WebUI.valuechanged(this, '").append(n.getActualID()).append("', event)").toString());
			}
		}

		renderTagend(n, m_o);
	}

	/**
	 * Render the basic radio button
	 * @see to.etc.domui.dom.html.INodeVisitor#visitInput(to.etc.domui.dom.html.Input)
	 */
	@Override
	public void visitRadioButton(final RadioButton< ? > n) throws Exception {
		basicNodeRender(n, m_o, true);
		renderType("radio");
		//		m_o.attr("value", n.getActualID());
		if(n.getName() != null)
			o().attr("name", n.getName());

		renderDiRo(n, n.isDisabled(), n.isReadOnly());
		renderChecked(n, n.isChecked());

		//-- jal 20110125 Start fixing bug# 917: the idiots in the room (IE 7, 8) do not properly handle onchange on checkbox, sigh.
		if(m_browserVersion.isIE()) {
			//-- To fix IE's 26385652791725917435'th bug use the clicked listener to handle change events too.
			if(n.internalNeedClickHandler() || n.getGroup().getOnValueChanged() != null) {
				m_o.attr("onclick", sb().append("WebUI.clickandchange(this, '").append(n.getActualID()).append("', event); return true;").toString());
			} else if(n.getOnClickJS() != null) {
				m_o.attr("onclick", n.getOnClickJS());
			}
		} else {
			if(n.internalNeedClickHandler()) {
				m_o.attr("onclick", sb().append("WebUI.clicked(this, '").append(n.getActualID()).append("', event); return true;").toString());
			} else if(n.getOnClickJS() != null) {
				m_o.attr("onclick", n.getOnClickJS());
			}
			if(null != n.getGroup().getOnValueChanged()) {
				m_o.attr("onchange", sb().append("WebUI.valuechanged(this, '").append(n.getActualID()).append("', event)").toString());
			}
		}
		renderTagend(n, m_o);
	}

	@Override
	public void visitImg(final Img n) throws Exception {
		o().setIndentEnabled(false); // 20100222 jal
		basicNodeRender(n, o());
		if(n.getAlign() != null)
			o().attr("align", n.getAlign().getCode());
		if(n.getAlt() != null)
			o().attr("alt", n.getAlt());
		if(n.getSrc() != null)
			o().attr("src", n.getSrc()); // 20110104 was rawAttr causing fails on & in delta????
		if(n.getImgBorder() >= 0)
			o().attr("border", n.getImgBorder());
		if(n.getImgWidth() != null)
			o().attr("width", n.getImgWidth());
		if(n.getImgHeight() != null)
			o().attr("height", n.getImgHeight());
		renderTagend(n, o());
	}

	@Override
	public void visitButton(final Button n) throws Exception {
		o().setIndentEnabled(false); // 20100222 jal
		basicNodeRender(n, o());
		renderDisabled(n, n.isDisabled());
		if(n.getType() != null && !isAttrRender())
			o().attr("type", n.getType().getCode());
		if(n.getValue() != null)
			o().attr("value", n.getValue());
		if(n.getAccessKey() != 0)
			o().attr("accesskey", "" + n.getAccessKey());
		renderTagend(n, o());
	}

	@Override
	public void visitLabel(final Label n) throws Exception {
		basicNodeRender(n, o());
		if(n.getFor() != null)
			o().attr("for", n.getFor());
		renderTagend(n, o());
	}

	@Override
	public void visitSelect(final Select n) throws Exception {
		basicNodeRender(n, o());
		if(n.isMultiple())
			o().attr("multiple", "multiple");
		renderDisabled(n, n.isDisabled()); // WATCH OUT: The SELECT tag HAS no READONLY attribute!!!
		if(n.getSize() > 0)
			o().attr("size", n.getSize());
		renderTagend(n, o());
	}

	@Override
	public void visitOption(final SelectOption n) throws Exception {
		basicNodeRender(n, o());
		renderDisabled(n, n.isDisabled());
		renderSelected(n, n.isSelected());
		o().attr("value", n.getActualID());
		renderTagend(n, o());
	}

	@Override
	public void visitBR(final BR n) throws Exception {
		basicNodeRender(n, o());
		renderTagend(n, o());
	}

	/**
	 * FIXME This now contains IE code where browser-standard code would just generate a proper TextArea with a content block. It needs to move to the crapware renderers.
	 *
	 * @see to.etc.domui.dom.html.INodeVisitor#visitTextArea(to.etc.domui.dom.html.TextArea)
	 */
	@Override
	public void visitTextArea(final TextArea n) throws Exception {
		basicNodeRender(n, o());
		if(n.getCols() > 0)
			o().attr("cols", n.getCols());
		if(n.getRows() > 0)
			o().attr("rows", n.getRows());

		renderDiRo(n, n.isDisabled(), n.isReadOnly());

		//-- Fix for bug 627: render textarea content in attribute to prevent zillion of IE fuckups.
		if(getMode() != HtmlRenderMode.FULL) {
			String txt = n.getRawValue();
			if(txt != null) {
				txt = StringTool.strToJavascriptString(txt, false);
				o().attr("domjs_value", txt); // FIXME THIS DOES NOT ALWAYS WORK
			}
		}
		renderTagend(n, o());
		o().setIndentEnabled(false); // jal 20090923 again: do not indent content (bug 627)
		//		if(n.getRawValue() != null)
		//			o().text(n.getRawValue());
		//		o().closetag(n.getTag());
	}

	@Override
	public void visitForm(final Form n) throws Exception {
		basicNodeRender(n, o());
		if(n.getAction() != null)
			o().attr("action", n.getAction());
		if(n.getMethod() != null)
			o().attr("method", n.getMethod());
		if(n.getEnctype() != null) {
			o().attr("enctype", n.getEnctype());
			o().attr("encoding", n.getEnctype()); // Another IE fuckup: needed to set multipart/form-data, see http://www.bennadel.com/blog/1273-Setting-Form-EncType-Dynamically-To-Multipart-Form-Data-In-IE-Internet-Explorer-.htm
		}
		if(n.getTarget() != null)
			o().attr("target", n.getTarget());
		renderTagend(n, o());
	}

	@Override
	@Deprecated
	public void visitLiteralXhtml(final LiteralXhtml n) throws Exception {
		basicNodeRender(n, m_o);
		renderTagend(n, m_o);

		//-- Render the content.
		if(n.getXml() != null) {
			if(n.getTag().equalsIgnoreCase("pre"))
				o().setIndentEnabled(false);
			o().writeRaw(n.getXml());
			if(n.getTag().equalsIgnoreCase("pre"))
				o().setIndentEnabled(true);
		}

		/*
		 * jal 20081212 Commented out: the end tag is generated in FullHtmlRenderer, in a special visit to
		 * the LiteralXhtml node. The closing tag </div> gets rendered there IF the mode is HTML but NOT when
		 * the mode is XML; in that case the base code for FullHtmlRenderer has already rendered the close
		 * tag.
		 */
		//		o().closetag(n.getTag());
	}

	@Override
	public void visitH(final HTag n) throws Exception {
		basicNodeRender(n, m_o);
		renderTagend(n, m_o);
	}

	//	protected void	renderDraggableCrud(NodeBase b) {
	//		if(! (b instanceof IDraggable))
	//			throw new IllegalStateException("Internal: nodetype "+b+" does not implement IDraggable, so DO NOT CALL ME!");
	//		IDraggable	d = (IDraggable)b;
	//		IDragHandler	dh = d.getDragHandler();
	//		if(dh == null) {
	//			// FIXME When UPDATING we MUST CLEAR any handlers set.
	//			return;
	//		}
	//	}


}
