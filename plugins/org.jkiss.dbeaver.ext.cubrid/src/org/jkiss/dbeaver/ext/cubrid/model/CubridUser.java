package org.jkiss.dbeaver.ext.cubrid.model;

import java.util.Map;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;


public class CubridUser implements DBSObject, CubridScriptObject{

	private String name;
	private String comment;
	private CubridStructContainer container;

	public CubridUser(String name) {
		this.name = name;
	}

	public CubridUser(CubridStructContainer container, String name, String comment) {
		this.name = name;
		this.comment = comment;
		this.container = container;
	}

	@Property(viewable = true, order = 1)
	public String getName() {
		return name;
	}

	@Nullable
	@Property(viewable = true, order = 2)
	public String getComment() {
		return comment;
	}

	@Override
	public String getDescription() {
		return null;
	}
	@Override
	public boolean isPersisted() {
		return true;
	}
	@Override
	public DBSObject getParentObject() {
		return this.container;
	}
	@Override
	public CubridDataSource getDataSource() {
		return this.container.getDataSource();
	}

	@Override
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
		return null;
	}

}