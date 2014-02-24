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

import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.simulator.ISimulator;
import org.geppetto.simulation.ServiceCreator;
import org.geppetto.simulation.SessionContext;
import org.geppetto.simulation.model.Model;
import org.geppetto.simulation.model.Simulator;

import com.massfords.humantask.BaseVisitor;
import com.massfords.humantask.TraversingVisitor;

/**
 * This visitor discovers and instantiates the services for each model interpreter and simulator.
 * A thread is used to instantiate the services so that a new instance of the services is created
 * at each time (the services use a ThreadScope).
 * 
 * @author matteocantarelli
 * 
 */
public class CreateSimulationServicesVisitor extends TraversingVisitor
{

	private SessionContext _sessionContext;

	public CreateSimulationServicesVisitor(SessionContext sessionContext)
	{
		super(new DepthFirstTraverserEntitiesFirst(), new BaseVisitor());
		_sessionContext = sessionContext;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.massfords.humantask.TraversingVisitor#visit(org.geppetto.simulation.model.Model)
	 */
	@Override
	public void visit(Model model)
	{
		super.visit(model);
		ServiceCreator<Model, IModelInterpreter> sc = new ServiceCreator<Model, IModelInterpreter>(model.getModelInterpreterId(), IModelInterpreter.class.getName(), model,
				_sessionContext.getModelInterpreters());
		Thread t = new Thread(sc);
		t.start();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.massfords.humantask.TraversingVisitor#visit(org.geppetto.simulation.model.Simulator)
	 */
	@Override
	public void visit(Simulator simulatorModel)
	{
		super.visit(simulatorModel);
		ServiceCreator<Simulator, ISimulator> sc = new ServiceCreator<Simulator, ISimulator>(simulatorModel.getSimulatorId(), ISimulator.class.getName(), simulatorModel,
				_sessionContext.getSimulators());
		Thread t = new Thread(sc);
		t.start();
	}

}
