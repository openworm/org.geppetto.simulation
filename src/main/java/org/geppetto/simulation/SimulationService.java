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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.data.model.AVariable;
import org.geppetto.core.data.model.SimpleVariable;
import org.geppetto.core.data.model.VariableList;
import org.geppetto.core.data.model.WatchList;
import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.model.ModelInterpreterException;
import org.geppetto.core.model.state.CompositeStateNode;
import org.geppetto.core.model.state.StateTreeRoot;
import org.geppetto.core.model.state.StateTreeRoot.SUBTREE;
import org.geppetto.core.model.state.visitors.CountTimeStepsVisitor;
import org.geppetto.core.model.state.visitors.SerializeTreeVisitor;
import org.geppetto.core.simulation.ISimulation;
import org.geppetto.core.simulation.ISimulationCallbackListener;
import org.geppetto.core.simulation.ISimulationCallbackListener.SimulationEvents;
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
import com.fasterxml.jackson.databind.module.SimpleModule;

@Service
public class SimulationService implements ISimulation
{

	@Autowired
	public AppConfig appConfig;

	BundleContext _bc = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

	private static Log logger = LogFactory.getLog(SimulationService.class);

	private final SessionContext _sessionContext = new SessionContext();

	private Timer _clientUpdateTimer;

	private SimulationThread _simThread;

	private ISimulationCallbackListener _simulationListener;

	private List<WatchList> _watchLists = new ArrayList<WatchList>();

	private boolean _watching = false;

	private List<URL> _scripts = new ArrayList<URL>();
	
	private double _globalTime = 0.00;
	
	private double _globalTimeStep = 0.00;

	public SimulationService(){
		logger.warn("New Simulation Service created");
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulation.ISimulation#init(java.net.URL, org.geppetto.core.simulation.ISimulationCallbackListener)
	 */
	@Override
	public void init(URL simConfigURL, ISimulationCallbackListener simulationListener) throws GeppettoInitializationException
	{		
		logger.warn("Initializing simulation");
		Simulation sim = SimulationConfigReader.readConfig(simConfigURL);
		_simulationListener = simulationListener;

		try
		{
			load(sim);
		}
		catch(GeppettoExecutionException e)
		{
			// TODO Auto-generated catch block
			throw new GeppettoInitializationException("Error Loading Simulation Model");
		}
	}

	/**
	 * Initializes simulation with JSON object containing simulation.
	 */
	@Override
	public void init(String simulationConfig, ISimulationCallbackListener simulationListener) throws GeppettoInitializationException
	{
		Simulation sim = SimulationConfigReader.readSimulationConfig(simulationConfig);
		_simulationListener = simulationListener;

		try
		{
			load(sim);
		}
		catch(GeppettoExecutionException e)
		{
			// TODO Auto-generated catch block
			throw new GeppettoInitializationException("Error Loading Simulation Model");
		}
	}

	public void load(Simulation sim) throws GeppettoInitializationException, GeppettoExecutionException
	{
		// clear watch lists
		this.clearWatchLists();
		if(_watching)
		{
			// stop the watching - will cause all previous stored watch values to be flushed
			this.stopWatch();
		}

		// refresh simulation context
		_sessionContext.reset();

		// retrieve model interpreters and simulators
		populateDiscoverableServices(sim);
		
		populateScripts(sim);
		
		_sessionContext.setMaxBufferSize(appConfig.getMaxBufferSize());

		loadModel();
	}

	private void loadModel() throws GeppettoInitializationException, GeppettoExecutionException
	{
		_simThread = new SimulationThread(_sessionContext);
		_simThread.loadModel();
		try
		{
			update(SimulationEvents.LOAD_MODEL);
		}
		catch(GeppettoExecutionException e)
		{
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
		// start the simulation
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
	public void stop() throws GeppettoExecutionException
	{

		logger.warn("Stopping simulation");
		// tell the thread to stop running the simulation
		_sessionContext.setRunning(false);
		_sessionContext.setStopped(true);

		if(_clientUpdateTimer != null){
			// stop the timer that updates the client
			_clientUpdateTimer.cancel();
		}

		// revert simulation to initial conditions
		_sessionContext.revertToInitialConditions();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulation.ISimulation#isRunning()
	 */
	@Override
	public boolean isRunning()
	{
		return _sessionContext.isRunning();
	}

	/**
	 * Takes a URL corresponding to simulation file and extracts information.
	 * 
	 * @throws GeppettoInitializationException
	 */
	@Override
	public String getSimulationConfig(URL simURL) throws GeppettoInitializationException
	{
		String simulationConfig = SimulationConfigReader.writeSimulationConfig(simURL);

		return simulationConfig;
	}

	/**
	 * Gets the list of all watchable variables in a give simulation
	 */
	@Override
	public VariableList listWatchableVariables()
	{
		return this.listVariablesHelper(true);
	}

	/**
	 * Gets the list of all forceable variables in a give simulation
	 */
	@Override
	public VariableList listForceableVariables()
	{
		return this.listVariablesHelper(false);
	}

	/**
	 * Fetches variables from simulators
	 * 
	 * @param isWatch
	 *            specifies is the helper should fetch watch- or force-able variables
	 * @return
	 */
	public VariableList listVariablesHelper(boolean isWatch)
	{
		VariableList varsList = new VariableList();
		List<AVariable> vars = new ArrayList<AVariable>();

		for(String aspectID : _sessionContext.getAspectIds())
		{
			ISimulator simulator = _sessionContext.getConfigurationByAspect(aspectID).getSimulator();

			if(simulator != null)
			{
				SimpleVariable v = new SimpleVariable();
				v.setAspect("aspect");
				v.setName(aspectID);
				
				vars.add(v);
				vars.addAll(isWatch ? simulator.getWatchableVariables().getVariables() : simulator.getForceableVariables().getVariables());
			}
		}

		varsList.setVariables(vars);

		return varsList;
	}

	@Override
	public void addWatchLists(List<WatchList> lists) throws GeppettoExecutionException
	{
		// add to local container
		_watchLists.addAll(lists);

		// iterate through aspects and set variables to be watched for each
		for(String aspectID : _sessionContext.getAspectIds())
		{
			ISimulator simulator = _sessionContext.getConfigurationByAspect(aspectID).getSimulator();

			if(simulator != null)
			{
				List<String> variableNames = new ArrayList<String>();

				for(WatchList list : lists)
				{
					for(String varPath : list.getVariablePaths())
					{
						// parse to extract aspect id from variable path
						// NOTE: this kinda sucks
						String aspectIDFromPath = null;
						String nakedVarName = null;

						if(varPath.contains("."))
						{
							// Split it.
							String[] split = varPath.split("\\.", 2);

							if(split.length != 2)
							{
								throw new GeppettoExecutionException("Error parsing variable path: unexpected format.");
							}

							aspectIDFromPath = split[0];
							nakedVarName = split[1];
						}
						else
						{
							throw new GeppettoExecutionException("Error parsing variable path: unexpected format.");
						}

						// add only variables for the given aspect
						if(aspectID.equals(aspectIDFromPath))
						{
							// TODO: check that those variables actually exists before adding them for watch
							variableNames.add(nakedVarName);
						}
					}
				}

				simulator.addWatchVariables(variableNames);
			}
		}
	}

	@Override
	public void startWatch()
	{
		// set local watch flag
		_watching = true;

		// iterate through aspects and instruct them to start watching
		for(String aspectID : _sessionContext.getAspectIds())
		{
			ISimulator simulator = _sessionContext.getConfigurationByAspect(aspectID).getSimulator();

			if(simulator != null)
			{
				simulator.startWatch();
			}
		}
	}

	@Override
	public void stopWatch()
	{
		// set local watch flag
		_watching = false;

		// iterate through aspects and instruct them to stop watching
		for(String aspectID : _sessionContext.getAspectIds())
		{
			ISimulator simulator = _sessionContext.getConfigurationByAspect(aspectID).getSimulator();

			if(simulator != null)
			{
				// stop watch and reset state tree for variable watch for each simulator
				simulator.stopWatch();
			}
		}
	}

	@Override
	public void clearWatchLists()
	{
		// stop watching - wills top all simulators and clear watch data for each
		this.stopWatch();

		// instruct aspects to clear watch variables
		for(String aspectID : _sessionContext.getAspectIds())
		{
			ISimulator simulator = _sessionContext.getConfigurationByAspect(aspectID).getSimulator();

			if(simulator != null)
			{
				simulator.clearWatchVariables();
			}
		}

		// clear locally stored watch lists
		_watchLists.clear();
	}

	@Override
	public List<WatchList> getWatchLists()
	{
		return _watchLists;
	}
	
	@Override
	public List<URL> getScripts(){
		return _scripts;
	}

	/**
	 * Starts simulation thread - under the hood the run method of the thread gets invoked.
	 */
	private void startSimulationThread()
	{

		for(String aspectID : _sessionContext.getAspectIds())
		{
			// Load Model if it is still in initial conditions
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
					update(SimulationEvents.SCENE_UPDATE);
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
	 * @param event 
	 * 
	 * @throws JsonProcessingException
	 */
	private void update(SimulationEvents event) throws GeppettoExecutionException
	{
		StringBuilder sceneBuilder = new StringBuilder();
		String variableWatchTree = null;
		String time = null;
		boolean updateAvailable = false;

		for(String aspectID : _sessionContext.getAspectIds())
		{
			// get models Map for the given aspect String = modelId / List<IModel> = a given model at different time steps
			if(_sessionContext.getSimulatorRuntimeByAspect(aspectID).getStateTree() != null)
			{
				StateTreeRoot stateTree = _sessionContext.getSimulatorRuntimeByAspect(aspectID).getStateTree();			
				
				CountTimeStepsVisitor countTimeStepsVisitor = new CountTimeStepsVisitor();
				stateTree.apply(countTimeStepsVisitor);
				// we send data to the frontend if it's either the first cycle or if there is a change in the state, i.e. something that might produce a frontend update
				// putting the constraint to have at least two states buffered before starting sending updates to client, this is to avoid the scenario where one thread is
				// about to remove one state from the tree because we are visualizing it) and we come here and we see there is one timestep so we go ahead but by
				// the time we are trying to visualise it there is nothing there because the previous thread completed. In this way we wait to have at least two buffered.
				if(countTimeStepsVisitor.getNumberOfTimeSteps() > 2 || _sessionContext.getSimulatorRuntimeByAspect(aspectID).getUpdatesProcessed() == 0)
				{
					logger.info("Available update found ");
					updateAvailable = true;

					try
					{						
						
						if(_watching)
						{
							CompositeStateNode variableWatchRoot = stateTree.getSubTree(SUBTREE.WATCH_TREE);
							CountTimeStepsVisitor countWatchVisitor = new CountTimeStepsVisitor();
							variableWatchRoot.apply(countWatchVisitor);
							
							if(countWatchVisitor.getNumberOfTimeSteps() > 2)
							{
								// serialize state tree for variable watch and store in a string
								SerializeTreeVisitor visitor = new SerializeTreeVisitor();
								variableWatchRoot.apply(visitor);
								variableWatchTree = visitor.getSerializedTree();
							}
						}

						CompositeStateNode timeNode = stateTree.getSubTree(SUBTREE.TIME_STEP);

						if(timeNode.getChildren().size() > 0){
							// serialize state tree for variable watch and store in a string
							SerializeTreeVisitor timeVisitor = new SerializeTreeVisitor();
							timeNode.apply(timeVisitor);
							time = timeVisitor.getSerializedTree();	
						}
						
						// create scene
						Scene scene;
						scene = _sessionContext.getConfigurationByAspect(aspectID).getModelInterpreter().getSceneFromModel(_sessionContext.getSimulatorRuntimeByAspect(aspectID).getModel(), stateTree);
						ObjectMapper mapper = new ObjectMapper();
						
						//a custom serializer is used to change what precision is used when serializing doubles in the scene
						SimpleModule customSerializationModule = new SimpleModule("customSerializationModule");
						customSerializationModule.addSerializer(new CustomSerializer(Double.class)); // assuming serializer declares correct class to bind to
						mapper.registerModule(customSerializationModule);
						   
						sceneBuilder.append(mapper.writer().writeValueAsString(scene));
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
					catch(Exception e)
					{
						throw new GeppettoExecutionException(e);
					}
				}
			}
		}

		if(updateAvailable)
		{
			logger.info("Update sent to listener");
			_simulationListener.updateReady(event,sceneBuilder.toString(), variableWatchTree, time);
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
	
	/**
	 * @param simConfig
	 * @throws InvalidSyntaxException
	 */
	private void populateScripts(Simulation simConfig) throws GeppettoInitializationException
	{
		//clear local scripts variable
		_scripts.clear();
		
		for(String script : simConfig.getScript())
		{
			URL scriptURL = null;
			try {
				scriptURL = new URL(script);
			} catch (MalformedURLException e) {
				throw new GeppettoInitializationException("Malformed script url " + script);
			}
			
			_scripts.add(scriptURL);
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
			throw new GeppettoInitializationException("No service found for id:" + discoveryId);
		}
		return service;
	}

	/**
	 * Calculate global time, an average of all simulator
	 */
	private void updateTime(){
		//TODO
	}

	@Override
	public String getSimulatorName() {
		String simulatorName = "Simulation";
		
		return simulatorName;
	}
	@Override
	public int getSimulationCapacity() {
		
		int capacity = this.appConfig.getSimulationCapacity();
		
		return capacity;
	}
}
