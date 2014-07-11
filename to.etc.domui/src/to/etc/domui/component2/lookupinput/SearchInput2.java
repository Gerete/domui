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
package to.etc.domui.component2.lookupinput;

import javax.annotation.*;

import to.etc.domui.component.input.*;
import to.etc.domui.component.layout.*;
import to.etc.domui.component.tbl.*;
import to.etc.domui.dom.css.*;
import to.etc.domui.dom.html.*;
import to.etc.domui.util.*;

/**
 * Represents keyword search panel that is used from other components, like LookupInput.
 * Shows input field, marker that shows found results and waiting image that is hidden by default.
 * Waiting image is passive here, but it is used from browser script.
 *
 * @author <a href="mailto:vmijic@execom.eu">Vladimir Mijic</a>
 * Created on 21 Jan 2010
 */
public class SearchInput2<T> extends Div {

	private int m_resultsCount = -1; //-1 states for not visible

	@Nonnull
	final private TextStr m_keySearch = new TextStr();

	private Div m_pnlSearchCount;

	@Nullable
	private IValueChanged<SearchInput2<T>> m_onLookupTyping;

	@Nullable
	private IValueChanged<SearchInput2<T>> m_onShowTypingResults;

	@Nullable
	public IRowRenderer<T> m_resultsHintPopupRowRenderer;

//	@Nonnull
//	final private Img m_imgWaiting = new Img("THEME/lui-keyword-wait.gif");
//
	private Div m_pnlSearchPopup;

	private int m_popupWidth;

	public SearchInput2() {
	}

	public SearchInput2(@Nullable String cssClass) {
		if(cssClass != null)
			m_keySearch.setCssClass(cssClass);
	}

	@Override
	public void createContent() throws Exception {
		setCssClass("ui-srip");

//		m_imgWaiting.setCssClass("ui-lui-waiting");
//		m_imgWaiting.setDisplay(DisplayType.NONE);
		if(m_keySearch.getCssClass() == null) {
			m_keySearch.setCssClass("ui-srip-keyword");
		}
		m_keySearch.setMaxLength(40);
		m_keySearch.setSize(14);
		m_keySearch.setMarker();

//		m_keySearch.setOnLookupTyping(new ILookupTypingListener<TextStr>() {
//			@Override
//			public void onLookupTyping(@Nonnull TextStr component, boolean done) throws Exception {
//				if(done) {
//					if(getOnShowResults() != null) {
//						getOnShowResults().onValueChanged(SearchInput2.this);
//					}
//				} else {
//					IValueChanged<SearchInput2<T>> olt = getOnLookupTyping();
//					if(olt != null) {
//						olt.onValueChanged(SearchInput2.this);
//					}
//				}
//			}
//		});

//		add(m_imgWaiting);
		add(m_keySearch);

		appendCreateJS("WebUI.SearchPopup.register('" + getActualID() + "','" + m_keySearch.getActualID() + "');");
		renderResultsCountPart();
	}

	@Nullable
	public IValueChanged<SearchInput2<T>> getOnLookupTyping() {
		return m_onLookupTyping;
	}

	public void setOnLookupTyping(@Nullable IValueChanged<SearchInput2<T>> onLookupTyping) {
		m_onLookupTyping = onLookupTyping;
	}

	@Nullable
	public String getKeySearchValue() {
		return m_keySearch.getValue();
	}

	/**
	 * Set number of results label. Use -1 for hiding label.
	 * @param results
	 */
	public void setResultsCount(int results) {
		if(results != m_resultsCount) {
			m_resultsCount = results;
			if(isBuilt()) {
				renderResultsCountPart();
			}
		}
	}

	private void renderResultsCountPart() {
		if(m_resultsCount == -1 || m_resultsCount == 1) {
			if(m_pnlSearchCount != null) {
				removeChild(m_pnlSearchCount);
			}
			m_pnlSearchCount = null;
		} else {
			if(m_pnlSearchCount == null) {
				m_pnlSearchCount = new Div();
				add(m_pnlSearchCount);
			}
			if(m_resultsCount == 0) {
				m_pnlSearchCount.setCssClass("ui-srip-keyword-no-res");
				m_pnlSearchCount.setText(Msgs.BUNDLE.getString(Msgs.UI_KEYWORD_SEARCH_NO_MATCH));
			} else if(m_resultsCount == ITableModel.DEFAULT_MAX_SIZE) {
				//usually this means that query cutoff rest data, corner case when real m_resultsCount == MAX_RESULTS is not that important
				m_pnlSearchCount.setCssClass("ui-srip-keyword-large");
				m_pnlSearchCount.setText(Msgs.BUNDLE.formatMessage(Msgs.UI_KEYWORD_SEARCH_LARGE_MATCH, "" + ITableModel.DEFAULT_MAX_SIZE));
			} else {
				if(m_resultsCount > ITableModel.DEFAULT_MAX_SIZE) {
					//in case that query does not cutoff rest of data (JDBC queries) report actual data size, but render as to large match
					m_pnlSearchCount.setCssClass("ui-srip-keyword-large");
				} else {
					m_pnlSearchCount.setCssClass("ui-srip-keyword-res");
				}
				m_pnlSearchCount.setText(Msgs.BUNDLE.formatMessage(Msgs.UI_KEYWORD_SEARCH_COUNT, "" + m_resultsCount));
			}
		}
	}

	public IValueChanged<SearchInput2<T>> getOnShowResults() {
		return m_onShowTypingResults;
	}

	public void setOnShowResults(IValueChanged<SearchInput2<T>> onShowResults) {
		m_onShowTypingResults = onShowResults;
	}

	@Override
	public void setFocus() {
		if(m_keySearch != null) {
			m_keySearch.setFocus();
		}
	}

	/**
	 * Get current window Z index and set its value to current control.
	 */
	private void fixZIndex() {
		//bug fix for IE when combining relative positioning, and overlapping control with absolute positioning.
		FloatingWindow parentFloatingWindow = findParent(FloatingWindow.class);
		int parentWindowZIndex = 0;
		if(parentFloatingWindow != null) {
			parentWindowZIndex = parentFloatingWindow.getZIndex();
		}
		if(parentWindowZIndex < 0) {
			parentWindowZIndex = 0;
		}
		setZIndex(parentWindowZIndex);
	}

	public void showResultsHintPopup(@Nullable final ITableModel<T> popupResults) throws Exception {
		if(!isBuilt()) {
			throw new IllegalStateException("Must be built already!");
		}
		if(popupResults == null) {
			if(m_pnlSearchPopup != null) {
				removeChild(m_pnlSearchPopup);
				m_pnlSearchPopup = null;
			}
			fixZIndex(); //do not delete, fix for bug in IE
			m_keySearch.setZIndex(getZIndex()); //do not delete, fix for bug in domui
		} else {
			if(m_pnlSearchPopup == null) {
				m_pnlSearchPopup = new Div();
				m_pnlSearchPopup.setCssClass("ui-srip-keyword-popup");
				if(getPopupWidth() > 0) {
					m_pnlSearchPopup.setWidth(getPopupWidth() + "px");
				}
				fixZIndex();
				//increase Z index both for current DIV and popup DIV.
				//20110304 vmijic - We need to do this in domui.js because bug in IE7. Code remains here commented as illustartion what is done in javascript.
				//setZIndex(getZIndex() + 1);
				//m_pnlSearchPopup.setZIndex(getZIndex());
				add(m_pnlSearchPopup);
			} else {
				m_pnlSearchPopup.removeAllChildren();
			}

			IRowRenderer<T> rhpr = m_resultsHintPopupRowRenderer;
			if(rhpr == null) {
				throw new IllegalStateException("Undefined m_resultsHintPopupRowRenderer!");
			}

			DataTable<T> tbl = new DataTable<T>(popupResults, rhpr);
			m_pnlSearchPopup.add(tbl);
			tbl.setWidth("100%");
			tbl.setOverflow(Overflow.HIDDEN);
			tbl.setPosition(PositionType.RELATIVE);
			m_pnlSearchPopup.setDisplay(DisplayType.NONE);
		}
	}

	public IRowRenderer<T> getResultsHintPopupRowRenderer() {
		return m_resultsHintPopupRowRenderer;
	}

	public void setResultsHintPopupRowRenderer(IRowRenderer<T> resultsHintPopupRowRenderer) {
		m_resultsHintPopupRowRenderer = resultsHintPopupRowRenderer;
	}

	/**
	 * Getter for hint. See {@link SearchInput2#setHint}.
	 * @param hint
	 */
	@Nullable
	public String getHint() {
		return m_keySearch.getTitle();
	}

	/**
	 * Set hint to keyword search input. Usually says how search condition is resolved.
	 * @param hint
	 */
	public void setHint(@Nullable String hint) {
		m_keySearch.setTitle(hint);
	}

	public int getPopupWidth() {
		return m_popupWidth;
	}

	public void setPopupWidth(int popupWidth) {
		m_popupWidth = popupWidth;
	}
}
