package to.etc.domui.component.tbl;

import java.util.*;

import to.etc.domui.util.*;
import to.etc.domui.util.query.*;
import to.etc.util.*;

/**
 * 
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Jun 16, 2008
 */
public class SimpleSearchModel<T> extends TableListModelBase<T> implements IKeyedTableModel<T>, ITruncateableDataModel, ISortableTableModel, IShelvedListener {
	/** Thingy to get a database session from, if needed, */
	private QDataContextSource		m_sessionSource;

	/** Generalized search query. */
	private QCriteria<T>			m_query;

	private List<T>					m_workResult;

	private boolean[]				m_workRefreshed;

	private boolean					m_truncated;

	private String					m_sort;

	private boolean					m_desc;

	private boolean					m_refreshAfterShelve;

	public SimpleSearchModel(QDataContextSource ss, QCriteria<T> qc) {
		m_query = qc;
		m_sessionSource = ss;
	}
	public void setRefreshAfterShelve(boolean refreshAfterShelve) {
		m_refreshAfterShelve = refreshAfterShelve;
	}
	public boolean isRefreshAfterShelve() {
		return m_refreshAfterShelve;
	}
	protected void	execQuery() throws Exception {
		long ts = System.nanoTime();
		QCriteria<T>	qc = m_query;				// Get the base query,
		if(qc.getLimit() <= 0)
			qc.limit(1001);
		if(m_sort != null) {						// Are we sorting?
			qc = qc.dup();							// Copy original query
			if(m_desc)
				qc.descending(m_sort);
			else
				qc.ascending(m_sort);
		}
		QDataContext	qs	= m_sessionSource.getDataContext();	// Create/get session
		m_workResult = qs.query(qc);		// Execute the query.
		if(m_workResult.size() > 1000) {
			m_workResult.remove(m_workResult.size()-1);
			m_truncated = true;
		} else
			m_truncated = false;
		ts = System.nanoTime() - ts;
		System.out.println("db: persistence framework query and materialize took "+StringTool.strNanoTime(ts));
	}

	public boolean isTruncated() {
		return m_truncated;
	}
	public int getTruncatedCount() {
		return isTruncated() ? 1000 : 0;
	}

	protected void	initResult() throws Exception {
		if(m_workResult == null)
			execQuery();
	}

	@Override
	protected List<T> getList() throws Exception {
		initResult();
		return m_workResult;
	}

	@Override
	@SuppressWarnings("deprecation")
	public List<T> getItems(int start, int end) throws Exception {
		initResult();
		if(start < 0)
			start = 0;
		if(end > getRows())
			end = getRows();
		if(end <= start)
			return Collections.EMPTY_LIST;
		if(isRefreshAfterShelve()) {
			//-- Make sure a refreshed map is present,
			if(m_workRefreshed == null)
				m_workRefreshed = new boolean[m_workResult.size()];
			QDataContext	qs = null;
			for(int i = start; i < end; i++) {
				if(! m_workRefreshed[i]) {
					if(qs == null)
						qs	= m_sessionSource.getDataContext();	// Create/get session
					qs.refresh(m_workResult.get(i));
					m_workRefreshed[i] = true;
				}
			}
		}

		return m_workResult.subList(start, end);
	}

	public T findRowObject(String key) throws Exception {
		throw new IllegalStateException("Not implemented");
	}

	/**
	 * @see to.etc.domui.component.tbl.ITableModel#getRowKey(int)
	 */
	public String getRowKey(int row) throws Exception {
		throw new IllegalStateException("Not implemented");
	}

	public void	clear() {
		m_workResult = null;
		m_workRefreshed = null;
	}
	
	/*--------------------------------------------------------------*/
	/*	CODING:	SortableTableModel implementation.					*/
	/*--------------------------------------------------------------*/

	/**
	 * When called this does a re-query using the specified sort property.
	 */
	public void sortOn(String key, boolean descending) throws Exception {
		if(DomUtil.isEqual(key, m_sort) && descending == m_desc)		// Nothing changed, get lost.
			return;
		clear();
		m_desc = descending;
		m_sort = key;
//		initResult();			20080730 jal lazily init,
		fireModelChanged();
	}
	public String getSortKey() {
		return m_sort;
	}
	public boolean isSortDescending() {
		return m_desc;
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	IShelveListener implementation.						*/
	/*--------------------------------------------------------------*/
	/**
	 * When the component is shelved we discard all results. This causes a requery when
	 * unshelved (when accessed).
	 *
	 * @see to.etc.domui.util.IShelvedListener#onShelve()
	 */
	public void onShelve() throws Exception {
		System.out.println("Shelving the model");
		clear();
	}
	public void onUnshelve() throws Exception {
	}
}
