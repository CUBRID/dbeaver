package org.jkiss.dbeaver.ext.cubrid.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.cubrid.model.CubridDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPSystemInfoObject;
import org.jkiss.dbeaver.model.struct.DBSObject;

public abstract class CubridInformation implements DBSObject, DBPSystemInfoObject{
	
	private CubridDataSource dataSource;
	
	protected CubridInformation(CubridDataSource dataSource)
    {
        this.dataSource = dataSource;
    }

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isPersisted() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public DBSObject getParentObject() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DBPDataSource getDataSource() {
		// TODO Auto-generated method stub
		return null;
	}

}
