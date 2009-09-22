package to.etc.domui.component.meta;

import to.etc.webapp.*;


/**
 * Utility class used as utility method library for working with meta data.
 * @author vmijic
 * Created on 4 Aug 2009
 */
public class MetaUtils {
	/**
	 * Parses value of param defined in metadataLine string as Integer value.
	 * @return Integer value represented as string, in case that param is not defined or NumberFormatException exception returns default value.
	 */
	public static int parseIntParam(final String metadataLine, final String paramName, final int defaultValue) {
		String paramValue = parseStringParam(metadataLine, paramName);
		if(paramValue == null || paramValue.trim().length() == 0)
			return defaultValue;
		try {
			return Integer.parseInt(paramValue);
		} catch(NumberFormatException e) {
			throw new ProgrammerErrorException("Invalid number in metadata string: " + metadataLine + ", parameter=" + paramName + ", value=" + paramValue);
		}
	}

	public static String parseStringParam(final String metadataLine, final String paramName) {
		String paramValue = null;
		final String paramItem = paramName.toLowerCase() + '=';
		if(metadataLine.contains(paramItem)) {
			if(metadataLine.indexOf(";", metadataLine.indexOf(paramItem)) > 0) {
				paramValue = metadataLine.substring(metadataLine.indexOf(paramItem) + paramItem.length(), metadataLine.indexOf(";", metadataLine.indexOf(paramItem)));
			} else {
				paramValue = metadataLine.substring(metadataLine.indexOf(paramItem) + paramItem.length());
			}
		}

		return paramValue;
	}
}
