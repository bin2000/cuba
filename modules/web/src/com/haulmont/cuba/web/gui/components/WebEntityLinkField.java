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

package com.haulmont.cuba.web.gui.components;

import com.haulmont.chile.core.datatypes.Datatype;
import com.haulmont.chile.core.datatypes.Datatypes;
import com.haulmont.chile.core.model.Instance;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.SoftDelete;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.ComponentsHelper;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.data.ValueSource;
import com.haulmont.cuba.gui.components.data.meta.EntityValueSource;
import com.haulmont.cuba.gui.config.WindowConfig;
import com.haulmont.cuba.gui.data.DataSupplier;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.impl.DatasourceImplementation;
import com.haulmont.cuba.gui.screen.compatibility.LegacyFrame;
import com.haulmont.cuba.web.widgets.CubaButtonField;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;

import static com.haulmont.cuba.gui.WindowManager.OpenType;

public class WebEntityLinkField<V> extends WebV8AbstractField<CubaButtonField<V>, V, V>
        implements EntityLinkField<V>, InitializingBean {

    protected static final String EMPTY_VALUE_STYLENAME = "empty-value";

    protected EntityLinkClickHandler clickHandler;

    protected String screen;
    protected OpenType screenOpenType = OpenType.THIS_TAB;
    protected ScreenCloseListener screenCloseListener;
    protected Map<String, Object> screenParams;

    protected MetaClass metaClass;
    protected ListComponent owner;

    protected Datasource.ItemChangeListener itemChangeListener;
    protected Datasource.ItemPropertyChangeListener itemPropertyChangeListener;

    /* Beans */
    protected MetadataTools metadataTools;

    public WebEntityLinkField() {
        component = createComponent();
        attachValueChangeListener(component);
    }

    @Inject
    public void setMetadataTools(MetadataTools metadataTools) {
        this.metadataTools = metadataTools;
    }

    protected CubaButtonField<V> createComponent() {
        return new CubaButtonField<>();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        initComponent();
    }

    protected void initComponent() {
        component.addClickListener(event -> {
            if (clickHandler != null) {
                clickHandler.onClick(WebEntityLinkField.this);
            } else {
                openEntityEditor();
            }
        });
        component.setCaptionFormatter((value, locale) -> {
            if (value == null) {
                return "";
            }

            if (value instanceof Instance) {
                return metadataTools.getInstanceName((Instance) value);
            }

            Datatype datatype = Datatypes.getNN(value.getClass());

            if (locale != null) {
                return datatype.format(value, locale);
            }

            return datatype.format(value);
        });
    }

    @Override
    public MetaClass getMetaClass() {
        ValueSource<V> valueSource = getValueSource();

        if (valueSource instanceof EntityValueSource) {
            MetaPropertyPath metaPropertyPath = ((EntityValueSource) valueSource).getMetaPropertyPath();
            MetaProperty metaProperty = metaPropertyPath.getMetaProperty();
            if (metaProperty.getRange().isClass()) {
                return metaProperty.getRange().asClass();
            }
        }
        return metaClass;
    }

    @Override
    public void setMetaClass(MetaClass metaClass) {
        ValueSource<V> valueSource = getValueSource();

        if (valueSource instanceof EntityValueSource) {
            throw new IllegalStateException("ValueSource is not null");
        }
        this.metaClass = metaClass;
    }

    @Override
    public ListComponent getOwner() {
        return owner;
    }

    @Override
    public void setOwner(ListComponent owner) {
        this.owner = owner;
    }

    @Override
    public void setValue(V value) {
        if (value != null) {
            if (getValueSource() == null && metaClass == null) {
                throw new IllegalStateException("ValueSource or metaclass must be set for field");
            }

            component.removeStyleName(EMPTY_VALUE_STYLENAME);

            MetaClass fieldMetaClass = getMetaClass();
            if (fieldMetaClass != null) {
                Class fieldClass = fieldMetaClass.getJavaClass();
                Class<?> valueClass = value.getClass();
                //noinspection unchecked
                if (!fieldClass.isAssignableFrom(valueClass)) {
                    throw new IllegalArgumentException(
                            String.format("Could not set value with class %s to field with class %s",
                                    fieldClass.getCanonicalName(),
                                    valueClass.getCanonicalName())
                    );
                }
            }
        } else {
            component.addStyleName("empty-value");
        }

        component.setValue(value);
    }

    @Override
    public String getStyleName() {
        return StringUtils.normalizeSpace(super.getStyleName().replace(EMPTY_VALUE_STYLENAME, ""));
    }

    @Override
    public String getScreen() {
        return screen;
    }

    @Override
    public void setScreen(String screen) {
        this.screen = screen;
    }

    @Override
    public EntityLinkClickHandler getCustomClickHandler() {
        return clickHandler;
    }

    @Override
    public void setCustomClickHandler(EntityLinkClickHandler clickHandler) {
        this.clickHandler = clickHandler;
    }

    @Override
    public OpenType getScreenOpenType() {
        return screenOpenType;
    }

    @Override
    public void setScreenOpenType(OpenType screenOpenType) {
        this.screenOpenType = screenOpenType;
    }

    @Override
    public Map<String, Object> getScreenParams() {
        return screenParams;
    }

    @Override
    public void setScreenParams(Map<String, Object> screenParams) {
        this.screenParams = screenParams;
    }

    @Nullable
    @Override
    public ScreenCloseListener getScreenCloseListener() {
        return screenCloseListener;
    }

    @Override
    public void setScreenCloseListener(@Nullable ScreenCloseListener closeListener) {
        this.screenCloseListener = closeListener;
    }

    protected void openEntityEditor() {
        V value = getValue();

        Entity entity;
        if (value instanceof Entity) {
            entity = (Entity) value;
        } else {
            entity = getDatasource().getItem();
        }

        if (entity == null) {
            return;
        }

        WindowManager wm;
        Window window = ComponentsHelper.getWindow(this);
        if (window == null) {
            throw new IllegalStateException("Please specify Frame for EntityLinkField");
        } else {
            wm = window.getWindowManager();
        }

        if (entity instanceof SoftDelete && ((SoftDelete) entity).isDeleted()) {
            Messages messages = AppBeans.get(Messages.NAME);
            wm.showNotification(
                    messages.getMainMessage("OpenAction.objectIsDeleted"),
                    Frame.NotificationType.HUMANIZED);
            return;
        }

        if (window.getFrameOwner() instanceof LegacyFrame) {
            LegacyFrame frameOwner = (LegacyFrame) window.getFrameOwner();

            DataSupplier dataSupplier = frameOwner.getDsContext().getDataSupplier();
            entity = dataSupplier.reload(entity, View.MINIMAL);
        } else {
            DataManager dataManager = beanLocator.get(DataManager.NAME);
            entity = dataManager.reload(entity, View.MINIMAL);
        }

        String windowAlias = screen;
        WindowConfig windowConfig = AppBeans.get(WindowConfig.NAME);
        if (windowAlias == null) {
            windowAlias = windowConfig.getEditorScreenId(entity.getMetaClass());
        }

        AbstractEditor editor = (AbstractEditor) wm.openEditor(
                windowConfig.getWindowInfo(windowAlias),
                entity,
                screenOpenType,
                screenParams != null ? screenParams : Collections.emptyMap()
        );
        editor.addCloseListener(actionId -> {
            // move focus to component
            component.focus();

            if (Window.COMMIT_ACTION_ID.equals(actionId)) {
                Entity item = editor.getItem();
                afterCommitOpenedEntity(item);
            }

            if (screenCloseListener != null) {
                screenCloseListener.windowClosed(editor, actionId);
            }
        });
    }

    protected void afterCommitOpenedEntity(Entity item) {
        if (getMetaProperty().getRange().isClass()) {
            if (getDatasource() != null) {
                boolean ownerDsModified = false;
                boolean nonModifiedInTable = false;
                if (owner != null && owner.getDatasource() != null) {
                    DatasourceImplementation ownerDs = ((DatasourceImplementation) owner.getDatasource());
                    nonModifiedInTable = !ownerDs.getItemsToUpdate().contains(getDatasource().getItem());

                    ownerDsModified = ownerDs.isModified();
                }

                boolean modified = getDatasource().isModified();
                setValue(null);
                setValue((V) item);
                ((DatasourceImplementation) getDatasource()).setModified(modified);

                // restore modified for owner datasource
                // remove from items to update if it was not modified before setValue
                if (owner != null && owner.getDatasource() != null) {
                    DatasourceImplementation ownerDs = ((DatasourceImplementation) owner.getDatasource());
                    if (nonModifiedInTable) {
                        ownerDs.getItemsToUpdate().remove(getDatasource().getItem());
                    }
                    ownerDs.setModified(ownerDsModified);
                }
            } else {
                setValue(null);
                setValue((V) item);
            }
        } else if (owner != null && owner.getDatasource() != null) {
            //noinspection unchecked
            owner.getDatasource().updateItem(item);

            if (owner instanceof Focusable) {
                // focus owner
                ((Focusable) owner).focus();
            }
        }
    }

    @Override
    public void focus() {
        component.focus();
    }

    @Override
    public int getTabIndex() {
        return component.getTabIndex();
    }

    @Override
    public void setTabIndex(int tabIndex) {
        component.setTabIndex(tabIndex);
    }
}