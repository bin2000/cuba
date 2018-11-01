/*
 * Copyright (c) 2008-2018 Haulmont.
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

package com.haulmont.cuba.gui.components.compatibility;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.gui.components.TwinColumn.OptionStyleItem;
import com.haulmont.cuba.gui.components.TwinColumn.StyleProvider;

import java.util.function.Function;

@Deprecated
public class TwinColumnStyleProviderAdapter<V> implements Function<OptionStyleItem<V>, String> {

    protected StyleProvider styleProvider;

    public TwinColumnStyleProviderAdapter(StyleProvider styleProvider) {
        this.styleProvider = styleProvider;
    }

    @Override
    public String apply(OptionStyleItem<V> item) {
        if (styleProvider != null && item.getItem() instanceof Entity) {
            return styleProvider.getStyleName((Entity) item.getItem(), ((Entity) item.getItem()).getId(), item.isSelected());
        } else {
            return null;
        }
    }

    public StyleProvider getStyleProvider() {
        return styleProvider;
    }
}
