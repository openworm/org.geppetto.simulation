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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.model.IModel;
import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.model.runtime.ACompositeNode;
import org.geppetto.core.model.runtime.ANode;
import org.geppetto.core.model.runtime.AspectNode;
import org.geppetto.core.model.runtime.AspectSubTreeNode.AspectTreeType;
import org.geppetto.core.model.runtime.EntityNode;
import org.geppetto.core.model.runtime.RuntimeTreeRoot;
import org.geppetto.core.model.simulation.GeppettoModel;
import org.geppetto.core.model.simulation.Model;

/**
 * This class stores the context of a given session.
 * Each user has its own session. Each session has its own simulation.
 * 
 * @author matteocantarelli
 * 
 */
public class SessionContextOld
{

	// The maximum number of steps that can be stored.
	// Note: this affects all the simulators
	private int _maxBufferSize = 100;

	// This are the services that have been created for this simulation
	private ConcurrentHashMap<Model, IModelInterpreter> _modelInterpreters = new ConcurrentHashMap<Model, IModelInterpreter>();
	// SIM TODO
//	private ConcurrentHashMap<Simulator, ISimulator> _simulators = new ConcurrentHashMap<Simulator, ISimulator>();
//	private ConcurrentHashMap<Simulator, IConversion> _conversions = new ConcurrentHashMap<Simulator, IConversion>();

	// This map contains the simulator runtime for each one of the simulators
	private ConcurrentHashMap<String, SimulatorRuntime> _simulatorRuntimes = new ConcurrentHashMap<String, SimulatorRuntime>();

	// The string in the map below is the instancepath for a specific model specified in a simulation file
	private ConcurrentHashMap<String, IModel> _models = new ConcurrentHashMap<String, IModel>();

	// This map caches which models are been executed for a given simulatore
	// SIM TODO
//	private ConcurrentHashMap<Simulator, List<Model>> _simulatorToModels = new ConcurrentHashMap<Simulator, List<Model>>();
//
//	// This map caches for each model what simulator is responsible for its simulation
//	private ConcurrentHashMap<Model, Simulator> _modelToSimulator = new ConcurrentHashMap<Model, Simulator>();

	// This is the Simulation tree that was loaded from the simulation file
	private GeppettoModel _simulation;

	// The logger
	private static Log _logger = LogFactory.getLog(SessionContextOld.class);

	// The status of the current simulation
	private SimulationRuntimeStatus _status = SimulationRuntimeStatus.IDLE;

	// TODO: comment this out
	// Head node that holds the entities
//	private RuntimeTreeRoot _runtimeTreeRoot = new RuntimeTreeRoot("scene");
//
//	public RuntimeTreeRoot getRuntimeTreeRoot()
//	{
//		return _runtimeTreeRoot;
//	}

	/**
	 * @return
	 */
	public SimulationRuntimeStatus getStatus()
	{
		return _status;
	}

	/**
	 * @param status
	 */
	public void setSimulationStatus(SimulationRuntimeStatus status)
	{
		_status = status;
	}

	/**
	 * Reverts the simulation state to initial conditions
	 */
	public void revertToInitialConditions()
	{

		setSimulationStatus(SimulationRuntimeStatus.IDLE);
		// for each aspect, revert runtime to initial conditions
		for(SimulatorRuntime simulatorRuntime : _simulatorRuntimes.values())
		{
			simulatorRuntime.revertToInitialConditions();
		}

//		this.resetRuntimeTree(this.getRuntimeTreeRoot().getChildren());
		
		// iterate through aspects and instruct them to start watching
		// SIM TODO
//		for(ISimulator simulator : getSimulators().values())
//		{
//			if(simulator != null)
//			{
//				simulator.resetWatch();
//			}
//		}
		_logger.info("Simulation reverted to initial conditions");
	}

	/**
	 * Resets the visualization and simulation tree for each aspect. 
	 * Used when resetting simulation after stopping it. 
	 * 
	 * @param nodes
	 */
	private void resetRuntimeTree(List<ANode> nodes)
	{
		for(ANode node : nodes)
		{
			if(node instanceof EntityNode)
			{
				if(((EntityNode) node).getChildren().size() > 0)
				{
					resetRuntimeTree(((ACompositeNode) node).getChildren());
				}
				else
				{
					for(AspectNode a : ((EntityNode) node).getAspects())
					{
						a.flushSubTree(AspectTreeType.VISUALIZATION_TREE);
						a.flushSubTree(AspectTreeType.SIMULATION_TREE);
					}
				}
			}
		}
	}

	/**
	 * Resets the simulation context
	 * NOTE: WIPES EVERYTHING
	 */
	public void reset()
	{
		_simulatorRuntimes.clear();
		_modelInterpreters.clear();
		// SIM TODO
//		_simulatorToModels.clear();
//		_modelToSimulator.clear();
//		_simulators.clear();
//		_conversions.clear();
		_models.clear();
		_simulation = null;
//		_runtimeTreeRoot = new RuntimeTreeRoot("scene");
		setSimulationStatus(SimulationRuntimeStatus.IDLE);
	}

	/**
	 * @return
	 */
	public int getMaxBufferSize()
	{
		return _maxBufferSize;
	}

	/**
	 * @param simulatorModel
	 * @return
	 */
	public SimulatorRuntime getSimulatorRuntime(String simulatorModel)
	{
		return _simulatorRuntimes.get(simulatorModel);
	}

	/**
	 * @param maxBufferSize
	 */
	public void setMaxBufferSize(int maxBufferSize)
	{
		_maxBufferSize = maxBufferSize;
	}

	/**
	 * @param simulatorModel
	 */
	public void addSimulatorRuntime(String simulatorModel)
	{
		SimulatorRuntime simulatorRuntime = new SimulatorRuntime();
		_simulatorRuntimes.put(simulatorModel, simulatorRuntime);
	}

	/**
	 * @return
	 */
	public GeppettoModel getSimulation()
	{
		return _simulation;
	}

	/**
	 * @param sim
	 */
	public void setSimulation(GeppettoModel simulation)
	{
		_simulation = simulation;
	}

	/**
	 * @param model
	 * @return
	 * @throws GeppettoInitializationException
	 */
	public IModelInterpreter getModelInterpreter(Model model) throws GeppettoInitializationException
	{
		if(!_modelInterpreters.containsKey(model))
		{
			throw new GeppettoInitializationException("The model interpreter for " + model.getInstancePath() + " was not found");
		}
		return _modelInterpreters.get(model);
	}

	/**
	 * @param simulator
	 * @return
	 * @throws GeppettoInitializationException
	 */
	// SIM TODO
//	public ISimulator getSimulator(Simulator simulatorModel) throws GeppettoInitializationException
//	{
//		if(!_simulators.containsKey(simulatorModel))
//		{
//			throw new GeppettoInitializationException("The simulator for " + simulatorModel.getInstancePath() + " was not found");
//		}
//		return _simulators.get(simulatorModel);
//	}

	/**
	 * @param simulator
	 * @return
	 * @throws GeppettoInitializationException
	 */
	// SIM TODO
//	public IConversion getConversion(Simulator simulatorModel) throws GeppettoInitializationException
//	{
//		return _conversions.get(simulatorModel);
//	}

	/**
	 * @param model
	 * @return
	 */
	public IModel getIModel(String instancePath)
	{
		if(!_models.containsKey(instancePath))
		{
			return null;
		}
		return _models.get(instancePath);
	}

	/**
	 * @return
	 */
	public Map<Model, IModelInterpreter> getModelInterpreters()
	{
		return _modelInterpreters;
	}

	/**
	 * @return
	 */
	// SIM TODO
//	public Map<Simulator, ISimulator> getSimulators()
//	{
//		return _simulators;
//	}
//
//	/**
//	 * @return
//	 */
//	public Map<Simulator, IConversion> getConversions()
//	{
//		return _conversions;
//	}

	/**
	 * @return
	 */
	public Map<String, IModel> getModels()
	{
		return _models;
	}

	/**
	 * @param model
	 * @return
	 */
	public IModel getIModel(Model model)
	{
		return _models.get(model.getInstancePath());
	}

	/**
	 * Maps the instance path of an aspect to a Simulator which is used to simulate that specific model
	 * 
	 * @param instancePath
	 * @param simulator
	 */
	// SIM TODO
//	public void mapSimulatorToModels(Simulator simulator, List<Model> models)
//	{
//		_simulatorToModels.put(simulator, models);
//	}

	/**
	 * @param modelInstancePath
	 * @return
	 */
	// SIM TODO
//	public List<Model> getModelsFromSimulator(Simulator simulator)
//	{
//		return _simulatorToModels.get(simulator);
//	}

	/**
	 * @param model
	 * @param simulator
	 */
	// SIM TODO
//	public void mapModelToSimulator(Model model, Simulator simulator)
//	{
//		_modelToSimulator.put(model, simulator);
//
//	}

	/**
	 * @param model
	 * @return
	 */
	// SIM TODO
//	public Simulator getSimulatorFromModel(Model model)
//	{
//		return _modelToSimulator.get(model);
//	}

}
