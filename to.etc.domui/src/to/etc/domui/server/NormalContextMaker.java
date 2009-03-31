package to.etc.domui.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import to.etc.domui.state.*;

final public class NormalContextMaker extends AbstractContextMaker {
	private String				m_applicationClassName;
	private ConfigParameters	m_config;
	private DomApplication		m_application;
	
	public NormalContextMaker(String applicationClassName, ConfigParameters pp) throws Exception {
		m_applicationClassName = applicationClassName;
		m_config = pp;

		//-- Load class,
		Class<?>	clz;
		try {
			clz	= getClass().getClassLoader().loadClass(applicationClassName);
		} catch(ClassNotFoundException x) {
			throw new IllegalStateException("The main application class '"+m_applicationClassName+"' cannot be found: "+x, x);
		}

		/*
		 * We have to create/replace the application class.
		 */
		try {
			m_application = (DomApplication)clz.newInstance();
		} catch(Exception x) {
			throw new IllegalStateException("The main application class '"+m_applicationClassName+"' cannot be INSTANTIATED: "+x, x);
		}

		m_application.internalInitialize(m_config);
	}

	/**
	 * Create a normal context.
	 * @see to.etc.domui.server.ContextMaker#createContext(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	public boolean handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		//-- Get session,
		try {
			HttpSession	sess	= request.getSession(true);
			AppSession	ass;
			synchronized(sess) {
				ass	= (AppSession) sess.getAttribute(AppSession.class.getName());
				if(ass == null) {
					ass	= m_application.createSession();
					sess.setAttribute(AppSession.class.getName(), ass);
				}
			}
			ass.internalInitialize(m_application);
			DomApplication.internalSetCurrent(m_application);
			RequestContextImpl ctx = new RequestContextImpl(m_application, ass, request, response);
			return execute(ctx);
		} finally {
			DomApplication.internalSetCurrent(null);
		}
	}
}
