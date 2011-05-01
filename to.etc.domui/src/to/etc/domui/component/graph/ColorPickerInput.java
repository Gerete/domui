package to.etc.domui.component.graph;

import to.etc.domui.component.input.*;
import to.etc.domui.dom.header.*;
import to.etc.domui.dom.html.*;

/**
 * An input button to enter a color code, with a small div behind it showing the
 * currently selected code's color.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on May 1, 2011
 */
public class ColorPickerInput extends Input implements IInputNode<String> {
	final private Div		m_coldiv = new Div();

	@Override
	public void createContent() throws Exception {
		setMaxLength(6);
		setSize(6);
		m_coldiv.setCssClass("ui-cpin-div");
//		setCssClass("ui-cpbt-btn");
//		add(m_hidden);
//		add(m_coldiv);
//		if(m_hidden.getRawValue() == null)
//			m_hidden.setRawValue("ffffff");
//		m_coldiv.setBackgroundColor("#" + m_hidden.getRawValue());
//		appendCreateJS("$('#"+getActualID()+"').ColorPicker({});");
		appendAfterMe(m_coldiv);
		if(! isDisabled())
			appendCreateJS("WebUI.colorPickerInput('#" + getActualID() +"','#"+m_coldiv.getActualID() + "','" + getRawValue() + "'," + Boolean.valueOf(getOnValueChanged() != null) + ");");
	}
	@Override
	public void onAddedToPage(Page p) {
		p.addHeaderContributor(HeaderContributor.loadJavascript("$js/colorpicker.js"), 100);
//		if(m_coldiv.getPage() == null)
//			appendAfterMe(m_coldiv);
	}
	@Override
	public void onRemoveFromPage(Page p) {
		m_coldiv.remove();
	}

	public void setValue(String value) {
		if(value == null)
			value = "000000"; // We do not allow null here.
		if(value.startsWith("#"))
			value = value.substring(1); // Remove any #
		setRawValue(value); // Set the color value;
		m_coldiv.setBackgroundColor("#" + value);
		if(!isBuilt())
			return;

		//-- Force update existing value.
		appendJavascript("$('#" + getActualID() + "').ColorPickerSetColor('" + value + "');");
	}
	public String	getValue() {
		String v = getRawValue();
		if(v == null || v.length() == 0)
			v = "000000";
		return v;
	}

	@Override
	public IBinder bind() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public boolean isBound() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public String getValueSafe() {
		return getValue();
	}
	@Override
	public boolean isMandatory() {
		return false;
	}
	@Override
	public void setMandatory(boolean ro) {
	}
	@Override
	public void setDisabled(boolean disabled) {
		if(isDisabled() == disabled)
			return;
		super.setDisabled(disabled);
		if(disabled)
			appendJavascript("WebUI.colorPickerDisable('#"+getActualID()+"');");
		else
			appendCreateJS("WebUI.colorPickerInput('#" + getActualID() +"','#"+m_coldiv.getActualID() + "','" + getRawValue() + "'," + Boolean.valueOf(getOnValueChanged() != null) + ");");
	}
}
