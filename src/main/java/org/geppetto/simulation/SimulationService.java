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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.common.IVariable;
import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.model.ModelInterpreterException;
import org.geppetto.core.model.state.StateTreeRoot;
import org.geppetto.core.model.state.visitors.CountTimeStepsVisitor;
import org.geppetto.core.simulation.ISimulation;
import org.geppetto.core.simulation.ISimulationCallbackListener;
import org.geppetto.core.simulator.ISimulator;
import org.geppetto.core.visualisation.model.Scene;
import org.geppetto.simulation.model.Aspect;
import org.geppetto.simulation.model.Simulation;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
class SimulationService implements ISimulation
{

	@Autowired
	public AppConfig appConfig;

	BundleContext _bc = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

	private static Log logger = LogFactory.getLog(SimulationService.class);

	private final SessionContext _sessionContext = new SessionContext();

	private Timer _clientUpdateTimer;

	private SimulationThread _simThread;

	private ISimulationCallbackListener _simulationListener;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulation.ISimulation#init(java.net.URL, org.geppetto.core.simulation.ISimulationCallbackListener)
	 */
	@Override
	public void init(URL simConfigURL, ISimulationCallbackListener simulationListener) throws GeppettoInitializationException
	{		
		Simulation sim = SimulationConfigReader.readConfig(simConfigURL);
		_simulationListener = simulationListener;
		
		try {
			load(sim);
		} catch (GeppettoExecutionException e) {
			// TODO Auto-generated catch block
			throw new GeppettoInitializationException("Error Loading Simulation Model");
		}
	}
	
	/**
	 * Initializes simulation with JSON object containing simulation. 
	 */
	@Override
	public void init(String simulationConfig, ISimulationCallbackListener simulationListener) throws GeppettoInitializationException {
		Simulation sim = SimulationConfigReader.readSimulationConfig(simulationConfig);
		_simulationListener = simulationListener;

		try {
			load(sim);
		} catch (GeppettoExecutionException e) {
			// TODO Auto-generated catch block
			throw new GeppettoInitializationException("Error Loading Simulation Model");
		}
	}
	
	public void load(Simulation sim) throws GeppettoInitializationException, GeppettoExecutionException{		
		// refresh simulation context
		_sessionContext.reset();

		// retrieve model interpreters and simulators
		populateDiscoverableServices(sim);

		_sessionContext.setMaxBufferSize(appConfig.getMaxBufferSize());

		loadModel();
	}

	private void loadModel() throws GeppettoInitializationException, GeppettoExecutionException {
		_simThread = new SimulationThread(_sessionContext);
		_simThread.loadModel();
		try {
			update();
		} catch (GeppettoExecutionException e) {
			// TODO Auto-generated catch block
			throw new GeppettoExecutionException("Error loading simulation model");
		}		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulation.ISimulation#start()
	 */
	@Override
	public void start()
	{
		logger.warn("Starting simulation");
		//start the simulation
		_sessionContext.setRunning(true);
		_sessionContext.setStopped(false);
		startSimulationThread();
		startClientUpdateTimer();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulation.ISimulation#stop()
	 */
	@Override
	public void pause()
	{
		logger.warn("Pausing simulation");
		// tell the thread to pause the simulation, but don't stop it
		_sessionContext.setRunning(false);
		_sessionContext.setStopped(false);
		// stop the timer that updates the client
		_clientUpdateTimer.cancel();		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulation.ISimulation#stop()
	 */
	@Override
	public void stop()
	{
		
		logger.warn("Stopping simulation");
		// tell the thread to stop running the simulation
		_sessionContext.setRunning(false);
		_sessionContext.setStopped(true);
		
		// stop the timer that updates the client
		_clientUpdateTimer.cancel();
				
		// revert simulation to initial conditions
		_sessionContext.revertToInitialConditions();			
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.geppetto.core.simulation.ISimulation#isRunning()
	 */
	@Override
	public boolean isRunning(){
		return _sessionContext.isRunning();
	}
	
	/**
	 * Takes a URL corresponding to simulation file and extracts information.
	 * @throws GeppettoInitializationException 
	 */
	@Override
	public String getSimulationConfig(URL simURL) throws GeppettoInitializationException {
		String simulationConfig = SimulationConfigReader.writeSimulationConfig(simURL);
		
		return simulationConfig;
	}
	
	/**
	 * Gets the list of all watchable variables in a give simulation
	 */
	@Override
	public List<IVariable> listWatchableVariables() {
		return this.listVariablesHelper(true);
	}

	/**
	 * Gets the list of all forceable variables in a give simulation
	 */
	@Override
	public List<IVariable> listForceableVariables() {
		return this.listVariablesHelper(false);
	}
	
	/**
	 * Fetches variables from simulators
	 * 
	 * @param isWatch specifies is the helper should fetch watch- or force-able variables
	 * @return
	 */
	public List<IVariable> listVariablesHelper(boolean isWatch)
	{
		List<IVariable> vars = new ArrayList<IVariable>();
		
		for(String aspectID : _sessionContext.getAspectIds())
		{
			ISimulator simulator = _sessionContext.getConfigurationByAspect(aspectID).getSimulator();

			if(simulator != null)
			{
				vars.addAll(isWatch? simulator.getWatchableVariables() : simulator.getForceableVariables());
			}
		}
		
		return vars;
	}
	
	/**
	 * Starts simulation thread - under the hood the run method of the thread gets invoked.
	 */
	private void startSimulationThread()
	{
		
		for(String aspectID : _sessionContext.getAspectIds())
		{
			//Load Model if it is still in initial conditions
			logger.warn(aspectID + " : " + _sessionContext.getSimulatorRuntimeByAspect(aspectID).isAtInitialConditions());
		}
		_simThread = new SimulationThread(_sessionContext);
		_simThread.start();
	}

	/**
	 * Starts client updates on a timer.
	 */
	private void startClientUpdateTimer()
	{
		_clientUpdateTimer = new Timer(SimulationService.class.getSimpleName() + " - Timer - " + new java.util.Date().getTime());
		_clientUpdateTimer.scheduleAtFixedRate(new TimerTask()
		{
			@Override
			public void run()
			{
				try
				{
					update();
				}
				catch(GeppettoExecutionException e)
				{
					throw new RuntimeException(e);
				}

			}
		}, appConfig.getUpdateCycle(), appConfig.getUpdateCycle());
	}

	/**
	 * Method that takes the oldest model in the buffer and send it to the client
	 * 
	 * @throws JsonProcessingException
	 */
	private void update() throws GeppettoExecutionException
	{
		StringBuilder sb = new StringBuilder();
		boolean updateAvailable = false;

		for(String aspectID : _sessionContext.getAspectIds())
		{
			// get models Map for the given aspect String = modelId / List<IModel> = a given model at different time steps
			if(_sessionContext.getSimulatorRuntimeByAspect(aspectID).getStateTree() != null)
			{
				StateTreeRoot stateTree = _sessionContext.getSimulatorRuntimeByAspect(aspectID).getStateTree();
				CountTimeStepsVisitor countTimeStepsVisitor=new CountTimeStepsVisitor();
				stateTree.apply(countTimeStepsVisitor);
				//we send data to the frontend if it's either the first cycle or if there is a change in the state, i.e. something that might produce a frontend update
				//putting the constraint to have at least two states buffered before starting sending updates to client, this is to avoid the scenario where one thread is 
				//about to remove one state from the tree because we are visualizing it) and we come here and we see there is one timestep so we go ahead but by
				//the time we are trying to visualise it there is nothing there because the previous thread completed. In this way we wait to have at least two buffered.
				if(countTimeStepsVisitor.getNumberOfTimeSteps()>2 || _sessionContext.getSimulatorRuntimeByAspect(aspectID).getUpdatesProcessed()==0)
				{
					logger.info("Available update found ");
					updateAvailable = true;
					// create scene
					Scene scene;
					try
					{
						scene = _sessionContext.getConfigurationByAspect(aspectID).getModelInterpreter().getSceneFromModel(_sessionContext.getSimulatorRuntimeByAspect(aspectID).getModel(), stateTree);
						ObjectMapper mapper = new ObjectMapper();
						sb.append(mapper.writer().writeValueAsString(scene));
						_sessionContext.getSimulatorRuntimeByAspect(aspectID).updateProcessed();
					}
					catch(ModelInterpreterException e)
					{
						throw new GeppettoExecutionException(e);
					}
					catch(JsonProcessingException e)
					{
						throw new GeppettoExecutionException(e);
					}
				}
			}
		}

		if(updateAvailable)
		{
			logger.info("Update sent to listener");
			_simulationListener.updateReady(sb.toString());			
		}
	}
	
	/**
	 * @param simConfig
	 * @throws InvalidSyntaxException
	 */
	private void populateDiscoverableServices(Simulation simConfig) throws GeppettoInitializationException
	{
		for(Aspect aspect : simConfig.getAspects())
		{
			String id = aspect.getId();
			String modelInterpreterId = aspect.getModelInterpreter();
			String simulatorId = aspect.getSimulator();
			String modelURL = aspect.getModelURL();

			IModelInterpreter modelInterpreter = this.<IModelInterpreter> getService(modelInterpreterId, IModelInterpreter.class.getName());			
			ISimulator simulator = this.<ISimulator> getService(simulatorId, ISimulator.class.getName());

			// populate context
			_sessionContext.addAspectId(id, modelInterpreter, simulator, modelURL);
		}
	}
	
	
	/*
	 * A generic routine to encapsulate boiler-plate code for dynamic service discovery
	 */
	/**
	 * @param discoveryId
	 * @param type
	 * @return
	 * @throws InvalidSyntaxException
	 */
	private <T> T getService(String discoveryId, String type) throws GeppettoInitializationException
	{
		T service = null;

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
			service = (T) _bc.getService(sr[0]);
		}

		if(service == null)
		{
			throw new GeppettoInitializationException("No service found for id:"+discoveryId);
		}
		return service;
	}
}
