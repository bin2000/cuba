/*
 * Copyright (c) 2008-2016 Haulmont.
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
 *
 */

package com.haulmont.cuba.gui.components.filter;

import com.haulmont.bali.events.Subscription;
import com.haulmont.chile.core.datatypes.impl.EnumClass;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.core.global.filter.Op;
import com.haulmont.cuba.core.global.filter.ParametersHelper;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.components.SizeUnit;
import com.haulmont.cuba.gui.components.filter.condition.AbstractCondition;
import com.haulmont.cuba.gui.components.filter.condition.CustomCondition;
import com.haulmont.cuba.gui.components.filter.condition.DynamicAttributesCondition;
import com.haulmont.cuba.gui.components.filter.condition.PropertyCondition;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParamWrapper implements Component, HasValue<Object> {

    public static final Pattern LIKE_PATTERN = Pattern.compile("\\slike\\s+" + ParametersHelper.QUERY_PARAMETERS_RE + "\\s*?(escape '(\\S+)')?",
            Pattern.CASE_INSENSITIVE);

    protected final AbstractCondition condition;
    protected final Param param;

    protected Component parent;

    public ParamWrapper(AbstractCondition condition, Param param) {
        this.condition = condition;
        this.param = param;
    }

    @Override
    public Object getValue() {
        Object value = param.getValue();
        if (value instanceof String
                && !StringUtils.isEmpty((String) value)
                && !((String) value).startsWith(ParametersHelper.CASE_INSENSITIVE_MARKER)) {
            // try to wrap value for case-insensitive "like" search
            if (condition instanceof PropertyCondition || condition instanceof DynamicAttributesCondition) {
                String escapedValue = value.toString();
                if (condition.getEntityMetaClass() != null) {
                    String thisStore = AppBeans.get(MetadataTools.class).getStoreName(condition.getEntityMetaClass());
                    GlobalConfig config = AppBeans.get(Configuration.class).getConfig(GlobalConfig.class);
                    if (config.getDisableEscapingLikeForDataStores() == null || !config.getDisableEscapingLikeForDataStores().contains(thisStore)) {
                        escapedValue = QueryUtils.escapeForLike(escapedValue);
                    }
                } else {
                    escapedValue = QueryUtils.escapeForLike(escapedValue);
                }
                Op op = condition.getOperator();
                if (Op.CONTAINS.equals(op) || op.equals(Op.DOES_NOT_CONTAIN)) {
                    value = wrapValueForLike(escapedValue);
                } else if (Op.STARTS_WITH.equals(op)) {
                    value = wrapValueForLike(escapedValue, false, true);
                } else if (Op.ENDS_WITH.equals(op)) {
                    value = wrapValueForLike(escapedValue, true, false);
                }
            } else if (condition instanceof CustomCondition) {
                String where = ((CustomCondition) condition).getWhere();
                Op op = condition.getOperator();
                Matcher matcher = LIKE_PATTERN.matcher(where);
                if (matcher.find()) {
                    String stringValue = value.toString();
                    boolean escape = StringUtils.isNotEmpty(matcher.group(3));
                    if (escape) {
                        String escapeChar = matcher.group(4);
                        if (StringUtils.isNotEmpty(escapeChar)) {
                            stringValue = QueryUtils.escapeForLike(stringValue, escapeChar);
                        }
                    }
                    if (Op.STARTS_WITH.equals(op)) {
                        value = wrapValueForLike(stringValue, false, true);
                    } else if (Op.ENDS_WITH.equals(op)) {
                        value = wrapValueForLike(stringValue, true, false);
                    } else {
                        value = wrapValueForLike(stringValue);
                    }
                }
            } else if (value instanceof EnumClass) {
                value = ((EnumClass) value).getId();
            }
        } else if (value instanceof Entity) {
            value = ((Entity) value).getId();
        } else if (value instanceof Collection) {
            List<Object> list = new ArrayList<>(((Collection) value).size());
            for (Object obj : ((Collection) value)) {
                list.add(obj instanceof Entity ? ((Entity) obj).getId() : obj);
            }
            value = list;
        } else if (value instanceof EnumClass) {
            value = ((EnumClass) value).getId();
        }
        return value;
    }

    protected String wrapValueForLike(Object value) {
        return wrapValueForLike(value, true, true);
    }

    protected String wrapValueForLike(Object value, boolean before, boolean after) {
        return ParametersHelper.CASE_INSENSITIVE_MARKER + (before ? "%" : "") + value + (after ? "%" : "");
    }

    @Override
    public void setValue(Object value) {
        param.setValue(value);
    }

    @Override
    public Subscription addValueChangeListener(Consumer<ValueChangeEvent<Object>> listener) {
        param.addValueChangeListener(new ParamValueChangeListenerWrapper(this, (Consumer) listener));

        return () -> removeValueChangeListener(listener);
    }

    @Override
    public void removeValueChangeListener(Consumer listener) {
        param.removeValueChangeListener(new ParamValueChangeListenerWrapper(this, listener));
    }

    protected static class ParamValueChangeListenerWrapper implements Param.ParamValueChangeListener {

        protected final ParamWrapper paramWrapper;
        protected final Consumer<ValueChangeEvent> valueChangeListener;

        public ParamValueChangeListenerWrapper(ParamWrapper paramWrapper,
                                               Consumer<ValueChangeEvent> valueChangeListener) {
            this.paramWrapper = paramWrapper;
            this.valueChangeListener = valueChangeListener;
        }

        @Override
        public void valueChanged(@Nullable Object prevValue, @Nullable Object value) {
            valueChangeListener.accept(new ValueChangeEvent(paramWrapper, prevValue, value));
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }

            if (obj.getClass() != getClass()) {
                return false;
            }

            ParamValueChangeListenerWrapper that = (ParamValueChangeListenerWrapper) obj;

            return this.valueChangeListener.equals(that.valueChangeListener);
        }

        @Override
        public int hashCode() {
            return valueChangeListener.hashCode();
        }
    }

    @Override
    public String getId() {
        return param.getName();
    }

    @Override
    public void setId(String id) {
    }

    @Override
    public Component getParent() {
        return parent;
    }

    @Override
    public void setParent(Component parent) {
        this.parent = parent;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void setEnabled(boolean enabled) {
    }

    @Override
    public boolean isResponsive() {
        return false;
    }

    @Override
    public void setResponsive(boolean responsive) {
    }

    @Override
    public boolean isVisible() {
        return false;
    }

    @Override
    public void setVisible(boolean visible) {
    }

    @Override
    public boolean isVisibleRecursive() {
        return false;
    }

    @Override
    public boolean isEnabledRecursive() {
        return false;
    }

    @Override
    public float getHeight() {
        return 0;
    }

    @Override
    public SizeUnit getHeightSizeUnit() {
        return SizeUnit.PIXELS;
    }

    @Override
    public void setHeight(String height) {
    }

    @Override
    public float getWidth() {
        return 0;
    }

    @Override
    public SizeUnit getWidthSizeUnit() {
        return SizeUnit.PIXELS;
    }

    @Override
    public void setWidth(String width) {
    }

    @Override
    public Alignment getAlignment() {
        return Alignment.TOP_LEFT;
    }

    @Override
    public void setAlignment(Alignment alignment) {
    }

    @Override
    public String getStyleName() {
        return null;
    }

    @Override
    public void setStyleName(String name) {
    }

    @Override
    public void addStyleName(String styleName) {
    }

    @Override
    public void removeStyleName(String styleName) {
    }

    @Override
    public <X> X unwrap(Class<X> internalComponentClass) {
        return null;
    }

    @Override
    public <X> X unwrapComposition(Class<X> internalCompositionClass) {
        return null;
    }
}