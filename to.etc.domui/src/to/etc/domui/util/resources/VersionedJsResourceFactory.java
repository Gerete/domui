package to.etc.domui.util.resources;

import to.etc.domui.server.*;
import to.etc.domui.util.*;

/**
 * Handle $js/xxx resources, which are versionable.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Jan 14, 2011
 */
public class VersionedJsResourceFactory implements IResourceFactory {
	@Override
	public int accept(String name) {
		if(name.startsWith("$js/"))
			return 15;
		return -1;
	}

	@Override
	public IResourceRef getResource(DomApplication da, String name, ResourceDependencyList rdl) throws Exception {
		//-- 1. Create a 'min version of the name
		name = name.substring(3); // Strip $js, leave leading /.
		int pos = name.lastIndexOf('.');
		String min = pos < 0 ? null : name.substring(0, pos) + "-min" + name.substring(pos);

		StringBuilder sb = new StringBuilder(64);
		IResourceRef r;
		if(!da.inDevelopmentMode() && min != null) {
			//-- Try all min versions in production, first
			sb.append("js/").append(da.getScriptVersion()).append(min);
			r = tryVersionedResource(da, sb.toString());
			if(r != null)
				return r;
			sb.setLength(0);
			sb.append("js").append(min);
			r = tryVersionedResource(da, sb.toString());
			if(r != null)
				return r;
		}

		//-- Try normal versions only in development.
		sb.setLength(0);
		sb.append("js/").append(da.getScriptVersion()).append(name);
		r = tryVersionedResource(da, sb.toString());
		if(r != null)
			return r;

		r = da.createClasspathReference("/resources/js" + name);
		//			System.out.println("RR: Default ref to " + name + " is " + r);
		return r;
	}

	private IResourceRef tryVersionedResource(DomApplication da, String name) {
		name = "/resources/" + name;
		if(!DomUtil.classResourceExists(getClass(), name))
			return null;
		return da.createClasspathReference(name);
	}
}
