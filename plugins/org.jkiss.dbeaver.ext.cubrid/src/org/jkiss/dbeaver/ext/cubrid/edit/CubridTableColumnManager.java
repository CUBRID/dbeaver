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

package org.jkiss.dbeaver.ext.cubrid.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cubrid.CubridConstants;
import org.jkiss.dbeaver.ext.cubrid.model.CubridTable;
import org.jkiss.dbeaver.ext.cubrid.model.CubridTableBase;
import org.jkiss.dbeaver.ext.cubrid.model.CubridTableColumn;
import org.jkiss.dbeaver.ext.cubrid.model.CubridUtils;
import org.jkiss.dbeaver.ext.cubrid.model.meta.CubridMetaModel;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableColumnManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;
import java.sql.Types;
import java.util.List;
import java.util.Map;

/**
 * Cubrid table column manager
 */
public class CubridTableColumnManager extends SQLTableColumnManager<CubridTableColumn, CubridTableBase> implements DBEObjectRenamer<CubridTableColumn> {
	
    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, CubridTableColumn> getObjectsCache(CubridTableColumn object) {
        return object.getParentObject().getContainer().getTableCache().getChildrenCache(object.getParentObject());
    }

    @Override
    public boolean canCreateObject(Object container) {
        return container instanceof CubridTable && CubridUtils.canAlterTable((CubridTable) container);
    }

    @Override
    public boolean canEditObject(CubridTableColumn object) {
        return CubridUtils.canAlterTable(object);
    }

    @Override
    public boolean canDeleteObject(CubridTableColumn object) {
        return CubridUtils.canAlterTable(object);
    }

    @Override
    protected CubridTableColumn createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container, Object copyFrom, Map<String, Object> options) throws DBException {
        CubridTableBase tableBase = (CubridTableBase) container;
        DBSDataType columnType = findBestDataType(tableBase, DBConstants.DEFAULT_DATATYPE_NAMES);

        int columnSize = 0;
        CubridTableColumn column = tableBase.getDataSource().getMetaModel().createTableColumnImpl(
            monitor,
            null,
            tableBase,
            getNewColumnName(monitor, context, tableBase),
            columnType == null ? "INTEGER" : columnType.getName(),
            columnType == null ? Types.INTEGER : columnType.getTypeID(),
            columnType == null ? Types.INTEGER : columnType.getTypeID(),
            -1,
            columnSize,
            columnSize,
            null,
            null,
            10,
            false,
            null,
            null,
            false,
            false,
            false,
            "",
            0,
            0,
            false
        );
        column.setPersisted(false);
        return column;
    }
    
    @Override
    public StringBuilder getNestedDeclaration(DBRProgressMonitor monitor, CubridTableBase owner, DBECommandAbstract<CubridTableColumn> command, Map<String, Object> options) {
    	StringBuilder decl = new StringBuilder(40);
    	CubridTableColumn column = command.getObject();
        String columnName = DBUtils.getQuotedIdentifier(column.getDataSource(), column.getName());
        
        if (command instanceof SQLObjectEditor.ObjectRenameCommand) {
            columnName = DBUtils.getQuotedIdentifier(column.getDataSource(), ((ObjectRenameCommand) command).getNewName());
        }
        decl.append(columnName);
        
        for (ColumnModifier<CubridTableColumn> modifier : new ColumnModifier[] {DataTypeModifier}) {
            modifier.appendModifier(monitor, column, decl, command);
        }
        if(column.getDataKind() == DBPDataKind.STRING && column.getCollation() != null) {
        	decl.append(" COLLATE ").append(SQLUtils.quoteString(column, column.getCollation().getName()));
    	}
        for (ColumnModifier<CubridTableColumn> modifier : new ColumnModifier[] {NotNullModifier}) {
            modifier.appendModifier(monitor, column, decl, command);
        }
        if(column.isInUniqueKey()) {
        	decl.append(" UNIQUE");
        }
        if(!CommonUtils.isEmpty(column.getDefaultValue())){
        	if(column.isShared()) {
            	decl.append(" SHARED ").append(SQLUtils.quoteString(column, column.getDefaultValue()));
            }else {
            	decl.append(" DEFAULT ").append(SQLUtils.quoteString(column, column.getDefaultValue()));
            }
        }
        if(column.isAutoIncrement() && column.getDataKind() == DBPDataKind.NUMERIC) {
    		Integer initialValue = column.getInitialValue() == null ? 1 : column.getInitialValue();
    		Integer incrementValue = column.getIncrementValue() == null ? 1 : column.getIncrementValue();
            decl.append(" AUTO_INCREMENT(").append(initialValue).append(",").append(incrementValue).append(")");
        }   
        if (!CommonUtils.isEmpty(column.getDescription())) {
            decl.append(" COMMENT ").append(SQLUtils.quoteString(column, column.getDescription()));
        }
                               
        return decl;
    }

    public StringBuilder addColumnNestedDeclaration(DBRProgressMonitor monitor, DBECommandAbstract<CubridTableColumn> command) {
    	StringBuilder decl = new StringBuilder(40);
    	final CubridTableColumn column = command.getObject();
    	
    	String columnName = DBUtils.getQuotedIdentifier(column.getDataSource(), column.getName());
        if (command instanceof SQLObjectEditor.ObjectRenameCommand) {
            columnName = DBUtils.getQuotedIdentifier(column.getDataSource(), ((ObjectRenameCommand) command).getNewName());
        }
        decl.append(columnName);
        
        for (ColumnModifier<CubridTableColumn> modifier : new ColumnModifier[] {DataTypeModifier, NullNotNullModifierConditional}) {
            modifier.appendModifier(monitor, column, decl, command);
        }
        if(column.isInUniqueKey()) {
        	decl.append(" UNIQUE");
        }   	
        if(!CommonUtils.isEmpty(column.getDefaultValue())){
        	if(column.isShared()) {
            	decl.append(" SHARED ").append(SQLUtils.quoteString(column, column.getDefaultValue()));
            }else {
            	decl.append(" DEFAULT ").append(SQLUtils.quoteString(column, column.getDefaultValue()));
            }
        }
        if(column.isAutoIncrement()) {
           decl.append(" AUTO_INCREMENT");
        }
        if(column.getDataKind() == DBPDataKind.STRING) {
    		decl.append(" COLLATE ").append(SQLUtils.quoteString(column, column.getCollation().getName()));
    	}
        if(!CommonUtils.isEmpty(column.getDescription())) {
            decl.append(" COMMENT ").append(SQLUtils.quoteString(column, column.getDescription()));
        }
        
        return decl;
    }
    
//    @Override
//    protected ColumnModifier[] getSupportedModifiers(CubridTableColumn column, Map<String, Object> options) {
//        // According to SQL92 DEFAULT comes before constraints
//        CubridMetaModel metaModel = column.getDataSource().getMetaModel();
//        if (!metaModel.supportsNotNullColumnModifiers(column)) {
//            return new ColumnModifier[]{
//                DataTypeModifier, metaModel.isColumnNotNullByDefault() ? NullNotNullModifier : NotNullModifier, DefaultModifier
//            };
//        } else {
//            return new ColumnModifier[]{
//                DataTypeModifier, DefaultModifier,
//                metaModel.isColumnNotNullByDefault() ? NullNotNullModifier : NotNullModifier
//            };
//        }
//    }

    @Override
    protected long getDDLFeatures(CubridTableColumn object) {
        long features = 0;
        if (CommonUtils.toBoolean(object.getDataSource().getContainer().getDriver().getDriverParameter(CubridConstants.PARAM_DDL_DROP_COLUMN_SHORT))) {
            features |= DDL_FEATURE_OMIT_COLUMN_CLAUSE_IN_DROP;
        }
        if (CommonUtils.toBoolean(object.getDataSource().getContainer().getDriver().getDriverParameter(CubridConstants.PARAM_DDL_DROP_COLUMN_BRACKETS))) {
            features |= DDL_FEATURE_USER_BRACKETS_IN_DROP;
        }
        if (CommonUtils.toBoolean(object.getDataSource().getContainer().getDriver().getDriverParameter(CubridConstants.PARAM_ALTER_TABLE_ADD_COLUMN))) {
            features |= FEATURE_ALTER_TABLE_ADD_COLUMN;
        }
        return features;
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options) throws DBException {
    	final CubridTableColumn column = command.getObject();
    	String table = column.getTable().getOwner().getName() + "." + column.getTable();
    	
    	if(column.isAutoIncrement()) {
        	if(column.getInitialValue() == null) {
        		 throw new NullPointerException("Column Initial Value is required.");
        	}        
        }
    	
        actionList.add(
                new SQLDatabasePersistAction(
                    "Modify column",
                    "ALTER TABLE " + table + " MODIFY COLUMN " + addColumnNestedDeclaration(monitor, command)));
        if (column.isAutoIncrement() && command.hasProperty("initialValue")) {
        	actionList.add(
                new SQLDatabasePersistAction(
                "Alter Auto Increment",
                "ALTER TABLE " + table + " AUTO_INCREMENT = " + column.getInitialValue()));
        }
    }
    
    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectRenameCommand command, Map<String, Object> options)
    {
    	final CubridTableColumn column = command.getObject();	
    	actions.add(
    	new SQLDatabasePersistAction(
    	"Rename column",
    	"ALTER TABLE " + column.getTable().getOwner().getName() + "." + column.getTable() + " RENAME COLUMN " +
    	DBUtils.getQuotedIdentifier(column.getDataSource(), command.getOldName()) + " AS " +
    	DBUtils.getQuotedIdentifier(column.getDataSource(), command.getNewName())));
    }

	@Override
	public void renameObject(DBECommandContext commandContext, CubridTableColumn object, Map<String, Object> options,
			String newName) throws DBException {
		processObjectRename(commandContext, object, options, newName);
		
	}
}
