/*
    Copyright 2010-2012, Strategic Gains, Inc.

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
package com.strategicgains.repoexpress.cassandra;

import java.util.Collection;
import java.util.List;

import com.datastax.driver.core.Query;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.utils.querybuilder.Clause;
import com.datastax.driver.core.utils.querybuilder.QueryBuilder;
import com.pearson.cassandra.mapper.CassandraMapper;
import com.pearson.cassandra.mapper.Datastore;
import com.strategicgains.repoexpress.AbstractObservableAdaptableRepository;
import com.strategicgains.repoexpress.Queryable;
import com.strategicgains.repoexpress.domain.Identifiable;
import com.strategicgains.repoexpress.exception.DuplicateItemException;
import com.strategicgains.repoexpress.exception.InvalidObjectIdException;
import com.strategicgains.repoexpress.exception.ItemNotFoundException;
import com.strategicgains.restexpress.query.FilterCallback;
import com.strategicgains.restexpress.query.FilterComponent;
import com.strategicgains.restexpress.query.OrderCallback;
import com.strategicgains.restexpress.query.OrderComponent;
import com.strategicgains.restexpress.query.QueryFilter;
import com.strategicgains.restexpress.query.QueryOrder;
import com.strategicgains.restexpress.query.QueryRange;

/**
 * Uses Cassandra as its back-end store. This repository can handle
 * "single-table inheritance" by passing all the supported types into the
 * constructor, with the inheritance root first.
 * 
 * @author toddf
 * @since Nov 20, 2010
 */
public class CassandraRepository<T extends Identifiable, I>
extends AbstractObservableAdaptableRepository<T, I>
implements Queryable<T>
{
	private Datastore<T> cassandra;
	private Class<T> inheritanceRoot;
	private CassandraMapper mapper;

	/**
	 * 
	 * @param cluster a pre-configured Cassandra cluster instance.
	 * @param entityClasses Class(es) managed by this repository. Inheritance root first.
	 */
	public CassandraRepository(Session session, Class<? extends T>... entityClasses)
	{
		super();
		initialize(session, entityClasses);
	}

	private void initialize(Session session, Class<? extends T>... entityClasses)
	{
		mapper = new CassandraMapper(session);
		mapper.mapClass(entityClasses);
		cassandra = mapper.createDatastore();
		cassandra.createKeyspace();
		cassandra.ensureIndexes();
	}

	@Override
	public T doCreate(T item)
	{
		if (exists(item.getId()))
		{
			throw new DuplicateItemException(item.getClass().getSimpleName()
			    + " ID already exists: " + item.getId());
		}

		return cassandra.save(item);
	}

	@Override
	public T doRead(String id)
	{
		T item = cassandra.read(inheritanceRoot, adaptId(id));

		if (item == null)
		{
			throw new ItemNotFoundException("ID not found: " + id);
		}

		return item;
	}

	@Override
	public T doUpdate(T item)
	{
		if (!exists(item.getId()))
		{
			throw new ItemNotFoundException(item.getClass().getSimpleName()
			    + " ID not found: " + item.getId());
		}

		cassandra.save(item);
		return item;
	}

	@Override
	public void doDelete(T object)
	{
		try
		{
			cassandra.delete(object);
		}
		catch (InvalidObjectIdException e)
		{
			throw new ItemNotFoundException("ID not found: " + object.getId());
		}
	}

	/**
	 * Implements a 'default' readAll' method that queries for all instances of
	 * the inheritance root class matching the given criteria.
	 * 
	 * This method does not invoke an observer method, so is not observable by
	 * default. Override, calling super() to get that functionality, or call
	 * notifyBeforeXXX() and/or notifyAfterXXX() methods, if desired.
	 * 
	 * @param filter
	 * @param range
	 * @param order
	 * @return a list of results. Never null.
	 */
	@Override
	public List<T> readAll(QueryFilter filter, QueryRange range, QueryOrder order)
	{
		return query(inheritanceRoot, filter, range, order);
	}

	/**
	 * Read each of the instances corresponding to the given Collection of IDs,
	 * returning the results as a list. If an ID in the provided Collection does
	 * not exist, it is simply not included in the returned results.
	 * 
	 * @param ids
	 *            a Collection of IDs to read.
	 */
	@Override
	public List<T> readList(Collection<String> ids)
	{
//		return mapper.select(getDataStore().find(inheritanceRoot).where(Clause.in("_id", ids.toArray())));
//		return getDataStore().find(inheritanceRoot).field("_id")
//		    .in(new AdaptedIdIterable(ids)).asList();
		return null;
	}

	/**
	 * Count the instances of the inheritance root (class) that match the given
	 * filter criteria.
	 * 
	 * @param filter
	 */
	@Override
	public long count(QueryFilter filter)
	{
		return count(inheritanceRoot, filter);
	}

	/**
	 * Count the instances of the given type matching the given filter criteria.
	 * 
	 * @param type
	 * @param filter
	 */
	public long count(Class<T> type, QueryFilter filter)
	{
//		return getBaseFilterQuery(type, filter).countAll();
		return 0l;
	}

	/**
	 * Returns true if the given id exists in the repository.
	 * 
	 * @param id
	 *            the identifier of the object.
	 */
	@Override
	public boolean exists(String id)
	{
		if (id == null) return false;

//		return (cassandra.getCount(cassandra.find(inheritanceRoot, "_id", adaptId(id))) > 0);

		// is the above line more efficient, or the following one?
		// return (datastore.find(inheritanceRoot, "_id",
		// adaptId(id)).countAll() > 0);
		
		return false;
	}

	// SECTION: UTILITY

	/**
	 * Get the underlying Cassandra Datastore object with which to construct
	 * queries against.
	 * 
	 * @return the underlying Cassandra Datastore.
	 */
	protected Datastore<T> getDataStore()
	{
		return cassandra;
	}

	/**
	 * Execute a query against the repository, using QueryFilter, QueryRange and
	 * QueryOrder as criteria against the type. Returns the results as a List.
	 * 
	 * @param type
	 * @param range
	 * @param filter
	 * @param order
	 */
	protected List<T> query(Class<T> type, QueryFilter filter,
	    QueryRange range, QueryOrder order)
	{
//		return getBaseQuery(type, filter, range, order).asList();
		return null;
	}

	/**
	 * Create and configure a basic query utilizing provided QueryFilter,
	 * QueryRange and QueryOrder criteria, returning the query.
	 * 
	 * @param type
	 * @param range
	 * @param filter
	 * @param order
	 */
	protected Query getBaseQuery(Class<T> type, QueryFilter filter,
	    QueryRange range, QueryOrder order)
	{
		Query q = getBaseFilterQuery(type, filter);
		configureQueryRange(q, range);
		configureQueryOrder(q, order);
		return q;
	}

	/**
	 * Create and configure a basic query utilizing just QueryFilter as
	 * criteria.
	 * 
	 * @param type
	 * @param filter
	 * @return a Cassandra Query instance configured for the QueryFilter criteria.
	 */
	private Query getBaseFilterQuery(Class<T> type, QueryFilter filter)
	{
//		Query q = getDataStore().find(type);
//		configureQueryFilter(q, filter);
//		return q;
		return null;
	}

	/**
	 * @param q
	 * @param range
	 */
	private void configureQueryRange(Query q, QueryRange range)
	{
//		if (range.isInitialized())
//		{
//			q.offset((int) range.getStart());
//			q.limit(range.getLimit());
//		}
	}

	private void configureQueryFilter(final Query q, QueryFilter filter)
	{
		filter.iterate(new FilterCallback()
		{
			@Override
			public void filterOn(FilterComponent c)
			{
//				q.field(c.getField()).contains(c.getValue().toLowerCase());
			}
		});
	}

	/**
	 * @param q
	 * @param order
	 */
	private void configureQueryOrder(Query q, QueryOrder order)
	{
		if (order.isSorted())
		{
			final StringBuilder sb = new StringBuilder();

			order.iterate(new OrderCallback()
			{
				boolean isFirst = true;

				@Override
				public void orderBy(OrderComponent component)
				{
					if (!isFirst)
					{
						sb.append(',');
					}

					if (component.isDescending())
					{
						sb.append('-');
					}

					sb.append(component.getFieldName());
					isFirst = false;
				}
			});

//			q.order(sb.toString());
		}
	}
}