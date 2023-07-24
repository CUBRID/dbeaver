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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cubrid.CubridConstants;
import org.jkiss.dbeaver.ext.cubrid.model.*;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeItem;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Cubrid table manager
 */
public class CubridTableManager extends SQLTableManager<CubridTableBase, CubridStructContainer> implements DBEObjectRenamer<CubridTableBase> {

    private static final Class<? extends DBSObject>[] CHILD_TYPES = CommonUtils.array(
        CubridTableColumn.class,
        CubridUniqueKey.class,
        CubridTableForeignKey.class,
        CubridTableIndex.class
    );

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, CubridTableBase> getObjectsCache(CubridTableBase object)
    {
        return object.getContainer().getTableCache();
    }

    @NotNull
    @Override
    public Class<? extends DBSObject>[] getChildTypes()
    {
        return CHILD_TYPES;
    }

    @Override
    public boolean canCreateObject(Object container) {
        return super.canCreateObject(container);
    }

    @Override
    protected CubridTableBase createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container, Object copyFrom, Map<String, Object> options)
    {
        CubridStructContainer structContainer = (CubridStructContainer) container;

        boolean isView = false;
        Object navContainer = options.get(DBEObjectManager.OPTION_CONTAINER);
        if (navContainer instanceof DBNDatabaseFolder) {
            List<DBXTreeNode> folderChildren = ((DBNDatabaseFolder) navContainer).getMeta().getChildren((DBNNode) navContainer);
            if (folderChildren.size() == 1 && folderChildren.get(0) instanceof DBXTreeItem && ((DBXTreeItem) folderChildren.get(0)).getPropertyName().equals("views")) {
                isView = true;
            }
        }
        String tableName = getNewChildName(monitor, structContainer, isView ? BASE_VIEW_NAME : BASE_TABLE_NAME);
        return structContainer.getDataSource().getMetaModel().createTableImpl(structContainer, tableName,
            isView ? CubridConstants.TABLE_TYPE_VIEW : CubridConstants.TABLE_TYPE_TABLE,
            null);
    }

    @Override
    protected boolean excludeFromDDL(NestedObjectCommand command, Collection<NestedObjectCommand> orderedCommands) {
        // Filter out indexes for unique constraints (if they have the same name)
        DBPObject object = command.getObject();
        if (object instanceof DBSTableIndex) {
            for (NestedObjectCommand ccom : orderedCommands) {
                if (ccom.getObject() instanceof DBSEntityConstraint &&
                    ccom.getObject() != object &&
                    ((DBSEntityConstraint) ccom.getObject()).getConstraintType().isUnique() &&
                    CommonUtils.equalObjects(
                        ((DBSTableIndex) object).getName(), ((DBSEntityConstraint) ccom.getObject()).getName()))
                {
                    return true;
                }
            }
        }
        return false;
    }
    
    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options) {
        CubridTableBase table = command.getObject();
        if (command.hasProperty(CubridConstants.OWNER)) {
        	actionList.add(
                new SQLDatabasePersistAction(
                "Change Owner",
                "ALTER TABLE " + table.getOldOwner().getName() + "." + table + " OWNER TO " + table.getOwner().getName()));
        }
        if (command.hasProperty(CubridConstants.DESCRIPTION)) {
	        actionList.add(
	            new SQLDatabasePersistAction(
	            "Change Comment",
	            "ALTER TABLE " + table.getOldOwner().getName() + "." + table + " COMMENT = " + SQLUtils.quoteString(table, CommonUtils.notEmpty(table.getDescription()))));
        }
        if (command.hasProperty(CubridConstants.COLLATION)) {
	        actionList.add(
	        	new SQLDatabasePersistAction(
	    		"Change Collation",
	            "ALTER TABLE " + table.getOldOwner().getName() + "." + table + " COLLATE " + table.getCollation().getName()));
        }
    }
    
    @Override
    protected void appendTableModifiers(DBRProgressMonitor monitor, CubridTableBase tableBase, NestedObjectCommand tableProps, StringBuilder ddl, boolean alter) {
        if (tableBase instanceof CubridTable) {
            CubridTable table = (CubridTable) tableBase;
			if ((!table.isPersisted() || tableProps.getProperty(CubridConstants.DESCRIPTION) != null) && table.getDescription() != null) { //$NON-NLS-1$
			    ddl.append("\nCOMMENT = ").append(SQLUtils.quoteString(tableBase, CommonUtils.notEmpty(tableBase.getDescription()))); //$NON-NLS-1$
			}
			if ((!table.isPersisted() || tableProps.getProperty(CubridConstants.COLLATION) != null) && table.getCollation().getName() != null) { //$NON-NLS-1$
			    ddl.append("\nCOLLATE ").append(table.getCollation().getName()); //$NON-NLS-1$
			}
        }
    }
    
    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectRenameCommand command, Map<String, Object> options) {
        final CubridDataSource dataSource = command.getObject().getDataSource();
        actions.add(
            new SQLDatabasePersistAction(
                "Rename table",
                "RENAME TABLE " +
                	command.getObject().getOldOwner().getName() + "." + DBUtils.getQuotedIdentifier(dataSource, command.getOldName()) +
                    " TO " + command.getObject().getOldOwner().getName() + "." + DBUtils.getQuotedIdentifier(dataSource, command.getNewName())) //$NON-NLS-1$
        );
    }

	@Override
	public void renameObject(DBECommandContext commandContext, CubridTableBase object, Map<String, Object> options,
			String newName) throws DBException {
			processObjectRename(commandContext, object, options, newName);
	}
	
	@Override
	protected String beginCreateTableStatement(DBRProgressMonitor monitor, CubridTableBase table, String tableName, Map<String, Object> options) throws DBException {
        return "CREATE " + getCreateTableType(table) + " " + table.getOwner().getName() + "." + tableName + " (" + GeneralUtils.getDefaultLineSeparator(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
	
	@Override
    protected void addStructObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, StructCreateCommand command, Map<String, Object> options) throws DBException {
        final CubridTableBase table = command.getObject();

        final NestedObjectCommand tableProps = command.getObjectCommands().get(table);
        if (tableProps == null) {
            log.warn("Object change command not found"); //$NON-NLS-1$
            return;
        }
        final String tableName = DBUtils.getEntityScriptName(table, options);

        final String slComment = SQLUtils.getDialectFromObject(table).getSingleLineComments()[0];
        final String lineSeparator = GeneralUtils.getDefaultLineSeparator();
        StringBuilder createQuery = new StringBuilder(100);
        createQuery.append(beginCreateTableStatement(monitor, table, tableName, options));
        boolean hasNestedDeclarations = false;
        final Collection<NestedObjectCommand> orderedCommands = getNestedOrderedCommands(command);
        for (NestedObjectCommand nestedCommand : orderedCommands) {
            if (nestedCommand.getObject() == table) {
                continue;
            }
            if (excludeFromDDL(nestedCommand, orderedCommands)) {
                continue;
            }
            final String nestedDeclaration = nestedCommand.getNestedDeclaration(monitor, table, options);
            if (!CommonUtils.isEmpty(nestedDeclaration)) {
                // Insert nested declaration
                if (hasNestedDeclarations) {
                    // Check for embedded comment
                    int lastLFPos = createQuery.lastIndexOf(lineSeparator);
                    int lastCommentPos = createQuery.lastIndexOf(slComment);
                    if (lastCommentPos != -1) {
                        while (lastCommentPos > 0 && Character.isWhitespace(createQuery.charAt(lastCommentPos - 1))) {
                            lastCommentPos--;
                        }
                    }
                    if (lastCommentPos < 0 || lastCommentPos < lastLFPos) {
                          createQuery.append(","); //$NON-NLS-1$
                    } else {
                           createQuery.insert(lastCommentPos, ","); //$NON-NLS-1$
                    }
                    createQuery.append(lineSeparator);
                }
                if (!hasNestedDeclarations && !hasAttrDeclarations(table)) {
                    createQuery.append("(\n\t").append(nestedDeclaration); //$NON-NLS-1$  
                } else {
                 createQuery.append("\t").append(nestedDeclaration); //$NON-NLS-1$
                }
                hasNestedDeclarations = true;
            } else {
                // This command should be executed separately
                final DBEPersistAction[] nestedActions = nestedCommand.getPersistActions(monitor, executionContext, options);
                if (nestedActions != null) {
                    Collections.addAll(actions, nestedActions);
                }
            }
        }
        if (hasAttrDeclarations(table) || hasNestedDeclarations) {
            createQuery.append(lineSeparator);
            createQuery.append(")"); //$NON-NLS-1$
            if (table.isReuseOID() == false) {
            	createQuery.append(" DONT_REUSE_OID"); 
            }
        }

        appendTableModifiers(monitor, table, tableProps, createQuery, false);
        actions.add( 0, new SQLDatabasePersistAction(ModelMessages.model_jdbc_create_new_table, createQuery.toString()) );
    }
	
	@Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
        CubridTableBase object = command.getObject();
        final String tableName = DBUtils.getEntityScriptName(object, options);
        actions.add(
            new SQLDatabasePersistAction(
                ModelMessages.model_jdbc_drop_table,
                "DROP " + getDropTableType(object) +  //$NON-NLS-2$
                " " + object.getOwner().getName() +"." + tableName + //$NON-NLS-2$
                (!DBUtils.isView(object) && CommonUtils.getOption(options, OPTION_DELETE_CASCADE) ? " CASCADE" : "") //$NON-NLS-2$
            )
        );
    }
}
