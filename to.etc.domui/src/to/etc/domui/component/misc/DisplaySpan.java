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
package to.etc.domui.component.misc;

import javax.annotation.*;

import to.etc.domui.component.input.*;
import to.etc.domui.component.meta.*;
import to.etc.domui.converter.*;
import to.etc.domui.dom.html.*;
import to.etc.domui.util.*;
import to.etc.webapp.nls.*;

/**
 * This is a special control which can be used to display all kinds of values as a span without any formatting. It is
 * a "span" containing some value that can be converted, translated and whatnot. It is meant for not too complex values
 * that are usually represented as a span. It is the non-formatted brother of {@link DisplayValue} which has formatting
 * applied to it.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Feb 15, 2010
 */
public class DisplaySpan<T> extends Span implements IDisplayControl<T>, IBindable, IConvertable<T> {
	@Nonnull
	private Class<T> m_valueClass;

	@Nullable
	private T m_value;

	/**If the value is to be converted use this converter for it. */
	@Nullable
	private IConverter<T> m_converter;

	@Nullable
	private INodeContentRenderer<T> m_renderer;

	/** The string to display when the field is empty. */
	@Nullable
	private String m_emptyString;

	public DisplaySpan(@Nonnull Class<T> valueClass) {
		m_valueClass = valueClass;
	}

	public DisplaySpan(@Nonnull T literal) {
		m_valueClass = (Class<T>) literal.getClass();
		m_value = literal;
	}

	@Nonnull
	public Class<T> getValueClass() {
		return m_valueClass;
	}

	/**
	 * Render the content in some way. It uses the following logic:
	 * <ul>
	 *	<li>If the value is null leave the cell with the "empty" value.</li>
	 * </ul>If a converter is present it MUST convert the value, and it's result is shown.</li>
	 * @see to.etc.domui.dom.html.NodeBase#createContent()
	 */
	@Override
	public void createContent() throws Exception {
		T val = getValue();

		//-- For an empty value put the empty string
		if(val == null) {
			setString(null);
			return;
		}

		//-- If a converter is present it *must* convert the value
		IConverter<T> converter = getConverter();
		if(converter != null) {
			String converted = converter.convertObjectToString(NlsContext.getLocale(), val);
			setString(converted);
			return;
		}

		//-- If a node renderer is set ask it to render content inside me. It is required to render proper info.
		INodeContentRenderer<T> renderer = getRenderer();
		if(renderer != null) {
			renderer.renderNodeContent(this, this, val, null); // Ask node renderer.
			if(getChildCount() == 0 && m_emptyString != null)
				add(m_emptyString);
			return;
		}

		//-- Getting slightly desperate here... Is there a "default converter" that we can use?
		IConverter<T> c = ConverterRegistry.findConverter(getValueClass()); // This version does return null if nothing is found, not a toString converter.
		if(c != null) {
			String converted = c.convertObjectToString(NlsContext.getLocale(), val);
			setString(converted);
			return;
		}

		//-- Ok: full-blown panic now. Let's try to get metadata on the value passed to see if something to show is available there.
		ClassMetaModel cmm = MetaManager.findClassMeta(val.getClass());
		if(cmm.getDomainValues() != null) {
			//-- This is a domain-based value like boolean or enum. Try to use it's default labels.
			String label = cmm.getDomainLabel(NlsContext.getLocale(), val);
			if(label == null)
				label = val.toString();
			setString(label);
			return;
		}

		/*
		 * In utter desperation try to create an INodeContentRenderer from the class meta data; this
		 * will create a toString renderer if all else fails..
		 */
		INodeContentRenderer<T> ncr = (INodeContentRenderer<T>) MetaManager.createDefaultComboRenderer(null, cmm);
		ncr.renderNodeContent(this, this, val, null);
		if(getChildCount() == 0 && m_emptyString != null)
			setText(m_emptyString);
	}

	private void setString(@Nullable String v) {
		if(v == null)
			v = m_emptyString;
		if(v != null)
			setText(v);
		else
			removeAllChildren();
	}

	/**
	 * See {@link IConvertable#getConverter()}.
	 * This returns null if no converter has been set. It also returns null if a default converter is used.
	 *
	 * @return
	 */
	@Override
	@Nullable
	public IConverter<T> getConverter() {
		return m_converter;
	}

	/**
	 * See {@link IConvertable#setConverter(IConverter)}.
	 * @param converter
	 */
	@Override
	public void setConverter(@Nullable IConverter<T> converter) {
		if(m_renderer != null && converter != null)
			throw new IllegalStateException("You cannot both use a renderer AND a converter. Set the renderer to null before setting a converter.");
		m_converter = converter;
	}

	/**
	 * The content renderer to use. <b>This gets called only if no converter is set</b>.
	 * @return
	 */
	@Nullable
	public INodeContentRenderer<T> getRenderer() {
		return m_renderer;
	}

	public void setRenderer(@Nullable INodeContentRenderer<T> renderer) {
		if(m_renderer == renderer)
			return;
		if(m_converter != null && renderer != null)
			throw new IllegalStateException("You cannot both use a renderer AND a converter. Set the converter to null before setting a renderer.");
		m_renderer = renderer;
		forceRebuild();
	}

	@Nullable
	public String getEmptyString() {
		return m_emptyString;
	}

	public void setEmptyString(@Nullable String emptyString) {
		m_emptyString = emptyString;
	}


	/*--------------------------------------------------------------*/
	/*	CODING:	IBindable interface (EXPERIMENTAL)					*/
	/*--------------------------------------------------------------*/
	/** When this is bound this contains the binder instance handling the binding. */
	@Nullable
	private SimpleBinder m_binder;

	/**
	 * Return the binder for this control.
	 * @see to.etc.domui.component.input.IBindable#bind()
	 */
	@Override
	@Nonnull
	public IBinder bind() {
		if(m_binder == null)
			m_binder = new SimpleBinder(this);
		return m_binder;
	}

	/**
	 * Returns T if this control is bound to some data value.
	 * @see to.etc.domui.component.input.IBindable#isBound()
	 */
	@Override
	public boolean isBound() {
		return m_binder != null && m_binder.isBound();
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	IDisplayControl interface.							*/
	/*--------------------------------------------------------------*/
	/**
	 * {@inheritDoc}
	 */
	@Override
	@Nullable
	public T getValue() {
		return m_value;
	}

	/**
	 * {@inheritDoc}
	 * @see to.etc.domui.dom.html.IDisplayControl#setValue(java.lang.Object)
	 */
	@Override
	public void setValue(@Nullable T v) {
		if(DomUtil.isEqual(m_value, v))
			return;
		m_value = v;
		forceRebuild();
	}

	public void defineFrom(@Nonnull PropertyMetaModel< ? > pmm) {
		String s = pmm.getDefaultHint();
		if(s != null)
			setTitle(s);

		// FIXME Define more fully.
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	IControl implementation.							*/
	/*--------------------------------------------------------------*/
	/**
	 * {@inheritDoc}
	 * @see to.etc.domui.dom.html.IHasChangeListener#getOnValueChanged()
	 */
	@Override
	public IValueChanged< ? > getOnValueChanged() {
		return null;
	}

	@Override
	public void setOnValueChanged(IValueChanged< ? > onValueChanged) {
		//FIXME 20120802 vmijic - currently we prevent exception throwing since it raises lot of issues in pages that are using this code, introduced by switching readonly instances of components by DisplayValue...
		//throw new UnsupportedOperationException("Display control");
	}

	@Override
	public T getValueSafe() {
		return getValue();
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	public void setReadOnly(boolean ro) {}

	@Override
	public boolean isDisabled() {
		return false;
	}

	@Override
	public boolean isMandatory() {
		return false;
	}

	@Override
	public void setMandatory(boolean ro) {}

	@Override
	public void setDisabled(boolean d) {}
}
