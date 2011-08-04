/*
 * Copyright 2011, eCollege, Inc.  All rights reserved.
 */
package com.strategicgains.repoexpress;

import com.strategicgains.repoexpress.domain.Identifiable;

/**
 * A base, abstract repository implementation that supports observation, as well as,
 * adaptation of the identifier via an IdentifierAdapter (e.g. from String in the
 * domain model to, say, some type of ObjectId in the data store and visa versa).
 * 
 * @author toddf
 * @since May 27, 2011
 */
public abstract class AbstractObservableAdaptableRepository<T extends Identifiable, I>
extends AbstractObservableRepository<T>
implements Adaptable<I>
{
	// Convert an ID to an OID (or whatever) before reading.
	private IdentiferAdapter<I> identifierAdapter = null;

	public IdentiferAdapter<I> getIdentifierAdapter()
	{
		return identifierAdapter;
	}

	public boolean hasIdentifierAdapter()
	{
		return (identifierAdapter != null);
	}

	public void setIdentifierAdapter(IdentiferAdapter<I> adapter)
	{
		this.identifierAdapter = adapter;
	}

	@SuppressWarnings("unchecked")
	@Override
	public I adaptId(String id)
	{
		if (hasIdentifierAdapter())
		{
			return getIdentifierAdapter().convert(id);
		}

		return (I) id;
	}
}
