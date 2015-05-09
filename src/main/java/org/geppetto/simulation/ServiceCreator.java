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
package org.geppetto.simulation;

import java.util.Map;

import org.geppetto.core.common.GeppettoErrorCodes;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.simulation.IProjectManagerCallbackListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * This runnable creates a service using the bundle context to retrieve a service 
 * reference. Once the service is retrieved it is added to a map which store
 * the pair model,service where model is whatever model was passed to the class.
 * 
 * Examples of model,service pairs are: 
 * Model->IModelInterpreter
 * Simulator->ISimulator
 * 
 * See {@link org.geppetto.simulation.CreateModelInterpreterServicesVisitor} for more info.
 * 
 * @author matteocantarelli
 *
 */
public class ServiceCreator<M,S> implements Runnable
{

	private BundleContext _bc = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
	private M _model;
	private Map<M,S> _map=null;
	private String _discoveryId=null;
	private String _type=null;
	private IProjectManagerCallbackListener _simulationCallBack;
	
	public ServiceCreator(String discoveryId, String type, M model, Map<M,S> map, IProjectManagerCallbackListener simulationCallBack)
	{
		super();
		this._discoveryId = discoveryId;
		this._type = type;
		this._map=map;
		_model=model;
		_simulationCallBack=simulationCallBack;
	}


	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run()
	{
		try
		{
			S service=getService(_discoveryId, _type);
			_map.put(_model, service);
		}
		catch(GeppettoInitializationException e)
		{
			_simulationCallBack.error(GeppettoErrorCodes.INITIALIZATION, this.getClass().getName(),null,e);
		}

	}
	
	/**
	 * A method to get a service of a given type
	 * 
	 * @param discoveryId
	 * @param type
	 * @return
	 * @throws InvalidSyntaxException
	 */
	private S getService(String discoveryId, String type) throws GeppettoInitializationException
	{
		S service = null;

		String filter = String.format("(discoverableID=%s)", discoveryId);
		ServiceReference<?>[] sr;
		try
		{
			sr = _bc.getServiceReferences(type, filter);
		}
		catch(InvalidSyntaxException e)
		{
			throw new GeppettoInitializationException(e);
		}
		if(sr != null && sr.length > 0)
		{
			service = (S) _bc.getService(sr[0]);
		}

		return service;
	}

}
