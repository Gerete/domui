package to.etc.domui.ajax;

import java.lang.annotation.*;

import to.etc.domui.server.*;
import to.etc.server.ajax.*;
import to.etc.server.injector.*;
import to.etc.server.misc.*;
import to.etc.util.*;

public class URLParameterProvider implements IParameterProvider {
	private final RequestContextImpl		m_ctx;

	public URLParameterProvider(final RequestContextImpl rctx) {
		m_ctx = rctx;
	}

	public Object findParameterValue(final Class< ? > targetcl, final Annotation[] annotations, final int paramIndex, final AjaxParam ap) throws Exception {
        String[]    pv = m_ctx.getRequest().getParameterValues(ap.value());			// Parameter by name
        if(pv == null || pv.length == 0)
            return NO_VALUE;
        if(pv.length > 1)
            throw new ParameterException("The value for the injector parameter '"+ap.value()+"' must be a single request value").setParameterName(ap.value());
        if(ap.json()) {
//        	System.out.println("ajax: json value for parameter '"+name+"' is "+pv[0]);
        	//-- Convert the input from JSON to whatever object is needed,
        	if(targetcl == Object.class) {
        		//-- Generic assignment
        		return JSONParser.parseJSON(pv[0]);
        	} else {
        		//-- Assign using a base class structure
        		return JSONParser.parseJSON(pv[0], targetcl);
        	}
        }

        //-- Autoconvert to proper type, using an URL converter
        return RuntimeConversions.convertTo(pv[0], targetcl);
	}
}