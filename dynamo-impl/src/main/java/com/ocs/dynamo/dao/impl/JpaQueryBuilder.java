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
package com.ocs.dynamo.dao.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.FetchParent;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.metamodel.Attribute;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import com.ocs.dynamo.constants.DynamoConstants;
import com.ocs.dynamo.dao.FetchJoinInformation;
import com.ocs.dynamo.dao.QueryFunction;
import com.ocs.dynamo.dao.SortOrder;
import com.ocs.dynamo.dao.SortOrders;
import com.ocs.dynamo.domain.AbstractEntity;
import com.ocs.dynamo.exception.OCSRuntimeException;
import com.ocs.dynamo.filter.And;
import com.ocs.dynamo.filter.Between;
import com.ocs.dynamo.filter.Compare;
import com.ocs.dynamo.filter.Contains;
import com.ocs.dynamo.filter.Filter;
import com.ocs.dynamo.filter.In;
import com.ocs.dynamo.filter.IsNull;
import com.ocs.dynamo.filter.Like;
import com.ocs.dynamo.filter.Modulo;
import com.ocs.dynamo.filter.Not;
import com.ocs.dynamo.filter.Or;
import com.ocs.dynamo.util.SystemPropertyUtils;

/**
 * @author patrick.deenen
 * @author bas.rutten Class for constructing JPA queries built on the criteria
 *         API
 */
public final class JpaQueryBuilder {

	/**
	 * Adds fetch join information to a query root
	 * 
	 * @param root       the query root
	 * @param fetchJoins the fetch joins
	 * @return <code>true</code> if the fetches include a collection,
	 *         <code>false</code> otherwise
	 */
	private static <T> boolean addFetchJoinInformation(FetchParent<T, ?> root, FetchJoinInformation... fetchJoins) {
		boolean collection = false;

		if (root != null && fetchJoins != null) {
			for (FetchJoinInformation s : fetchJoins) {

				// Support nested properties
				FetchParent<T, ?> fetch = root;
				String[] ppath = s.getProperty().split("\\.");
				for (String prop : ppath) {
					fetch = fetch.fetch(prop, translateJoinType(s.getJoinType()));
				}
			}

			// check if any collection is fetched. If so then the results need
			// to be cleaned up using "distinct"
			collection = isCollectionFetch(root);
		}
		return collection;
	}

	/**
	 * Adds the "order by" clause to a criteria query
	 * 
	 * @param builder    the criteria builder
	 * @param cq         the criteria query
	 * @param root       the query root
	 * @param distinct   whether a "distinct" is applied to the query
	 * @param sortOrders the sort orders
	 * @return
	 */
	private static <T, R> CriteriaQuery<R> addSortInformation(CriteriaBuilder builder, CriteriaQuery<R> cq,
			Root<T> root, boolean distinct, SortOrder... sortOrders) {
		return addSortInformation(builder, cq, root, (List<Selection<?>>) null, distinct, sortOrders);
	}

	/**
	 * Adds the "order by" clause to a criteria query
	 *
	 * @param builder     the criteria builder
	 * @param cq          the criteria query
	 * @param root        the query root
	 * @param multiSelect whether to select multiple properties
	 * @param distinct    whether a "distinct"is applied to the query. This
	 *                    influences how the sort part is built
	 * @param sortOrders  the sort orders
	 * @return the criteria query with any relevant sorting instructions added to it
	 */
	private static <T, R> CriteriaQuery<R> addSortInformation(CriteriaBuilder builder, CriteriaQuery<R> cq,
			Root<T> root, List<Selection<?>> multiSelect, boolean distinct, SortOrder... sortOrders) {
		List<Selection<?>> ms = new ArrayList<>();
		if (multiSelect != null && !multiSelect.isEmpty()) {
			ms.addAll(multiSelect);
		}
		if (sortOrders != null && sortOrders.length > 0) {
			List<javax.persistence.criteria.Order> orders = new ArrayList<>();
			for (SortOrder sortOrder : sortOrders) {
				Expression<?> property = distinct ? getPropertyPath(root, sortOrder.getProperty(), true)
						: getPropertyPathForSort(root, sortOrder.getProperty());
				ms.add(property);
				orders.add(sortOrder.isAscending() ? builder.asc(property) : builder.desc(property));
			}
			cq.orderBy(orders);
		}
		if (multiSelect != null && !ms.isEmpty()) {
			cq.multiselect(ms);
		}
		return cq;
	}

	/**
	 * Creates a predicate based on an "And" filter
	 * 
	 * @param builder    the criteria builder
	 * @param root       the root object
	 * @param filter     the "And" filter
	 * @param parameters the parameters passed to the query
	 * @return
	 */
	private static Predicate createAndPredicate(CriteriaBuilder builder, Root<?> root, Filter filter,
			Map<String, Object> parameters) {
		And and = (And) filter;
		List<Filter> filters = new ArrayList<>(and.getFilters());

		Predicate predicate = null;
		if (!filters.isEmpty()) {
			predicate = createPredicate(filters.remove(0), builder, root, parameters);
			while (!filters.isEmpty()) {
				Predicate next = createPredicate(filters.remove(0), builder, root, parameters);
				if (next != null) {
					predicate = builder.and(predicate, next);
				}
			}
		}
		return predicate;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Predicate createCaseInsensitiveLikePredicate(CriteriaBuilder builder, Root<?> root, Like like) {
		String unaccentName = SystemPropertyUtils.getUnAccentFunctionName();
		if (!StringUtils.isEmpty(unaccentName)) {
			return builder.like(
					builder.function(unaccentName, String.class,
							builder.lower((Expression) getPropertyPath(root, like.getPropertyId(), true))),
					removeAccents(like.getValue().toLowerCase()));
		}

		return builder.like(builder.lower((Expression) getPropertyPath(root, like.getPropertyId(), true)),
				like.getValue().toLowerCase());
	}

	/**
	 * Creates a predicate based on a "Compare" filter
	 * 
	 * @param builder the criteria builder
	 * @param root    the query root
	 * @param filter  the Compare filter
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Predicate createComparePredicate(CriteriaBuilder builder, Root<?> root, Filter filter) {
		Compare compare = (Compare) filter;
		Path path = getPropertyPath(root, compare.getPropertyId(), true);
		Expression<Comparable> property = path;
		Object value = compare.getValue();

		// number representation may contain locale specific separators.
		// Here, we remove
		// those and make sure a period is used in all cases
		if (value instanceof String) {

			// strip out any "%" sign from decimal fields
			value = ((String) value).replace('%', ' ').trim();

			String str = (String) value;
			if (StringUtils.isNumeric(str.replace(".", "").replace(",", ""))) {
				// first remove all periods (which may be used as
				// thousand
				// separators), then replace comma by period
				str = str.replace(".", "").replace(',', '.');
				value = str;
			}
		}

		switch (compare.getOperation()) {
		case EQUAL:
			if (value instanceof Class<?>) {
				// When instance of class the use type expression
				return builder.equal(path.type(), builder.literal(value));
			}
			return builder.equal(property, value);
		case GREATER:
			return builder.greaterThan(property, (Comparable) value);
		case GREATER_OR_EQUAL:
			return builder.greaterThanOrEqualTo(property, (Comparable) value);
		case LESS:
			return builder.lessThan(property, (Comparable) value);
		case LESS_OR_EQUAL:
			return builder.lessThanOrEqualTo(property, (Comparable) value);
		default:
			return null;
		}
	}

	/**
	 * Creates a query that performs a count
	 * 
	 * @param entityManager the entity manager
	 * @param entityClass   the entity class
	 * @param filter        the filter to apply
	 * @param distinct      whether to return only distinct results
	 * @return
	 */
	public static <T> TypedQuery<Long> createCountQuery(EntityManager entityManager, Class<T> entityClass,
			Filter filter, boolean distinct) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Long> cq = builder.createQuery(Long.class);
		Root<T> root = cq.from(entityClass);

		cq.select(distinct ? builder.countDistinct(root) : builder.count(root));

		Map<String, Object> pars = createParameterMap();
		Predicate p = createPredicate(filter, builder, root, pars);
		if (p != null) {
			cq.where(p);
		}
		TypedQuery<Long> query = entityManager.createQuery(cq);
		setParameters(query, pars);
		return query;
	}

	/**
	 * Creates a query for retrieving all distinct values for a certain field
	 * 
	 * @param filter        the search filter
	 * @param entityManager the entity manager
	 * @param entityClass   the class of the entity to query
	 * @param distinctField the name of the field for which to retrieve the distinct
	 *                      values
	 * @param sortOrders
	 * @return
	 */
	public static <T> TypedQuery<Tuple> createDistinctQuery(Filter filter, EntityManager entityManager,
			Class<T> entityClass, String distinctField, SortOrder... sortOrders) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Tuple> cq = builder.createTupleQuery();
		Root<T> root = cq.from(entityClass);

		// select only the distinctField
		cq.multiselect(getPropertyPath(root, distinctField, true));

		// Set where clause
		Map<String, Object> pars = createParameterMap();
		Predicate p = createPredicate(filter, builder, root, pars);
		if (p != null) {
			cq.where(p);
		}
		cq.distinct(true);
		cq = addSortInformation(builder, cq, root, true, sortOrders);

		TypedQuery<Tuple> query = entityManager.createQuery(cq);
		setParameters(query, pars);

		return query;
	}

	/**
	 * Creates a query that fetches objects based on their IDs
	 * 
	 * @param entityManager the entity manager
	 * @param entityClass   the entity class
	 * @param ids           the IDs of the desired entities
	 * @param sortOrders    the sort orders
	 * @param fetchJoins    the desired fetch joins
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static <ID, T> TypedQuery<T> createFetchQuery(EntityManager entityManager, Class<T> entityClass,
			List<ID> ids, Filter additionalFilter, SortOrders sortOrders, FetchJoinInformation... fetchJoins) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		CriteriaQuery<T> cq = builder.createQuery(entityClass);
		Root<T> root = cq.from(entityClass);

		boolean distinct = addFetchJoinInformation(root, fetchJoins);

		// use parameters to prevent Hibernate from creating different query plan
		// every time
		Expression<String> exp = root.get(DynamoConstants.ID);
		ParameterExpression<List> idExpression = builder.parameter(List.class, DynamoConstants.IDS);
		cq.distinct(distinct);

		Map<String, Object> pars = createParameterMap();
		if (additionalFilter != null) {
			Predicate pr = createPredicate(additionalFilter, builder, root, pars);
			if (pr != null) {
				cq.where(pr, exp.in(idExpression));
			} else {
				cq.where(exp.in(idExpression));
			}
		} else {
			cq.where(exp.in(idExpression));
		}

		addSortInformation(builder, cq, root, distinct, sortOrders == null ? null : sortOrders.toArray());
		TypedQuery<T> query = entityManager.createQuery(cq);

		query.setParameter(DynamoConstants.IDS, ids);

		if (additionalFilter != null) {
			setParameters(query, pars);
		}

		return query;
	}

	/**
	 * Create a query for fetching a single object
	 * 
	 * @param entityManager the entity manager
	 * @param entityClass   the entity class
	 * @param id            ID of the object to return
	 * @param fetchJoins    fetch joins to include
	 * @return
	 */
	public static <ID, T> TypedQuery<T> createFetchSingleObjectQuery(EntityManager entityManager, Class<T> entityClass,
			ID id, FetchJoinInformation[] fetchJoins) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		CriteriaQuery<T> cq = builder.createQuery(entityClass);
		Root<T> root = cq.from(entityClass);

		addFetchJoinInformation(root, fetchJoins);
		Expression<String> exp = root.get(DynamoConstants.ID);

		boolean parameterSet = true;
		if (id instanceof Integer) {
			ParameterExpression<Integer> p = builder.parameter(Integer.class, DynamoConstants.ID);
			cq.where(builder.equal(exp, p));
		} else if (id instanceof Long) {
			ParameterExpression<Long> p = builder.parameter(Long.class, DynamoConstants.ID);
			cq.where(builder.equal(exp, p));
		} else if (id instanceof String) {
			ParameterExpression<String> p = builder.parameter(String.class, DynamoConstants.ID);
			cq.where(builder.equal(exp, p));
		} else {
			// no parameter but query directly
			parameterSet = false;
			cq.where(builder.equal(root.get(DynamoConstants.ID), id));
		}

		TypedQuery<T> query = entityManager.createQuery(cq);
		if (parameterSet) {
			query.setParameter(DynamoConstants.ID, id);
		}

		return query;
	}

	/**
	 * Creates a query for retrieving the IDs of the entities that match the
	 * provided filter
	 * 
	 * @param entityManager the entity manager
	 * @param entityClass   the entity class
	 * @param filter        the filter to apply
	 * @param sortOrders    the sorting to apply
	 * @return
	 */
	public static <T> TypedQuery<Tuple> createIdQuery(EntityManager entityManager, Class<T> entityClass, Filter filter,
			SortOrder... sortOrders) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Tuple> cq = builder.createTupleQuery();
		Root<T> root = cq.from(entityClass);

		List<Selection<?>> selection = new ArrayList<>();
		selection.add(root.get(DynamoConstants.ID));

		Map<String, Object> pars = createParameterMap();
		Predicate p = createPredicate(filter, builder, root, pars);
		if (p != null) {
			cq.where(p);
		}

		// When joins are added (by getPropertyPath) do distinct query
		if (!root.getJoins().isEmpty()) {
			cq.distinct(true);
		}

		// add order clause - this is also important in case of an ID query
		// since we do need to return the correct IDs!
		// note: "distinct" must be false here
		cq = addSortInformation(builder, cq, root, selection, false, sortOrders);
		TypedQuery<Tuple> query = entityManager.createQuery(cq);
		setParameters(query, pars);
		return query;
	}

	private static Predicate createLikePredicate(CriteriaBuilder builder, Root<?> root, Filter filter) {
		Like like = (Like) filter;
		if (like.isCaseSensitive()) {
			return createLikePredicate(builder, root, like);
		} else {
			return createCaseInsensitiveLikePredicate(builder, root, like);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Predicate createLikePredicate(CriteriaBuilder builder, Root<?> root, Like like) {
		String unaccentName = SystemPropertyUtils.getUnAccentFunctionName();
		if (!StringUtils.isEmpty(unaccentName)) {
			return builder.like(
					builder.function(unaccentName, String.class,
							(Expression) getPropertyPath(root, like.getPropertyId(), true)),
					removeAccents(like.getValue()));
		}

		return builder.like((Expression) getPropertyPath(root, like.getPropertyId(), true), like.getValue());
	}

	/**
	 * Create a modulo predicate
	 * 
	 * @param builder
	 * @param filter
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Predicate createModuloPredicate(CriteriaBuilder builder, Root<?> root, Filter filter) {
		Modulo modulo = (Modulo) filter;
		if (modulo.getModExpression() != null) {
			// compare to a literal expression
			return builder.equal(builder.mod((Expression) getPropertyPath(root, modulo.getPropertyId(), true),
					(Expression) getPropertyPath(root, modulo.getModExpression(), true)), modulo.getResult());
		} else {
			// compare to a property
			return builder.equal(builder.mod((Expression) getPropertyPath(root, modulo.getPropertyId(), true),
					modulo.getModValue().intValue()), modulo.getResult());
		}
	}

	/**
	 * Creates a predicate for a logical or
	 * 
	 * @param builder    the criteria builder
	 * @param root       the query root
	 * @param filter     the filter to apply
	 * @param parameters the query parameter mapping
	 * @return
	 */
	private static Predicate createOrPredicate(CriteriaBuilder builder, Root<?> root, Filter filter,
			Map<String, Object> parameters) {
		Or or = (Or) filter;
		List<Filter> filters = new ArrayList<>(or.getFilters());

		Predicate predicate = null;
		if (!filters.isEmpty()) {
			predicate = createPredicate(filters.remove(0), builder, root, parameters);
			while (!filters.isEmpty()) {
				Predicate next = createPredicate(filters.remove(0), builder, root, parameters);
				if (next != null) {
					predicate = builder.or(predicate, next);
				}
			}
		}

		return predicate;
	}

	private static Map<String, Object> createParameterMap() {
		return new HashMap<>();
	}

	/**
	 * Creates a predicate based on a Filter
	 * 
	 * @param filter  the filter
	 * @param builder the criteria builder
	 * @param root    the entity root
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Predicate createPredicate(Filter filter, CriteriaBuilder builder, Root<?> root,
			Map<String, Object> parameters) {
		if (filter == null) {
			return null;
		}

		if (filter instanceof And) {
			return createAndPredicate(builder, root, filter, parameters);
		} else if (filter instanceof Or) {
			return createOrPredicate(builder, root, filter, parameters);
		} else if (filter instanceof Not) {
			Not not = (Not) filter;
			return builder.not(createPredicate(not.getFilter(), builder, root, parameters));
		} else if (filter instanceof Between) {
			Between between = (Between) filter;
			Expression property = getPropertyPath(root, between.getPropertyId(), true);
			return builder.between(property, (Comparable) between.getStartValue(), (Comparable) between.getEndValue());
		} else if (filter instanceof Compare) {
			return createComparePredicate(builder, root, filter);
		} else if (filter instanceof IsNull) {
			IsNull isNull = (IsNull) filter;
			Path path = getPropertyPath(root, isNull.getPropertyId(), true);
			if (path.type() != null && java.util.Collection.class.isAssignableFrom(path.type().getJavaType())) {
				return builder.isEmpty(path);
			}
			return builder.isNull(path);
		} else if (filter instanceof Like) {
			return createLikePredicate(builder, root, filter);
		} else if (filter instanceof Contains) {
			Contains contains = (Contains) filter;
			return builder.isMember(contains.getValue(),
					(Expression) getPropertyPath(root, contains.getPropertyId(), false));
		} else if (filter instanceof In) {
			In in = (In) filter;
			if (in.getValues() != null && !in.getValues().isEmpty()) {
				Expression<?> exp = getPropertyPath(root, in.getPropertyId(), true);
				String parName = in.getPropertyId().replace('.', '_');
				// Support multiple parameters
				if (parameters.containsKey(parName)) {
					parName = parName + System.currentTimeMillis();
				}

				ParameterExpression<Collection> p = builder.parameter(Collection.class, parName);
				parameters.put(parName, in.getValues());
				return exp.in(p);
			} else {
				Expression exp = getPropertyPath(root, in.getPropertyId(), true);
				return exp.in(Lists.newArrayList(-1));
			}
		} else if (filter instanceof Modulo) {
			return createModuloPredicate(builder, root, filter);
		}

		throw new UnsupportedOperationException("Filter: " + filter.getClass().getName() + " not recognized");
	}

	/**
	 * Creates a query that selects objects based on the specified filter
	 * 
	 * @param filter        the filter
	 * @param entityManager the entity manager
	 * @param entityClass   the entity class
	 * @param sortOrders    the sorting information
	 * @return
	 */
	public static <T> TypedQuery<T> createSelectQuery(Filter filter, EntityManager entityManager, Class<T> entityClass,
			FetchJoinInformation[] fetchJoins, SortOrder... sortOrders) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		CriteriaQuery<T> cq = builder.createQuery(entityClass);
		Root<T> root = cq.from(entityClass);

		boolean distinct = addFetchJoinInformation(root, fetchJoins);
		cq.select(root);
		cq.distinct(distinct);

		Map<String, Object> pars = createParameterMap();
		Predicate p = createPredicate(filter, builder, root, pars);
		if (p != null) {
			cq.where(p);
		}
		cq = addSortInformation(builder, cq, root, distinct, sortOrders);
		TypedQuery<T> query = entityManager.createQuery(cq);
		setParameters(query, pars);
		return query;
	}

	/**
	 * Creates a query that fetches properties instead of entities. Supports
	 * aggregated functions; when used will automatically add group by expressions
	 * for all properties in the select list without an aggregated function.
	 * 
	 * @param filter           the filter
	 * @param entityManager    the entity manager
	 * @param entityClass      the entity class
	 * @param selectProperties the properties to use in the selection
	 * @param sortOrders       the sorting information
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T> TypedQuery<Object[]> createSelectQuery(Filter filter, EntityManager entityManager,
			Class<T> entityClass, String[] selectProperties, SortOrders sortOrders) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Object[]> cq = builder.createQuery(Object[].class);
		Root<T> root = cq.from(entityClass);
		ArrayList<Expression<?>> grouping = new ArrayList<>();
		boolean aggregated = false;

		// Set select
		if (selectProperties != null && selectProperties.length > 0) {
			Selection<?>[] selections = new Selection<?>[selectProperties.length];
			int i = 0;
			for (String sp : selectProperties) {

				// Support nested properties
				String[] ppath = sp.split("\\.");
				// Test for function
				QueryFunction f = null;
				Path path = null;
				try {
					if (ppath.length > 1) {
						f = QueryFunction.valueOf(ppath[ppath.length - 1]);
						path = getPropertyPath(root, sp.substring(0, sp.lastIndexOf('.')), true);
					}
				} catch (Exception e) {
					// Do nothing; not a supported function; assume property name
				}
				if (f != null) {
					switch (f) {
					case AF_AVG:
						selections[i] = builder.avg(path);
						break;
					case AF_COUNT:
						selections[i] = builder.count(path);
						break;
					case AF_COUNT_DISTINCT:
						selections[i] = builder.countDistinct(path);
						break;
					case AF_SUM:
						selections[i] = builder.sum(path);
						break;
					default:
						throw new OCSRuntimeException("Unsupported function");
					}
					aggregated = true;
				} else {
					path = getPropertyPath(root, sp, true);
					selections[i] = path;
					grouping.add(path);
				}
				i++;
			}
			cq.select(builder.array(selections));
		}

		Map<String, Object> pars = createParameterMap();
		Predicate p = createPredicate(filter, builder, root, pars);
		if (p != null) {
			cq.where(p);
		}
		if (aggregated) {
			cq.groupBy(grouping);
		}
		cq = addSortInformation(builder, cq, root, true, sortOrders == null ? null : sortOrders.toArray());
		TypedQuery<Object[]> query = entityManager.createQuery(cq);
		setParameters(query, pars);
		return query;
	}

	/**
	 * Creates a query to fetch an object based on a value of a unique property
	 * 
	 * @param entityManager the entity manager
	 * @param entityClass   the entity class
	 * @param fetchJoins    the fetch joins to include
	 * @param propertyName  name of the property to search on
	 * @param value         value of the property to search on
	 * @return
	 */
	public static <T> CriteriaQuery<T> createUniquePropertyFetchQuery(EntityManager entityManager, Class<T> entityClass,
			FetchJoinInformation[] fetchJoins, String propertyName, Object value, boolean caseSensitive) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		CriteriaQuery<T> cq = builder.createQuery(entityClass);
		Root<T> root = cq.from(entityClass);

		addFetchJoinInformation(root, fetchJoins);

		Predicate equals;
		if (value instanceof String && !caseSensitive) {
			equals = builder.equal(builder.upper(root.get(propertyName).as(String.class)),
					((String) value).toUpperCase());
		} else {
			equals = builder.equal(root.get(propertyName), value);
		}
		cq.where(equals);
		cq.distinct(true);

		return cq;
	}

	/**
	 * Creates a query used to retrieve a single entity based on a unique property
	 * value
	 * 
	 * @param entityManager
	 * @param entityClass
	 * @param propertyName
	 * @param value
	 * @return
	 */
	public static <T> CriteriaQuery<T> createUniquePropertyQuery(EntityManager entityManager, Class<T> entityClass,
			String propertyName, Object value, boolean caseSensitive) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		CriteriaQuery<T> cq = builder.createQuery(entityClass);
		Root<T> root = cq.from(entityClass);

		Predicate equals = null;
		if (value instanceof String && !caseSensitive) {
			equals = builder.equal(builder.upper(root.get(propertyName).as(String.class)),
					((String) value).toUpperCase());
		} else {
			equals = builder.equal(root.get(propertyName), value);
		}
		cq.where(equals);

		return cq;
	}

	/**
	 * Gets property path.
	 * 
	 * @param root       the root where path starts form
	 * @param propertyId the property ID
	 * @param join       set to true if you want implicit joins to be created for
	 *                   ALL collections
	 * @return the path to property
	 */
	@SuppressWarnings("unchecked")
	private static Path<Object> getPropertyPath(Root<?> root, Object propertyId, boolean join) {
		String[] propertyIdParts = ((String) propertyId).split("\\.");

		Path<?> path = null;
		Join<?, ?> curJoin = null;
		for (int i = 0; i < propertyIdParts.length; i++) {
			String part = propertyIdParts[i];
			if (path == null) {
				path = root.get(part);
			} else {
				path = path.get(part);
			}
			// Check collection join
			if (join && Collection.class.isAssignableFrom(path.type().getJavaType())) {
				// Reuse existing join
				Join<?, ?> detailJoin = null;
				Collection<Join<?, ?>> joins = (Collection<Join<?, ?>>) (curJoin == null ? root.getJoins()
						: curJoin.getJoins());
				if (joins != null) {
					for (Join<?, ?> j : (Set<Join<?, ?>>) joins) {
						if (propertyIdParts[i].equals(j.getAttribute().getName())) {
							path = j;
							detailJoin = j;
							break;
						}
					}
				}
				// when no existing join then add new
				if (detailJoin == null) {
					if (curJoin == null) {
						curJoin = root.join(propertyIdParts[i]);
					} else {
						curJoin = curJoin.join(propertyIdParts[i]);
					}
					path = curJoin;
				}
			}
		}
		return (Path<Object>) path;
	}

	/**
	 * Adds a property path specifically for sorting
	 * 
	 * @param root
	 * @param propertyId
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private static Expression<?> getPropertyPathForSort(Root<?> root, Object propertyId) {
		String[] propertyIdParts = ((String) propertyId).split("\\.");

		Path<?> path = null;
		Join<?, ?> curJoin = null;
		for (int i = 0; i < propertyIdParts.length; i++) {
			String part = propertyIdParts[i];
			if (path == null) {
				path = root.get(part);
			} else {
				path = path.get(part);
			}

			if (AbstractEntity.class.isAssignableFrom(path.type().getJavaType())
					|| Collection.class.isAssignableFrom(path.type().getJavaType())) {
				// Reuse existing join
				Join<?, ?> detailJoin = null;
				Collection<Join<?, ?>> joins = (Collection<Join<?, ?>>) (curJoin == null ? root.getJoins()
						: curJoin.getJoins());
				if (joins != null) {
					for (Join<?, ?> j : joins) {
						if (propertyIdParts[i].equals(j.getAttribute().getName())) {
							path = j;
							detailJoin = j;
							break;
						}
					}
				}
				// when no existing join then add new
				if (detailJoin == null) {
					if (curJoin == null) {
						curJoin = root.join(propertyIdParts[i], JoinType.LEFT);
					} else {
						curJoin = curJoin.join(propertyIdParts[i], JoinType.LEFT);
					}
					path = curJoin;
				}
			}

		}
		return path;
	}

	/**
	 * Indicates whether at least one of the specified fetches is a fetch that
	 * fetches a collection
	 * 
	 * @param parent the fetch parent
	 * @return
	 */
	private static boolean isCollectionFetch(FetchParent<?, ?> parent) {
		boolean result = false;

		for (Fetch<?, ?> fetch : parent.getFetches()) {
			Attribute<?, ?> attribute = fetch.getAttribute();

			boolean nested = isCollectionFetch(fetch);
			result = result || attribute.isCollection() || nested;
		}
		return result;
	}

	private static String removeAccents(String input) {
		return com.ocs.dynamo.utils.StringUtils.removeAccents(input);
	}

	/**
	 * Sets the values of all parameters used in the query
	 * 
	 * @param query the query
	 * @param pars  the parameter values
	 */
	private static void setParameters(TypedQuery<?> query, Map<String, Object> pars) {
		for (Entry<String, Object> entry : pars.entrySet()) {
			query.setParameter(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Translates a JoinType
	 * 
	 * @param type the type to translate
	 * @return
	 */
	private static JoinType translateJoinType(com.ocs.dynamo.dao.JoinType type) {
		switch (type) {
		case INNER:
			return JoinType.INNER;
		case LEFT:
			return JoinType.LEFT;
		default:
			return JoinType.RIGHT;
		}
	}

	private JpaQueryBuilder() {
		// hidden private constructor
	}
}
