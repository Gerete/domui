package to.etc.domui.databinding;

import java.util.*;

import javax.annotation.*;

import to.etc.domui.dom.errors.*;
import to.etc.domui.dom.html.*;

/**
 * Maintains all bindings, and their validation/error status.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Jul 21, 2013
 */
public class BindingContext {
	final private Set<Binding> m_bindingsInErrorSet = new HashSet<Binding>();

	/** Maps POJO instances to a map of bindings on it's properties. */
	final private Map<Object, Map<String, Object>> m_instanceBindingMap = new HashMap<Object, Map<String, Object>>();

	/**
	 * Create a binding between unnamed entities.
	 * @param source
	 * @param to
	 * @return
	 */
	public <T> Binding join(@Nonnull IObservableValue<T> source, @Nonnull IObservableValue<T> to) {

		throw new IllegalStateException();
	}

	void registerBinding(@Nonnull Binding binding) {

	}


	/*--------------------------------------------------------------*/
	/*	CODING:	Utility methods.									*/
	/*--------------------------------------------------------------*/

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
			for(IObservableValue< ? > ov : m_observables) {
				IObservableValue<Object> ovo = (IObservableValue<Object>) ov;
				ovo.addChangeListener(ovs);
			}
			return this;
		}
	}


	/**
	 * Start for an "unidirectional" binding: Bind.from().to();
	 * @param source
	 * @param property
	 * @return
	 */
	public <T> UnidirectionalBinding from(@Nonnull T source, @Nonnull String property) {
		if(null == source || null == property)
			throw new IllegalArgumentException("source/property cannot be null");
		IObservableValue< ? > sourceo = createObservable(source, property);
		return new UnidirectionalBinding(this, sourceo);
	}

	public <T> UnidirectionalBinding from(@Nonnull IObservableValue< ? > sourceo) {
		return new UnidirectionalBinding(this, sourceo);
	}

	/**
	 * Start for a bidirectional binding: if either side changes it updates the other. The
	 * other side is defined with {@link #to(Object, String)}.
	 * @param source
	 * @param property
	 * @return
	 */
	@Nonnull
	public <T> JoinBinding join(@Nonnull T source, @Nonnull String property) {
		if(null == source || null == property)
			throw new IllegalArgumentException("source/property cannot be null");
		IObservableValue< ? > sourceo = createObservable(source, property);
		return new JoinBinding(this, sourceo);
	}

	/**
	 * Bidirectional binding of a source Observable to some target.
	 */
	public <T> JoinBinding join(@Nonnull IObservableValue< ? > sourceo) {
		return new JoinBinding(this, sourceo);
	}

	/**
	 * Start a listening option.
	 * @param source
	 * @param property
	 * @return
	 */
	@Nonnull
	public <T> Listener onchange(@Nonnull T source, @Nonnull String... properties) {
		IObservableValue< ? >[] obsar = new IObservableValue< ? >[properties.length];
		for(int i = properties.length; --i >= 0;) {
			obsar[i] = createObservable(source, properties[i]);
		}
		return new Listener(obsar);
	}

	@Nonnull
	static <T> IObservableValue< ? > createObservable(@Nonnull T source, @Nonnull String property) {
		if(source instanceof IObservableEntity) {
			IObservableEntity oe = (IObservableEntity) source;
			IObservableValue< ? > op = oe.observableProperty(property);
			return op;
		}

		throw new IllegalArgumentException("The class  " + source.getClass() + " is not Observable.");
	}


	/*--------------------------------------------------------------*/
	/*	CODING:	Errors.												*/
	/*--------------------------------------------------------------*/

	public <T> void bindMessage(@Nonnull T instance, @Nonnull String property, @Nonnull NodeBase control) {


	}


	/**
	 * Callable by business logic, this notifies that an error has occurred or was cleared on some object.
	 * @param instance
	 * @param property
	 * @param error
	 */
	public <T> void setProperyError(@Nonnull T instance, @Nonnull String property, @Nullable UIMessage error) {
		Map<String, Object> imap = m_instanceBindingMap.get(instance);
		if(null == imap) {
			imap = new HashMap<String, Object>();
			m_instanceBindingMap.put(instance, imap);
		}

		Object b = imap.get(property);
		if(null == b) {
			//-- No binding known. Put an error object in here.
			imap.put(property, error);
		} else {
			if(b instanceof UIMessage) {
				imap.put(property, error);
			} else {
				Binding bi = (Binding) b;
				bi.setMessage(error);
			}
		}
	}

	public void bindingError(@Nonnull Binding binding, @Nullable UIMessage uiMessage) {

	}


}
