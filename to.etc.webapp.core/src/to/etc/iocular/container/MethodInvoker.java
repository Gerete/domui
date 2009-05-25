package to.etc.iocular.container;

import java.io.IOException;
import java.lang.reflect.Method;
import to.etc.iocular.def.ComponentRef;
import to.etc.util.IndentWriter;

/**
 * Defines a method invocation.
 *
 * FIXME This should also contain a reference to the OBJECT this call is to be placed on!!!
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on May 25, 2009
 */
final public class MethodInvoker {
	private final Method			m_method;

	private final ComponentRef[]	m_actuals;

	public MethodInvoker(final Method method, final ComponentRef[] actuals) {
		m_method = method;
		m_actuals = actuals;
	}

	public int getScore() {
		return m_method.getParameterTypes().length;
	}

	/**
	 * Actually invoke the method on some thingy. FIXME The "this" object should be part of the MethodInvoker definition.
	 *
	 * @param bc
	 * @return
	 * @throws Exception
	 */
	@Deprecated
	public Object invoke(final Object thisobject, final BasicContainer bc, final Object selfobject) throws Exception {
		Object[]	param = new Object[ m_actuals.length ];
		for(int i = m_actuals.length; --i >= 0;) {
			if(m_actuals[i].isSelf())
				param[i] = selfobject;
			else
				param[i] = bc.retrieve(m_actuals[i]);
		}

		return m_method.invoke(thisobject, param);
	}

	public void dump(final IndentWriter iw) throws IOException {
		iw.print("Method ");
		iw.print(m_method.toGenericString());
		iw.print(" (score ");
		iw.print(Integer.toString(getScore()));
		iw.println(")");
		if(m_actuals.length != 0) {
			iw.println("- Method parameter build plan(s):");
			iw.inc();
			for(int i = 0; i < m_actuals.length; i++) {
				iw.println("argument# "+i);
				iw.inc();
				if(m_actuals[i] == null)
					iw.println("!?!?!?! null REF!!??!");
				else
					m_actuals[i].dump(iw);
				iw.dec();
			}
			iw.dec();
		}
	}
}