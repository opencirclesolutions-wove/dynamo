package com.ocs.dynamo.ui.composite.form;

import java.util.Comparator;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.google.common.collect.Lists;
import com.ocs.dynamo.domain.TestEntity;
import com.ocs.dynamo.domain.model.AttributeModel;
import com.ocs.dynamo.domain.model.EntityModel;
import com.ocs.dynamo.domain.model.EntityModelFactory;
import com.ocs.dynamo.domain.model.impl.EntityModelFactoryImpl;
import com.ocs.dynamo.service.TestEntityService;
import com.ocs.dynamo.test.BaseMockitoTest;
import com.ocs.dynamo.ui.composite.layout.FormOptions;
import com.vaadin.ui.Layout;
import com.vaadin.ui.UI;

public class DetailsEditLayoutTest extends BaseMockitoTest {

	private EntityModelFactory factory = new EntityModelFactoryImpl();

	@Mock
	private UI ui;

	private TestEntity e1;

	private TestEntity e2;

	@Mock
	private TestEntityService service;
	
	private boolean buttonBarPostconstruct = false;

	@Override
	public void setUp() {
		super.setUp();
		e1 = new TestEntity(1, "Kevin", 12L);
		e2 = new TestEntity(2, "Bob", 14L);

		Mockito.when(service.getEntityClass()).thenReturn(TestEntity.class);
	}

	/**
	 * Test a table in editable mode
	 */
	@Test
	public void testEditable() {
		EntityModel<TestEntity> em = factory.getModel(TestEntity.class);

		DetailsEditLayout<Integer, TestEntity> layout = createLayout(em, em.getAttributeModel("testEntities"), false,
				new FormOptions().setShowRemoveButton(true));
		Assert.assertTrue(layout.getAddButton().isVisible());
		Assert.assertTrue(buttonBarPostconstruct);

		layout.setValue(Lists.newArrayList(e1, e2));
		Assert.assertEquals(2, layout.getFormCount().intValue());

		layout.getAddButton().click();
		Assert.assertEquals(3, layout.getFormCount().intValue());
	}

	/**
	 * Test read only with search functionality
	 */
	@Test
	public void testReadOnly() {
		EntityModel<TestEntity> em = factory.getModel(TestEntity.class);

		DetailsEditLayout<Integer, TestEntity> layout = createLayout(em, em.getAttributeModel("testEntities"), true,
				new FormOptions().setDetailsGridSearchMode(true));

		// adding is not possible
		Assert.assertFalse(layout.getAddButton().isVisible());
	}

	private DetailsEditLayout<Integer, TestEntity> createLayout(EntityModel<TestEntity> em, AttributeModel am,
			boolean viewMode, FormOptions fo) {

		DetailsEditLayout<Integer, TestEntity> layout = new DetailsEditLayout<Integer, TestEntity>(service, em, am,
				viewMode, fo, Comparator.comparing(TestEntity::getName)) {

			private static final long serialVersionUID = -4333833542380882076L;

			@Override
			protected void removeEntity(TestEntity toRemove) {
				// not needed
			}

			@Override
			protected TestEntity createEntity() {
				return new TestEntity();
			}

			@Override
			protected void postProcessButtonBar(Layout buttonBar) {
				buttonBarPostconstruct = true;
			}
		};
		layout.initContent();
		return layout;
	}
}