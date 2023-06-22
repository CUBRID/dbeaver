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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cubrid.model.CubridObjectContainer.SystemViewCache;
import org.jkiss.dbeaver.ext.cubrid.model.CubridObjectContainer.SystemTableCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;

import java.util.Collection;
import java.util.List;

/**
 * Cubrid struct container
 */
public interface CubridStructContainer extends DBSObjectContainer, DBSProcedureContainer
{

    @NotNull
    @Override
    CubridDataSource getDataSource();

    CubridStructContainer getObject();

    CubridCatalog getCatalog();

    CubridSchema getSchema();

    TableCache getTableCache();
    
    SystemTableCache getSystemTableCache();
    
    SystemViewCache getSystemViewCache();

    IndexCache getIndexCache();

    ConstraintKeysCache getConstraintKeysCache();

    ForeignKeysCache getForeignKeysCache();

    TableTriggerCache getTableTriggerCache();

    CubridObjectContainer.CubridSequenceCache getSequenceCache();

    CubridObjectContainer.CubridSynonymCache getSynonymCache();

    List<? extends CubridView> getViews(DBRProgressMonitor monitor) throws DBException;
    
    List<? extends CubridView> getSystemViews(DBRProgressMonitor monitor) throws DBException;
    
    List<? extends CubridTable> getPhysicalTables(DBRProgressMonitor monitor) throws DBException;
    
    List<? extends CubridTable> getPhysicalSystemTables(DBRProgressMonitor monitor) throws DBException;
    
    List<? extends CubridTableBase> getTables(DBRProgressMonitor monitor) throws DBException;

    CubridTableBase getTable(DBRProgressMonitor monitor, String name) throws DBException;

    Collection<? extends CubridTableIndex> getIndexes(DBRProgressMonitor monitor) throws DBException;

    Collection<? extends CubridPackage> getPackages(DBRProgressMonitor monitor) throws DBException;

    Collection<? extends CubridProcedure> getProcedures(DBRProgressMonitor monitor) throws DBException;

    Collection<? extends CubridProcedure> getProceduresOnly(DBRProgressMonitor monitor) throws DBException;

    CubridProcedure getProcedure(DBRProgressMonitor monitor, String uniqueName) throws DBException;

    Collection<? extends CubridProcedure> getProcedures(DBRProgressMonitor monitor, String name) throws DBException;

    Collection<? extends CubridProcedure> getFunctionsOnly(DBRProgressMonitor monitor) throws DBException;

    Collection<? extends CubridSequence> getSequences(DBRProgressMonitor monitor) throws DBException;

    Collection<? extends CubridSynonym> getSynonyms(DBRProgressMonitor monitor) throws DBException;

    Collection<? extends CubridTrigger> getTriggers(DBRProgressMonitor monitor) throws DBException;

    Collection<? extends CubridTrigger> getTableTriggers(DBRProgressMonitor monitor) throws DBException;

    Collection<? extends DBSDataType> getDataTypes(DBRProgressMonitor monitor) throws DBException;


}
