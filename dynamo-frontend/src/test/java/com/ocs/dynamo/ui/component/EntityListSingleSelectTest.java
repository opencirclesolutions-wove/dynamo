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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.ocs.dynamo.dao.SortOrder;
import com.ocs.dynamo.domain.TestEntity;
import com.ocs.dynamo.domain.model.EntityModelFactory;
import com.ocs.dynamo.domain.model.SelectMode;
import com.ocs.dynamo.domain.model.impl.EntityModelFactoryImpl;
import com.ocs.dynamo.filter.EqualsPredicate;
import com.ocs.dynamo.service.TestEntityService;
import com.ocs.dynamo.test.BaseMockitoTest;

@Disabled
public class EntityListSingleSelectTest extends BaseMockitoTest {

	private EntityModelFactory factory = new EntityModelFactoryImpl();

	@Mock
	private TestEntityService service;

	@Test
	public void testAll() {
		EntityListSingleSelect<Integer, TestEntity> select = new EntityListSingleSelect<>(
				factory.getModel(TestEntity.class), null, service);
		assertEquals(SelectMode.ALL, select.getSelectMode());
		verify(service).findAll((SortOrder[]) null);
	}

	@Test
	public void testFixed() {
		EntityListSingleSelect<Integer, TestEntity> select = new EntityListSingleSelect<>(
				factory.getModel(TestEntity.class), null, List.of(new TestEntity()));
		assertEquals(SelectMode.FIXED, select.getSelectMode());
		verifyNoInteractions(service);
	}

	@Test
	public void testFilter() {

		EntityListSingleSelect<Integer, TestEntity> select = new EntityListSingleSelect<>(
				factory.getModel(TestEntity.class), null, service, new EqualsPredicate<TestEntity>("name", "Bob"));
		assertEquals(SelectMode.FILTERED_PAGED, select.getSelectMode());

		verify(service).fetch(isNull(), eq(0), anyInt());
	}

	@Test
	public void testRefreshFiltered() {

		EntityListSingleSelect<Integer, TestEntity> select = new EntityListSingleSelect<>(
				factory.getModel(TestEntity.class), null, service, new EqualsPredicate<TestEntity>("name", "Bob"));
		assertEquals(SelectMode.FILTERED_PAGED, select.getSelectMode());

		select.refresh();

		verify(service, times(2)).fetch(isNull(), eq(0), anyInt());
	}

	@Test
	public void testRefreshAll() {

		EntityListSingleSelect<Integer, TestEntity> select = new EntityListSingleSelect<>(
				factory.getModel(TestEntity.class), null, service, new EqualsPredicate<TestEntity>("name", "Bob"));
		assertEquals(SelectMode.FILTERED_PAGED, select.getSelectMode());

		select.refresh();
		verify(service, times(2)).fetch(isNull(), eq(0), anyInt());
	}
}
