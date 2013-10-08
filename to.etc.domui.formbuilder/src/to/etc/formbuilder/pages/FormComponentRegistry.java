package to.etc.formbuilder.pages;

import java.lang.reflect.*;
import java.util.*;

import javax.annotation.*;

import to.etc.domui.dom.html.*;

/**
 * This singleton class will collect all DomUI components that are usable inside the form builder.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Oct 8, 2013
 */
final public class FormComponentRegistry {
	static private final FormComponentRegistry m_instance = new FormComponentRegistry();

	private List<IFbComponent> m_componentList = new ArrayList<IFbComponent>();

	@Nonnull
	public static FormComponentRegistry getInstance() {
		return m_instance;
	}

	public synchronized void register(@Nonnull IFbComponent component) {
		List<IFbComponent> compl = new ArrayList<IFbComponent>(m_componentList);
		compl.add(component);
		m_componentList = Collections.unmodifiableList(compl);
	}

	public synchronized List<IFbComponent> getComponentList() {
		return m_componentList;
	}

	/**
	 * Auto-register a component class.
	 * @param componentClass
	 */
	public void registerComponent(@Nonnull Class< ? > componentClass) {
		if(IFbComponent.class.isAssignableFrom(componentClass)) {
			registerComponentHelper((Class< ? extends IFbComponent>) componentClass);
			return;
		}

		//-- Does this class have a companion class implementing the UI interface?
		String name = componentClass.getName();
		int lastindex = name.lastIndexOf('.');
		name = name.substring(0, lastindex + 1) + "Fb" + name.substring(lastindex + 2);
		Class< ? > altc = null;
		try {
			altc = componentClass.getClassLoader().loadClass(name);			// Try to load alternate class
		} catch(Exception x) {}

		if(altc != null) {
			if(IFbComponent.class.isAssignableFrom(altc)) {
				registerComponentHelper((Class< ? extends IFbComponent>) altc);
				return;
			}
			throw new IllegalStateException(componentClass + "'s companion class " + altc.getName() + " does not implement " + IFbComponent.class.getName());
		}

		//-- Register component as-is, if it extends a DomUI NodeBase and has a parameterless constructor
		Constructor< ? > cons;
		try {
			cons = componentClass.getConstructor();
		} catch(Exception x) {
			throw new IllegalStateException(componentClass + " has no parameterless constructor");
		}

		if(!NodeBase.class.isAssignableFrom(componentClass))
			throw new IllegalStateException(componentClass + " does not extend " + NodeBase.class.getName());

		//-- auto-register
		IFbComponent component = autoRegister((Class< ? extends NodeBase>) componentClass);
		register(component);
	}

	private IFbComponent autoRegister(@Nonnull Class< ? extends NodeBase> componentClass) {
		return new AutoComponent(componentClass);
	}

	private void registerComponentHelper(@Nonnull Class< ? extends IFbComponent> componentClass) {
		// TODO Auto-generated method stub

	}

	static {
//		getInstance().registerComponent(Text.class);
		getInstance().registerComponent(TextArea.class);
	}


}
