/*******************************************************************************
 * The MIT License (MIT)
 *
 * Copyright (c) 2011, 2013 OpenWorm.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.simulator.ISimulator;
import org.geppetto.simulation.model.Model;
import org.geppetto.simulation.model.Simulation;
import org.geppetto.simulation.model.Simulator;

public class SessionContext
{

	private ConcurrentHashMap<Model,IModelInterpreter> _modelInterpreters=new ConcurrentHashMap<Model,IModelInterpreter>();
	private ConcurrentHashMap<Simulator,ISimulator> _simulators=new ConcurrentHashMap<Simulator,ISimulator>();
	
	private ConcurrentHashMap<String,SimulatorRuntime> _runtimeByAspect= new ConcurrentHashMap<String,SimulatorRuntime>();
	private ConcurrentHashMap<String,AspectConfiguration> _configurationByAspect= new ConcurrentHashMap<String,AspectConfiguration>();
	

	
	private List<String> _aspectIDs = new ArrayList<String>();
	
	private Simulation _simulation;
	
	private static Log logger = LogFactory.getLog(SessionContext.class);

	/*
	 * simulation flags 
	 */
	private boolean _runningCycleSemaphore = false;
	private boolean _isRunning = false;
	private boolean _isStopped = false;

	private int _maxBufferSize = 100;
	
	/*
	 * Reverts the simulation state to initial conditions
	 * */
	public void revertToInitialConditions()
	{
		// for each aspect, revert runtime to initial conditions
		for(String aspectID : this.getAspectIds())
		{
			this.getSimulatorRuntimeByAspect(aspectID).revertToInitialConditions();
		}

		_runningCycleSemaphore = false;
		_isRunning = false;
		
		logger.warn("Reverted to initial conditions");
	}
	
	/*
	 * Resets the simulation context
	 * NOTE: WIPES EVERYTHING
	 * */
	public void reset()
	{
		_runtimeByAspect.clear();
		_configurationByAspect.clear();
		_aspectIDs.clear();
		_runningCycleSemaphore = false;
		_isRunning = false;
		_isStopped = false;
		_simulation=null;
	}

	public boolean isRunning()
	{
		return _isRunning;
	}
	
	public void setRunning(boolean isRunning)
	{
		_isRunning=isRunning;
	}
	
	public boolean isStopped()
	{
		return _isStopped;
	}
	
	public void setStopped(boolean isStopped)
	{
		_isStopped = isStopped;
	}

	public boolean isRunningCycleSemaphore()
	{
		return _runningCycleSemaphore;
	}
	
	public void setRunningCycleSemaphore(boolean runningCycleSemaphore)
	{
		_runningCycleSemaphore=runningCycleSemaphore;
	}
	
	public int getMaxBufferSize()
	{
		return _maxBufferSize;
	}

	public List<String> getAspectIds()
	{
		return _aspectIDs;
	}

	public SimulatorRuntime getSimulatorRuntimeByAspect(String aspectId)
	{
		return _runtimeByAspect.get(aspectId);
	}

	public AspectConfiguration getConfigurationByAspect(String aspectId)
	{
		return _configurationByAspect.get(aspectId);
	}

	public void setProcessedElements(String aspectId, int i)
	{
		_runtimeByAspect.get(aspectId).setProcessedElements(i);
	}

	public void setMaxBufferSize(int maxBufferSize)
	{
		_maxBufferSize=maxBufferSize;
	}

	public void addAspectId(String id, IModelInterpreter modelInterpreter, ISimulator simulator, String modelURL)
	{
		_aspectIDs.add(id);
		SimulatorRuntime simulatorRuntime=new SimulatorRuntime();
		AspectConfiguration aspectConfiguration=new AspectConfiguration();
		simulatorRuntime.setProcessedElements(0);
		aspectConfiguration.setModelInterpreter(modelInterpreter);
		aspectConfiguration.setSimulator(simulator);
		aspectConfiguration.setUrl(modelURL);
		_runtimeByAspect.put(id, simulatorRuntime);
		_configurationByAspect.put(id, aspectConfiguration);
	}

	/**
	 * @return
	 */
	public Simulation getSimulation()
	{
		return _simulation;
	}

	/**
	 * @param sim
	 */
	public void setSimulation(Simulation simulation)
	{
		_simulation=simulation;
	}

	/**
	 * @param model
	 * @return
	 * @throws GeppettoInitializationException
	 */
	public IModelInterpreter getModelInterpreter(Model model) throws GeppettoInitializationException
	{
		if(!_modelInterpreters.contains(model))
		{
			throw new GeppettoInitializationException("The model interpreter for "+model.getInstancePath()+ " was not found");
		}
		return _modelInterpreters.get(model);
	}
	
	/**
	 * @param simulator
	 * @return
	 * @throws GeppettoInitializationException
	 */
	public ISimulator getSimulator(Simulator simulatorModel) throws GeppettoInitializationException
	{
		if(!_simulators.contains(simulatorModel))
		{
			throw new GeppettoInitializationException("The simulator for "+simulatorModel.getInstancePath()+ " was not found");
		}
		return _simulators.get(simulatorModel);
	}


	/**
	 * @return
	 */
	public Map<Model,IModelInterpreter> getModelInterpreters()
	{
		return _modelInterpreters;
	}
	
	/**
	 * @return
	 */
	public Map<Simulator,ISimulator> getSimulators()
	{
		return _simulators;
	}
	
}
