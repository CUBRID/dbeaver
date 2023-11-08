/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.cubrid.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.cubrid.CubridConstants;
import org.jkiss.dbeaver.ext.cubrid.model.meta.CubridMetaObject;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSTrigger;
import java.sql.ResultSet;

/**
 * CubridTrigger
 */
public class CubridTrigger implements DBSTrigger, DBPNamedObject2
{
    @NotNull
    private CubridStructContainer container;
    private String name;
    private String owner;
    private String targetOwner;
    private String targetClass;
    private Double priority;
    private Integer event;
    private Integer conditionTime;
    private String condition;
    private Integer actionTime;
    private Integer actionType;
    private String actionDefinition;
    private String description;
    protected String source;

    public CubridTrigger(@NotNull CubridStructContainer container, CubridMetaObject triggerObject, ResultSet dbResult) {
        this.container = container;
        this.name = CubridUtils.safeGetStringTrimmed(triggerObject, dbResult, CubridConstants.NAME);
        this.owner = CubridUtils.safeGetString(triggerObject, dbResult, CubridConstants.OWNER_NAME);
        this.targetOwner = CubridUtils.safeGetString(triggerObject, dbResult, CubridConstants.TARGET_OWNER_NAME);
        this.targetClass = CubridUtils.safeGetString(triggerObject, dbResult, CubridConstants.TARGET_CLASS_NAME);
        this.priority = CubridUtils.safeGetDouble(triggerObject, dbResult, CubridConstants.PRIORITY);
        this.event = CubridUtils.safeGetInteger(triggerObject, dbResult, CubridConstants.EVENT);
        this.conditionTime = CubridUtils.safeGetInteger(triggerObject, dbResult, CubridConstants.CONDITION_TIME);
        this.condition = CubridUtils.safeGetString(triggerObject, dbResult, CubridConstants.CONDITION);
        this.actionTime = CubridUtils.safeGetInteger(triggerObject, dbResult, CubridConstants.ACTION_TIME);
        this.actionType = CubridUtils.safeGetInteger(triggerObject, dbResult, CubridConstants.ACTION_TYPE);
        this.actionDefinition = CubridUtils.safeGetString(triggerObject, dbResult, CubridConstants.ACTION_DEFINITION);
        this.description = CubridUtils.safeGetString(triggerObject, dbResult, CubridConstants.COMMENT);
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, order = 1)
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }
    
    @Property(viewable = true, editable = true, order = 10)
    public String getOwner() {
        return owner;
    }
    
    @Property(viewable = true, editable = true, order = 15)
    public String getTargetOwner() {
        return targetOwner;
    }
    
    @Property(viewable = true, editable = true, order = 18)
    public String getTargetClass() {
        return targetClass;
    }
    
    @Property(viewable = true, editable = true, order = 20)
    public Number getPriority() {
        return priority;
    }
    
    @Property(viewable = true, editable = true, order = 30)
    public String getEvent() {
    	if(event != null) {
	    	switch(event) {
	    		case 0:
	    			return "UPDATE";
	    		case 1:
	    			return "UPDATE STATEMENT";
	    		case 2:
	    			return "DELETE";
	    		case 3:
	    			return "DELETE STATEMENT";
	    		case 4:
	    			return "INSERT";
	    		case 5:
	    			return "INSERT STATEMENT";
	    		case 8:
	    			return "COMMIT";
	    		case 9:
	    			return "ROLLBACK";
	    		default:
	    			return "";
	    	}
    	} else { 		
    		return "";
    	}
    }
    
    @Property(viewable = true, editable = true, order = 30)
    public String getConditionTime() {
    	if(conditionTime != null) {  		
	        switch(conditionTime) {
			case 1:
				return "BEFORE";
			case 2:
				return "AFTER";
			case 3:
				return "DEFERRED";
			default:
				return "";
	        }
	    } else {
	    	return "";
	    }
    }
    
    @Property(viewable = true, editable = true, order = 40)
    public String getCondition() {
        return condition;
    }
    
    @Property(viewable = true, editable = true, order = 50)
    public String getActionTime() {
    	if(actionTime != null) {
	        switch(actionTime) {
			case 1:
				return "BEFORE";
			case 2:
				return "AFTER";
			case 3:
				return "DEFERRED";
			default:
				return "";
	        }
        } else {
        	return "";
        }
    }
    
    @Property(viewable = true, editable = true, order = 60)
    public String getActionType() {
    	if(actionType != null) {
	    	switch(actionType) {
			case 1:
				return "INSERT, UPDATE, DELETE, CALL";
			case 2:
				return "REJECT";
			case 3:
				return "INVALIDATE_TRANSACTION";
			case 4:
				return "PRINT";
			default:
				return "";
	    	}
    	} else {
        	return "";
        }
    }
    
    @Property(viewable = true, editable = true, order = 70)
    public String getActionDefinition() {
        return actionDefinition;
    }

    @Nullable
    @Override
    @Property(viewable = true, length = PropertyLength.MULTILINE, order = 100)
    public String getDescription()
    {
        return description;
    }

    protected void setDescription(String description)
    {
        this.description = description;
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

    @NotNull
    public CubridStructContainer getContainer() {
        return container;
    }

    @Override
    public CubridStructContainer getParentObject()
    {
        return container;
    }

    @NotNull
    @Override
    public CubridDataSource getDataSource()
    {
        return (CubridDataSource) container.getDataSource();
    }

	@Override
	public DBSTable getTable() {
		// TODO Auto-generated method stub
		return null;
	}

}