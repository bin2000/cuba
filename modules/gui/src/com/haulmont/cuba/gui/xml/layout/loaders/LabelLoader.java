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
package com.haulmont.cuba.gui.xml.layout.loaders;

import com.google.common.base.Strings;
import com.haulmont.cuba.gui.GuiDevelopmentException;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.data.value.ContainerValueSource;
import com.haulmont.cuba.gui.model.InstanceContainer;
import com.haulmont.cuba.gui.model.ScreenData;
import com.haulmont.cuba.gui.screen.FrameOwner;
import com.haulmont.cuba.gui.screen.UiControllerUtils;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;

public class LabelLoader extends AbstractDatasourceComponentLoader<Label> {
    @Override
    public void createComponent() {
        resultComponent = factory.create(Label.NAME);
        loadId(resultComponent, element);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void loadComponent() {
        assignXmlDescriptor(resultComponent, element);

        loadContainer(resultComponent, element);
        if (resultComponent.getValueSource() == null) {
            loadDatasource(resultComponent, element);
        }

        loadVisible(resultComponent, element);
        loadAlign(resultComponent, element);
        loadStyleName(resultComponent, element);

        String htmlEnabled = element.attributeValue("htmlEnabled");
        if (StringUtils.isNotEmpty(htmlEnabled)) {
            resultComponent.setHtmlEnabled(Boolean.parseBoolean(htmlEnabled));
        }

        String value = element.attributeValue("value");
        if (StringUtils.isNotEmpty(value)) {
            value = loadResourceString(value);
            resultComponent.setValue(value);
        }
        
        loadDescription(resultComponent, element);
        loadContextHelp(resultComponent, element);

        loadIcon(resultComponent, element);

        loadWidth(resultComponent, element);
        loadHeight(resultComponent, element);

        loadResponsive(resultComponent, element);
        loadCss(resultComponent, element);

        resultComponent.setFormatter(loadFormatter(element));
    }

    // CAUTION copied from AbstractFieldLoader
    @SuppressWarnings("unchecked")
    protected void loadContainer(Label component, Element element) {
        String containerId = element.attributeValue("dataContainer");
        String property = element.attributeValue("property");

        // In case a component has only a property,
        // we try to obtain `dataContainer` from a parent element.
        // For instance, a component is placed within the Form component
        if (Strings.isNullOrEmpty(containerId) && property != null) {
            containerId = getParentDataContainer(element);
        }

        if (!Strings.isNullOrEmpty(containerId)) {
            if (property == null) {
                throw new GuiDevelopmentException(
                        String.format("Can't set container '%s' for component '%s' because 'property' " +
                                "attribute is not defined", containerId, component.getId()), context.getFullFrameId());
            }

            FrameOwner frameOwner = context.getFrame().getFrameOwner();
            ScreenData screenData = UiControllerUtils.getScreenData(frameOwner);
            InstanceContainer container = screenData.getContainer(containerId);

            component.setValueSource(new ContainerValueSource<>(container, property));
        }
    }

    // CAUTION copied from AbstractFieldLoader
    private String getParentDataContainer(Element element) {
        Element parent = element.getParent();
        while (parent != null) {
            if (layoutLoaderConfig.getLoader(parent.getName()) != null) {
                return parent.attributeValue("dataContainer");
            }
            parent = parent.getParent();
        }
        return null;
    }
}