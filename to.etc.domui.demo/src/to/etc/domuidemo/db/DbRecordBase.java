package to.etc.domuidemo.db;

import to.etc.domui.component.meta.*;
import to.etc.domui.databinding.observables.*;
import to.etc.webapp.query.*;

/**
 * Convenience base class for persistent classes.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Apr 20, 2010
 */
abstract public class DbRecordBase<T> extends ObservableObject implements IIdentifyable<T> {
	/**
	 * Show a generic identification string for a database class.
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return MetaManager.identify(this);
	}
}
