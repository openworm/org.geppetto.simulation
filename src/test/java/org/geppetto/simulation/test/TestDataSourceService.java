/*******************************************************************************
 * The MIT License (MIT)
 * 
 * Copyright (c) 2011 - 2015 OpenWorm.
 * http://openworm.org
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     	OpenWorm - http://openworm.org/people.html
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights 
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell 
 * copies of the Software, and to permit persons to whom the Software is 
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR 
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE 
 * USE OR OTHER DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package org.geppetto.simulation.test;

import org.geppetto.core.datasources.ADataSourceService;
import org.geppetto.core.datasources.ExecuteQueryVisitor;
import org.geppetto.core.datasources.GeppettoDataSourceException;
import org.geppetto.core.datasources.IDataSourceService;
import org.geppetto.core.datasources.IQueryListener;
import org.geppetto.model.Query;
import org.geppetto.model.QueryResults;
import org.geppetto.model.util.GeppettoModelTraversal;
import org.geppetto.model.util.GeppettoVisitingException;
import org.geppetto.model.variables.Variable;
import org.geppetto.model.variables.VariablesFactory;

/**
 * @author matteocantarelli
 *
 */
public class TestDataSourceService extends ADataSourceService implements IDataSourceService
{

	public TestDataSourceService()
	{
		super("");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.model.QueryProvider#getNumberOfResults(org.geppetto.core.model.Query, org.geppetto.model.variables.Variable)
	 */
	@Override
	public int getNumberOfResults(Query query, Variable variable) throws GeppettoDataSourceException
	{
		Query fetchVariableQuery = getConfiguration().getFetchVariableQuery();
		ExecuteQueryVisitor runQueryVisitor = new ExecuteQueryVisitor(this.getConfiguration(), getTemplate(), variable, getGeppettoModelAccess(), true, ConnectionType.POST);
		try
		{
			GeppettoModelTraversal.apply(fetchVariableQuery, runQueryVisitor);

		}
		catch(GeppettoVisitingException e)
		{
			throw new GeppettoDataSourceException(e);
		}

		return runQueryVisitor.getCount();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.model.QueryProvider#getNumberOfResults(org.geppetto.core.model.Query, org.geppetto.model.variables.Variable, org.geppetto.core.model.QueryResults)
	 */
	@Override
	public int getNumberOfResults(Query query, Variable variable, QueryResults results) throws GeppettoDataSourceException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.model.QueryProvider#execute(org.geppetto.core.model.Query, org.geppetto.model.variables.Variable, org.geppetto.core.model.QueryListener)
	 */
	@Override
	public QueryResults execute(Query query, Variable variable, IQueryListener listener) throws GeppettoDataSourceException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.model.QueryProvider#execute(org.geppetto.core.model.Query, org.geppetto.model.variables.Variable, org.geppetto.core.model.QueryResults,
	 * org.geppetto.core.model.QueryListener)
	 */
	@Override
	public QueryResults execute(Query query, Variable variable, QueryResults results, IQueryListener listener) throws GeppettoDataSourceException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.model.IDataSource#fetchVariable(java.lang.String)
	 */
	@Override
	public void fetchVariable(String variableId) throws GeppettoDataSourceException
	{
		Variable fetchedVariable = VariablesFactory.eINSTANCE.createVariable();
		fetchedVariable.setId(variableId);
		getGeppettoModelAccess().addVariable(fetchedVariable);
		Query fetchVariableQuery = getConfiguration().getFetchVariableQuery();
		ExecuteQueryVisitor runQueryVisitor = new ExecuteQueryVisitor(this.getConfiguration(), getTemplate(), fetchedVariable, getGeppettoModelAccess(), ConnectionType.POST);
		try
		{
			GeppettoModelTraversal.apply(fetchVariableQuery, runQueryVisitor);

		}
		catch(GeppettoVisitingException e)
		{
			throw new GeppettoDataSourceException(e);
		}
	}

}
