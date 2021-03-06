package to.etc.domui.component.image;

import java.io.*;

import javax.annotation.*;

import org.slf4j.*;

import to.etc.domui.component.buttons.*;
import to.etc.domui.component.misc.*;
import to.etc.domui.component.upload.*;
import to.etc.domui.dom.errors.*;
import to.etc.domui.dom.html.*;
import to.etc.domui.parts.*;
import to.etc.domui.server.*;
import to.etc.domui.state.*;
import to.etc.domui.themes.*;
import to.etc.domui.util.*;
import to.etc.domui.util.upload.*;
import to.etc.util.*;
import to.etc.webapp.nls.*;

/**
 * This control allows selecting a single image as an upload. The image is
 * stored in VpBlob. The uploaded image is resized to a max. size which can
 * be specified and shown as a thumbnail of (max) 32x32. All images are
 * saved as png.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Nov 5, 2014
 */
public class ImageSelectControl extends Div implements IUploadAcceptingComponent, IControl<IUIImage> {

	static private final Logger LOG = LoggerFactory.getLogger(ImageSelectControl.class);

	static private final BundleRef BUNDLE = BundleRef.create(ImageSelectControl.class, "messages");

	@Nullable
	private String m_emptyIcon;

	@Nonnull
	private Dimension m_displayDimensions = new Dimension(32, 32);

	@Nonnull
	private Dimension m_maxDimensions = new Dimension(1024, 1024);

	@Nullable
	private IUIImage m_value;

	private boolean m_disabled;

	private boolean m_mandatory;

	@Nullable
	private FileInput m_input;

	private boolean m_readOnly;

	private IValueChanged< ? > m_onValueChanged;

	public ImageSelectControl(@Nullable IUIImage value) {
		m_value = value;
	}

	public ImageSelectControl() {}

	@Override
	public void createContent() throws Exception {
		setCssClass("ui-isct");

		//-- Show as the thumbnail followed by [clear] [load another].
		Img img = new Img();
		add(img);
		img.setImgWidth(Integer.toString(m_displayDimensions.getWidth()));
		img.setImgHeight(Integer.toString(m_displayDimensions.getHeight()));
		img.setAlign(ImgAlign.LEFT);

		if(m_value == null) {
			String emptyIcon = getEmptyIcon();
			if(null == emptyIcon) {
				img.setSrc(Theme.ISCT_EMPTY);
			} else {
				img.setSrc(emptyIcon);
			}
		} else {
			String url = getComponentDataURL("THUMB", new PageParameters("datx", System.currentTimeMillis() + ""));
			img.setSrc(url);
		}

		if(!isDisabled() && ! isReadOnly()) {
			add(" ");
			HoverButton sib = new HoverButton(Theme.ISCT_ERASE, new IClicked<HoverButton>() {
				@Override
				public void clicked(HoverButton clickednode) throws Exception {
					setValue(null);
					forceRebuild();
					setImageChanged();
				}
			});
			add(sib);
			sib.setTitle(Msgs.BUNDLE.getString(Msgs.ISCT_EMPTY_TITLE));

			add(" ");
			Form f = new Form();
			add(f);
			f.setCssClass("ui-szless ui-isct-form");
			f.setEnctype("multipart/form-data");
			f.setMethod("POST");
			StringBuilder sb = new StringBuilder();
			ComponentPartRenderer.appendComponentURL(sb, UploadPart.class, this, UIContext.getRequestContext());
			sb.append("?uniq=" + System.currentTimeMillis()); // Uniq the URL to prevent IE's caching.
			f.setAction(sb.toString());

			FileInput fi = new FileInput();
			m_input = fi;
			f.add(fi);
			fi.setSpecialAttribute("onchange", "WebUI.fileUploadChange(event)");
			fi.setSpecialAttribute("fuallowed", "jpg,jpeg,png");
		}
	}

	/**
	 * Called to render the image inside the component.
	 * @see to.etc.domui.dom.html.NodeBase#componentHandleWebDataRequest(to.etc.domui.server.RequestContextImpl, java.lang.String)
	 */
	@Override
	public void componentHandleWebDataRequest(RequestContextImpl ctx, String action) throws Exception {
		if("THUMB".equals(action)) {
			IUIImage image = m_value;
			if(null != image)
				renderImage(ctx, image);
			return;
		}

		super.componentHandleWebDataRequest(ctx, action);
	}

	private void renderImage(@Nonnull RequestContextImpl ctx, @Nonnull IUIImage thumbnail) throws Exception {
		IUIImageInstance ii = thumbnail.getImage(m_displayDimensions, true);
		OutputStream os = ctx.getRequestResponse().getOutputStream(ii.getMimeType(), null, ii.getImageSize());
		InputStream is = ii.getImage();
		try {
			FileTool.copyFile(os, is);
			os.close();
		} finally {
			FileTool.closeAll(os, is);
		}
	}

	@Override
	public boolean handleUploadRequest(RequestContextImpl param, ConversationContext conversation) throws Exception {
		FileInput fi = m_input;
		if(null == fi)
			return true;

		try {
			UploadItem[] uiar = param.getFileParameter(fi.getActualID());
			if(uiar != null) {
				if(uiar.length != 1)
					throw new IllegalStateException("Upload presented <> 1 file!?");

				updateImage(conversation, uiar[0]);
			}
		} catch(FileUploadException fxu) {
			forceRebuild();
			MessageFlare.display(this, fxu.getMessage());
			return true;
		}
		forceRebuild();
		setImageChanged();
		return true;
	}

	private void setImageChanged() throws Exception {
		if(m_onValueChanged != null)
			((IValueChanged<Object>) m_onValueChanged).onValueChanged(this);
	}

	private void updateImage(@Nonnull ConversationContext cc, @Nonnull UploadItem ui) throws Exception {
		File newUploadedImage = ui.getFile();
		cc.registerTempFile(newUploadedImage);

		try {
			m_value = LoadedImage.create(newUploadedImage, m_maxDimensions, null);
		} catch(Exception x) {
			MessageFlare.display(this, MsgType.ERROR, BUNDLE.getString("image.invalid"));
			LOG.error("File: " + newUploadedImage.getName() + " can't be uploaded. Looks like corrupted file", x);
		}
	}

	@Nonnull public Dimension getDisplayDimensions() {
		return m_displayDimensions;
	}

	public void setDisplayDimensions(@Nonnull Dimension displayDimensions) {
		m_displayDimensions = displayDimensions;
	}

	@Nonnull public Dimension getMaxDimensions() {
		return m_maxDimensions;
	}

	public void setMaxDimensions(@Nonnull Dimension maxDimensions) {
		m_maxDimensions = maxDimensions;
	}

	@Override
	@Nullable
	public IUIImage getValue() {
		return m_value;
	}

	@Override
	public void setValue(@Nullable IUIImage value) {
		m_value = value;
	}

	@Override
	public IValueChanged< ? > getOnValueChanged() {
		return m_onValueChanged;
	}

	@Override
	public void setOnValueChanged(IValueChanged< ? > onValueChanged) {
		m_onValueChanged = onValueChanged;
	}

	@Override
	public IUIImage getValueSafe() {
		return DomUtil.getValueSafe(this);
	}

	@Override
	public boolean isReadOnly() {
		return m_readOnly;
	}

	@Override
	public void setReadOnly(boolean ro) {
		if(m_readOnly == ro)
			return;
		m_readOnly = ro;
		forceRebuild();
	}

	@Override
	public boolean isMandatory() {
		return m_mandatory;
	}

	@Override
	public void setMandatory(boolean ro) {
		if(m_mandatory == ro)
			return;
		m_mandatory = ro;
		forceRebuild();
	}

	@Override
	public boolean isDisabled() {
		return m_disabled;
	}

	@Override
	public void setDisabled(boolean disabled) {
		if(disabled == m_disabled)
			return;
		m_disabled = disabled;
		forceRebuild();
	}

	/**
	 * If you want to show another image then the "empty.png" image that is default shown when no image is available.
	 *
	 * @return
	 */
	@Nullable
	public String getEmptyIcon() {
		return m_emptyIcon;
	}

	/**
	 * Set the source for the image to show, if no image is given, as an absolute web app path. If the name is prefixed
	 * with THEME/ it specifies an image from the current THEME's directory.
	 * @param src
	 */
	public void setEmptyIcon(@Nonnull String src) {
		if(!DomUtil.isEqual(src, m_emptyIcon))
			changed();
		m_emptyIcon = src;
	}

	/**
	 * Set the source as a Java resource based off the given class.
	 * @param base
	 * @param resurl
	 */
	public void setEmptyIcon(@Nonnull Class< ? > base, @Nonnull String resurl) {
		String s = DomUtil.getJavaResourceRURL(base, resurl);
		setEmptyIcon(s);
	}
}
