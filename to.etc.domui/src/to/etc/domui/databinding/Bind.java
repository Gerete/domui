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
package to.etc.domui.databinding;

import javax.annotation.*;

import to.etc.domui.component.meta.*;
import to.etc.domui.trouble.*;
import to.etc.domui.util.*;

/**
 * Static helper to create bindings using the IObservable model.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Apr 24, 2013
 */
public class Bind {
	@Nonnull
	final protected IObservableValue< ? > m_from;

	protected IObservableValue< ? > m_to;

	protected IReadWriteModel< ? > m_tomodel;

	private IValueChangeListener< ? > m_fromListener;

	private IValueChangeListener< ? > m_toListener;

	/** If not null, the IUniConverter OR IJoinConverter which converts values for this binding. */
	@Nullable
	protected IUniConverter< ? , ? > m_converter;

	/**
	 * Unidirectional bind from a -&gt; b.
	 *
	 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
	 * Created on Apr 30, 2013
	 */
	public static class UniBind extends Bind {
		private IValueChangeListener< ? > m_fromListener;

		private UniBind(@Nonnull IObservableValue< ? > sourceo) {
			super(sourceo);
		}

		/**
		 * Belongs to a {@link Bind#from(Object, String)} call and defines the target side of
		 * an unidirectional binding. Changes in "from" move to "to", but changes in "to" do
		 * not move back.
		 *
		 * @param target
		 * @param property
		 * @return
		 */
		@Nonnull
		public <T, V> UniBind to(@Nonnull final T target, @Nonnull String property) throws Exception {
			if(null == target || null == property)
				throw new IllegalArgumentException("target/property cannot be null");

			//-- Unidirectional binds only need to have access to a setter, we do not need an Observable.
			final PropertyMetaModel<V> pmm = (PropertyMetaModel<V>) MetaManager.getPropertyMeta(target.getClass(), property);
			m_tomodel = new IReadWriteModel<V>() {
				@Override
				public V getValue() throws Exception {
					throw new IllegalStateException("Unexpected 'get' in unidirectional binding");
				}

				@Override
				public void setValue(V value) throws Exception {
					pmm.setValue(target, value);
				}
			};
			//
			//			IObservableValue<V> targeto = (IObservableValue<V>) createObservable(target, property);
			//			m_to = targeto;
			addSourceListener();

			//-- Immediately move the value of source to target too 2
			moveSourceToTarget();
			return this;
		}

		public <F, T> UniBind convert(@Nonnull IUniConverter<F, T> converter) {
			m_converter = converter;
			return this;
		}
	}

	/**
	 * Bidirectional binding a &lt;--&gt; b.
	 *
	 *
	 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
	 * Created on Apr 30, 2013
	 */
	public static class JoinBind extends Bind {
		private IValueChangeListener< ? > m_fromListener;

		private JoinBind(@Nonnull IObservableValue< ? > sourceo) {
			super(sourceo);
		}

		/**
		 * Belongs to a {@link Bind#from(Object, String)} call and defines the target side of
		 * an unidirectional binding. Changes in "from" move to "to", but changes in "to" do
		 * not move back.
		 *
		 * @param target
		 * @param property
		 * @return
		 */
		@Nonnull
		public <T, V, X, Y> JoinBind to(@Nonnull T target, @Nonnull String property, @Nullable IJoinConverter<X, Y> convert) throws Exception {
			if(null == target || null == property)
				throw new IllegalArgumentException("target/property cannot be null");
			m_converter = convert;
			IObservableValue<V> targeto = (IObservableValue<V>) createObservable(target, property);
			m_to = targeto;
			addSourceListener();
			addTargetListener();

			//-- Immediately move the value of source to target too 2
			moveSourceToTarget();
			return this;
		}

		public <T, V> JoinBind to(@Nonnull T target, @Nonnull String property) throws Exception {
			return to(target, property, null);
		}
	}

	/**
	 * Listen binding.
	 *
	 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
	 * Created on Apr 30, 2013
	 */
	static public final class Listener {
		private IObservableValue< ? >[] m_observables;

		private int m_recurse;

		public Listener(@Nonnull IObservableValue< ? >[] obsar) {
			m_observables = obsar;
		}

		@Nonnull
		public Listener call(@Nonnull final IBindingListener lsnr) {
			IValueChangeListener<Object> ovs = new IValueChangeListener<Object>() {
				@Override
				public void handleChange(@Nonnull ValueChangeEvent<Object> event) throws Exception {
					if(m_recurse > 0)
						return;
					try {
						m_recurse++;
						lsnr.valuesChanged();
					} finally {
						m_recurse--;
					}
				}
			};
			for(IObservableValue<?> ov: m_observables) {
				IObservableValue<Object> ovo = (IObservableValue<Object>) ov;
				ovo.addChangeListener(ovs);
			}
			return this;
		}
	}


	private Bind(@Nonnull IObservableValue< ? > sourceo) {
		m_from = sourceo;
	}

	/**
	 * Start for an "unidirectional" binding: Bind.from().to();
	 * @param source
	 * @param property
	 * @return
	 */
	static public <T> UniBind from(@Nonnull T source, @Nonnull String property) {
		if(null == source || null == property)
			throw new IllegalArgumentException("source/property cannot be null");
		IObservableValue< ? > sourceo = createObservable(source, property);
		return new UniBind(sourceo);
	}

	static public <T> UniBind from(@Nonnull IObservableValue< ? > sourceo) {
		return new UniBind(sourceo);
	}

	/**
	 * Start for a bidirectional binding: if either side changes it updates the other. The
	 * other side is defined with {@link #to(Object, String)}.
	 * @param source
	 * @param property
	 * @return
	 */
	@Nonnull
	static public <T> JoinBind join(@Nonnull T source, @Nonnull String property) {
		if(null == source || null == property)
			throw new IllegalArgumentException("source/property cannot be null");
		IObservableValue< ? > sourceo = createObservable(source, property);
		return new JoinBind(sourceo);
	}

	/**
	 * Bidirectional binding of a source Observable to some target.
	 */
	static public <T> JoinBind join(@Nonnull IObservableValue< ? > sourceo) {
		return new JoinBind(sourceo);
	}

	/**
	 * Start a listening option.
	 * @param source
	 * @param property
	 * @return
	 */
	@Nonnull
	static public <T> Listener onchange(@Nonnull T source, @Nonnull String... properties) {
		IObservableValue< ? >[] obsar = new IObservableValue< ? >[properties.length];
		for(int i = properties.length; --i >= 0;) {
			obsar[i] = createObservable(source, properties[i]);
		}
		return new Listener(obsar);
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Internal.											*/
	/*--------------------------------------------------------------*/
	/**
	 * Internal: move source to target using an optional conversion.
	 * @throws Exception
	 */
	protected void moveSourceToTarget() throws Exception {
		Object val;
		try {
			val = ((IObservableValue<Object>) m_from).getValue();
		} catch(ValidationException vx) {
			return;
		}
		IUniConverter<Object, Object> uc = (IUniConverter<Object, Object>) m_converter;
		if(null != uc) {
			val = uc.convertSourceToTarget(val);
		}
		if(m_to != null)
			((IObservableValue<Object>) m_to).setValue(val);
		else
			((IReadWriteModel<Object>) m_tomodel).setValue(val);
	}

	protected void moveTargetToSource() throws Exception {
		Object val;
		try {
			val = ((IObservableValue<Object>) m_to).getValue();
		} catch(ValidationException vx) {
			return;
		}

		IJoinConverter<Object, Object> uc = (IJoinConverter<Object, Object>) m_converter;
		if(null != uc) {
			val = uc.convertTargetToSource(val);
		}
		((IObservableValue<Object>) m_from).setValue(val);

	}


	/*--------------------------------------------------------------*/
	/*	CODING:	Event listener.										*/
	/*--------------------------------------------------------------*/

	/*--------------------------------------------------------------*/
	/*	CODING:	Helper code.										*/
	/*--------------------------------------------------------------*/
	/**
	 * Add a listener on FROM, so that changes there propagate to TO.
	 * @param value
	 */
	protected <V> void addSourceListener() {
		IValueChangeListener<V> ml = new IValueChangeListener<V>() {
			@Override
			public void handleChange(@Nonnull ValueChangeEvent<V> event) throws Exception {
				moveSourceToTarget();
			}
		};
		m_fromListener = ml;
		((IObservableValue<V>) m_from).addChangeListener(ml);
	}

	/**
	 * Add a listener on FROM, so that changes there propagate to TO.
	 * @param value
	 */
	protected <V> void addTargetListener() {
		IValueChangeListener<V> ml = new IValueChangeListener<V>() {
			@Override
			public void handleChange(@Nonnull ValueChangeEvent<V> event) throws Exception {
				moveTargetToSource();
			}
		};
		m_toListener = ml;
		((IObservableValue<V>) m_to).addChangeListener(ml);
	}

	@Nonnull
	static private <T> IObservableValue< ? > createObservable(@Nonnull T source, @Nonnull String property) {
		if(source instanceof IObservableEntity) {
			IObservableEntity oe = (IObservableEntity) source;
			IObservableValue< ? > op = oe.observableProperty(property);
			return op;
		}

		throw new IllegalArgumentException("The class  " + source.getClass() + " is not Observable.");
	}

}
