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
import java.util.List;

import com.ocs.dynamo.dao.SortOrders;
import com.ocs.dynamo.domain.AbstractEntity;
import com.ocs.dynamo.domain.model.AttributeModel;
import com.ocs.dynamo.domain.model.EntityModel;
import com.ocs.dynamo.domain.model.SelectMode;
import com.ocs.dynamo.filter.AndPredicate;
import com.ocs.dynamo.service.BaseService;
import com.ocs.dynamo.ui.Refreshable;
import com.ocs.dynamo.ui.utils.SortUtils;
import com.ocs.dynamo.ui.utils.VaadinUtils;
import com.ocs.dynamo.utils.EntityModelUtils;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.provider.SortOrder;
import com.vaadin.flow.function.SerializablePredicate;

/**
 * Combo box for displaying a list of entities.
 */
public class EntityComboBox<ID extends Serializable, T extends AbstractEntity<ID>> extends ComboBox<T>
		implements Refreshable, Cascadable<T> {

	private static final long serialVersionUID = 3041574615271340579L;

	/**
	 * Additional filter for cascading
	 */
	private SerializablePredicate<T> additionalFilter;

	private final AttributeModel attributeModel;

	private final EntityModel<T> entityModel;

	/**
	 * The current search filter (may include an additional filter)
	 */
	private SerializablePredicate<T> filter;

	/**
	 * The original search filter (without additional filter for cascading)
	 */
	private SerializablePredicate<T> originalFilter;

	private SelectMode selectMode = SelectMode.FILTERED;

	private final BaseService<ID, T> service;

	private final SortOrder<?>[] sortOrders;

	/**
	 * Constructor
	 *
	 * @param targetEntityModel the entity model for the entities that are displayed
	 *                          in the combo box
	 * @param attributeModel    the attribute model for the attribute to which the
	 *                          selected value will be assigned
	 * @param service           the service used to retrieve entities
	 * @param mode              the select mode
	 * @param filter            optional filters to apply to the search
	 * @param sharedProvider    shared data provider when using component inside
	 *                          grid
	 * @param items             the items to display (in fixed mode)
	 * @param sortOrder         the sort order(s) to apply
	 * 
	 */
	public EntityComboBox(EntityModel<T> targetEntityModel, AttributeModel attributeModel, BaseService<ID, T> service,
			SelectMode mode, SerializablePredicate<T> filter, List<T> items,
			DataProvider<T, SerializablePredicate<T>> sharedProvider, SortOrder<?>... sortOrders) {
		this.service = service;
		this.selectMode = mode;
		this.sortOrders = sortOrders;
		this.attributeModel = attributeModel;
		this.filter = filter;
		this.entityModel = targetEntityModel;
		if (attributeModel != null) {
			this.setLabel(attributeModel.getDisplayName(VaadinUtils.getLocale()));
		}

		DataProvider<T, SerializablePredicate<T>> provider = sharedProvider;
		initProvider(provider, items, mode);

		setItemLabelGenerator(t -> EntityModelUtils.getDisplayPropertyValue(t, targetEntityModel));
		setSizeFull();
	}

	/**
	 * Constructor (for a filtering combo box)
	 *
	 * @param targetEntityModel the entity model for the entities that are displayed
	 *                          in the box
	 * @param attributeModel    the attribute model
	 * @param service           the service for querying the database
	 * @param filter            the filter to apply when searching
	 * @param sortOrder         the sort orders to apply when searching
	 */
	@SafeVarargs
	public EntityComboBox(EntityModel<T> targetEntityModel, AttributeModel attributeModel, BaseService<ID, T> service,
			SerializablePredicate<T> filter, SortOrder<?>... sortOrders) {
		this(targetEntityModel, attributeModel, service, SelectMode.FILTERED, filter, null, null, sortOrders);
	}

	/**
	 * Constructor - for the "ALL"mode
	 *
	 * @param targetEntityModel the entity model for the entities that are displayed
	 * @param attributeModel    the attribute model of the property that is bound to
	 *                          this component
	 * @param service           the service used to retrieve the entities
	 * @param sharedProvider    the shared data provider
	 */
	@SafeVarargs
	public EntityComboBox(EntityModel<T> targetEntityModel, AttributeModel attributeModel, BaseService<ID, T> service,
			SortOrder<?>... sortOrders) {
		this(targetEntityModel, attributeModel, service, SelectMode.ALL, null, null, null, sortOrders);
	}

	/**
	 * Constructor - for the "FIXED" mode
	 *
	 * @param targetEntityModel the entity model for the entities that are displayed
	 * @param attributeModel    the attribute model of the property that is bound to
	 *                          this component
	 * @param items             the list of entities to display
	 */
	public EntityComboBox(EntityModel<T> targetEntityModel, AttributeModel attributeModel, List<T> items,
			DataProvider<T, SerializablePredicate<T>> sharedProvider) {
		this(targetEntityModel, attributeModel, null, SelectMode.FIXED, null, items, sharedProvider);
	}

	@SuppressWarnings("unchecked")
	public void afterNewEntityAdded(T entity) {
		if (getDataProvider() instanceof ListDataProvider) {
			ListDataProvider<T> provider = (ListDataProvider<T>) getDataProvider();
			provider.getItems().add(entity);
		} else {
			updateProvider((DataProvider<T, SerializablePredicate<T>>) getDataProvider());
		}
		setValue(entity);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void castAndSetDataProvider(DataProvider<T, SerializablePredicate<T>> provider) {
		if (provider instanceof CallbackDataProvider) {
			setDataProvider((CallbackDataProvider) provider);
		} else if (provider instanceof ListDataProvider) {
			setDataProvider(new IgnoreDiacriticsCaptionFilter<>(entityModel, true, false), (ListDataProvider) provider);
		}
	}

	@Override
	public void clearAdditionalFilter() {
		this.additionalFilter = null;
		this.filter = originalFilter;
		refresh();
	}

	private CallbackDataProvider<T, String> createCallbackProvider() {
		return CallbackProviderHelper.createCallbackProvider(service, entityModel, filter,
				new SortOrders(SortUtils.translateSortOrders(sortOrders)));
	}

	@Override
	public SerializablePredicate<T> getAdditionalFilter() {
		return additionalFilter;
	}

	public AttributeModel getAttributeModel() {
		return attributeModel;
	}

	public SerializablePredicate<T> getFilter() {
		return filter;
	}

	public SelectMode getSelectMode() {
		return selectMode;
	}

	public SortOrder<?>[] getSortOrder() {
		return sortOrders;
	}

	/**
	 * Initializes the data provider
	 * 
	 * @param provider already existing provider (in case of shared provider)
	 * @param items    fixed list of items to display
	 * @param mode     the desired mode
	 * @return
	 */
	private void initProvider(DataProvider<T, SerializablePredicate<T>> provider, List<T> items, SelectMode mode) {
		if (provider == null) {
			if (SelectMode.ALL.equals(mode)) {
				ListDataProvider<T> listProvider = new ListDataProvider<>(
						service.findAll(SortUtils.translateSortOrders(sortOrders)));
				setDataProvider(new IgnoreDiacriticsCaptionFilter<>(entityModel, true, false), listProvider);
			} else if (SelectMode.FILTERED.equals(mode)) {
				CallbackDataProvider<T, String> callbackProvider = createCallbackProvider();
				setDataProvider(callbackProvider);
			} else if (SelectMode.FIXED.equals(mode)) {
				setDataProvider(new IgnoreDiacriticsCaptionFilter<>(entityModel, true, false),
						new ListDataProvider<>(items));
			}
		} else {
			castAndSetDataProvider(provider);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void refresh() {
		T stored = this.getValue();
		clear();
		DataProvider<T, ?> provider = getDataProvider();
		updateProvider((DataProvider<T, SerializablePredicate<T>>) provider);
		setValue(stored);
	}

	/**
	 * Sets the provided filter then refreshes the component
	 * 
	 * @param filter
	 */
	public void refresh(SerializablePredicate<T> filter) {
		this.originalFilter = filter;
		this.filter = filter;
		refresh();
	}

	@Override
	public void setAdditionalFilter(SerializablePredicate<T> additionalFilter) {
		setValue(null);
		this.additionalFilter = additionalFilter;
		this.filter = originalFilter == null ? additionalFilter : new AndPredicate<>(originalFilter, additionalFilter);
		refresh();
	}

	/**
	 * Updates the data provider after a refresh
	 * 
	 * @param provider the provider to update
	 */
	private void updateProvider(DataProvider<T, SerializablePredicate<T>> provider) {
		if (SelectMode.ALL.equals(selectMode)) {
			ListDataProvider<T> listProvider = (ListDataProvider<T>) provider;
			// add all items (but sorted)
			listProvider.getItems().clear();
			listProvider.getItems().addAll(service.findAll(SortUtils.translateSortOrders(sortOrders)));
		} else if (SelectMode.FILTERED.equals(selectMode)) {
			// add a filtered selection of items
			setDataProvider(createCallbackProvider());
		}
	}

}
