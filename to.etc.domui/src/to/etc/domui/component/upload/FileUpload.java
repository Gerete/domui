package to.etc.domui.component.upload;

import java.util.*;

import to.etc.domui.component.buttons.*;
import to.etc.domui.dom.html.*;
import to.etc.domui.parts.*;
import to.etc.domui.state.*;
import to.etc.domui.util.upload.*;

/**
 * Represents a file upload thingy which handles ajaxy uploads. The basic model
 * is as follows:
 * <ul>
 *	<li>FileUpload components use a hidden iframe to handle the actual uploading in the background.</li>
 *	<li>The background upload gets started as soon as a selection is made.</li>
 *	<li>While the upload is running the user interface is blocking</li>
 *	<li>The uploaded file will be attached as a tempfile to the form's component. It will be deleted
 *		as soon as the context or the page is destroyed.</li>
 *	<li>No upload progress reporting is done.</li>
 * </ul>
 * <h2>Client side upload thingy</h2>
 * <p>In the browser the primary upload thingy is an input type='file' component. This gets used to allow
 * the user to add a file. As soon as a file is selected an onchange event will ensure the posting of
 * the file to the server using an AJAX/IFrame upload. This upload will be received by the Upload part
 * which will attach the file to the control doing the upload.
 * When the upload is complete the input type="file" thing gets replaced by a "file reference" containing
 * the input filename and a delete button. If the input thingy allows for multiple files to be uploaded
 * another input type="file" gets added above that.
 * </p>
 * <h3>Important info regarding IE use of iframe: http://p2p.wrox.com/topic.asp?whichpage=1&TOPIC_ID=62981&#153594</h3>
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Oct 13, 2008
 */
public class FileUpload extends Div {
	private String m_allowedExtensions;

	private int m_maxSize;

	private boolean m_required;

	private int m_maxFiles = 1;

	List<UploadItem> m_files = new ArrayList<UploadItem>();

	private FileInput m_input;

	public FileUpload() {}

	public FileUpload(int maxfiles, String allowedExt) {
		m_maxFiles = maxfiles;
		m_allowedExtensions = allowedExt;
	}

	@Override
	public void createContent() throws Exception {
		renderSelectedFiles();
	}

	private void renderSelectedFiles() {
		Table t = new Table();
		add(t);
		TBody b = new TBody();
		t.add(b);

		if(!isFull()) {
			b.addRow();
			TD td = b.addCell();
			td.setColspan(2);

			Form f = new Form();
			td.add(f);
			f.setCssClass("ui-szless");
			f.setEnctype("multipart/form-data");
			f.setMethod("POST");
			StringBuilder sb = new StringBuilder();
			ComponentPartRenderer.appendComponentURL(sb, UploadPart.class, this, PageContext.getRequestContext());
			sb.append("?uniq=" + System.currentTimeMillis()); // Uniq the URL to prevent IE's stupid caching.
			f.setAction(sb.toString());

			FileInput fi = new FileInput();
			f.add(fi);
			fi.setSpecialAttribute("onchange", "WebUI.fileUploadChange(event)");
			fi.setSpecialAttribute("fuallowed", m_allowedExtensions);
			fi.setSpecialAttribute("fumaxsz", Integer.toString(m_maxSize));
			m_input = fi;
		}
		for(final UploadItem ufi : m_files) {
			b.addRow();
			TD td = b.addCell();
			td.setText(ufi.getRemoteFileName() + " (" + ufi.getContentType() + ")");
			td = b.addCell();
			td.add(new DefaultButton("delete", new IClicked<DefaultButton>() {
				@Override
				public void clicked(DefaultButton bx) throws Exception {
					m_files.remove(ufi);
					forceRebuild();
				}
			}));
		}
	}

	FileInput getInput() {
		return m_input;
	}

	public List<UploadItem> getFiles() {
		return m_files;
	}

	public boolean isFull() {
		return m_files.size() >= m_maxFiles;
	}

	public String getAllowedExtensions() {
		return m_allowedExtensions;
	}

	public void setAllowedExtensions(String allowedExtensions) {
		m_allowedExtensions = allowedExtensions;
	}

	public int getMaxSize() {
		return m_maxSize;
	}

	public void setMaxSize(int maxSize) {
		m_maxSize = maxSize;
	}

	public boolean isRequired() {
		return m_required;
	}

	public void setRequired(boolean required) {
		m_required = required;
	}

	public int getMaxFiles() {
		return m_maxFiles;
	}

	public void setMaxFiles(int maxFiles) {
		m_maxFiles = maxFiles;
	}
}
