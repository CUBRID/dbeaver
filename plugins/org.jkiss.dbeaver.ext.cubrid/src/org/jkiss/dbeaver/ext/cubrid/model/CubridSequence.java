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
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSSequence;

/**
 * CubridSequence
 */
public class CubridSequence implements DBSSequence, DBPQualifiedObject, DBPNamedObject2
{
    private CubridStructContainer container;
    private String unique_name;
    private String name;
    private String description;
    private Number lastValue;
    private Number minValue;
    private Number maxValue;
    private Number incrementBy;
    private Number cyclic;
    private Number cachedNum;

    public CubridSequence(CubridStructContainer container, CubridMetaObject sequenceObject, ResultSet dbResult) {
    	this.container = container;
    	if (dbResult != null) {
	        this.unique_name = CubridUtils.safeGetString(sequenceObject, dbResult, CubridConstants.UNIQUE_NAME);
		    this.name = CubridUtils.safeGetStringTrimmed(sequenceObject, dbResult, CubridConstants.SEQUENCE_NAME);
		    this.description = CubridUtils.safeGetString(sequenceObject, dbResult, CubridConstants.COMMENT);
		    this.lastValue = CubridUtils.safeGetInteger(sequenceObject, dbResult, CubridConstants.CURRENT_VALUE);
		    this.minValue = CubridUtils.safeGetInteger(sequenceObject, dbResult, CubridConstants.MIN_VAL);
		    this.maxValue = CubridUtils.safeGetInteger(sequenceObject, dbResult, CubridConstants.MAX_VAL);
		    this.incrementBy = CubridUtils.safeGetInteger(sequenceObject, dbResult, CubridConstants.INCREMENT_VAL);
		    this.cyclic = CubridUtils.safeGetInteger(sequenceObject, dbResult, CubridConstants.CYCLIC);
		    this.cachedNum = CubridUtils.safeGetInteger(sequenceObject, dbResult, CubridConstants.CACHED_NUM);
    	}
    }

    public String getOwner() {
    	return unique_name != null ? this.unique_name.split("\\.")[0] : null;
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

    @Nullable
    @Override
    @Property(viewable = true, length = PropertyLength.MULTILINE, order = 10)
    public String getDescription() {
        return description;
    }

    @Nullable
    @Override
    public DBSObject getParentObject() {
        return container;
    }

    @NotNull
    @Override
    public CubridDataSource getDataSource() {
        return container.getDataSource();
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(),
            container.getCatalog(),
            container.getSchema(),
            this);
    }

    @Override
    @Property(viewable = true, order = 2)
    public Number getLastValue() {
        return lastValue;
    }

    public void setLastValue(Number lastValue) {
        this.lastValue = lastValue;
    }

    @Override
    @Property(viewable = true, order = 3)
    public Number getMinValue() {
        return minValue;
    }

    public void setMinValue(Number minValue) {
        this.minValue = minValue;
    }

    @Override
    @Property(viewable = true, order = 4)
    public Number getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(Number maxValue) {
        this.maxValue = maxValue;
    }

    @Override
    @Property(viewable = true, order = 5)
    public Number getIncrementBy() {
        return incrementBy;
    }

    public void setIncrementBy(Number incrementBy) {
        this.incrementBy = incrementBy;
    }
    
    @Property(viewable = true, order = 6)
    public Number getCyclic() {
        return cyclic;
    }

    @Property(viewable = true, order = 7)
    public Number getCachedNum() {
        return cachedNum;
    }
}
