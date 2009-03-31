package to.etc.domui.parts;

import java.util.*;

import to.etc.domui.server.*;
import to.etc.domui.server.parts.*;
import to.etc.domui.trouble.*;
import to.etc.domui.util.resources.*;

/**
 * A generated button image from a button definition file. This works like
 * the normal button part but uses a property file (web or application resource) to
 * define the button's layout and colors. The only parameters that are specified
 * by the user of the button are the button's text and an optional button icon. All
 * other thingies come from a resource file.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Jul 21, 2008
 */
public class PropBtnPart implements BufferedPartFactory {
	static public class ButtonPartKey {
		String		m_propfile;

		String		m_icon;

		String		m_text;

		int			m_start;
		int 		m_end;
		String		m_color;
		String		m_img;
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((m_color == null) ? 0 : m_color.hashCode());
			result = prime * result + m_end;
			result = prime * result + ((m_icon == null) ? 0 : m_icon.hashCode());
			result = prime * result + ((m_img == null) ? 0 : m_img.hashCode());
			result = prime * result + ((m_propfile == null) ? 0 : m_propfile.hashCode());
			result = prime * result + m_start;
			result = prime * result + ((m_text == null) ? 0 : m_text.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;
			if(obj == null)
				return false;
			if(getClass() != obj.getClass())
				return false;
			ButtonPartKey other = (ButtonPartKey) obj;
			if(m_color == null) {
				if(other.m_color != null)
					return false;
			}
			else if(!m_color.equals(other.m_color))
				return false;
			if(m_end != other.m_end)
				return false;
			if(m_icon == null) {
				if(other.m_icon != null)
					return false;
			}
			else if(!m_icon.equals(other.m_icon))
				return false;
			if(m_img == null) {
				if(other.m_img != null)
					return false;
			}
			else if(!m_img.equals(other.m_img))
				return false;
			if(m_propfile == null) {
				if(other.m_propfile != null)
					return false;
			}
			else if(!m_propfile.equals(other.m_propfile))
				return false;
			if(m_start != other.m_start)
				return false;
			if(m_text == null) {
				if(other.m_text != null)
					return false;
			}
			else if(!m_text.equals(other.m_text))
				return false;
			return true;
		}

	}

	/**
	 * Decode the parameters for this button thingy.
	 * @see to.etc.domui.server.parts.BufferedPartFactory#decodeKey(java.lang.String, to.etc.domui.server.IParameterInfo)
	 */
	public Object decodeKey(String rurl, IParameterInfo info) throws Exception {
		ButtonPartKey	k = new ButtonPartKey();
		k.m_propfile= info.getParameter("src");
		k.m_text	= info.getParameter("txt");
		k.m_icon	= info.getParameter("icon");
		k.m_color	= info.getParameter("color");
		String s = info.getParameter("end");
		if(s == null)
			k.m_end		= -1;
		else
			k.m_end	= Integer.parseInt(s);
		s = info.getParameter("start");
		if(s == null)
			k.m_start	= -1;
		else
			k.m_start	= Integer.parseInt(s);
		k.m_img	= info.getParameter("img");
		return k;
	}

	/**
	 * Generate the button class.
	 * @see to.etc.domui.server.parts.BufferedPartFactory#generate(java.io.OutputStream, to.etc.domui.server.DomApplication, java.lang.Object)
	 */
	public void generate(PartResponse pr, DomApplication da, Object key, ResourceDependencyList rdl) throws Exception {
		ButtonPartKey k = (ButtonPartKey) key;
		Properties	p	= PartUtil.loadProperties(da, k.m_propfile, rdl);
		if(p == null)
			throw new ThingyNotFoundException("The button property file '"+k.m_propfile+"' was not found.");
//		System.out.println("Recreating image "+k.m_propfile+"/"+k.m_text);

		//-- Instantiate the renderer class
		String	rc	= p.getProperty("renderer");
		PropButtonRenderer	r	= null;
		if(rc == null)
			r	= new PropButtonRenderer();
		else {
			try {
				Class<?>	cl = getClass().getClassLoader().loadClass(rc);
				if(! PropButtonRenderer.class.isAssignableFrom(cl))
					throw new IllegalStateException("The class does not extend PropButtonRenderer");
				r	= (PropButtonRenderer) cl.newInstance();
			} catch(Exception x) {
				throw new ThingyNotFoundException("Cannot locate/instantiate the button renderer class '"+rc+"' (specified in "+k.m_propfile+")");
			}
		}

		//-- Delegate.
		if(p.getProperty("webui.webapp") == null || ! da.inDevelopmentMode()) {			// Not gotten from WebContent or not in DEBUG mode? Then we may cache!
			pr.setCacheTime(da.getDefaultExpiryTime());
		}
		r.generate(pr, da, k, p, rdl);
	}
}
