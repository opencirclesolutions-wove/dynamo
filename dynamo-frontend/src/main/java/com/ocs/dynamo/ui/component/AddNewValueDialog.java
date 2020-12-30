/*
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.ocs.dynamo.ui.component;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;

import com.ocs.dynamo.domain.AbstractEntity;
import com.ocs.dynamo.domain.model.AttributeModel;
import com.ocs.dynamo.domain.model.EntityModel;
import com.ocs.dynamo.exception.OCSNonUniqueException;
import com.ocs.dynamo.service.BaseService;
import com.ocs.dynamo.service.MessageService;
import com.ocs.dynamo.ui.composite.dialog.SimpleModalDialog;
import com.ocs.dynamo.ui.utils.VaadinUtils;
import com.ocs.dynamo.utils.ClassUtils;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

/**
 * A pop-up dialog for adding a new value to a domain form a QuickAddEntity
 * components
 * 
 * @author bas.rutten
 *
 */
public abstract class AddNewValueDialog<ID extends Serializable, T extends AbstractEntity<ID>> extends SimpleModalDialog {

    private static final long serialVersionUID = 6208738706327329145L;

    private final MessageService messageService;

    private TextField valueField;

    private BaseService<ID, T> service;

    private EntityModel<T> entityModel;

    private AttributeModel attributeModel;

    /**
     * Constructor
     * 
     * @param entityModel
     * @param attributeModel
     * @param service
     * @param messageService
     */
    public AddNewValueDialog(EntityModel<T> entityModel, AttributeModel attributeModel, BaseService<ID, T> service,
            MessageService messageService) {
        super(true);
        this.entityModel = entityModel;
        this.attributeModel = attributeModel;
        this.service = service;
        this.messageService = messageService;
    }

    /**
     * Callback method that fires after the user has created a new entity
     * 
     * @param entity the newly added entity
     */
    protected abstract void afterNewEntityAdded(T entity);

    @Override
    protected void doBuild(VerticalLayout parent) {
        // add a text field that hold the new value

        VerticalLayout container = new DefaultVerticalLayout(true, true);
        parent.add(container);

        valueField = new TextField(messageService.getMessage("ocs.enter.new.value", VaadinUtils.getLocale()));
        valueField.setSizeFull();
        valueField.focus();
        container.add(valueField);
    }

    @Override
    protected boolean doClose() {
        String value = valueField.getValue();
        if (!StringUtils.isEmpty(value)) {
            T t = service.createNewEntity();

            // disallow values that are too long
            String propName = attributeModel.getQuickAddPropertyName();
            Integer maxLength = entityModel.getAttributeModel(propName).getMaxLength();

            if (maxLength != null && value.length() > maxLength) {
                showNotification(messageService.getMessage("ocs.value.too.long", VaadinUtils.getLocale()));
                return false;
            }
            ClassUtils.setFieldValue(t, propName, value);

            try {
                t = service.save(t);
                afterNewEntityAdded(t);
                return true;
            } catch (OCSNonUniqueException ex) {
                // not unique - produce warning
                showNotification(ex.getMessage());
            }
        } else {
            showNotification(messageService.getMessage("ocs.value.required", VaadinUtils.getLocale()));
        }
        return false;
    }

    public TextField getValueField() {
        return valueField;
    }

    @Override
    protected String getTitle() {
        return messageService.getMessage("ocs.enter.new.value", VaadinUtils.getLocale());
    }
}
