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

import java.util.HashMap;
import java.util.Map;

import org.geppetto.core.common.GeppettoErrorCodes;
import org.geppetto.core.data.model.IPersistedData;
import org.geppetto.core.model.IModel;
import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.model.runtime.RuntimeTreeRoot;
import org.geppetto.core.model.simulation.GeppettoModel;
import org.geppetto.core.simulation.ISimulationCallbackListener;
import org.geppetto.simulation.visitor.CreateModelInterpreterServicesVisitor;
import org.geppetto.simulation.visitor.CreateRuntimeTreeVisitor;
import org.geppetto.simulation.visitor.InstancePathDecoratorVisitor;
import org.geppetto.simulation.visitor.LoadSimulationVisitor;
import org.geppetto.simulation.visitor.ParentsDecoratorVisitor;
import org.geppetto.simulation.visitor.PopulateVisualTreeVisitor;

public class RuntimeExperiment implements ISimulationCallbackListener
{

	private Map<String, IModelInterpreter> modelInterpreters = new HashMap<String, IModelInterpreter>();

	private RuntimeTreeRoot root;

	public RuntimeExperiment(GeppettoModel simulation, IPersistedData geppettoModel)
	{
		init(simulation);
	}

	private void init(GeppettoModel simulation)
	{
		// decorate Simulation model
		InstancePathDecoratorVisitor instancePathdecoratorVisitor = new InstancePathDecoratorVisitor();
		simulation.accept(instancePathdecoratorVisitor);
		ParentsDecoratorVisitor parentDecoratorVisitor = new ParentsDecoratorVisitor();
		simulation.accept(parentDecoratorVisitor);

		// // clear watch lists
		// this.clearWatchLists();
		//
		// _sessionContext.setSimulation(simulation);

		Map<String, IModelInterpreter> modelInterpreters = new HashMap<>();
		// retrieve model interpreters and simulators
		CreateModelInterpreterServicesVisitor createServicesVisitor = new CreateModelInterpreterServicesVisitor(modelInterpreters, this);
		simulation.accept(createServicesVisitor);

		// // populateScripts(simulation);
		//
		// // _sessionContext.setMaxBufferSize(appConfig.getMaxBufferSize());

		Map<String, IModel> model = new HashMap<>();
		LoadSimulationVisitor loadSimulationVisitor = new LoadSimulationVisitor(modelInterpreters, model, this);
		simulation.accept(loadSimulationVisitor);

		CreateRuntimeTreeVisitor runtimeTreeVisitor = new CreateRuntimeTreeVisitor(modelInterpreters, model, this);
		simulation.accept(runtimeTreeVisitor);

		root = runtimeTreeVisitor.getRuntimeModel();

		PopulateVisualTreeVisitor populateVisualVisitor = new PopulateVisualTreeVisitor(this);
		root.apply(populateVisualVisitor);

		// TODO: figure out how to build the modelInterpreters map
		// retrieve model interpreters and simulators
		// TODO: use the GeppettoModel that is in RuntimeProject
		// CreateModelInterpreterServicesVisitor createServicesVisitor = new CreateModelInterpreterServicesVisitor(modelInterpreters, _simulationListener);
		// simulation.accept(createServicesVisitor);

	}

	public void release()
	{
		// TODO: release the instantiated services
	}

	@Override
	public void updateReady(SimulationEvents event, String requestID, String sceneUpdate)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void error(GeppettoErrorCodes error, String classSource, String errorMessage, Exception e)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void message(String message)
	{
		// TODO Auto-generated method stub

	}

}
