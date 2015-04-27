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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.model.ModelInterpreterException;
import org.geppetto.core.model.runtime.AspectSubTreeNode;
import org.geppetto.core.model.runtime.RuntimeTreeRoot;
import org.geppetto.core.model.simulation.GeppettoModel;
import org.geppetto.core.model.state.visitors.SerializeTreeVisitor;
import org.geppetto.core.model.state.visitors.SerializeUpdateSimulationTreeVisitor;
import org.geppetto.core.simulation.ISimulation;
import org.geppetto.core.simulation.ISimulationCallbackListener;
import org.geppetto.core.simulation.ISimulationCallbackListener.SimulationEvents;
import org.geppetto.simulation.visitor.CreateModelInterpreterServicesVisitor;
import org.geppetto.simulation.visitor.CreateRuntimeTreeVisitor;
import org.geppetto.simulation.visitor.ExitVisitor;
import org.geppetto.simulation.visitor.InstancePathDecoratorVisitor;
import org.geppetto.simulation.visitor.LoadSimulationVisitor;
import org.geppetto.simulation.visitor.ParentsDecoratorVisitor;
import org.geppetto.simulation.visitor.PopulateModelTreeVisitor;
import org.geppetto.simulation.visitor.PopulateSimulationTreeVisitor;
import org.geppetto.simulation.visitor.PopulateVisualTreeVisitor;
import org.geppetto.simulation.visitor.SetParametersVisitor;
import org.geppetto.simulation.visitor.WriteModelVisitor;
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
	public void init(URL simConfigURL, String requestID, ISimulationCallbackListener simulationListener) throws GeppettoInitializationException
	{
		long start = System.currentTimeMillis();
		long end = System.currentTimeMillis();

		try
		{
			_logger.info("Initializing simulation");
			GeppettoModel sim = SimulationConfigReader.readConfig(simConfigURL);
			_simulationListener = simulationListener;
			end = System.currentTimeMillis();
			_logger.info("Reading configuration file, took " + (end - start) + " ms ");

			load(sim, requestID);
		}
		catch(GeppettoInitializationException e)
		{
			_logger.error("Error: ", e);
			throw new GeppettoInitializationException("Error Loading Simulation Model");
		}
		
		end = System.currentTimeMillis();
		_logger.info("Total initialization time took " + (end - start) + " ms ");
	}

	/**
	 * Initializes simulation with JSON object containing simulation.
	 * 
	 * @throws ModelInterpreterException
	 */
	@Override
	public void init(String simulationConfig, String requestID, ISimulationCallbackListener simulationListener) throws GeppettoInitializationException
	{
		long start = System.currentTimeMillis();
		long end = System.currentTimeMillis();
		_logger.info("Reading configuration file, took " + (end - start) + " ms ");
		try
		{
			_logger.info("Initializing simulation");
			GeppettoModel simulation = SimulationConfigReader.readSimulationConfig(simulationConfig);
			_simulationListener = simulationListener;
			end = System.currentTimeMillis();
			_logger.info("Reading configuration file, took " + (end - start) + " ms ");
			load(simulation, requestID);
		}
		catch(GeppettoInitializationException e)
		{
			_logger.error("Error: ", e);
			throw new GeppettoInitializationException("Error Loading Simulation Model");
		}
		
		end = System.currentTimeMillis();
		_logger.info("Total initialization time took " + (end - start) + " ms ");
	}

	/**
	 * @param simulation
	 * @throws GeppettoInitializationException
	 * @throws GeppettoExecutionException
	 * @throws ModelInterpreterException
	 */
	public void load(GeppettoModel simulation, String requestID) throws GeppettoInitializationException
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

		_sessionContext.setSimulation(simulation);

		// retrieve model interpreters and simulators
		CreateModelInterpreterServicesVisitor createServicesVisitor = new CreateModelInterpreterServicesVisitor(_sessionContext, _simulationListener);
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

		updateClientWithSimulation(requestID);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulation.ISimulation#start()
	 */
	@Override
	public void start(String requestId)
	{
		_logger.info("Starting simulation");

		_simulationThread = new SimulationThread(_sessionContext, _simulationListener, requestId, appConfig.getUpdateCycle());
		_simulationThread.start();
		_sessionContext.setSimulationStatus(SimulationRuntimeStatus.RUNNING);
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

		_sessionContext.setSimulationStatus(SimulationRuntimeStatus.STOPPED);

		// join threads prior to stopping to avoid concurentmodification exceptions of watchtree
		try
		{
			if(_simulationThread != null)
			{
				_simulationThread.join();
			}
		}
		catch(InterruptedException e)
		{
			_logger.error("Error: ", e);
		}

		_logger.warn("Stopping simulation ");
		// tell the thread to stop running the simulation

		// revert simulation to initial conditions
		_sessionContext.revertToInitialConditions();

		// iterate through aspects and instruct them to stop
		// SIM TODO
//		for(ISimulator simulator : _sessionContext.getSimulators().values())
//		{
//			if(simulator != null)
//			{
//				simulator.setInitialized(false);
//			}
//		}
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

	public ISimulationCallbackListener getSimulationCallbackListener()
	{
		return this._simulationListener;
	}

	@Override
	public void setWatchedVariables(List<String> watchedVariables) throws GeppettoExecutionException, GeppettoInitializationException
	{
		//Update the RunTimeTreeModel
		SerializeUpdateSimulationTreeVisitor iterateWatchableVariableListVisitor = new SerializeUpdateSimulationTreeVisitor(watchedVariables);
		this._sessionContext.getRuntimeTreeRoot().apply(iterateWatchableVariableListVisitor);

		//SIM TODO
		//Call the function for each simulator
//		for(Simulator simulatorModel : _sessionContext.getSimulators().keySet())
//		{
//			ISimulator simulator = _sessionContext.getSimulator(simulatorModel);
//			IVariableWatchFeature watchFeature = ((IVariableWatchFeature) simulator.getFeature(GeppettoFeature.VARIABLE_WATCH_FEATURE));
//			if(watchFeature != null)
//			{
//				watchFeature.setWatchedVariables(watchedVariables);
//			}
//		}
	}



	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulation.ISimulation#clearWatchLists()
	 */
	@Override
	public void clearWatchLists()
	{
		//Update the RunTimeTreeModel setting watched to false for every node
		SerializeUpdateSimulationTreeVisitor iterateWatchableVariableListVisitor = new SerializeUpdateSimulationTreeVisitor();
		iterateWatchableVariableListVisitor.setMode("setWatched");
		this._sessionContext.getRuntimeTreeRoot().apply(iterateWatchableVariableListVisitor);
		
		//SIM TODO
		// instruct aspects to clear watch variables
//		for(ISimulator simulator : _sessionContext.getSimulators().values())
//		{
//			if(simulator != null)
//			{
//				IVariableWatchFeature watchFeature = ((IVariableWatchFeature) simulator.getFeature(GeppettoFeature.VARIABLE_WATCH_FEATURE));
//				if(watchFeature != null)
//				{
//					watchFeature.clearWatchVariables();
//				}
//			}
//		}

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
	 * 
	 * @param event
	 * @throws GeppettoExecutionException
	 * @throws ModelInterpreterException
	 * 
	 */
	private void updateClientWithSimulation(String requestID)
	{

		SerializeTreeVisitor updateClientVisitor = new SerializeTreeVisitor();
		_sessionContext.getRuntimeTreeRoot().apply(updateClientVisitor);

		ExitVisitor exitVisitor = new ExitVisitor(_simulationListener);
		_sessionContext.getRuntimeTreeRoot().apply(exitVisitor);

		String scene = updateClientVisitor.getSerializedTree();

		if(scene != null)
		{
			_simulationListener.updateReady(SimulationEvents.LOAD_MODEL, requestID, scene);
			_logger.info("Simulation sent to callback listener");
		}
	}

	/**
	 * @param simConfig
	 * @throws InvalidSyntaxException
	 */
	private void populateScripts(GeppettoModel simConfig) throws GeppettoInitializationException
	{
		// clear local scripts variable
		_scripts.clear();

		// SIM TODO
//		for(String script : simConfig.getScript())
//		{
//			URL scriptURL = null;
//			try
//			{
//				scriptURL = new URL(script);
//			}
//			catch(MalformedURLException e)
//			{
//				throw new GeppettoInitializationException("Malformed script url " + script, e);
//			}
//			_scripts.add(scriptURL);
//		}
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
	 * Takes the id of aspect, and uses that to populate it's corresponding aspect node with nodes for the model tree.
	 * 
	 * @param aspectID
	 * @return
	 */
	@Override
	public String getModelTree(String instancePath)
	{
		PopulateModelTreeVisitor populateModelVisitor = new PopulateModelTreeVisitor(_simulationListener, instancePath);
		this._sessionContext.getRuntimeTreeRoot().apply(populateModelVisitor);

		String modelTree = "[";
		for(Map.Entry<String, AspectSubTreeNode> entry : populateModelVisitor.getPopulatedModelTree().entrySet())
		{
			SerializeTreeVisitor updateClientVisitor = new SerializeTreeVisitor();
			entry.getValue().apply(updateClientVisitor);
			modelTree += "{\"aspectInstancePath\":" + '"' + entry.getKey() + '"' + ",\"modelTree\":{" + updateClientVisitor.getSerializedTree() + "} },";
		}
		modelTree = modelTree.substring(0, modelTree.length() - 1);
		modelTree += "]";

		return modelTree;
	}

	/**
	 * Takes the id of aspect and the file format and write the model to this format
	 * 
	 * @param aspectID
	 * @param format
	 * @return
	 */
	@Override
	public String writeModel(String instancePath, String format)
	{
		WriteModelVisitor writeModelVisitor = new WriteModelVisitor(_simulationListener, instancePath, format);
		this._sessionContext.getRuntimeTreeRoot().apply(writeModelVisitor);

		// Read returned value

		return "";
	}

	@Override
	public boolean setParameters(String model, Map<String, String> parameters) {
		//TODO: Take code in visitor and put here instead once AspectConfiguration 
		//in in place. We will be able to get modelinterpreter from there instead
		//of having to use visitor to call setParameter of model services.
		SetParametersVisitor parameterVisitor = new SetParametersVisitor(_simulationListener,parameters, model);

		return _sessionContext.getRuntimeTreeRoot().apply(parameterVisitor);
	}	

	public String getSimulationTree(String instancePath)
	{
		PopulateSimulationTreeVisitor populateSimulationVisitor = new PopulateSimulationTreeVisitor(_simulationListener, instancePath);
		this._sessionContext.getRuntimeTreeRoot().apply(populateSimulationVisitor);

		String simulationTree = "[";
		for(Map.Entry<String, AspectSubTreeNode> entry : populateSimulationVisitor.getPopulatedSimulationTree().entrySet())
		{
			SerializeTreeVisitor updateClientVisitor = new SerializeTreeVisitor();
			entry.getValue().apply(updateClientVisitor);
			simulationTree += "{\"aspectInstancePath\":" + '"' + entry.getKey() + '"' + ",\"simulationTree\":{" + updateClientVisitor.getSerializedTree() + "} },";
		}
		simulationTree = simulationTree.substring(0, simulationTree.length() - 1);
		simulationTree += "]";

		return simulationTree;
	}
}
