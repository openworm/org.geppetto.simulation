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
package org.geppetto.simulation.visitor;

import java.util.HashMap;
import java.util.Map;

import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.model.ModelInterpreterException;
import org.geppetto.core.model.simulation.Aspect;
import org.geppetto.core.model.simulation.Entity;
import org.geppetto.core.model.simulation.Model;
import org.geppetto.core.model.simulation.Simulator;
import org.geppetto.core.model.state.CompositeStateNode;
import org.geppetto.core.model.state.SimpleStateNode;
import org.geppetto.core.model.state.StateTreeRoot;
import org.geppetto.core.model.state.StateTreeRoot.SUBTREE;
import org.geppetto.core.model.state.visitors.SerializeTreeVisitor;
import org.geppetto.core.model.values.AValue;
import org.geppetto.core.visualisation.model.CAspect;
import org.geppetto.core.visualisation.model.CEntity;
import org.geppetto.core.visualisation.model.CValue;
import org.geppetto.core.visualisation.model.Scene;
import org.geppetto.simulation.CustomSerializer;
import org.geppetto.simulation.SessionContext;
import org.geppetto.simulation.SimulatorRuntime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.massfords.humantask.BaseVisitor;
import com.massfords.humantask.TraversingVisitor;

/**
 * @author matteocantarelli
 * 
 */
public class BuildClientUpdateVisitor extends TraversingVisitor
{

	private SessionContext _sessionContext;

	private Scene _scene = new Scene();
	private CompositeStateNode _simulationStateTreeRoot = new CompositeStateNode("variable_watch");

	private CEntity _currentClientEntity = null;

	private Map<Simulator, AValue> _timeSteps = new HashMap<Simulator, AValue>();

	/**
	 * @param sessionContext
	 */
	public BuildClientUpdateVisitor(SessionContext sessionContext)
	{
		super(new DepthFirstTraverserEntitiesFirst(), new BaseVisitor());
		_sessionContext = sessionContext;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.massfords.humantask.TraversingVisitor#visit(org.geppetto.simulation.model.Aspect)
	 */
	@Override
	public void visit(Aspect aspect)
	{
		Model model = aspect.getModel();
		CEntity visualEntity = null;
		CAspect clientAspect = new CAspect();
		clientAspect.setId(aspect.getId());

		if(model != null)
		{
			try
			{
				IModelInterpreter modelInterpreter = _sessionContext.getModelInterpreter(model);
				Simulator simulator = _sessionContext.getSimulatorFromModel(model);
				StateTreeRoot stateTree = _sessionContext.getSimulatorRuntime(simulator).getStateTree();

				visualEntity = modelInterpreter.getVisualEntity(_sessionContext.getIModel(model.getInstancePath()), aspect, stateTree);

				if(visualEntity.getAspects().size() == 1)
				{
					// there is only going to be one aspect inside the entity that was returned by the model interpreter
					clientAspect.getVisualModel().addAll(visualEntity.getAspects().get(0).getVisualModel());

					// we add all the entities that were added by the model interpreter
					// the model specified for an entity in the Geppetto configuration file
					// could wrap inside multiple entities
					_currentClientEntity.getChildren().addAll(visualEntity.getChildren());
				}
				else
				{
					throw new RuntimeException(new GeppettoExecutionException("The visual entity returned by the model interpreter has more than one aspect"));
				}

			}
			catch(GeppettoInitializationException e)
			{
				throw new RuntimeException(e);
			}
			catch(ModelInterpreterException e)
			{
				throw new RuntimeException(e);
			}
		}

		_currentClientEntity.getAspects().add(clientAspect);

		// Add the watch tree of this simulator to the root
		Simulator simulator = aspect.getSimulator();
		if(simulator != null)
		{
			SimulatorRuntime simulatorRuntime = _sessionContext.getSimulatorRuntime(simulator);
			simulatorRuntime.incrementStepsConsumed();
			_simulationStateTreeRoot.addChild(simulatorRuntime.getStateTree().getSubTree(SUBTREE.WATCH_TREE));

			CValue time = new CValue();
			CompositeStateNode timeNode = simulatorRuntime.getStateTree().getSubTree(SUBTREE.TIME_STEP);
			if(timeNode.getChildren().size() > 0)
			{
				AValue timeValue = ((SimpleStateNode) timeNode.getChildren().get(0)).consumeFirstValue();
				_timeSteps.put(simulator, timeValue);
				time.setScale(timeValue.getScalingFactor());
				time.setUnit(timeValue.getUnit());
				time.setValue(timeValue.getStringValue());
			}
			else
			{
				throw new RuntimeException(new GeppettoExecutionException("The simulator" + simulator.getInstancePath() + " has no timestep information"));
			}

		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.massfords.humantask.TraversingVisitor#visit(org.geppetto.simulation.model.Entity)
	 */
	@Override
	public void visit(Entity entity)
	{
		CEntity beforeEntity = _currentClientEntity;

		CEntity visualEntity = new CEntity();
		visualEntity.setId(entity.getId());
		if(entity.getParentEntity() == null)
		{
			// this is an entity in the root of the simulation
			_scene.getEntities().add(visualEntity);
		}
		else
		{
			_currentClientEntity.getChildren().add(visualEntity);
		}

		_currentClientEntity = visualEntity;
		super.visit(entity);
		_currentClientEntity = beforeEntity;
	}

	/**
	 * @return
	 */
	public String getSerializedScene()
	{
		// a custom serializer is used to change what precision is used when serializing doubles in the scene
		ObjectMapper mapper = new ObjectMapper();
		SimpleModule customSerializationModule = new SimpleModule("customSerializationModule");
		customSerializationModule.addSerializer(new CustomSerializer(Double.class)); // assuming serializer declares correct class to bind to
		mapper.registerModule(customSerializationModule);

		_scene.setTime(getGlobalTime());

		try
		{
			return mapper.writer().writeValueAsString(_scene);
		}
		catch(JsonProcessingException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * @return
	 */
	private CValue getGlobalTime()
	{
		//Just returns the first value of the list of time steps as the global time
		//TODO: Add a check that all time steps are the same which should be the case
		for(AValue timeValue : _timeSteps.values())
		{
			CValue time = new CValue();
			time.setScale(timeValue.getScalingFactor());
			time.setUnit(timeValue.getUnit());
			time.setValue(timeValue.getStringValue());
			return time;
		}
		return null;
	}

	/**
	 * NOTE: Currently the scene and the watch tree are separated. Theoretically the entire update could be part of the same tree. This split is an heritage of a previous implementation, not changing
	 * it yet as it's not clear if there would be a performance benefit or not
	 * 
	 * @return
	 */
	public String getSerializedWatchTree()
	{
		// serialize state tree for variable watch and store in a string
		SerializeTreeVisitor visitor = new SerializeTreeVisitor();
		_simulationStateTreeRoot.apply(visitor);
		return visitor.getSerializedTree();
	}

}
