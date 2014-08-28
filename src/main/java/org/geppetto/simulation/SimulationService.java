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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.data.model.AVariable;
import org.geppetto.core.data.model.SimpleVariable;
import org.geppetto.core.data.model.VariableList;
import org.geppetto.core.data.model.WatchList;
import org.geppetto.core.model.ModelInterpreterException;
import org.geppetto.core.model.runtime.RuntimeTreeRoot;
import org.geppetto.core.model.simulation.Model;
import org.geppetto.core.model.simulation.Simulation;
import org.geppetto.core.model.simulation.Simulator;
import org.geppetto.core.model.state.visitors.SerializeTreeVisitor;
import org.geppetto.core.simulation.ISimulation;
import org.geppetto.core.simulation.ISimulationCallbackListener;
import org.geppetto.core.simulation.ISimulationCallbackListener.SimulationEvents;
import org.geppetto.core.simulator.ISimulator;
import org.geppetto.simulation.visitor.CreateRuntimeTreeVisitor;
import org.geppetto.simulation.visitor.CreateSimulationServicesVisitor;
import org.geppetto.simulation.visitor.InstancePathDecoratorVisitor;
import org.geppetto.simulation.visitor.LoadSimulationVisitor;
import org.geppetto.simulation.visitor.ParentsDecoratorVisitor;
import org.geppetto.simulation.visitor.PopulateModelTreeVisitor;
import org.geppetto.simulation.visitor.PopulateVisualTreeVisitor;
import org.osgi.framework.InvalidSyntaxException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SimulationService implements ISimulation
{

	@Autowired 
	public AppConfig appConfig;
	private static Log _logger = LogFactory.getLog(SimulationService.class);
	private final SessionContext _sessionContext = new SessionContext();
	private SimulationThread _simulationThread;
	private ISimulationCallbackListener _simulationListener;
	private List<WatchList> _watchLists = new ArrayList<WatchList>();
	private boolean _watching = false;
	private List<URL> _scripts = new ArrayList<URL>();

	/**
	 * 
	 */
	public SimulationService()
	{
		_logger.info("New Simulation Service created");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulation.ISimulation#init(java.net.URL, org.geppetto.core.simulation.ISimulationCallbackListener)
	 */
	@Override
	public void init(URL simConfigURL, ISimulationCallbackListener simulationListener) throws GeppettoInitializationException
	{
		_logger.info("Initializing simulation");
		Simulation sim = SimulationConfigReader.readConfig(simConfigURL);
		_simulationListener = simulationListener;

		try
		{
			load(sim);
		}
		catch(GeppettoInitializationException e)
		{
			throw new GeppettoInitializationException("Error Loading Simulation Model");
		}
	}

	/**
	 * Initializes simulation with JSON object containing simulation.
	 * @throws ModelInterpreterException 
	 */
	@Override
	public void init(String simulationConfig, ISimulationCallbackListener simulationListener) throws GeppettoInitializationException
	{
		Simulation simulation = SimulationConfigReader.readSimulationConfig(simulationConfig);
		_simulationListener = simulationListener;

		try
		{
			load(simulation);
		}
		catch(GeppettoInitializationException e)
		{
			throw new GeppettoInitializationException("Error Loading Simulation Model");
		}
	}

	/**
	 * @param simulation
	 * @throws GeppettoInitializationException
	 * @throws GeppettoExecutionException
	 * @throws ModelInterpreterException 
	 */
	public void load(Simulation simulation) throws GeppettoInitializationException
	{

		// refresh simulation context
		_sessionContext.reset();
		
		// decorate Simulation model
		InstancePathDecoratorVisitor instancePathdecoratorVisitor = new InstancePathDecoratorVisitor();
		simulation.accept(instancePathdecoratorVisitor);
		ParentsDecoratorVisitor parentDecoratorVisitor = new ParentsDecoratorVisitor();
		simulation.accept(parentDecoratorVisitor);

		// clear watch lists
		this.clearWatchLists();
		if(_watching)
		{
			// stop the watching - will cause all previous stored watch values to be flushed
			this.stopWatch();
		}

		_sessionContext.setSimulation(simulation);

		// retrieve model interpreters and simulators
		CreateSimulationServicesVisitor createServicesVisitor = new CreateSimulationServicesVisitor(_sessionContext, _simulationListener);
		simulation.accept(createServicesVisitor);

		populateScripts(simulation);

		_sessionContext.setMaxBufferSize(appConfig.getMaxBufferSize());
		
		LoadSimulationVisitor loadSimulationVisitor = new LoadSimulationVisitor(_sessionContext, _simulationListener);
		simulation.accept(loadSimulationVisitor);
		
		CreateRuntimeTreeVisitor runtimeTreeVisitor = new CreateRuntimeTreeVisitor(_sessionContext, _simulationListener);
		simulation.accept(runtimeTreeVisitor);
		
		RuntimeTreeRoot runtimeModel = runtimeTreeVisitor.getRuntimeModel();
		
		PopulateVisualTreeVisitor populateVisualVisitor = new PopulateVisualTreeVisitor(_simulationListener);
		runtimeModel.apply(populateVisualVisitor);
		
		updateClientWithSimulation();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulation.ISimulation#start()
	 */
	@Override
	public void start()
	{
		_logger.info("Starting simulation");

		_sessionContext.setSimulationStatus(SimulationRuntimeStatus.RUNNING);
		_simulationThread = new SimulationThread(_sessionContext,_simulationListener, appConfig.getUpdateCycle());
		_simulationThread.start();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulation.ISimulation#stop()
	 */
	@Override
	public void pause()
	{
		_logger.info("Pausing simulation");
		// tell the thread to pause the simulation, but don't stop it
		_sessionContext.setSimulationStatus(SimulationRuntimeStatus.PAUSED);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulation.ISimulation#stop()
	 */
	@Override
	public void stop() throws GeppettoExecutionException
	{

		_logger.warn("Stopping simulation");
		// tell the thread to stop running the simulation

		// revert simulation to initial conditions
		_sessionContext.revertToInitialConditions();
		
		_sessionContext.setSimulationStatus(SimulationRuntimeStatus.STOPPED);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulation.ISimulation#isRunning()
	 */
	@Override
	public boolean isRunning()
	{
		return _sessionContext.getStatus().equals(SimulationRuntimeStatus.RUNNING);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulation.ISimulation#getSimulationConfig(java.net.URL)
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

		for(Simulator simulatorModel : _sessionContext.getSimulators().keySet())
		{
			ISimulator simulator=_sessionContext.getSimulators().get(simulatorModel);
			if(simulator != null)
			{
				SimpleVariable v = new SimpleVariable();
				v.setAspect("aspect");
				v.setName(simulatorModel.getParentAspect().getId());
				
				vars.add(v);
				vars.addAll(isWatch ? simulator.getWatchableVariables().getVariables() : simulator.getForceableVariables().getVariables());
			}
		}

		varsList.setVariables(vars);

		return varsList;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulation.ISimulation#addWatchLists(java.util.List)
	 */
	@Override
	public void addWatchLists(List<WatchList> lists) throws GeppettoExecutionException, GeppettoInitializationException
	{
		// add to local container
		_watchLists.addAll(lists);

		// iterate through aspects and set variables to be watched for each
		for(Simulator simulatorModel : _sessionContext.getSimulators().keySet())
		{

			ISimulator simulator = _sessionContext.getSimulator(simulatorModel);

			if(simulator != null)
			{
				List<String> variableNames = new ArrayList<String>();

				for(WatchList list : lists)
				{
					for(String varPath : list.getVariablePaths())
					{

						for(Model model : _sessionContext.getModelsFromSimulator(simulatorModel))
						{
							// A variable watch belongs to a specific simulator if the simulator
							// is responsible for the specific model where this variable comes from
							// The instance path here is used to perform this check
							if(varPath.startsWith(model.getInstancePath()))
							{
								variableNames.add(varPath);
							}
						}
					}
				}
				simulator.addWatchVariables(variableNames);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulation.ISimulation#startWatch()
	 */
	@Override
	public void startWatch()
	{
		// set local watch flag
		_watching = true;

		// iterate through aspects and instruct them to start watching
		for(ISimulator simulator : _sessionContext.getSimulators().values())
		{
			if(simulator != null)
			{
				simulator.startWatch();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulation.ISimulation#stopWatch()
	 */
	@Override
	public void stopWatch()
	{
		// set local watch flag
		_watching = false;

		// iterate through aspects and instruct them to stop watching
		for(ISimulator simulator : _sessionContext.getSimulators().values())
		{

			if(simulator != null)
			{
				// stop watch and reset state tree for variable watch for each simulator
				simulator.stopWatch();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulation.ISimulation#clearWatchLists()
	 */
	@Override
	public void clearWatchLists()
	{
		// stop watching - wills top all simulators and clear watch data for each
		this.stopWatch();

		// instruct aspects to clear watch variables
		for(ISimulator simulator : _sessionContext.getSimulators().values())
		{
			if(simulator != null)
			{
				simulator.clearWatchVariables();
			}
		}

		// clear locally stored watch lists
		_watchLists.clear();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulation.ISimulation#getWatchLists()
	 */
	@Override
	public List<WatchList> getWatchLists()
	{
		return _watchLists;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulation.ISimulation#getScripts()
	 */
	@Override
	public List<URL> getScripts()
	{
		return _scripts;
	}

	/**
	 * Method that takes the oldest model in the buffer and send it to the client
	 * @param event 
	 * @throws GeppettoExecutionException 
	 * @throws ModelInterpreterException 
	 * 
	 */
	private void updateClientWithSimulation() 
	{
	
		SerializeTreeVisitor updateClientVisitor = new SerializeTreeVisitor();
		_sessionContext.getRuntimeTreeRoot().apply(updateClientVisitor);

		String scene = updateClientVisitor.getSerializedTree();
		if(scene!=null){
			_simulationListener.updateReady(SimulationEvents.LOAD_MODEL, scene);
			_logger.info("Simulation sent to callback listener");
		}
	}

	/**
	 * @param simConfig
	 * @throws InvalidSyntaxException
	 */
	private void populateScripts(Simulation simConfig) throws GeppettoInitializationException
	{
		// clear local scripts variable
		_scripts.clear();

		for(String script : simConfig.getScript())
		{
			URL scriptURL = null;
			try
			{
				scriptURL = new URL(script);
			}
			catch(MalformedURLException e)
			{
				throw new GeppettoInitializationException("Malformed script url " + script, e);
			}
			_scripts.add(scriptURL);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulation.ISimulation#getSimulatorName()
	 */
	@Override
	public String getSimulatorName()
	{
		String simulatorName = "Simulation";
 
		return simulatorName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulation.ISimulation#getSimulationCapacity()
	 */
	@Override
	public int getSimulationCapacity()
	{

		int capacity = this.appConfig.getSimulationCapacity();

		return capacity;
	}
	
	/**
	 * Takes the id of aspect, and uses that to populate it's corresponding aspect node with nodes
	 * for the model tree.
	 * 
	 * @param aspectID
	 * @return 
	 */
	@Override
	public String getModelTree(String instancePath){
		PopulateModelTreeVisitor populateModelVisitor = new PopulateModelTreeVisitor(_simulationListener, instancePath);
		this._sessionContext.getRuntimeTreeRoot().apply(populateModelVisitor);
		
		SerializeTreeVisitor updateClientVisitor = new SerializeTreeVisitor();
		populateModelVisitor.getPopulatedModelTree().apply(updateClientVisitor);

		String modelTree = updateClientVisitor.getSerializedTree();
		
		return modelTree;
	}
}
