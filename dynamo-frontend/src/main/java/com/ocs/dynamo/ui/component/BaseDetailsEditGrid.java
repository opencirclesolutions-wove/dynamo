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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.google.common.collect.Lists;
import com.ocs.dynamo.dao.FetchJoinInformation;
import com.ocs.dynamo.domain.AbstractEntity;
import com.ocs.dynamo.domain.model.AttributeModel;
import com.ocs.dynamo.domain.model.EntityModel;
import com.ocs.dynamo.exception.OCSRuntimeException;
import com.ocs.dynamo.service.BaseService;
import com.ocs.dynamo.service.MessageService;
import com.ocs.dynamo.service.ServiceLocatorFactory;
import com.ocs.dynamo.ui.NestedComponent;
import com.ocs.dynamo.ui.UseInViewMode;
import com.ocs.dynamo.ui.composite.dialog.ModelBasedSearchDialog;
import com.ocs.dynamo.ui.composite.grid.ModelBasedGrid;
import com.ocs.dynamo.ui.composite.layout.FormOptions;
import com.ocs.dynamo.ui.composite.type.GridEditMode;
import com.ocs.dynamo.ui.utils.VaadinUtils;
import com.ocs.dynamo.util.SystemPropertyUtils;
import com.vaadin.data.BeanValidationBinder;
import com.vaadin.data.Binder;
import com.vaadin.data.Binder.BindingBuilder;
import com.vaadin.data.BinderValidationStatus;
import com.vaadin.data.Converter;
import com.vaadin.data.HasValue;
import com.vaadin.data.ValueProvider;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.SortOrder;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.Layout;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

/**
 * Base class for grid components that are displayed inside an edit form. These
 * components can be used to manage a one-to-many or many-to-many collection.
 * 
 * For small collections that can be managed in-memory and fetched through JPA
 * relationship fetching, use the DetailsEditGrid subclass. For larger
 * collections that should not be managed in-memory, use the
 * ServiceBasedDetailsEditGrid instead
 * 
 * @author Bas Rutten
 *
 * @param <U> the type of the bound property
 * @param <ID> the type of the ID of the entities that are being displayed
 * @param <T> the type of the entities that are being displayed
 */
public abstract class BaseDetailsEditGrid<U, ID extends Serializable, T extends AbstractEntity<ID>>
		extends CustomField<U> implements NestedComponent, UseInViewMode {

	private static final long serialVersionUID = 997617632007985450L;

	/**
	 * The number of rows to display - this default to 3 but can be overwritten
	 */
	private int pageLength = SystemPropertyUtils.getDefaultListSelectRows();

	/**
	 * Optional field filters for restricting the contents of combo boxes
	 */
	private Map<String, SerializablePredicate<?>> fieldFilters = new HashMap<>();

	/**
	 * The button that can be used to add rows to the grid
	 */
	private Button addButton;

	/**
	 * The entity model of the entity to display
	 */
	private final EntityModel<T> entityModel;

	/**
	 * The service that is used to communicate with the database
	 */
	private BaseService<ID, T> service;

	/**
	 * The message service
	 */
	private final MessageService messageService;

	/**
	 * Whether the component is in view mode. If this is the case, editing is not
	 * allowed and no buttons will be displayed
	 */
	private boolean viewMode;

	/**
	 * Form options that determine which buttons and functionalities are available
	 */
	private FormOptions formOptions;

	/**
	 * The grid for displaying the actual items
	 */
	private ModelBasedGrid<ID, T> grid;

	/**
	 * List of buttons to update after a detail is selected
	 */
	private List<Button> componentsToUpdate = new ArrayList<>();

	/**
	 * The currently selected item
	 */
	private T selectedItem;

	/**
	 * Map with a binder for every row
	 */
	private Map<T, Binder<T>> binders = new HashMap<>();

	/**
	 * Button used to open the search dialog
	 */
	private Button searchDialogButton;

	/**
	 * Overridden entity model for the search dialog
	 */
	private EntityModel<T> searchDialogEntityModel;

	/**
	 * Filters to apply to the search dialog
	 */
	private List<SerializablePredicate<T>> searchDialogFilters;

	/**
	 * Sort order to apply to the search dialog
	 */
	private SortOrder<T> searchDialogSortOrder;

	/**
	 * The UI
	 */
	private UI ui = UI.getCurrent();

	/**
	 * Search dialog
	 */
	private ModelBasedSearchDialog<ID, T> dialog;

	/**
	 * Consumer that is used to remove an entity
	 */
	private Consumer<T> removeEntityConsumer;

	/**
	 * The supplier that is used for creating a new entity in response to a click on
	 * the Add button
	 */
	private Supplier<T> createEntitySupplier;

	/**
	 * The attribute model of the attribute that is managed by this component
	 */
	private AttributeModel attributeModel;

	/**
	 * Indicates whether the component is used in service-based mode, in this case
	 * the values in the column cannot be edited directly
	 */
	private boolean serviceBasedEditMode;

	/**
	 * Constructor
	 * 
	 * @param service
	 * @param entityModel
	 * @param attributeModel
	 * @param viewMode
	 * @param formOptions
	 * @param joins
	 */
	public BaseDetailsEditGrid(BaseService<ID, T> service, EntityModel<T> entityModel, AttributeModel attributeModel,
			boolean viewMode, boolean serviceBasedEditMode, FormOptions formOptions, FetchJoinInformation... joins) {
		this.service = service;
		this.entityModel = entityModel;
		this.messageService = ServiceLocatorFactory.getServiceLocator().getMessageService();
		this.viewMode = viewMode;
		this.formOptions = formOptions;
		this.attributeModel = attributeModel;
		this.serviceBasedEditMode = serviceBasedEditMode;
	}

	/**
	 * Applies a filter to restrict the values to be displayed
	 */
	protected abstract void applyFilter();

	/**
	 * Checks which buttons in the button bar must be enabled after an item has been
	 * selected
	 *
	 * @param selectedItem the selected item
	 */
	protected void checkButtonState(T selectedItem) {
		for (Button b : componentsToUpdate) {
			b.setEnabled(selectedItem != null && mustEnableButton(b, selectedItem));
		}
	}

	/**
	 * Constructs the button that is used for adding new items
	 *
	 * @param buttonBar the button bar
	 */
	protected void constructAddButton(Layout buttonBar) {
		addButton = new Button(messageService.getMessage("ocs.add", VaadinUtils.getLocale()));
		addButton.setIcon(VaadinIcons.PLUS);
		addButton.addClickListener(event -> doAdd());
		addButton.setVisible((isGridEditEnabled()
				|| (!isViewMode() && serviceBasedEditMode && !formOptions.isDetailsGridSearchMode()))
				&& !formOptions.isHideAddButton());
		buttonBar.addComponent(addButton);
	}

	/**
	 * Constructs the button bar
	 *
	 * @param parent the layout to which to add the button bar
	 */
	protected void constructButtonBar(Layout parent) {
		Layout buttonBar = new DefaultHorizontalLayout();
		parent.addComponent(buttonBar);
		constructAddButton(buttonBar);
		constructSearchButton(buttonBar);
		postProcessButtonBar(buttonBar);
	}

	/**
	 * Callback method for inserting custom converter
	 * 
	 * @param am the attribute model
	 * @return
	 */
	protected Converter<String, ?> constructCustomConverter(AttributeModel am) {
		return null;
	}

	/**
	 * Method that is called to create a custom field. Override in subclasses if
	 * needed
	 *
	 * @param entityModel    the entity model of the entity that is displayed in the
	 *                       grid
	 * @param attributeModel the attribute model of the attribute for which we are
	 *                       constructing a field
	 * @param viewMode       whether the form is in view mode
	 * @return
	 */
	protected AbstractComponent constructCustomField(EntityModel<T> entityModel, AttributeModel attributeModel,
			boolean viewMode) {
		// overwrite in subclasses
		return null;
	}

	/**
	 * Constructs a button that brings up a search dialog
	 *
	 * @param buttonBar
	 */
	protected void constructSearchButton(Layout buttonBar) {

		searchDialogButton = new Button(messageService.getMessage("ocs.search", VaadinUtils.getLocale()));
		searchDialogButton.setIcon(VaadinIcons.SEARCH);
		searchDialogButton.setDescription(messageService.getMessage("ocs.search.description", VaadinUtils.getLocale()));
		searchDialogButton.addClickListener(event -> {

			// service must be specified
			if (service == null) {
				throw new OCSRuntimeException(
						messageService.getMessage("ocs.no.service.specified", VaadinUtils.getLocale()));
			}

			dialog = new ModelBasedSearchDialog<ID, T>(service,
					searchDialogEntityModel != null ? searchDialogEntityModel : entityModel, searchDialogFilters,
					searchDialogSortOrder == null ? null : Lists.newArrayList(searchDialogSortOrder), true, true) {

				private static final long serialVersionUID = 1512969437992973122L;

				@Override
				protected boolean doClose() {
					// add the selected items to the grid
					Collection<T> selected = getSelectedItems();
					if (selected != null) {
						// afterItemsSelected(selected);
						handleDialogSelection(selected);
						getDataProvider().refreshAll();
					}
					return true;
				}
			};
			dialog.build();
			ui.addWindow(dialog);
		});
		searchDialogButton.setVisible(!viewMode && formOptions.isDetailsGridSearchMode());
		buttonBar.addComponent(searchDialogButton);
	}

	/**
	 * The code to carry out after clicking on the add button
	 */
	protected abstract void doAdd();

	/**
	 * The code to carry out after clicking the edit button
	 */
	protected void doEdit(T t) {
	}

	public Button getAddButton() {
		return addButton;
	}

	public AttributeModel getAttributeModel() {
		return attributeModel;
	}

	public Map<T, Binder<T>> getBinders() {
		return binders;
	}

	public Supplier<T> getCreateEntitySupplier() {
		return createEntitySupplier;
	}

	/**
	 * Returns the data provider
	 */
	protected abstract DataProvider<T, SerializablePredicate<T>> getDataProvider();

	public EntityModel<T> getEntityModel() {
		return entityModel;
	}

	public Map<String, SerializablePredicate<?>> getFieldFilters() {
		return fieldFilters;
	}

	public FormOptions getFormOptions() {
		return formOptions;
	}

	public ModelBasedGrid<ID, T> getGrid() {
		return grid;
	}

	public Button getSearchDialogButton() {
		return searchDialogButton;
	}

	public EntityModel<T> getSearchDialogEntityModel() {
		return searchDialogEntityModel;
	}

	public List<SerializablePredicate<T>> getSearchDialogFilters() {
		return searchDialogFilters;
	}

	public SortOrder<T> getSearchDialogSortOrder() {
		return searchDialogSortOrder;
	}

	public T getSelectedItem() {
		return selectedItem;
	}

	public BaseService<ID, T> getService() {
		return service;
	}

	/**
	 * Method that is called after closing the popup dialog - handles the selected
	 * items in the dialog
	 * 
	 * @param selected the collection of selected items
	 */
	protected abstract void handleDialogSelection(Collection<T> selected);

	/**
	 * Constructs the actual component
	 */
	@Override
	protected Component initContent() {
		grid = new ModelBasedGrid<ID, T>(getDataProvider(), entityModel, getFieldFilters(), isGridEditEnabled(),
				GridEditMode.SIMULTANEOUS) {

			private static final long serialVersionUID = 6143503902550597524L;

			@Override
			protected Converter<String, ?> constructCustomConverter(AttributeModel am) {
				return BaseDetailsEditGrid.this.constructCustomConverter(am);
			}

			@Override
			protected AbstractComponent constructCustomField(EntityModel<T> entityModel, AttributeModel am) {
				return BaseDetailsEditGrid.this.constructCustomField(entityModel, am, false);
			}

			@Override
			protected BindingBuilder<T, ?> doBind(T t, AbstractComponent field) {
				if (!binders.containsKey(t)) {
					binders.put(t, new BeanValidationBinder<>(entityModel.getEntityClass()));
					binders.get(t).setBean(t);
				}
				Binder<T> binder = binders.get(t);
				return binder.forField((HasValue<?>) field);
			}
		};

		// allow editing by showing a pop-up dialog (only for service-based version)
		if (serviceBasedEditMode && !formOptions.isDetailsGridSearchMode() && !isViewMode()) {
			getGrid().addComponentColumn((ValueProvider<T, Component>) t -> {
				Button edit = new Button();
				edit.setIcon(VaadinIcons.PENCIL);
				edit.addClickListener(event -> {
					doEdit(getService().fetchById(t.getId()));
				});
				return edit;
			});
		}

		// add a remove button directly in the grid
		if (!isViewMode() && formOptions.isShowRemoveButton()) {
			getGrid().addComponentColumn((ValueProvider<T, Component>) t -> {
				Button remove = new Button();
				remove.setIcon(VaadinIcons.TRASH);
				remove.addClickListener(event -> {
					binders.remove(t);
					// callback method so the entity can be removed from its
					// parent
					if (removeEntityConsumer != null) {
						removeEntityConsumer.accept(t);
					}
					getDataProvider().refreshAll();
				});
				return remove;
			});
		}

		grid.setHeightByRows(pageLength);
		grid.setSelectionMode(SelectionMode.SINGLE);

		VerticalLayout layout = new DefaultVerticalLayout(false, true);
		layout.addComponent(grid);

		// add a change listener (to make sure the buttons are correctly
		// enabled/disabled)
		grid.addSelectionListener(event -> {
			if (grid.getSelectedItems().iterator().hasNext()) {
				selectedItem = (T) grid.getSelectedItems().iterator().next();
				onSelect(selectedItem);
				checkButtonState(selectedItem);
			}
		});
		grid.getDataProvider().addDataProviderListener(event -> grid.updateCaption());
		grid.updateCaption();

		// apply filter
		applyFilter();

		// add the buttons
		constructButtonBar(layout);

		postConstruct();
		return layout;
	}

	/**
	 * Indicates whether it is possible to add/modify items directly via the grid
	 *
	 * @return
	 */
	private boolean isGridEditEnabled() {
		return !viewMode && !formOptions.isDetailsGridSearchMode() && !formOptions.isReadOnly()
				&& !serviceBasedEditMode;
	}

	/**
	 * 
	 * @return whether the component is in view mode
	 */
	public boolean isViewMode() {
		return viewMode;
	}

	/**
	 * Method that is called in order to enable/disable a button after selecting an
	 * item in the grid
	 *
	 * @param button
	 * @return
	 */
	protected boolean mustEnableButton(Button button, T selectedItem) {
		// overwrite in subclasses if needed
		return true;
	}

	/**
	 * Respond to a selection of an item in the grid
	 */
	protected void onSelect(Object selected) {
		// overwrite when needed
	}

	/**
	 * Perform any necessary post construction
	 */
	protected void postConstruct() {
		// overwrite in subclasses
	}

	/**
	 * Callback method that is used to modify the button bar. Override in subclasses
	 * if needed
	 *
	 * @param buttonBar
	 */
	protected void postProcessButtonBar(Layout buttonBar) {
		// overwrite in subclass if needed
	}

	/**
	 * Registers a button that must be enabled/disabled after an item is selected.
	 * use the "mustEnableButton" callback method to impose additional constraints
	 * on when the button must be enabled
	 *
	 * @param button the button to register
	 */
	public void registerButton(Button button) {
		if (button != null) {
			button.setEnabled(false);
			componentsToUpdate.add(button);
		}
	}

	public void setCreateEntitySupplier(Supplier<T> createEntitySupplier) {
		this.createEntitySupplier = createEntitySupplier;
	}

	public void setFieldFilters(Map<String, SerializablePredicate<?>> fieldFilters) {
		this.fieldFilters = fieldFilters;
	}

	public void setFormOptions(FormOptions formOptions) {
		this.formOptions = formOptions;
	}

	public void setPageLength(int pageLength) {
		this.pageLength = pageLength;
	}

	public void setRemoveEntityConsumer(Consumer<T> removeEntityConsumer) {
		this.removeEntityConsumer = removeEntityConsumer;
	}

	public void setSearchDialogEntityModel(EntityModel<T> searchDialogEntityModel) {
		this.searchDialogEntityModel = searchDialogEntityModel;
	}

	public void setSearchDialogFilters(List<SerializablePredicate<T>> searchDialogFilters) {
		this.searchDialogFilters = searchDialogFilters;
		if (dialog != null) {
			dialog.setFilters(searchDialogFilters);
		}
	}

	public void setSearchDialogSortOrder(SortOrder<T> searchDialogSortOrder) {
		this.searchDialogSortOrder = searchDialogSortOrder;
	}

	public void setSelectedItem(T selectedItem) {
		this.selectedItem = selectedItem;
		checkButtonState(selectedItem);
	}

	public void setService(BaseService<ID, T> service) {
		this.service = service;
	}

	@Override
	public boolean validateAllFields() {
		boolean error = false;
		for (Entry<T, Binder<T>> entry : binders.entrySet()) {
			entry.getValue().setBean(entry.getKey());
			BinderValidationStatus<T> status = entry.getValue().validate();
			error |= !status.isOk();
		}
		return error;
	}

}
