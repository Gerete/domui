package to.etc.domui.component.input;

import javax.annotation.*;

import to.etc.domui.component.meta.*;
import to.etc.domui.dom.html.*;
import to.etc.domui.util.*;

/**
 * This is a binder to be used when IDisplayControl's are to be bound
 * to a model.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Feb 15, 2010
 */
public class DisplayOnlyBinder implements IBinder {
	@Nonnull
	private IDisplayControl< ? > m_control;

	/** If this contains whatever property-related binding this contains the property's meta model, needed to use it's value accessor. */
	@Nullable
	private PropertyMetaModel m_propertyModel;

	/** If this is bound to some model this contains the model, */
	@Nullable
	private IReadOnlyModel< ? > m_model;

	/** If this is bound to an object instance directly it contains the instance */
	@Nullable
	private Object m_instance;

	/** If this thing is bound to some event listener... */
	@Nullable
	private IBindingListener< ? > m_listener;

	public DisplayOnlyBinder(@Nonnull IDisplayControl< ? > control) {
		if(control == null)
			throw new IllegalArgumentException("The control cannot be null.");
		m_control = control;
	}

	/**
	 * Returns T if this contains an actual binding. We are bound if property is set OR a listener is set.
	 * @see to.etc.domui.component.input.IBinder#isBound()
	 */
	public boolean isBound() {
		return m_propertyModel != null || m_listener != null;
	}

	/**
	 * Bind to a property of the object returned by this model.
	 * @see to.etc.domui.component.input.IBinder#to(java.lang.Class, to.etc.domui.util.IReadOnlyModel, java.lang.String)
	 */
	public <T> void to(@Nonnull Class<T> theClass, @Nonnull IReadOnlyModel<T> model, @Nonnull String property) {
		if(theClass == null || property == null || model == null)
			throw new IllegalArgumentException("Argument cannot be null");
		m_listener = null;
		m_propertyModel = MetaManager.getPropertyMeta(theClass, property);
		m_model = model;
		m_instance = null;
	}

	/**
	 * Bind to a property on some model whose metadata is passed.
	 * @param <T>
	 * @param model
	 * @param pmm
	 */
	public <T> void to(@Nonnull IReadOnlyModel<T> model, @Nonnull PropertyMetaModel pmm) {
		if(pmm == null || model == null)
			throw new IllegalArgumentException("Argument cannot be null");
		m_listener = null;
		m_propertyModel = pmm;
		m_model = model;
		m_instance = null;
	}

	/**
	 *
	 * @see to.etc.domui.component.input.IBinder#to(to.etc.domui.component.input.IBindingListener)
	 */
	public void to(@Nonnull IBindingListener< ? > listener) {
		if(listener == null)
			throw new IllegalArgumentException("Argument cannot be null");
		m_propertyModel = null;
		m_instance = null;
		m_model = null;
		m_listener = listener;
	}

	/**
	 * Bind to a property of the instance specified.
	 *
	 * @see to.etc.domui.component.input.IBinder#to(java.lang.Object, java.lang.String)
	 */
	public void to(@Nonnull Object instance, @Nonnull String property) {
		if(instance == null || property == null)
			throw new IllegalArgumentException("The instance in a component bind request CANNOT be null!");
		to(instance, MetaManager.getPropertyMeta(instance.getClass(), property));
	}

	/**
	 * Bind to a propertyMetaModel and the given instance.
	 * @param instance
	 * @param pmm
	 */
	public void to(@Nonnull Object instance, @Nonnull PropertyMetaModel pmm) {
		if(instance == null || pmm == null)
			throw new IllegalArgumentException("Parameters in a bind request CANNOT be null!");
		m_listener = null;
		m_model = null;
		m_propertyModel = pmm;
		m_instance = instance;
	}


	/*--------------------------------------------------------------*/
	/*	CODING:	IModelBinding interface implementation.				*/
	/*--------------------------------------------------------------*/
	/**
	 * Move the control value to wherever it's needed. If this is a listener binding it calls the listener,
	 * else it moves the value either to the model's value or the instance's value.
	 * @see to.etc.domui.component.form.IModelBinding#moveControlToModel()
	 */
	public void moveControlToModel() throws Exception {
		if(m_listener != null)
			((IBindingListener<NodeBase>) m_listener).moveControlToModel((NodeBase) m_control); // Stupid generics idiocy requires cast
		else {
			Object val = m_control.getValue();
			Object base = m_instance == null ? m_model.getValue() : m_instance;
			IValueAccessor<Object> a = (IValueAccessor<Object>) m_propertyModel.getAccessor();
			a.setValue(base, val);
		}
	}

	public void moveModelToControl() throws Exception {
		if(m_listener != null)
			((IBindingListener<NodeBase>) m_listener).moveModelToControl((NodeBase) m_control); // Stupid generics idiocy requires cast
		else {
			Object base = m_instance == null ? m_model.getValue() : m_instance;
			IValueAccessor< ? > vac = m_propertyModel.getAccessor();
			if(vac == null)
				throw new IllegalStateException("Null IValueAccessor<T> returned by PropertyMeta " + m_propertyModel);
			Object pval = vac.getValue(base);
			((IDisplayControl<Object>) m_control).setValue(pval);
		}
	}

	/**
	 * Not applicable for display-only controls.
	 * @see to.etc.domui.component.form.IModelBinding#setControlsEnabled(boolean)
	 */
	public void setControlsEnabled(boolean on) {
	}
}