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
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * CubridDataType
 */
public class CubridDataType extends JDBCDataType<CubridStructContainer>
{
    public CubridDataType(CubridStructContainer owner, int valueType, String name, @Nullable String remarks, boolean unsigned, boolean searchable, int precision, int minScale, int maxScale) {
        super(owner, valueType, name, remarks, unsigned, searchable, precision, minScale, maxScale);
    }

    public CubridDataType(CubridStructContainer owner, DBSTypedObject typed) {
        super(owner, typed);
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return super.getName();
    }

    @Nullable
    @Override
    @Property(viewable = true, length = PropertyLength.MULTILINE, order = 100)
    public String getDescription()
    {
        return super.getDescription();
    }

    @Override
    @Property(viewable = true, order = 20)
    public Integer getPrecision()
    {
        return super.getPrecision();
    }

    @Override
    @Property(viewable = true, order = 25)
    public int getMinScale()
    {
        return super.getMinScale();
    }

    @Override
    @Property(viewable = true, order = 26)
    public int getMaxScale()
    {
        return super.getMaxScale();
    }

}
