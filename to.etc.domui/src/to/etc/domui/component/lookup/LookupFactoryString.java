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
package to.etc.domui.component.lookup;

import javax.annotation.*;

import to.etc.domui.component.input.*;
import to.etc.domui.component.meta.*;
import to.etc.domui.dom.html.*;
import to.etc.webapp.query.*;

@SuppressWarnings("unchecked")
final class LookupFactoryString implements ILookupControlFactory {
	@Override
	public <T, X extends IControl<T>> int accepts(final @Nonnull SearchPropertyMetaModel spm, final X control) {
		if(control != null) {
			if(!(control instanceof Text< ? >))
				return -1;
			Text< ? > t = (Text< ? >) control;
			if(t.getInputClass() != String.class)
				return -1;
		}
		return 1; // Accept all properties (will fail on incompatible ones @ input time)
	}

	@Override
	public <T, X extends IControl<T>> ILookupControlInstance<T> createControl(final @Nonnull SearchPropertyMetaModel spm, final X control) {
		final PropertyMetaModel<T> pmm = (PropertyMetaModel<T>) MetaUtils.getLastProperty(spm);
		Class<T> iclz = pmm.getActualType();

		//-- Boolean/boolean types? These need a tri-state checkbox
		if(iclz == Boolean.class || iclz == Boolean.TYPE) {
			throw new IllegalStateException("I need a tri-state checkbox component to handle boolean lookup thingies.");
		}

		//-- Treat everything else as a String using a converter.
		final Text<T> txt = new Text<T>(iclz);
		if(pmm.getDisplayLength() > 0) {
			int sz = pmm.getDisplayLength();
			if(sz > 40)
				sz = 40;
			txt.setSize(sz);
		} else {
			//-- We must decide on a length....
			int sz = 0;
			if(pmm.getLength() > 0) {
				sz = pmm.getLength();
				if(sz > 40)
					sz = 40;
			}
			if(sz != 0)
				txt.setSize(sz);
		}
		if(pmm.getConverter() != null)
			txt.setConverter(pmm.getConverter());
		if(pmm.getLength() > 0)
			txt.setMaxLength(pmm.getLength());
		String hint = MetaUtils.findHintText(spm);
		if(hint != null)
			txt.setTitle(hint);

		//-- Converter thingy is known. Now add a
		return new BaseAbstractLookupControlImpl<T>(txt) {
			@Override
			public @Nonnull AppendCriteriaResult appendCriteria(@Nonnull QCriteria< ? > crit) throws Exception {
				Object value = null;
				try {
					value = txt.getValue();
				} catch(Exception x) {
					return AppendCriteriaResult.INVALID; // Has validation error -> exit.
				}
				if(value == null || (value instanceof String && ((String) value).trim().length() == 0))
					return AppendCriteriaResult.EMPTY; // Is okay but has no data

				// FIXME Handle minimal-size restrictions on input (search field metadata


				//-- Put the value into the criteria..
				if(value instanceof String) {
					String str = (String) value;
					str = str.trim().replace("*", "%") + "%";
					crit.ilike(spm.getPropertyName(), str);
				} else {
					crit.eq(spm.getPropertyName(), value); // property == value
				}
				return AppendCriteriaResult.VALID;
			}

			@Override
			public T getValue() {
				return txt.getValue();
			}

			@Override
			public void setValue(T value) {
				txt.setValue(value);
			}
		};
	}
}
