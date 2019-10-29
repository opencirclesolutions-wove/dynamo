package com.ocs.dynamo.ui.component;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.google.common.collect.Lists;
import com.ocs.dynamo.domain.TestEntity;
import com.ocs.dynamo.domain.comparator.AttributeComparator;
import com.ocs.dynamo.domain.model.AttributeModel;
import com.ocs.dynamo.domain.model.EntityModel;
import com.ocs.dynamo.domain.model.EntityModelFactory;
import com.ocs.dynamo.domain.model.impl.EntityModelFactoryImpl;
import com.ocs.dynamo.service.TestEntityService;
import com.ocs.dynamo.test.BaseMockitoTest;
import com.ocs.dynamo.ui.composite.layout.FormOptions;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.provider.ListDataProvider;

public class DetailsEditGridTest extends BaseMockitoTest {

	private EntityModelFactory factory = new EntityModelFactoryImpl();

	@Mock
	private UI ui;

	private TestEntity e1;

	private TestEntity e2;

	@Mock
	private TestEntityService service;

	@Before
	public void setUp() {
		e1 = new TestEntity(1, "Kevin", 12L);
		e1.setId(1);
		e2 = new TestEntity(2, "Bob", 14L);
		e2.setId(2);
	}

	/**
	 * Test a grid in editable mode
	 */
	@Test
	public void testEditable() {
		EntityModel<TestEntity> em = factory.getModel(TestEntity.class);

		DetailsEditGrid<Integer, TestEntity> grid = createGrid(em, em.getAttributeModel("testEntities"), false, false,
				new FormOptions().setShowRemoveButton(true));
		Assert.assertTrue(grid.getAddButton().isVisible());
		Assert.assertFalse(grid.getSearchDialogButton().isVisible());

		grid.setValue(Lists.newArrayList(e1, e2));

		Assert.assertEquals(2, grid.getItemCount());

		// test that the add button will add a row
		grid.getAddButton().click();
		Assert.assertEquals(3, grid.getItemCount());

		// explicitly set field value
		grid.setValue(Lists.newArrayList(e1));
		Assert.assertEquals(1, grid.getItemCount());
	}

	/**
	 * Test read only with search functionality
	 */
	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void testReadOnlyWithSearch() {
		EntityModel<TestEntity> em = factory.getModel(TestEntity.class);

		DetailsEditGrid<Integer, TestEntity> grid = createGrid(em, null, false, true,
				new FormOptions().setDetailsGridSearchMode(true));
		grid.setService(service);

		ListDataProvider<TestEntity> lep = (ListDataProvider<TestEntity>) grid.getGrid().getDataProvider();
		Assert.assertEquals(0, lep.getItems().size());

		// adding is not possible
		Assert.assertFalse(grid.getAddButton().isVisible());
		// but bringing up the search dialog is
		Assert.assertTrue(grid.getSearchDialogButton().isVisible());

//		grid.getSearchDialogButton().click();
//		ArgumentCaptor<ModelBasedSearchDialog> captor = ArgumentCaptor.forClass(ModelBasedSearchDialog.class);
//		Mockito.verify(ui).add(captor.capture());
//
//		ModelBasedSearchDialog dialog = captor.getValue();
//
//		// select item and close dialog
//		dialog.select(e1);
//		dialog.getOkButton().click();

	}

	@Test
	public void testReadOnly() {
		EntityModel<TestEntity> em = factory.getModel(TestEntity.class);

		DetailsEditGrid<Integer, TestEntity> grid = createGrid(em, em.getAttributeModel("testEntities"), true, false,
				new FormOptions());
		Assert.assertFalse(grid.getAddButton().isVisible());
		Assert.assertFalse(grid.getSearchDialogButton().isVisible());

		grid.setValue(Lists.newArrayList(e1, e2));
		Assert.assertEquals(2, grid.getItemCount());
	}

	private DetailsEditGrid<Integer, TestEntity> createGrid(EntityModel<TestEntity> em, AttributeModel am,
			boolean viewMode, boolean tableReadOnly, FormOptions fo) {

		if (tableReadOnly) {
			fo.setReadOnly(true);
		}

		DetailsEditGrid<Integer, TestEntity> grid = new DetailsEditGrid<Integer, TestEntity>(em, am, viewMode, fo);

		grid.setCreateEntitySupplier(() -> new TestEntity());
		//MockUtil.injectUI(grid, ui);
		grid.initContent();
		grid.setComparator(new AttributeComparator<>("name"));
		return grid;
	}
}
