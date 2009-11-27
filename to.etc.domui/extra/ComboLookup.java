package to.etc.domui.component.input;

import java.util.*;

import to.etc.domui.component.meta.*;
import to.etc.domui.dom.errors.*;
import to.etc.domui.dom.html.*;
import to.etc.domui.server.*;
import to.etc.domui.trouble.*;
import to.etc.domui.util.*;

/**
 * INCOMPLETE! A select button which does automagic lookup of it's data.
 *
 * <h2>Selected value binding</h2>
 * <p>The current version of this control retrieves it's list-of-values once, and
 * 	keeps it in memory. It can then easily retrieve the selected item by just
 *	determining the index of the selection as passed in the request. Each option
 *	box uses as it's value it's own ID; to retrieve the index of the actual selected
 *	item we just have to check the index of the Option Node in the select's child
 *	list.
 * </p>
 * <h2>Future value binding</h2>
 * <p>This combo returns as it's value one of the items added to it's list of
 * items. For normal items we will use the index of the item in the source
 * item list (as returned by IComboDataSet) as the "key" to indicate which item is
 * selected. This means that <b>every</b> retrieval of the list <b>must</b> return
 * the same list, and in the same order as it was added. This method of operation
 * works flawless for things like static lists and lists of enum classes.</p>
 * <p>It does not work for lists that are obtained from a database, since the order
 * of the items returned cannot be guaranteed, the database may have aquired extra
 * items between calls etc. For these kinds of results we need something to translate
 * the item to a "key value string", and to convert that string back to the actual
 * selected instance when needed.</p>
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Jul 11, 2008
 */
public class ComboLookup<T> extends Select implements IInputNode<T>, IHasModifiedIndication {
	private Class< ? extends IComboDataSet<T>> m_dataSetClass;

	private IComboDataSet<T> m_dataSet;

	private INodeContentRenderer<T> m_contentRenderer;

	private List<T> m_dataList;

	private T m_currentValue;

	private String m_emptyText;

	//	private Class<IKeyTranslator<T>>	m_keyTranslatorClass;

	public ComboLookup(Class< ? extends IComboDataSet<T>> set, INodeContentRenderer<T> renderer) {
		m_dataSetClass = set;
		m_contentRenderer = renderer;
	}

	public ComboLookup(IComboDataSet<T> set, INodeContentRenderer<T> renderer) {
		m_dataSet = set;
		m_contentRenderer = renderer;
	}

	public ComboLookup(Class< ? extends IComboDataSet<T>> set) {
		m_dataSetClass = set;
	}

	private void calculateContentRenderer(Object val) {
		if(val == null)
			throw new IllegalStateException("Cannot calculate content renderer for null value");
		ClassMetaModel cmm = MetaManager.findClassMeta(val.getClass());
		m_contentRenderer = (INodeContentRenderer<T>) MetaManager.createDefaultComboRenderer(null, cmm);
	}

	@Override
	public void createContent() throws Exception {
		List<T> res = getData();
		if(res == null)
			return;
		if(!isMandatory()) {
			//-- Add 1st "empty" thingy representing the unchosen.
			SelectOption o = new SelectOption();
			if(getEmptyText() != null)
				o.setText(getEmptyText());
			add(o);
			o.setSelected(m_currentValue == null);
		}

		for(T val : res) {
			if(m_contentRenderer == null) {
				calculateContentRenderer(val);
			}

			SelectOption o = new SelectOption();
			add(o);
			m_contentRenderer.renderNodeContent(this, o, val, null);
			o.setSelected(val == m_currentValue);
		}
	}

	@Override
	public void forceRebuild() {
		super.forceRebuild();
		m_dataList = null;
	}

	public List<T> getData() throws Exception {
		if(m_dataList == null) {
			IComboDataSet<T> builder = m_dataSet != null ? m_dataSet : DomApplication.get().createInstance(m_dataSetClass);
			m_dataList = builder.getComboDataSet(getPage().getConversation(), null);
		}
		return m_dataList;
	}

	/**
	 * @see to.etc.domui.dom.html.IInputNode#getValue()
	 */
	public T getValue() {
		if(isMandatory() && m_currentValue == null) {
			setMessage(UIMessage.error(Msgs.BUNDLE, Msgs.MANDATORY));
			throw new ValidationException(Msgs.NOT_VALID, "null");
		}
		return m_currentValue;
	}

	/**
	 * @see to.etc.domui.dom.html.IInputNode#setValue(java.lang.Object)
	 */
	public void setValue(T v) {
		if(DomUtil.isEqual(v, m_currentValue))
			return;
		m_currentValue = v;
		forceRebuild();
	}

	/**
	 * @see to.etc.domui.dom.html.IInputNode#getValueSafe()
	 */
	@Override
	public T getValueSafe() {
		return DomUtil.getValueSafe(this);
	}

	/**
	 * @see to.etc.domui.dom.html.IInputNode#hasError()
	 */
	@Override
	public boolean hasError() {
		getValueSafe();
		return super.hasError();
	}

	public String getEmptyText() {
		return m_emptyText;
	}

	public void setEmptyText(String emptyText) {
		m_emptyText = emptyText;
	}

	@Override
	public boolean acceptRequestParameter(String[] values) throws Exception {
		String in = values[0]; // Must be the ID of the selected Option thingy.
		SelectOption selo = (SelectOption) getPage().findNodeByID(in);
		T oldvalue = m_currentValue;
		if(selo == null) {
			updateCurrent(null); // Nuttin' selected @ all.
		} else {
			int index = findChildIndex(selo); // Must be found
			if(index == -1)
				throw new IllegalStateException("Where has my child " + in + " gone to??");
			if(!isMandatory()) {
				//-- If the index is 0 we have the "unselected" thingy; if not we need to decrement by 1 to skip that entry.
				if(index == 0)
					updateCurrent(null); // "Unselected"
				index--; // IMPORTANT Index becomes -ve if value lookup may not be done!
			}

			if(index >= 0) {
				List<T> data = getData();
				if(index >= data.size()) {
					updateCurrent(null); // Unexpected: value has gone.
				} else
					updateCurrent(data.get(index)); // Retrieve actual value.
			}
		}

		//-- Determine if things have changed...
		ClassMetaModel cmm = (m_currentValue != null ? MetaManager.findClassMeta(m_currentValue.getClass()) : null);
		if(MetaManager.areObjectsEqual(oldvalue, m_currentValue, cmm))
			return false; // Unchanged

		DomUtil.setModifiedFlag(this);
		return true;
	}

	private void updateCurrent(T newval) {
		m_currentValue = newval;
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	IBindable interface (EXPERIMENTAL)					*/
	/*--------------------------------------------------------------*/

	/** When this is bound this contains the binder instance handling the binding. */
	private SimpleBinder m_binder;

	/**
	 * Return the binder for this control.
	 * @see to.etc.domui.component.input.IBindable#bind()
	 */
	public IBinder bind() {
		if(m_binder == null)
			m_binder = new SimpleBinder(this);
		return m_binder;
	}

	/**
	 * Returns T if this control is bound to some data value.
	 *
	 * @see to.etc.domui.component.input.IBindable#isBound()
	 */
	public boolean isBound() {
		return m_binder != null && m_binder.isBound();
	}
}