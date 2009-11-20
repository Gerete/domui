package to.etc.domui.dom.errors;


/**
 * FIXME Bad name
 *
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Sep 22, 2009
 */
public interface INodeErrorDelegate {
	/**
	 * This sets a message (an error, warning or info message) on this control. If the
	 * control already has an error then we check if the severity of the new error is
	 * higher than the severity of the existing one; only in that case will the error
	 * be removed. To clear the error message call clearMessage().
	 *
	 * @param message
	 */
	public UIMessage setMessage(UIMessage msg);

	/**
	 * Remove this-component's "current" error message, if present.
	 */
	public void clearMessage();

	public UIMessage getMessage();
}
