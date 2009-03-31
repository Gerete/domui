package to.etc.domui.dom.html;

import to.etc.domui.util.*;

public class Table extends NodeContainer {
	private String		m_cellPadding;
	private String		m_cellSpacing;
	private String		m_tableWidth;
	private String		m_tableHeight;
	private int			m_tableBorder = -1;
//	private THead		m_head;
//	private TBody		m_body;

	public Table() {
		super("table");
	}
	@Override
	public void visit(NodeVisitor v) throws Exception {
		v.visitTable(this);
	}
	public String getCellPadding() {
		return m_cellPadding;
	}
	public void setCellPadding(String cellPadding) {
		if(DomUtil.isEqual(cellPadding, m_cellPadding))
			return;
		changed();
		m_cellPadding = cellPadding;
	}
	public String getCellSpacing() {
		return m_cellSpacing;
	}
	public void setCellSpacing(String cellSpacing) {
		if(DomUtil.isEqual(cellSpacing, m_cellSpacing))
			return;
		changed();
		m_cellSpacing = cellSpacing;
	}
	public String getTableWidth() {
		return m_tableWidth;
	}
	public void setTableWidth(String tableWidth) {
		if(! DomUtil.isEqual(tableWidth, m_tableWidth))
			changed();
		m_tableWidth = tableWidth;
	}
	public int getTableBorder() {
		return m_tableBorder;
	}
	public void setTableBorder(int tableBorder) {
		if(tableBorder != m_tableBorder)
			changed();
		m_tableBorder = tableBorder;
	}

	/**
	 * Quicky thingy to set a table header.
	 * @param labels
	 */
	public void	setTableHead(String... labels) {
		THead	h	= getHead();
		h.forceRebuild();
		TR	row	= new TR();
		h.add(row);
		for(String s: labels) {
			TH	th = new TH();
			row.add(th);
			th.setText(s);
		}
	}

	@Override
	public void add(int index, NodeBase nd) {
		if(nd instanceof TR) {
			if(true)
				throw new IllegalStateException("Add TR's to the TBody, not the Table");
			System.out.println("info: Please use a TBody in a table to add rows to; I now have to add it by myself, slowly.");
			getBody().add(index, nd);
		} else
			super.add(index, nd);
	}
	@Override
	public void add(NodeBase nd) {
		if(nd instanceof TR) {
			if(true)
				throw new IllegalStateException("Add TR's to the TBody, not the Table");
			System.out.println("info: Please use a TBody in a table to add rows to; I now have to add it by myself, slowly.");
			getBody().add(nd);
		} else
			super.add(nd);
	}
	@Override
	public void add(String txt) {
		throw new IllegalStateException("Dont be silly- cannot add text to a table");
	}

	public TBody	getBody() {
		for(int i = getChildCount(); --i >= 0;) {
			NodeBase n = getChild(i);
			if(n instanceof TBody)
				return (TBody)n;
		}
		TBody b = new TBody();
		super.add(b);
		return b;
	}
	public THead	getHead() {
		for(int i = getChildCount(); --i >= 0;) {
			NodeBase n = getChild(i);
			if(n instanceof THead)
				return (THead)n;
		}
		THead b = new THead();
		super.add(b);
		return b;
	}

	public TBody	addBody() {
		TBody b = new TBody();
		add(b);
		return b;
	}
	public String getTableHeight() {
		return m_tableHeight;
	}
	public void setTableHeight(String tableHeight) {
		m_tableHeight = tableHeight;
	}
}
