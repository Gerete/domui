package to.etc.domui.component.tbl;

import java.util.*;

import javax.annotation.*;

import to.etc.domui.component.meta.*;
import to.etc.domui.component.meta.impl.*;

/**
 * Row renderer that is used for MultipleSelectionLookup control.
 * First selection indicator column is additionaly rendered from outer code, so abstract methods that resolve selection column and total row width must be set additionaly.
 *
 * @author <a href="mailto:vmijic@execom.eu">Vladimir Mijic</a>
 * Created on 27 Oct 2009
 */
public abstract class MultipleSelectionRowRenderer<T> extends SimpleRowRenderer<T> {

	public MultipleSelectionRowRenderer(Class<T> dataClass, String[] cols) {
		super(dataClass, cols);
	}

	public MultipleSelectionRowRenderer(Class<T> dataClass) {
		super(dataClass);
	}

	public MultipleSelectionRowRenderer(@Nonnull final Class<T> dataClass, @Nonnull final ClassMetaModel cmm, final String... cols) {
		super(dataClass, cmm);
	}

	/**
	 * Initialize, using the genericized table column set. Reserve some extra space for selection indicator column that is added as first column.
	 * @param clz
	 * @param xdpl
	 */
	@Override
	protected void initialize(final List<ExpandedDisplayProperty> xdpl) {
		//-- For all properties in the list, use metadata to define'm
		final int[] widths = new int[80];
		setTotalWidth(0);
		int ix = 0;
		addColumns(xdpl, widths);
		ix = 0;
		for(final SimpleColumnDef scd : m_columnList) {
			final int pct = (100 * widths[ix++] * (getRowWidth() - getSelectionColWidth())) / (getTotalWidth() * getRowWidth());
			scd.setWidth(pct + "%");
		}
	}

	public abstract int getRowWidth();

	public abstract int getSelectionColWidth();
}
