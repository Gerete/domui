package to.etc.domui.logic.errors;

import to.etc.domui.component.meta.*;

import javax.annotation.*;
import java.util.*;

/**
 * EXPERIMENTAL This class keeps all logic errors generated by a given (set of) processes.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Nov 8, 2014
 */
public class ProblemModel {
	@Nullable
	final private ProblemModel m_parent;

	/**
	 * Maps [object, property] to a set of errors. If the property is not known it is mapped as null.
	 */
	@Nonnull
	private Map<Object, Map<PropertyMetaModel< ? >, Set<ProblemInstance>>> m_map = new HashMap<>();

	public ProblemModel() {
		m_parent = null;
	}

	/**
	 * Add a problem occurrence to the set.
	 * @param problem
	 */
	void addProblem(@Nonnull ProblemInstance problem) {
		Map<PropertyMetaModel< ? >, Set<ProblemInstance>> mapOnProp = m_map.get(problem.getInstance());
		if(mapOnProp == null) {
			mapOnProp = new HashMap<PropertyMetaModel< ? >, Set<ProblemInstance>>();
			m_map.put(problem.getInstance(), mapOnProp);
		}
		Set<ProblemInstance> messages = mapOnProp.get(problem.getProperty());
		if(messages == null) {
			messages = new HashSet<ProblemInstance>();
			mapOnProp.put(problem.getProperty(), messages);
		}
		messages.add(problem);
	}

	public void clear() {
		m_map.clear();
	}

	/**
	 * Remove a problem occurrence from the set.
	 * @param problem
	 * @param instance
	 * @param pmm
	 */
	<T, P> void clear(@Nonnull Problem problem, @Nonnull T instance, @Nullable PropertyMetaModel<P> pmm) {
		Map<PropertyMetaModel< ? >, Set<ProblemInstance>> mapOnProp = m_map.get(instance);
		if(mapOnProp != null) {
			Set<ProblemInstance> messages = mapOnProp.get(pmm);
			if(messages != null) {
				for(ProblemInstance pi : messages) {
					if(pi.getProblem().equals(problem)) {
						messages.remove(pi);
						return;
					}
				}
			}
		}
	}

	/**
	 * Get all errors on the specified instance <i>alone</i>, i.e. not those reported on the instance's properties.
	 * @param businessObject
	 * @return
	 */
	@Nonnull
	public <T> Set<ProblemInstance> getErrorsOn(@Nonnull T businessObject) {
		return getErrorsOn(businessObject, (PropertyMetaModel< ? >) null);
	}

	/**
	 * Return errors on this instance and all direct properties of the instance.
	 * @param businessObject
	 * @return
	 */
	public <T> Set<ProblemInstance> getAllErrorsOn(@Nonnull T businessObject) {
		Set<ProblemInstance> res = new HashSet<>();
		Map<PropertyMetaModel< ? >, Set<ProblemInstance>> mapOnProp = m_map.get(businessObject);
		if(mapOnProp != null) {
			for(Set<ProblemInstance> pset: mapOnProp.values()) {
				res.addAll(pset);
			}
		}
		return res;
	}

	@Nonnull
	public <T, V> Set<ProblemInstance> getErrorsOn(@Nonnull T businessObject, @Nullable PropertyMetaModel<V> property) {
		Map<PropertyMetaModel< ? >, Set<ProblemInstance>> mapOnProp = m_map.get(businessObject);
		if(mapOnProp != null) {
			Set<ProblemInstance> messagesList = mapOnProp.get(property);
			if(messagesList != null) {
				return messagesList; 						//consider making copy list
			}
		}
		return Collections.EMPTY_SET;
	}

	@Nonnull
	public <T> Set<ProblemInstance> getErrorsOn(@Nonnull T businessObject, @Nonnull String property) {
		return getErrorsOn(businessObject, MetaManager.getPropertyMeta(businessObject.getClass(), property));
	}

	public boolean hasErrors() {
		for(Map<PropertyMetaModel< ? >, Set<ProblemInstance>> m1 : m_map.values()) {
			for(Set<ProblemInstance> set : m1.values()) {
				if(set.size() > 0)
					return true;
			}
		}
		return false;
	}

	/**
	 * Return all errors as a read/write set for processing.
	 * @return
	 */
	@Nonnull
	public ProblemSet getErrorSet() {
		return new ProblemSet(m_map);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(Map<PropertyMetaModel< ? >, Set<ProblemInstance>> map : m_map.values()) {
			for(Set<ProblemInstance> piSet : map.values()) {
				for(ProblemInstance pi : piSet) {
					if(sb.length() != 0)
						sb.append('\n');
					sb.append(pi.getProblem()).append(" @");
					PropertyMetaModel< ? > pmm = pi.getProperty();
					if(null != pmm) {
						sb.append(pmm.getName()).append("/");
					}
					sb.append(MetaManager.identify(pi.getInstance()));
				}
			}
		}
		return sb.toString();
	}
}
