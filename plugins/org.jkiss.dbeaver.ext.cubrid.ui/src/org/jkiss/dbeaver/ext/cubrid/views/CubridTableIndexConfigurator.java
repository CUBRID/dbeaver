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

package org.jkiss.dbeaver.ext.cubrid.views;

import org.jkiss.dbeaver.ext.cubrid.model.CubridTableBase;
import org.jkiss.dbeaver.ext.cubrid.model.CubridTableColumn;
import org.jkiss.dbeaver.ext.cubrid.model.CubridTableIndex;
import org.jkiss.dbeaver.ext.cubrid.model.CubridTableIndexColumn;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditIndexPage;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.Map;

/**
 * Cubrid table index configurator
 */
public class CubridTableIndexConfigurator implements DBEObjectConfigurator<CubridTableIndex> {

    @Override
    public CubridTableIndex configureObject(DBRProgressMonitor monitor, Object table, CubridTableIndex index, Map<String, Object> options) {
        CubridTableBase tableBase = (CubridTableBase) table;
        boolean supportUniqueIndexes = tableBase.supportUniqueIndexes();
        Collection<DBSIndexType> tableIndexTypes = tableBase.getTableIndexTypes();
        return new UITask<CubridTableIndex>() {
            @Override
            protected CubridTableIndex runTask() {
                EditIndexPage editPage = new EditIndexPage(
                    "Create index",
                    index,
                    tableIndexTypes, supportUniqueIndexes);
                if (!editPage.edit()) {
                    return null;
                }
                index.setIndexType(editPage.getIndexType());
                StringBuilder idxName = new StringBuilder(64);
                idxName.append(CommonUtils.escapeIdentifier(index.getTable().getName()));
                int colIndex = 1;
                for (DBSEntityAttribute tableColumn : editPage.getSelectedAttributes()) {
                    if (colIndex == 1) {
                        idxName.append("_").append(CommonUtils.escapeIdentifier(tableColumn.getName()));
                    }
                    index.addColumn(
                        new CubridTableIndexColumn(
                            index,
                            (CubridTableColumn) tableColumn,
                            colIndex++,
                            !Boolean.TRUE.equals(editPage.getAttributeProperty(tableColumn, EditIndexPage.PROP_DESC))));
                }
                idxName.append("_IDX");
                index.setName(DBObjectNameCaseTransformer.transformObjectName(index, idxName.toString()));
                index.setUnique(editPage.isUnique());
                return index;
            }
        }.execute();
    }

}
