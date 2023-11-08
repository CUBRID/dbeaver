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

import java.sql.ResultSet;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.cubrid.CubridConstants;
import org.jkiss.dbeaver.ext.cubrid.model.meta.CubridMetaObject;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Cubrid synonym (alias).
 * There is no synonyms support in JDBC API. Each Cubrid-based extension must provide its own implementation.
 */
public class CubridSynonym implements DBSObject, DBPNamedObject2
{
    private CubridStructContainer container;
    private String name;
    private String description;
    private String owner;
    private String targetName;

    public CubridSynonym(CubridStructContainer container, CubridMetaObject synonymObject, ResultSet dbResult) {
        this.container = container;
        this.name = CubridUtils.safeGetString(synonymObject, dbResult, CubridConstants.SYNONYM_NAME);
        this.owner = CubridUtils.safeGetString(synonymObject, dbResult, CubridConstants.SYNONYM_OWNER_NAME);
        this.targetName = CubridUtils.safeGetString(synonymObject, dbResult, CubridConstants.TARGET_NAME);
        this.description = CubridUtils.safeGetString(synonymObject, dbResult, CubridConstants.COMMENT);
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Property(viewable = true, order = 2)
    public String getOwner() {
        return owner;
    }

    @Property(viewable = true, order = 3)
    public String getTargetName() {
        return targetName;
    }

    @Nullable
    @Override
    @Property(viewable = true, length = PropertyLength.MULTILINE, order = 10)
    public String getDescription() {
        return description;
    }

    @Nullable
    @Override
    public CubridStructContainer getParentObject() {
        return container;
    }

    @NotNull
    @Override
    public CubridDataSource getDataSource() {
        return container.getDataSource();
    }
}
