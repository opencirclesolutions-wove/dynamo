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

import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaQuery;

import org.springframework.stereotype.Repository;

import com.ocs.dynamo.dao.impl.DefaultDaoImpl;
import com.ocs.dynamo.dao.impl.JpaQueryBuilder;
import com.ocs.dynamo.exception.OCSRuntimeException;
import com.ocs.dynamo.functional.domain.Domain;
import com.ocs.dynamo.functional.domain.DomainChild;
import com.ocs.dynamo.functional.domain.DomainParent;
import com.ocs.dynamo.functional.domain.QDomain;
import com.ocs.dynamo.functional.domain.QDomainChild;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.jpa.impl.JPAQuery;

/**
 * Data access implementation for managing domain entities.
 * 
 * @author Patrick Deenen (patrick@opencircle.solutions)
 *
 */
@Repository("domainDao")
public class DomainDaoImpl extends DefaultDaoImpl<Integer, Domain> implements DomainDao {

    public DomainDaoImpl() {
        super(QDomain.domain, Domain.class);
    }

    public DomainDaoImpl(EntityPathBase<Domain> dslRoot, Class<Domain> entityClass) {
        super(dslRoot, entityClass);
    }

    @Override
    public <D extends Domain> List<D> findAllByType(Class<D> type) {
        CriteriaQuery<D> cq = getEntityManager().getCriteriaBuilder().createQuery(type);
        cq.from(type);
        return getEntityManager().createQuery(cq).getResultList();
    }

    @Override
    public <D extends Domain> D findByTypeAndUniqueProperty(Class<D> type, String propertyName, Object value, boolean caseSensitive) {
        CriteriaQuery<D> cq = JpaQueryBuilder.createUniquePropertyQuery(getEntityManager(), type, propertyName, value, caseSensitive);
        TypedQuery<D> query = getEntityManager().createQuery(cq);
        try {
            return query.getSingleResult();
        } catch (NoResultException ex) {
            return null;
        } catch (NonUniqueResultException ex) {
            throw new OCSRuntimeException("Query for unique property returned multiple results", ex);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public <C extends DomainChild<C, P>, P extends DomainParent<C, P>> List<C> findChildren(P parent) {
        JPAQuery query = new JPAQuery<>(getEntityManager()).from(QDomainChild.domainChild);
        query.where(QDomainChild.domainChild.parent.eq(parent));
        return query.from(QDomainChild.domainChild).fetch();
    }

}
