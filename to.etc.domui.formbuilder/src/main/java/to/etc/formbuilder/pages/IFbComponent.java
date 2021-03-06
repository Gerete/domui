package to.etc.formbuilder.pages;

import java.util.*;

import javax.annotation.*;

import to.etc.domui.dom.html.*;

public interface IFbComponent {
	@Nonnull
	public String getTypeID();

	public void drawSelector(@Nonnull NodeContainer container) throws Exception;

	@Nonnull
	public String getShortName();

	@Nonnull
	public String getLongName();

	@Nonnull
	public String getCategoryName();

	@Nonnull
	public NodeBase createNodeInstance() throws Exception;

	/**
	 * Return all properties for this component.
	 * @return
	 */
	@Nonnull
	public Set<PropertyDefinition> getProperties();
}
