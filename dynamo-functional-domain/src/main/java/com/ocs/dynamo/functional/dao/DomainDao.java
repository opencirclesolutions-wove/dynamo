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
package com.ocs.dynamo.functional.dao;

import java.util.List;

import com.ocs.dynamo.dao.BaseDao;
import com.ocs.dynamo.functional.domain.Domain;
import com.ocs.dynamo.functional.domain.DomainChild;
import com.ocs.dynamo.functional.domain.DomainParent;

/**
 * Data access interface for managing domain entities.
 * 
 * 
 * @author Patrick Deenen (patrick@opencircle.solutions)
 *
 */
public interface DomainDao extends BaseDao<Integer, Domain> {

    /**
     * Returns the children for a certain parent
     * 
     * @param parent the parent entity
     * @return the children for the given parent
     */
    <C extends DomainChild<C, P>, P extends DomainParent<C, P>> List<C> findChildren(P parent);

    /**
     *
     * Returns a list of all domain entities of the requested type
     * @param type the type
     * @return the list of matching entities
     */
    <D extends Domain> List<D> findAllByType(Class<D> type);

    /**
     * Finds an entity based on a unique property value
     * 
     * @param propertyName  the name of the property
     * @param value         the desired value of the property
     * @param caseSensitive whether the match is case-sensitive
     * @return the resulting entity
     */
    <D extends Domain> D findByTypeAndUniqueProperty(Class<D> type, String propertyName, Object value, boolean caseSensitive);

}
