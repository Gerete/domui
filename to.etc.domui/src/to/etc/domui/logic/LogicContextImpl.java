package to.etc.domui.logic;

import to.etc.domui.component.meta.*;
import java.lang.reflect.*;
import java.util.*;

import javax.annotation.*;

import to.etc.domui.dom.errors.*;
import to.etc.domui.logic.errors.*;
import to.etc.webapp.*;
import to.etc.webapp.query.*;

/**
 * This context class encapsulates instantiated business logic classes, and cache data used by those
 * classes.
 *
 * Root logic context class.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Oct 15, 2012
 */
final public class LogicContextImpl implements ILogicContext {
	@Nonnull
	final private QDataContext m_dc;

	/** If we're using a dependency injection framework this should be that framework's injector for logicontext classes. */
	@Nullable
	final ILogiInjector m_injector;

	@Nonnull
	final private Map<String, QDataContext> m_dataContextMap = new HashMap<>();

	@Nonnull
	final private Map<Class< ? >, Map<Object, ILogic>> m_instanceMap = new HashMap<>();

	@Nonnull
	final private Map<Class< ? >, Object> m_classMap = new HashMap<>();

	@Nonnull
	final private List<UIMessage> m_actionMessageList = new ArrayList<>();

	@Nonnull
	final private ProblemModel m_errorModel = new ProblemModel();

	/**
	 * Create and set the default data context to use.
	 * @param dataContext
	 */
	public LogicContextImpl(@Nonnull QDataContext dataContext) {
		m_dataContextMap.put(QContextManager.DEFAULT, dataContext);
		m_dc = dataContext;
		m_injector = null;
	}

	public LogicContextImpl(@Nonnull QDataContext dataContext, @Nonnull ILogiInjector injector) {
		m_dataContextMap.put(QContextManager.DEFAULT, dataContext);
		m_dc = dataContext;
		m_injector = injector;
	}

	/**
	 * Return the default QDataContext.
	 * @return
	 */
	@Override
	@Nonnull
	public QDataContext dc() {
		return m_dc;
	}

	/**
	 * Get the shared instance for the "Class of Xxxx" business logic, handling concepts for all instances of Xxxx.
	 * @param classClass
	 * @return
	 * @throws Exception
	 */
	@Override
	@Nonnull
	public <L> L get(@Nonnull Class<L> classClass) throws Exception {
		ILogiInjector ij = m_injector;
		if(null != ij) {
			return ij.getInstance(classClass);
		}

		L logic = (L) m_classMap.get(classClass);
		if(null == logic) {
			Constructor<L> c;
			try {
				c = classClass.getConstructor(ILogicContext.class);
			} catch(Exception x) {
				throw new ProgrammerErrorException("Could not create an instance of " + classClass + ": constructor(ILogicContext) not found");
			}

			//-- Create the instance.
			logic = c.newInstance(this);
			if(null == logic)
				throw new IllegalStateException("Cobol'74 exception: no nullities defined in 2014.");
			m_classMap.put(classClass, logic);
		}
		return logic;
	}

	/**
	 * Find/create the specified logic wrapper for instance.
	 * @param clz
	 * @param instance
	 * @return
	 * @throws Exception
	 */
	@Override
	@Nonnull
	public <L extends ILogic, K, T extends IIdentifyable<K>> L get(@Nonnull Class<L> clz, @Nonnull T instance) throws Exception {
		//-- Already exists in this context?
		Map<Object, ILogic> cmap = m_instanceMap.get(clz);
		if(null == cmap) {
			cmap = new HashMap<Object, ILogic>();
			m_instanceMap.put(clz, cmap);
		}

		/*
		 * FIXME: We should not support unsaved instances most probably.
		 * To support unsaved object, we try to use the identity hashcode 1st. Only if that fails we'll use the ID.
		 */
		int ihc = System.identityHashCode(instance);
		Object key = "ihc-" + ihc;
		ILogic logic = cmap.get(key);
		if(null == logic) {
			K altkey = instance.getId();												// Get the ID for this instance.
			if(null != altkey) {
				logic = cmap.get(altkey);
				key = altkey;
			}
		}
		if(null != logic)
			return (L) logic;

		//-- Nothing there. We need to create an instance.
		for(Constructor< ? > c : clz.getConstructors()) {
			Class< ? >[] formalar = c.getParameterTypes();						// We only accept constructor(ILogicContext, T)
			if(formalar.length != 2)
				continue;
			if(!formalar[0].isAssignableFrom(ILogicContext.class))
				continue;
			if(!formalar[1].isAssignableFrom(instance.getClass()))
				continue;

			//-- We got L(ILogicContext, T). Instantiate the object using it.
			L ni = (L) c.newInstance(this, instance);
			if(null == ni)
				throw new IllegalStateException("Cobol'74 exception: no nullities defined in 2014.");

			ILogiInjector ij = m_injector;
			if(null != ij) {
				ij.injectMembers(ni);
			}

			cmap.put(key, ni);
			return ni;
		}

		throw new ProgrammerErrorException("Could not create an instance of " + clz + ": constructor(ILogicContext, " + instance.getClass().getName() + ") not found");
	}


	/**
	 * Register an instance of a logic implementation for a given instance, when a new instance has been created. A check
	 * is made to ensure we do not register duplicates.
	 *
	 * @param registrationType
	 * @param logicClass
	 * @param dataClass
	 * @param <T>
	 * @param <K>
	 * @param <V>
	 */
	@Override public <T extends ILogic, K, V extends IIdentifyable<K>> void register(Class<?> registrationType, T logicClass, V dataClass) {
		Map<Object, ILogic> cmap = m_instanceMap.get(registrationType);
		if(null == cmap) {
			cmap = new HashMap<Object, ILogic>();
			m_instanceMap.put(registrationType, cmap);
		}

		/*
		 * FIXME: We should not support unsaved instances most probably.
		 * To support unsaved object, we try to use the identity hashcode 1st. Only if that fails we'll use the ID.
		 */
		int ihc = System.identityHashCode(dataClass);
		Object key = "ihc-" + ihc;
		ILogic logic = cmap.get(key);
		if(null == logic) {
			K altkey = dataClass.getId();										// Get the ID for this instance.
			if(null != altkey) {
				logic = cmap.get(altkey);
				key = altkey;
			}
		}

		if(null != logic)
			throw new ProgrammerErrorException("Duplicate registration of logic class "+registrationType.getName()+" for instance "+ MetaManager.identify(dataClass));

		K id = dataClass.getId();
		if(null == id)
			throw new IllegalStateException("You cannot register an instance having a null ID");

		ILogiInjector ij = m_injector;
		if(null != ij) {
			ij.injectMembers(logicClass);
		}

		cmap.put(id, logicClass);
	}

	@Override
	@Nonnull
	public ProblemModel getErrorModel() {
		return m_errorModel;
	}
}
