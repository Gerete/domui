package to.etc.domui.server;

import javax.annotation.*;

import to.etc.domui.util.upload.*;

/**
 *
 *
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Oct 3, 2013
 */
public interface IRequestResponse {
	@Nonnull
	public String getRequestURI();

	/**
	 * Called when uploaded files are no longer needed; this can then discard of them.
	 */
	public void releaseUploads();

	@Nonnull
	public String getUserAgent();

	/**
	 * Return the base URL to the web application from the current requests. This uses hostname, protocol, portname
	 * and web application context from the incoming requests and returns it. The returned URL is guaranteed to end
	 * in a slash.
	 * @return
	 */
	@Nonnull
	public String getApplicationURL();

	@Nonnull
	public String[] getParameters(@Nonnull String name);

	@Nullable
	public String getParameter(@Nonnull String name);

	@Nonnull
	public String[] getParameterNames();

	@Nonnull
	public String[] getFileParameters();

	@Nonnull
	public UploadItem[] getFileParameter(@Nonnull String name);


}
