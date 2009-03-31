package to.etc.domui.util.query;

public interface QNodeVisitor {
	public void		visitCriteria(QCriteria<?> qc) throws Exception;
	public void		visitBinaryNode(QBinaryNode n) throws Exception;
	public void		visitUnaryNode(QUnaryNode n) throws Exception;
	public void		visitLiteral(QLiteral n) throws Exception;
	public void		visitMulti(QMultiNode n) throws Exception;
	public void		visitPropertyNode(QPropertyNode n) throws Exception;
	public void		visitOrder(QOrder o) throws Exception;
	public void		visitBetween(QBetweenNode n) throws Exception;
}
