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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
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
public class EntityTokenSelectTest extends BaseMockitoTest {

	private EntityModelFactory factory = new EntityModelFactoryImpl();

	@Mock
	private TestEntityService service;

	@Test
	public void testAll() {
		EntityTokenSelect<Integer, TestEntity> select = new EntityTokenSelect<>(factory.getModel(TestEntity.class),
				null, service);
		assertEquals(SelectMode.ALL, select.getSelectMode());
		verify(service).findAll((SortOrder[]) null);
	}

	@Test
	public void testFixed() {
		EntityTokenSelect<Integer, TestEntity> select = new EntityTokenSelect<>(factory.getModel(TestEntity.class),
				null, List.of(new TestEntity()));
		assertEquals(SelectMode.FIXED, select.getSelectMode());
		verifyNoInteractions(service);
	}

	@Test
	public void testFilter() {

		EntityTokenSelect<Integer, TestEntity> select = new EntityTokenSelect<>(factory.getModel(TestEntity.class),
				null, service, new EqualsPredicate<TestEntity>("name", "Bob"));
		assertEquals(SelectMode.FILTERED, select.getSelectMode());

		verify(service).find(any(com.ocs.dynamo.filter.Filter.class), isNull());
	}

	@Test
	public void testRefreshFiltered() {

		EntityTokenSelect<Integer, TestEntity> select = new EntityTokenSelect<>(factory.getModel(TestEntity.class),
				null, service, new EqualsPredicate<TestEntity>("name", "Bob"));
		assertEquals(SelectMode.FILTERED, select.getSelectMode());

		select.refresh();

		verify(service).find(any(com.ocs.dynamo.filter.Filter.class), isNull());
	}

	@Test
	public void testRefreshAll() {

		EntityTokenSelect<Integer, TestEntity> select = new EntityTokenSelect<>(factory.getModel(TestEntity.class),
				null, service, new EqualsPredicate<TestEntity>("name", "Bob"));
		assertEquals(SelectMode.FILTERED, select.getSelectMode());

		select.refresh();

		verify(service).find(any(com.ocs.dynamo.filter.Filter.class), isNull());
	}
}
