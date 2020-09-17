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

import java.io.Serializable;
import java.util.function.Function;
import java.util.function.Supplier;

import com.ocs.dynamo.dao.FetchJoinInformation;
import com.ocs.dynamo.domain.AbstractEntity;
import com.ocs.dynamo.domain.model.EntityModel;
import com.ocs.dynamo.service.BaseService;
import com.ocs.dynamo.ui.CanAssignEntity;
import com.vaadin.flow.data.provider.SortOrder;
import com.vaadin.flow.function.SerializablePredicate;

/**
 * A tabular edit layout that keeps a reference to a parent object of the
 * collection being edited
 *
 * @author bas.rutten
 * @param <ID> the type of the ID of the entity
 * @param <T> the type of the entity
 * @param <ID2> the type of the ID of the parent entity
 * @param <Q> the type of the parent entity
 */
public class EditableGridDetailLayout<ID extends Serializable, T extends AbstractEntity<ID>, ID2 extends Serializable, Q extends AbstractEntity<ID2>>
        extends EditableGridLayout<ID, T> implements CanAssignEntity<ID2, Q> {

    private static final long serialVersionUID = -3432301286152665223L;

    private final BaseService<ID2, Q> parentService;

    private Q parentEntity;

    /**
     * The joins to use when refreshing the parent entity
     */
    private FetchJoinInformation[] parentJoins;

    private Function<Q, SerializablePredicate<T>> parentFilterSupplier;

    /**
     * Constructor
     *
     * @param service       the service for retrieving the child entities
     * @param parentEntity  the parent entity
     * @param parentService the service for refreshing the parent entity
     * @param entityModel   the entity model
     * @param formOptions   the form options
     * @param sortOrder     the sort orders to apply
     * @param joins         the relations to fetch
     */
    public EditableGridDetailLayout(BaseService<ID, T> service, Q parentEntity, BaseService<ID2, Q> parentService,
            EntityModel<T> entityModel, FormOptions formOptions, SortOrder<?> sortOrder, FetchJoinInformation... joins) {
        super(service, entityModel, formOptions, sortOrder, joins);
        this.parentService = parentService;
        this.parentEntity = parentEntity;
    }

    @Override
    public void assignEntity(Q parentEntity) {
        setParentEntity(getParentService().fetchById(parentEntity.getId(), getParentJoins()));
    }

    @Override
    protected void buildFilter() {
        this.filter = parentFilterSupplier == null ? null : parentFilterSupplier.apply(getParentEntity());
    }

    public Q getParentEntity() {
        return parentEntity;
    }

    public Function<Q, SerializablePredicate<T>> getParentFilterSupplier() {
        return parentFilterSupplier;
    }

    public FetchJoinInformation[] getParentJoins() {
        return parentJoins;
    }

    public BaseService<ID2, Q> getParentService() {
        return parentService;
    }

    @Override
    public void setFilterSupplier(Supplier<SerializablePredicate<T>> filterSupplier) {
        throw new UnsupportedOperationException("Use the setParentFilterSupplier method instead");
    }

    public void setParentEntity(Q parentEntity) {
        this.parentEntity = parentEntity;
    }

    public void setParentFilterSupplier(Function<Q, SerializablePredicate<T>> parentFilterSupplier) {
        this.parentFilterSupplier = parentFilterSupplier;
    }

    public void setParentJoins(FetchJoinInformation[] parentJoins) {
        this.parentJoins = parentJoins;
    }

}
