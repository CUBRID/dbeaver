package org.jkiss.dbeaver.ext.cubrid.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.meta.Property;

public class CubridOwner extends CubridInformation{
	private String name;
	protected CubridOwner(CubridDataSource dataSource, String name) {
		super(dataSource);
		this.name = name;
	}
	
	@NotNull
	@Override
	@Property(viewable = true, order = 1)
	public String getName()
	{
		return name;
	}
	
	@Override
	public String getDescription() {
		return null;
	}
}
