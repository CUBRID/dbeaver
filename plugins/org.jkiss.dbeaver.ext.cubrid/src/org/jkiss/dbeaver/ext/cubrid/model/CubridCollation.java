package org.jkiss.dbeaver.ext.cubrid.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.meta.Property;


public class CubridCollation extends CubridInformation{
	
	private String name;
	protected CubridCollation(CubridDataSource dataSource, String name) {
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
