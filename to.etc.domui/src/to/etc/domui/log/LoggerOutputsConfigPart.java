package to.etc.domui.log;

import java.util.*;

import javax.annotation.*;

import to.etc.domui.component.meta.*;
import to.etc.domui.component.ntbl.*;
import to.etc.domui.component.tbl.*;
import to.etc.domui.dom.errors.*;
import to.etc.domui.log.data.*;
import to.etc.domui.util.*;
import to.etc.log.*;

public class LoggerOutputsConfigPart extends LoggerConfigPartBase<LoggerOutputDef> {

	private Map<String, String> m_storedData;

	protected LoggerOutputsConfigPart() {
		super(LoggerOutputDef.class);
		setRowChangeListener(new IRowEditorEvent<LoggerOutputDef, LoggerRowEditor<LoggerOutputDef>>() {

			@Override
			public boolean onRowChanged(@Nonnull TableModelTableBase<LoggerOutputDef> model, @Nonnull LoggerRowEditor<LoggerOutputDef> editor, @Nonnull LoggerOutputDef instance, boolean isNew) throws Exception {
				if(MetaManager.hasDuplicates(model.getModel().getItems(0, model.getModel().getRows()), instance, LoggerOutputDef.pKEY)) {
					editor.setMessage(UIMessage.error(LoggerOutputDef.pKEY, Msgs.BUNDLE, Msgs.V_INVALID));
					return false;
				}
				return true;
			}
		});
	}

	@Override
	public void createContent() throws Exception {
		super.createContent();
	}

	private @Nonnull
	List<LoggerOutputDef> convert(@Nonnull Map<String, String> loggerOutsDef) {
		List<LoggerOutputDef> res = loggerOutsDef.isEmpty() ? Collections.EMPTY_LIST : new ArrayList<LoggerOutputDef>(loggerOutsDef.size());
		for(String key : loggerOutsDef.keySet()) {
			res.add(new LoggerOutputDef(key, loggerOutsDef.get(key)));
		}
		return res;
	}

	@Override
	protected List<LoggerOutputDef> getData() {
		m_storedData = MyLoggerFactory.getLoggerOutsDef();
		return convert(m_storedData);
	}

	@Override
	protected String[] getDisplayCols() {
		return new String[]{LoggerOutputDef.pKEY, LoggerOutputDef.pOUTPUT};
	}

	@Override
	protected @Nonnull
	LoggerOutputDef initializeNewInstance() {
		return new LoggerOutputDef(null, null);
	}

	@Override
	public boolean validateChanges() {
		return true;
	}

	@Override
	public boolean saveChanges() throws Exception {
		List<LoggerOutputDef> list = getModel().getItems(0, getModel().getRows());
		for(LoggerOutputDef item : list) {
			if(!m_storedData.containsKey(item.getKey())) {
				if(!DomUtil.isBlank(item.getOutput())) {
					MyLoggerFactory.setOut(item.getKey(), item.getOutput());
				}
			} else {
				String storedOut = m_storedData.get(item.getKey());
				m_storedData.remove(item.getKey());
				if(!storedOut.equalsIgnoreCase(item.getOutput())) {
					MyLoggerFactory.setOut(item.getKey(), item.getOutput());
				}
			}
		}
		for(String key : m_storedData.keySet()) {
			MyLoggerFactory.setOut(key, null);
		}
		return true;
	}
}
