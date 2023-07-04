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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.cubrid.CubridConstants;
import org.jkiss.dbeaver.ext.cubrid.model.meta.CubridMetaModel;
import org.jkiss.dbeaver.ext.cubrid.model.meta.CubridMetaObject;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.AbstractExecutionSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyDeferability;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.utils.CommonUtils;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.*;

/**
 * Cubrid table
 */
public abstract class CubridTableBase extends JDBCTable<CubridDataSource, CubridStructContainer> implements DBPRefreshableObject, DBPSystemObject, DBPScriptObject {
    private static final Log log = Log.getLog(CubridTableBase.class);

    private String tableType;
    private boolean isSystem;
    private boolean isUtility;
    private String description;
    private CubridOwner owner;
    private CubridOwner oldOwner;
    private boolean reuseOID;
    private CubridCollation collation;
    private Long rowCount;
    private List<? extends CubridTrigger> triggers;
    private final String tableCatalogName;
    private final String tableSchemaName;

    public CubridTableBase(
        CubridStructContainer container,
        @Nullable String tableName,
        @Nullable String tableType,
        @Nullable JDBCResultSet dbResult) {
        super(container, tableName, dbResult != null);
        this.tableType = tableType;
        if (this.tableType == null) {
            this.tableType = "";
        }
        
        if (dbResult != null) {
            this.description = CubridUtils.safeGetString(container.getTableCache().tableObject, dbResult, JDBCConstants.REMARKS);
            this.reuseOID = (CubridUtils.safeGetString(container.getTableCache().tableObject, dbResult, CubridConstants.REUSE_OID)).equals("YES") ? true : false;
            
            String collation_name = CubridUtils.safeGetString(container.getTableCache().tableObject, dbResult, CubridConstants.COLLATION);
            
            for(CubridCollation cbCollation : getDataSource().getCollations()){
              if(cbCollation.getName().equals(collation_name)) {
                this.collation = cbCollation;
              }
            }
            
            for(CubridOwner cbOwner : getDataSource().getOwners()){
                if(cbOwner.getName().equals(CubridUtils.safeGetString(container.getTableCache().tableObject, dbResult, CubridConstants.OWNER))) {
                  this.owner = cbOwner;
                  this.oldOwner = cbOwner;
                }
              }
            
        }

        final CubridMetaModel metaModel = container.getDataSource().getMetaModel();
        this.isSystem = metaModel.isSystemTable(this);
        this.isUtility = metaModel.isUtilityTable(this);
        
        boolean mergeEntities = container.getDataSource().isMergeEntities();
        if (mergeEntities && dbResult != null) {
            tableCatalogName = CubridUtils.safeGetString(container.getTableCache().tableObject, dbResult, JDBCConstants.TABLE_CATALOG);
            tableSchemaName = CubridUtils.safeGetString(container.getTableCache().tableObject, dbResult, JDBCConstants.TABLE_SCHEM);
        } else {
            tableCatalogName = null;
            tableSchemaName = null;
        }
    }
    
    @Override
    public TableCache getCache() {
        return getContainer().getTableCache();
    }

    @Override
    protected boolean isTruncateSupported() {
        return CommonUtils.getBoolean(
            getDataSource().getContainer().getDriver().getDriverParameter(CubridConstants.PARAM_SUPPORTS_TRUNCATE),
            false);
    }

    @Override
    public CubridStructContainer getParentObject() {
        return getContainer().getObject();
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        if (isView() && context == DBPEvaluationContext.DDL && !getDataSource().getMetaModel().useCatalogInObjectNames()) {
            // [SQL Server] workaround. You can't use catalog name in operations with views.
            return DBUtils.getFullQualifiedName(
                getDataSource(),
                getSchema(),
                this);
        }
        return DBUtils.getFullQualifiedName(
            getDataSource(),
            getCatalog(),
            getSchema(),
            this);
    }

    @Override
    public boolean isSystem() {
        return this.isSystem;
    }

    public void setSystem(boolean system) {
        isSystem = system;
    }

    public boolean isUtility() {
        return isUtility;
    }

    @Property(viewable = true, order = 2)
    public String getTableType() {
        return tableType;
    }

    @Property(viewable = true, optional = true, order = 3)
    public CubridCatalog getCatalog() {
        if (!CommonUtils.isEmpty(tableCatalogName)) {
            getDataSource().getCatalog(tableCatalogName);
        }
        return getContainer().getCatalog();
    }

    //@Property(viewable = true, optional = true, order = 3)
    public String getCatalogName() {
        return tableCatalogName;
    }

    @Property(viewable = true, optional = true, order = 4)
    public CubridSchema getSchema() {
        CubridStructContainer container = getContainer();
        if (!CommonUtils.isEmpty(tableSchemaName)) {
            if (!(container instanceof CubridCatalog)) {
                return getDataSource().getSchema(tableSchemaName);
            } else {
                try {
                    return ((CubridCatalog)container).getSchema(new VoidProgressMonitor(), tableSchemaName);
                } catch (Exception e) {
                    log.error(e);
                    return null;
                }
            }
        }
        return container.getSchema();
    }

    //@Property(viewable = true, optional = true, order = 4)
    public String getSchemaName() {
        return tableSchemaName;
    }

    @Nullable
    @Override
    public List<? extends CubridTableColumn> getAttributes(@NotNull DBRProgressMonitor monitor)
        throws DBException {
        return this.getContainer().getTableCache().getChildren(monitor, getContainer(), this);
    }

    @Override
    public CubridTableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName)
        throws DBException {
        return this.getContainer().getTableCache().getChild(monitor, getContainer(), this, attributeName);
    }

    public void addAttribute(CubridTableColumn column) {
        this.getContainer().getTableCache().getChildrenCache(this).cacheObject(column);
    }

    public void removeAttribute(CubridTableColumn column) {
        this.getContainer().getTableCache().getChildrenCache(this).removeObject(column, false);
    }

    @Override
    public Collection<? extends CubridTableIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException {
        if (getDataSource().getInfo().supportsIndexes()) {
            // Read indexes using cache
            return this.getContainer().getIndexCache().getObjects(monitor, getContainer(), this);
        }
        return null;
    }

    @Nullable
    @Override
    public List<CubridUniqueKey> getConstraints(@NotNull DBRProgressMonitor monitor)
        throws DBException {
        if (getDataSource().getInfo().supportsReferentialIntegrity() || getDataSource().getInfo().supportsIndexes()) {
            // ensure all columns are already cached
            getAttributes(monitor);
            return getContainer().getConstraintKeysCache().getObjects(monitor, getContainer(), this);
        }
        return null;
    }

    public CubridUniqueKey getConstraint(@NotNull DBRProgressMonitor monitor, String name) throws DBException {
        if (getDataSource().getInfo().supportsReferentialIntegrity() || getDataSource().getInfo().supportsIndexes()) {
            // ensure all columns are already cached
            getAttributes(monitor);
            return getContainer().getConstraintKeysCache().getObject(monitor, getContainer(), this, name);
        }
        return null;
    }

    public void addUniqueKey(CubridUniqueKey constraint) {
        getContainer().getConstraintKeysCache().cacheObject(constraint);
    }

    @Override
    public Collection<CubridTableForeignKey> getReferences(@NotNull DBRProgressMonitor monitor)
        throws DBException {
        if (getDataSource().getInfo().supportsReferentialIntegrity()) {
            return loadReferences(monitor);
        }
        return null;
    }

    @Override
    public Collection<? extends CubridTableForeignKey> getAssociations(@NotNull DBRProgressMonitor monitor)
        throws DBException {
        if (getDataSource().getInfo().supportsReferentialIntegrity()) {
            return getContainer().getForeignKeysCache().getObjects(monitor, getContainer(), this);
        }
        return null;
    }

    public CubridTableForeignKey getAssociation(@NotNull DBRProgressMonitor monitor, String name)
        throws DBException {
        if (getDataSource().getInfo().supportsReferentialIntegrity()) {
            return getContainer().getForeignKeysCache().getObject(monitor, getContainer(), this, name);
        }
        return null;
    }

    @Association
    @Nullable
    public Collection<CubridTableBase> getSubTables() {
        return null;
    }

    @Nullable
    @Override
    @Property(viewable = true, editableExpr = "object.dataSource.metaModel.tableCommentEditable", updatableExpr = "object.dataSource.metaModel.tableCommentEditable", length = PropertyLength.MULTILINE, order = 3)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    @Nullable
    @Property(viewable = true, editable = true, updatable = true, listProvider = OwnerListProvider.class, order = 2)
    public CubridOwner getOwner() {
        return owner;
    }
    
    public CubridOwner getOldOwner() {
    	return this.oldOwner;
    }

    public void setOwner(CubridOwner owner) {
        this.owner = owner;
    }
    
    @Nullable
    @Property(viewable = true, editable = true, updatable = true, listProvider = CollationListProvider.class, order = 9)
    public CubridCollation getCollation() {
        return collation;
    }

    public void setCollation(CubridCollation collation) {
        this.collation = collation;
    }
    
    @Property(viewable = true, order = 52)
    public boolean isReuseOID()
    {
        return reuseOID;
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        this.getContainer().getIndexCache().clearObjectCache(this);
        this.getContainer().getConstraintKeysCache().clearObjectCache(this);
        this.getContainer().getForeignKeysCache().clearObjectCache(this);
        return this.getContainer().getTableCache().refreshObject(monitor, getContainer(), this);
    }

    // Comment row count calculation - it works too long and takes a lot of resources without serious reason
    @Nullable
    @Property(viewable = false, expensive = true, order = 5, category = DBConstants.CAT_STATISTICS)
    public Long getRowCount(DBRProgressMonitor monitor) {
        if (rowCount != null) {
            return rowCount;
        }
        if (isView() || !isPersisted()) {
            // Do not count rows for views
            return null;
        }
        if (Boolean.FALSE.equals(getDataSource().getContainer().getDriver().getDriverParameter(CubridConstants.PARAM_SUPPORTS_SELECT_COUNT))) {
            // Select count not supported
            return null;
        }
        if (rowCount == null) {
            // Query row count
            try (DBCSession session = DBUtils.openUtilSession(monitor, this, "Read row count")) {
                rowCount = countData(
                    new AbstractExecutionSource(this, session.getExecutionContext(), this), session, null, DBSDataContainer.FLAG_NONE);
            } catch (DBException e) {
                // do not throw this error - row count is optional info and some providers may fail
                log.debug("Can't fetch row count: " + e.getMessage());
//                if (indexes != null) {
//                    rowCount = getRowCountFromIndexes(monitor);
//                }
            }
        }
        if (rowCount == null) {
            rowCount = -1L;
        }

        return rowCount;
    }

    
    @Nullable
    public Long getRowCountFromIndexes(DBRProgressMonitor monitor) {
        try {
            // Try to get cardinality from some unique index
            // Cardinality
            final Collection<? extends CubridTableIndex> indexList = getIndexes(monitor);
            if (!CommonUtils.isEmpty(indexList)) {
                for (CubridTableIndex index : indexList) {
                    if (index.isUnique()/* || index.getIndexType() == DBSIndexType.STATISTIC*/) {
                        final long cardinality = index.getCardinality();
                        if (cardinality > 0) {
                            return cardinality;
                        }
                    }
                }
            }
        } catch (DBException e) {
            log.error(e);
        }
        return null;
    }

    public boolean isPhysicalTable() {
        return !isView();
    }

    public abstract String getDDL();

    private List<CubridTableForeignKey> loadReferences(DBRProgressMonitor monitor)
        throws DBException {
        if (!isPersisted() || !getDataSource().getInfo().supportsReferentialIntegrity()) {
            return new ArrayList<>();
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table relations")) {
            // Read foreign keys in two passes
            // First read entire resultset to prevent recursive metadata requests
            // some drivers don't like it
            final CubridMetaObject fkObject = getDataSource().getMetaObject(CubridConstants.OBJECT_FOREIGN_KEY);
            final List<ForeignKeyInfo> fkInfos = loadReferenceInfoList(session, fkObject);

            List<CubridTableForeignKey> fkList = new ArrayList<>();
            Map<String, CubridTableForeignKey> fkMap = new HashMap<>();
            for (ForeignKeyInfo info : fkInfos) {
                DBSForeignKeyModifyRule deleteRule = JDBCUtils.getCascadeFromNum(info.deleteRuleNum);
                DBSForeignKeyModifyRule updateRule = JDBCUtils.getCascadeFromNum(info.updateRuleNum);
                DBSForeignKeyDeferability deferability;
                switch (info.deferabilityNum) {
                    case DatabaseMetaData.importedKeyInitiallyDeferred:
                        deferability = DBSForeignKeyDeferability.INITIALLY_DEFERRED;
                        break;
                    case DatabaseMetaData.importedKeyInitiallyImmediate:
                        deferability = DBSForeignKeyDeferability.INITIALLY_IMMEDIATE;
                        break;
                    case DatabaseMetaData.importedKeyNotDeferrable:
                        deferability = DBSForeignKeyDeferability.NOT_DEFERRABLE;
                        break;
                    default:
                        deferability = DBSForeignKeyDeferability.UNKNOWN;
                        break;
                }

                if (info.fkTableName == null) {
                    log.debug("Null FK table name");
                    continue;
                }
                //String fkTableFullName = DBUtils.getFullyQualifiedName(getDataSource(), info.fkTableCatalog, info.fkTableSchema, info.fkTableName);
                CubridTableBase fkTable = getDataSource().findTable(monitor, info.fkTableCatalog, info.fkTableSchema, info.fkTableName);
                if (fkTable == null) {
                    log.warn("Can't find FK table " + info.fkTableName);
                    continue;
                }
                CubridTableColumn pkColumn = this.getAttribute(monitor, info.pkColumnName);
                if (pkColumn == null) {
                    log.warn("Can't find PK column " + info.pkColumnName);
                    continue;
                }
                CubridTableColumn fkColumn = fkTable.getAttribute(monitor, info.fkColumnName);
                if (fkColumn == null) {
                    log.warn("Can't find FK table " + fkTable.getFullyQualifiedName(DBPEvaluationContext.DDL) + " column " + info.fkColumnName);
                    continue;
                }

                // Find PK
                CubridUniqueKey pk = null;
                if (!CommonUtils.isEmpty(info.pkName)) {
                    pk = DBUtils.findObject(this.getConstraints(monitor), info.pkName);
                    if (pk == null) {
                        log.debug("Unique key '" + info.pkName + "' not found in table " + this.getFullyQualifiedName(DBPEvaluationContext.DDL) + " for FK " + info.fkName);
                    }
                }
                if (pk == null) {
                    Collection<CubridUniqueKey> uniqueKeys = this.getConstraints(monitor);
                    if (uniqueKeys != null) {
                        for (CubridUniqueKey pkConstraint : uniqueKeys) {
                            if (pkConstraint.getConstraintType().isUnique() && DBUtils.getConstraintAttribute(monitor, pkConstraint, pkColumn) != null) {
                                pk = pkConstraint;
                                break;
                            }
                        }
                    }
                }
                if (pk == null) {
                    log.warn("Can't find unique key for table " + this.getFullyQualifiedName(DBPEvaluationContext.DDL) + " column " + pkColumn.getName());
                    // Too bad. But we have to create new fake PK for this FK
                    //String pkFullName = getFullyQualifiedName() + "." + info.pkName;
                    pk = this.getDataSource().getMetaModel().createConstraintImpl(this, info.pkName, DBSEntityConstraintType.PRIMARY_KEY, null, true);
                    pk.addColumn(new CubridTableConstraintColumn(pk, pkColumn, info.keySeq));
                    // Add this fake constraint to it's owner
                    this.addUniqueKey(pk);
                }

                // Find (or create) FK
                CubridTableForeignKey fk;
                if (CommonUtils.isEmpty(info.fkName)) {
                    // Make fake FK name
                    info.fkName = info.fkTableName.toUpperCase() + "_FK" + info.keySeq;
                    fk = DBUtils.findObject(fkTable.getAssociations(monitor), info.fkName);
                } else {
                    fk = DBUtils.findObject(fkTable.getAssociations(monitor), info.fkName);
                    if (fk == null) {
                        log.warn("Can't find foreign key '" + info.fkName + "' for table " + fkTable.getFullyQualifiedName(DBPEvaluationContext.DDL));
                        // No choice, we have to create fake foreign key :(
                    }
                }
                if (fk != null && !fkList.contains(fk)) {
                    fkList.add(fk);
                }

                if (fk == null) {
                    fk = fkMap.get(info.fkName);
                    if (fk == null) {
                        fk = fkTable.getDataSource().getMetaModel().createTableForeignKeyImpl(fkTable, info.fkName, null, pk, deleteRule, updateRule, deferability, true);
                        fkMap.put(info.fkName, fk);
                        fkList.add(fk);
                    }
                    CubridTableForeignKeyColumnTable fkColumnInfo = new CubridTableForeignKeyColumnTable(fk, fkColumn, info.keySeq, pkColumn);
                    fk.addColumn(fkColumnInfo);
                }
            }

            return fkList;
        } catch (SQLException ex) {
            if (ex instanceof SQLFeatureNotSupportedException) {
                log.debug("Error reading references", ex);
                return Collections.emptyList();
            } else {
                throw new DBException(ex, getDataSource());
            }
        }
    }

    public List<ForeignKeyInfo> loadReferenceInfoList(@NotNull JDBCSession session, CubridMetaObject fkObject) throws SQLException {
        final List<ForeignKeyInfo> fkInfos = new ArrayList<>();
        JDBCDatabaseMetaData metaData = session.getMetaData();
        // Load indexes
        try (JDBCResultSet dbResult = metaData.getExportedKeys(
            getCatalog() == null ? null : getCatalog().getName(),
            getSchema() == null ? null : getSchema().getName(),
            getName())) {
            while (dbResult.next()) {
                ForeignKeyInfo fkInfo = new ForeignKeyInfo();
                fkInfo.fetchColumnsInfo(fkObject, dbResult);
                fkInfos.add(fkInfo);
            }
        }
        return fkInfos;
    }

    @Nullable
    @Association
    public List<? extends CubridTrigger> getTriggers(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (triggers == null) {
            CubridStructContainer parentObject = getParentObject();
            if (parentObject != null) {
                TableTriggerCache tableTriggerCache = parentObject.getTableTriggerCache();
                if (tableTriggerCache != null) {
                    triggers = tableTriggerCache.getObjects(monitor, parentObject, this);
                }
            } else {
                loadTriggers(monitor);
            }
        }
        return triggers;
    }

    private void loadTriggers(DBRProgressMonitor monitor) throws DBException {
        triggers = getDataSource().getMetaModel().loadTriggers(monitor, getContainer(), this);
        if (triggers == null) {
            triggers = new ArrayList<>();
        } else {
            DBUtils.orderObjects(triggers);
        }
    }

    public List<? extends CubridTrigger> getTriggerCache() {
        return triggers;
    }

    public boolean supportUniqueIndexes() {
        return true;
    }

    public Collection<DBSIndexType> getTableIndexTypes() {
        return Collections.singletonList(DBSIndexType.OTHER);
    }
    
    public static class CollationListProvider implements IPropertyValueListProvider<CubridTableBase> {
        @Override
        public boolean allowCustomValue()
        {
            return false;
        }
        @Override
        public Object[] getPossibleValues(CubridTableBase object)
        {
        	return object.getDataSource().getCollations().toArray();
        }
    }
    
    public static class OwnerListProvider implements IPropertyValueListProvider<CubridTableBase> {
        @Override
        public boolean allowCustomValue()
        {
            return false;
        }
        @Override
        public Object[] getPossibleValues(CubridTableBase object)
        {
        	return object.getDataSource().getOwners().toArray();
        }
    }
}
