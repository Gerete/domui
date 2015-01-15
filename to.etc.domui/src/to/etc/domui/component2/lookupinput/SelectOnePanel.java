package to.etc.domui.component2.lookupinput;

import java.util.*;

import javax.annotation.*;

import to.etc.domui.component.combobox.*;
import to.etc.domui.dom.html.*;
import to.etc.domui.util.*;

/**
 * A panel that is meant as a dropdown for a {@link SearchInput2} or {@link ComboBoxBase}. It
 * shows a list of selectable items that floats connected to a parent; the items can be selected
 * with either keyboard or mouse.
 *
 * <h2>Selecting elements</h2>
 *
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Jul 13, 2014
 */
public class SelectOnePanel<T> extends Div implements IHasChangeListener {
	@Nonnull
	final private List<T> m_itemList;

	final private INodeContentRenderer<T> m_renderer;

	private int m_index;

	private IValueChanged< ? > m_valueChanged;

	public SelectOnePanel(@Nonnull List<T> itemList, @Nonnull INodeContentRenderer<T> renderer) {
		m_itemList = itemList;
		m_renderer = renderer;
	}

	@Override
	public void createContent() throws Exception {
		setCssClass("ui-ssop");
		Table tbl = new Table();
		add(tbl);
		TBody body = new TBody();
		tbl.add(body);
		tbl.setCellPadding("0");
		tbl.setCellSpacing("0");
		for(T item : m_itemList) {
			renderItem(body, item);
		}
		appendCreateJS("new WebUI.SelectOnePanel('" + getActualID() + "');");
		m_index = -1;
	}

	private void renderItem(@Nonnull TBody body, @Nonnull T item) throws Exception {
		TR row = body.addRow();
		row.setCssClass("ui-ssop-row");
		row.setClicked(new IClicked<NodeBase>() {
			@Override
			public void clicked(NodeBase clickednode) throws Exception {

			}
		});
		TD cell = row.addCell();
		m_renderer.renderNodeContent(row, cell, item, null);
	}

	@Override
	public boolean acceptRequestParameter(String[] values) throws Exception {
		int value = -1;
		if(values.length == 1) {
			String s = values[0];
			if(s != null && s.trim().length() > 0) {
				try {
					value = Integer.parseInt(s);
				} catch(Exception x) {}
			}
		}

		boolean changed = m_index != value;
		m_index = value;
		System.out.println("selectOnePanel: value=" + value + ", changed=" + changed);
		return changed;
	}

	public int getIndex() {
		return m_index;
	}

	@Nullable
	public T getValue() {
		int index = getIndex();
		if(index < 0 || index >= m_itemList.size())
			return null;
		return m_itemList.get(index);
	}

	@Override
	public IValueChanged< ? > getOnValueChanged() {
		return m_valueChanged;
	}

	@Override
	public void setOnValueChanged(IValueChanged< ? > valueChanged) {
		m_valueChanged = valueChanged;
	}
}