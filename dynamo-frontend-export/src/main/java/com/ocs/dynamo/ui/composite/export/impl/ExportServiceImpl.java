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
package com.ocs.dynamo.ui.composite.export.impl;

import java.io.Serializable;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;

import com.ocs.dynamo.dao.FetchJoinInformation;
import com.ocs.dynamo.domain.AbstractEntity;
import com.ocs.dynamo.domain.model.EntityModel;
import com.ocs.dynamo.filter.Filter;
import com.ocs.dynamo.filter.FilterConverter;
import com.ocs.dynamo.service.BaseService;
import com.ocs.dynamo.service.ServiceLocatorFactory;
import com.ocs.dynamo.ui.composite.export.CustomXlsStyleGenerator;
import com.ocs.dynamo.ui.composite.export.ExportService;
import com.ocs.dynamo.ui.composite.export.PivotParameters;
import com.ocs.dynamo.ui.composite.type.ExportMode;
import com.ocs.dynamo.ui.utils.SortUtils;
import com.ocs.dynamo.ui.utils.VaadinUtils;
import com.vaadin.flow.data.provider.SortOrder;
import com.vaadin.flow.function.SerializablePredicate;

/**
 * Implementation of the export service
 * 
 * @author Bas Rutten
 *
 */
@Service
public class ExportServiceImpl implements ExportService {

	@Override
	@SuppressWarnings("unchecked")
	public <ID extends Serializable, T extends AbstractEntity<ID>> byte[] exportCsv(EntityModel<T> entityModel,
			ExportMode mode, SerializablePredicate<T> predicate, List<SortOrder<?>> sortOrders,
			FetchJoinInformation... joins) {
		BaseService<ID, T> service = (BaseService<ID, T>) ServiceLocatorFactory.getServiceLocator()
				.getServiceForEntity(entityModel.getEntityClass());
		FilterConverter<T> converter = new FilterConverter<>(entityModel);
		Filter filter = converter.convert(predicate);

		ModelBasedCsvExportTemplate<ID, T> template = new ModelBasedCsvExportTemplate<>(service, entityModel, mode,
				SortUtils.translateSortOrders(sortOrders), filter, joins);
		return template.process();
	}

	@Override
	public <ID extends Serializable, T extends AbstractEntity<ID>> byte[] exportCsvFixed(EntityModel<T> entityModel,
			ExportMode mode, List<T> items) {
		ModelBasedCsvExportTemplate<ID, T> template = new ModelBasedCsvExportTemplate<>(null, entityModel, mode, null,
				null);
		return template.processFixed(items);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <ID extends Serializable, T extends AbstractEntity<ID>> byte[] exportExcel(EntityModel<T> entityModel,
			ExportMode mode, SerializablePredicate<T> predicate, List<SortOrder<?>> sortOrders,
			Supplier<CustomXlsStyleGenerator<ID, T>> customGenerator, FetchJoinInformation... joins) {
		BaseService<ID, T> service = (BaseService<ID, T>) ServiceLocatorFactory.getServiceLocator()
				.getServiceForEntity(entityModel.getEntityClass());
		FilterConverter<T> converter = new FilterConverter<>(entityModel);
		Filter filter = converter.convert(predicate);
		ModelBasedExcelExportTemplate<ID, T> template = new ModelBasedExcelExportTemplate<>(service, entityModel, mode,
				SortUtils.translateSortOrders(sortOrders), filter,
				entityModel.getDisplayNamePlural(VaadinUtils.getLocale()), customGenerator, joins);
		return template.process();
	}

	@Override
	public <ID extends Serializable, T extends AbstractEntity<ID>> byte[] exportExcelFixed(EntityModel<T> entityModel,
			ExportMode mode, Supplier<CustomXlsStyleGenerator<ID, T>> customGenerator, List<T> items) {
		ModelBasedExcelExportTemplate<ID, T> template = new ModelBasedExcelExportTemplate<>(null, entityModel, mode,
				null, null, entityModel.getDisplayNamePlural(VaadinUtils.getLocale()), customGenerator);
		return template.processFixed(items);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <ID extends Serializable, T extends AbstractEntity<ID>> byte[] exportExcelPivot(EntityModel<T> entityModel,
			SerializablePredicate<T> predicate, List<SortOrder<?>> sortOrders,
			Supplier<CustomXlsStyleGenerator<ID, T>> customGenerator, PivotParameters pivotParameters,
			FetchJoinInformation... joins) {
		BaseService<ID, T> service = (BaseService<ID, T>) ServiceLocatorFactory.getServiceLocator()
				.getServiceForEntity(entityModel.getEntityClass());
		FilterConverter<T> converter = new FilterConverter<>(entityModel);
		Filter filter = converter.convert(predicate);
		ModelBasedExcelPivotExportTemplate<ID, T> template = new ModelBasedExcelPivotExportTemplate<>(service,
				entityModel, SortUtils.translateSortOrders(sortOrders), filter,
				entityModel.getDisplayNamePlural(VaadinUtils.getLocale()), customGenerator, pivotParameters, joins);
		return template.process();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <ID extends Serializable, T extends AbstractEntity<ID>> byte[] exportCsvPivot(EntityModel<T> entityModel,
			SerializablePredicate<T> predicate, List<SortOrder<?>> sortOrders, PivotParameters pivotParameters,
			FetchJoinInformation... joins) {
		BaseService<ID, T> service = (BaseService<ID, T>) ServiceLocatorFactory.getServiceLocator()
				.getServiceForEntity(entityModel.getEntityClass());
		FilterConverter<T> converter = new FilterConverter<>(entityModel);
		Filter filter = converter.convert(predicate);
		ModelBasedCsvPivotExportTemplate<ID, T> template = new ModelBasedCsvPivotExportTemplate<>(service, entityModel,
				SortUtils.translateSortOrders(sortOrders), filter,
				entityModel.getDisplayNamePlural(VaadinUtils.getLocale()), pivotParameters, joins);
		return template.process();
	}

}
