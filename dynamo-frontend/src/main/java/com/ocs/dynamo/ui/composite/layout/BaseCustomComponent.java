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
package com.ocs.dynamo.ui.composite.layout;

import javax.persistence.OptimisticLockException;

import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.ocs.dynamo.domain.model.AttributeModel;
import com.ocs.dynamo.domain.model.EntityModelFactory;
import com.ocs.dynamo.exception.OCSRuntimeException;
import com.ocs.dynamo.exception.OCSValidationException;
import com.ocs.dynamo.service.MessageService;
import com.ocs.dynamo.service.ServiceLocatorFactory;
import com.ocs.dynamo.ui.Buildable;
import com.ocs.dynamo.ui.UIHelper;
import com.ocs.dynamo.ui.component.DefaultVerticalLayout;
import com.ocs.dynamo.ui.utils.VaadinUtils;
import com.ocs.dynamo.util.SystemPropertyUtils;
import com.ocs.dynamo.utils.ClassUtils;
import com.ocs.dynamo.utils.FormatUtils;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Base class for custom components - contains convenience methods for getting
 * various often-used services
 * 
 * @author bas.rutten
 */
@Slf4j
public abstract class BaseCustomComponent extends DefaultVerticalLayout implements Buildable {

	private static final long serialVersionUID = -8982555842423738005L;

	private UIHelper helper = ServiceLocatorFactory.getServiceLocator().getService(UIHelper.class);

	@Getter
	private MessageService messageService = ServiceLocatorFactory.getServiceLocator().getMessageService();

	/**
	 * Constructs a (formatted) label based on the attribute model
	 *
	 * @param entity         the entity that is being displayed
	 * @param attributeModel the attribute model
	 * @return the span containing the label value
	 */
	protected Span constructLabel(Object entity, AttributeModel attributeModel) {
		Object value = ClassUtils.getFieldValue(entity, attributeModel.getName());
		String formatted = FormatUtils.formatPropertyValue(getEntityModelFactory(), getMessageService(), attributeModel,
				value, ", ", VaadinUtils.getLocale(), VaadinUtils.getTimeZoneId());
		return new Span(formatted == null ? "" : formatted);
	}

	protected EntityModelFactory getEntityModelFactory() {
		return ServiceLocatorFactory.getServiceLocator().getEntityModelFactory();
	}

	protected <T> T getService(Class<T> clazz) {
		return ServiceLocatorFactory.getServiceLocator().getService(clazz);
	}

	/**
	 * Generic handling of error messages after a save operation
	 *
	 * @param ex the exception that occurred
	 */
	public void handleSaveException(RuntimeException ex) {
		if (ex instanceof OCSValidationException) {
			// validation exception
			log.warn(ex.getMessage(), ex);
			showErrorNotification(((OCSValidationException) ex).getErrors().get(0));
		} else if (ex instanceof OCSRuntimeException) {
			// any other OCS runtime exception
			log.error(ex.getMessage(), ex);
			showErrorNotification(ex.getMessage());
		} else if (ex instanceof OptimisticLockException || ex instanceof ObjectOptimisticLockingFailureException) {
			// optimistic lock
			log.error(ex.getMessage(), ex);
			showErrorNotification(message("ocs.optimistic.lock"));
		} else {
			// any other save exception
			log.error(ex.getMessage(), ex);
			showErrorNotification(message("ocs.error.occurred"));
		}
	}

	/**
	 * Retrieves a message from the message bundle
	 *
	 * @param key the key of the message
	 * @return the retrieved message
	 */
	protected String message(String key) {
		return getMessageService().getMessage(key, VaadinUtils.getLocale());
	}

	/**
	 * Retrieves a message from the message bundle
	 *
	 * @param key  the key of the message
	 * @param args any arguments that are used in the message
	 * @return the retrieved message
	 */
	protected String message(String key, Object... args) {
		return getMessageService().getMessage(key, VaadinUtils.getLocale(), args);
	}

	/**
	 * Navigates to the specified view
	 *
	 * @param view the ID of the view
	 */
	protected void navigate(String viewName) {
		helper.navigate(viewName);
	}

	/**
	 * Navigates to the specified view with the specified mode
	 * 
	 * @param viewName the name of the view
	 * @param mode     the desired mode
	 */
	protected void navigate(String viewName, String mode) {
		helper.navigate(viewName, mode);
	}

	/**
	 * Shows an error message
	 * 
	 * @param message the message to show
	 */
	protected void showErrorNotification(String message) {
		showNotifification(message, Position.MIDDLE, NotificationVariant.LUMO_ERROR);
	}

	/**
	 * Shows a notification message - this method will check for the availability of
	 * a Vaadin Page object and if this is not present, write the notification to
	 * the log instead
	 *
	 * @param message the message
	 * @param type    the type of the message (error, warning, tray etc.)
	 */
	protected void showNotifification(String message, Position position, NotificationVariant variant) {
		if (UI.getCurrent() != null && UI.getCurrent().getPage() != null) {
			Notification.show(message, SystemPropertyUtils.getDefaultMessageDisplayTime(), position)
					.addThemeVariants(variant);
		} else {
			log.info(message);
		}
	}

	/**
	 * Shows a success message
	 * 
	 * @param message the message to show
	 */
	protected void showSuccessNotification(String message) {
		showNotifification(message, Position.MIDDLE, NotificationVariant.LUMO_SUCCESS);
	}

	/**
	 * Shows a tray message
	 * 
	 * @param message the message to show
	 */
	protected void showTrayNotification(String message) {
		showNotifification(message, Position.BOTTOM_END, NotificationVariant.LUMO_SUCCESS);
	}

}
