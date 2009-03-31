package to.etc.domui.dom.errors;

import java.util.*;

import to.etc.domui.dom.html.*;
import to.etc.domui.server.*;

/**
 * When controls or business logic encounters errors that need to be
 * reported back to the user they add an error to either a control (usually
 * for validation/conversion errors) or to the page itself (for errors where
 * there's no clear "location" where the problem has occured).
 *
 * Making these errors visible is not the responsibility of a component, but
 * is delegated to one or more ErrorMessageListeners. These listeners get
 * called when an error is registered with a component (or when an error
 * is cleared).
 *
 * The error listener is responsible for handling the actual reporting of the error,
 * and it usually does this by altering the output tree, for instance by adding
 * the error message to the page's defined "error box" and making that box visible. Other
 * listeners can change the CSS Class of the error node in question, causing it to be
 * displayed in a different color for instance.
 *
 * If a page has no registered error handlers it "inherits" the default error handlers
 * from the current Application. By overriding that one you can easily alter the way
 * errors are reported in the entire application.
 *
 * Special components that handle error messages also exist, and these components usually
 * register themselves as listeners when they are added to the tree. This is the best method
 * of handling error reporting because the page designer can easily determine where they are
 * shown.
 */
public class ErrorFenceHandler implements IErrorFence {
	private NodeContainer				m_container;

	/**
	 * The list of thingies that need to know about page errors.
	 */
	private List<IErrorMessageListener>	m_errorListeners = Collections.EMPTY_LIST;

	private List<UIMessage>				m_messageList = Collections.EMPTY_LIST;

	public ErrorFenceHandler(NodeContainer container) {
		m_container = container;
	}
	public NodeContainer getContainer() {
		return m_container;
	}

	/**
	 * Add a new error message listener to the page.
	 */
	public void addErrorListener(IErrorMessageListener eml) {
		if(m_errorListeners == Collections.EMPTY_LIST)
			m_errorListeners = new ArrayList<IErrorMessageListener>(4);
		if(! m_errorListeners.contains(eml))
			m_errorListeners.add(eml);
	}

	/**
	 * Discard an error message listener.
	 * @param eml
	 */
	public void	removeErrorListener(IErrorMessageListener eml) {
		m_errorListeners.remove(eml);
	}

	public void addMessage(NodeBase source, UIMessage uim) {
		if(m_messageList == Collections.EMPTY_LIST)
			m_messageList = new ArrayList<UIMessage>(15);
		m_messageList.add(uim);
		
		// ; now call all pending listeners. If this page has NO listeners we use the application default.
		if(m_errorListeners.size() == 0) {
			//-- No default listeners: this means errors will not be visible. Ask the application to add an error handling component.
			DomApplication.get().addDefaultErrorComponent(getContainer());	// Ask the application to add,
		}
		for(IErrorMessageListener eml : m_errorListeners) {
			try {
				eml.errorMessageAdded(source.getPage(), uim);
			} catch(Exception x) {
				x.printStackTrace();
			}
		}
	}

	public void removeMessage(NodeBase source, UIMessage uim) {
		if(! m_messageList.remove(uim))				// Must be known to the page or something's wrong..
			return;

		//-- Call the listeners.
		List<IErrorMessageListener>	list = m_errorListeners;
		for(IErrorMessageListener eml : list) {
			try {
				eml.errorMessageRemoved(source.getPage(), uim);
			} catch(Exception x) {
				x.printStackTrace();
			}
		}
	}

	public void	clearGlobalMessages(NodeBase source, String code) {
		List<UIMessage>	todo = new ArrayList<UIMessage>();
		for(UIMessage m: m_messageList) {
			if(m.getErrorNode() == null && (code == null || code.equals(m.getCode())))
				todo.add(m);
		}

		//-- Remove all messages from the list,
		for(UIMessage m: todo)
			removeMessage(source, m);
	}

//	public void	clearError(NodeBase component, String code) {
//		List<UIMessage>	todo = new ArrayList<UIMessage>();
//		for(UIMessage m: m_messageList) {
//			if(m.getErrorNode() == component && (code != null && m.getCode().equals(code)))
//				todo.add(m);
//		}
//		for(UIMessage m: todo) {
//			internalRemoveMessage(m);
//		}
//	}
}
