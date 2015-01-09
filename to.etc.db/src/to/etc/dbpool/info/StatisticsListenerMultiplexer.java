/*
 * DomUI Java User Interface - shared code
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
package to.etc.dbpool.info;

import java.util.*;

import to.etc.dbpool.*;

/**
 * Multiplexer for all listeners to stats
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Jan 28, 2011
 */
public class StatisticsListenerMultiplexer implements IStatisticsListener {
	private Map<String, IStatisticsListener> m_list = new HashMap<String, IStatisticsListener>();

	private void check() {
		if(m_list.size() == 0)
			System.out.println("No listeners registered!!");
	}

	public void addCollector(String key, IStatisticsListener ic) {
		if(null != m_list.put(key, ic)) {
			System.out.println("POOLERR: Duplicate statistics collector with key=" + key);
		}
	}

	public IStatisticsListener removeCollector(String key) {
		return m_list.remove(key);
	}

	public void connectionAllocated() {
		check();
		for(IStatisticsListener ic : m_list.values())
			ic.connectionAllocated();
	}

	@Override
	public void statementPrepared(StatementProxy sp, long prepareDuration) {
		check();
		for(IStatisticsListener ic : m_list.values())
			ic.statementPrepared(sp, prepareDuration);
	}

	@Override
	public void queryStatementExecuted(StatementProxy sp, long executeDuration, long fetchDuration, int rowCount, boolean prepared) {
		check();
		for(IStatisticsListener ic : m_list.values())
			ic.queryStatementExecuted(sp, executeDuration, fetchDuration, rowCount, prepared);
	}

	@Override
	public void executeUpdateExecuted(StatementProxy sp, long updateDuration, int updatedrowcount) {
		check();
		for(IStatisticsListener ic : m_list.values())
			ic.executeUpdateExecuted(sp, updateDuration, updatedrowcount);
	}

	@Override
	public void executeExecuted(StatementProxy sp, long updateDuration, Boolean result) {
		check();
		for(IStatisticsListener ic : m_list.values())
			ic.executeExecuted(sp, updateDuration, result);
	}

	@Override
	public void executePreparedUpdateExecuted(StatementProxy sp, long updateDuration, int rowcount) {
		check();
		for(IStatisticsListener ic : m_list.values())
			ic.executePreparedUpdateExecuted(sp, updateDuration, rowcount);
	}

	@Override public void executeBatchExecuted(long executeDuration, int totalStatements, int totalRows, List<BatchEntry> list) {
		check();
		for(IStatisticsListener ic : m_list.values())
			ic.executeBatchExecuted(executeDuration, totalStatements, totalRows, list);
	}

	@Override
	public void finish() {
		check();
		for(IStatisticsListener ic : m_list.values())
			ic.finish();
	}
}
