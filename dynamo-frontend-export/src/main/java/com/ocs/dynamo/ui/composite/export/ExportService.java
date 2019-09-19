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
package com.ocs.dynamo.ui.composite.export;

import java.io.Serializable;
import java.util.List;

import com.ocs.dynamo.dao.FetchJoinInformation;
import com.ocs.dynamo.domain.AbstractEntity;
import com.ocs.dynamo.domain.model.EntityModel;
import com.ocs.dynamo.ui.composite.type.ExportMode;
import com.vaadin.data.provider.SortOrder;
import com.vaadin.server.SerializablePredicate;

/**
 * Service for exporting grid contents to XLSX or CSV
 * 
 * @author Bas Rutten
 *
 */
public interface ExportService {

	/**
	 * Exports to CSV
	 * 
	 * @param entityModel the entity model of the entity to export
	 * @param predicate   the predicate
	 * @param sortOrders  the list of sort orders
	 * @param joins       the joins to use when fetching data
	 * @return
	 */
	public <ID extends Serializable, T extends AbstractEntity<ID>> byte[] exportCsv(EntityModel<T> entityModel,
			ExportMode mode, SerializablePredicate<T> predicate, List<SortOrder<?>> sortOrders,
			FetchJoinInformation... joins);

	/**
	 * Exports to Excel
	 * 
	 * @param entityModel     the entity model of the entity to export
	 * @param predicate       the predicate
	 * @param mode            the desired export mode
	 * @param sortOrders      the list of sort orders
	 * @param customGenerator the custom style generator *
	 * @param joins           the joins to use when fetching data
	 * @return
	 */
	public <ID extends Serializable, T extends AbstractEntity<ID>> byte[] exportExcel(EntityModel<T> entityModel,
			ExportMode mode, SerializablePredicate<T> predicate, List<SortOrder<?>> sortOrders,
			CustomXlsStyleGenerator<ID, T> customGenerator, FetchJoinInformation... joins);

	/**
	 * Exports a fixed set to Excel
	 * 
	 * @param entityModel the entity model
	 * @param mode        the export mode
	 * @param items       the set of items to export
	 * @return
	 */
	public <ID extends Serializable, T extends AbstractEntity<ID>> byte[] exportExcelFixed(EntityModel<T> entityModel,
			ExportMode mode, CustomXlsStyleGenerator<ID, T> customGenerator, List<T> items);

	/**
	 * Exports a fixed set to CSV
	 * 
	 * @param entityModel the entity model
	 * @param mode        the export mode
	 * @param itemsthe    set of items to export
	 * @return
	 */
	public <ID extends Serializable, T extends AbstractEntity<ID>> byte[] exportCsvFixed(EntityModel<T> entityModel,
			ExportMode mode, List<T> items);
}