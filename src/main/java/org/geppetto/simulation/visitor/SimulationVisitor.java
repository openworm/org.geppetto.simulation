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

import org.geppetto.core.common.GeppettoErrorCodes;
import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.model.simulation.Simulator;
import org.geppetto.core.simulation.ISimulationCallbackListener;
import org.geppetto.core.simulation.TimeConfiguration;
import org.geppetto.core.simulator.ISimulator;
import org.geppetto.simulation.SessionContext;
import org.geppetto.simulation.SimulatorRuntime;
import org.geppetto.simulation.SimulatorRuntimeStatus;

import com.massfords.humantask.BaseVisitor;
import com.massfords.humantask.TraversingVisitor;

/**
 * This is the simulation visitor which traverse the simulation tree and orchestrates the simulation of the different models.
 * 
 * 
 * @author matteocantarelli
 * 
 */
public class SimulationVisitor extends TraversingVisitor
{

	private SessionContext _sessionContext;
	private ISimulationCallbackListener _simulationCallback;

	public SimulationVisitor(SessionContext sessionContext, ISimulationCallbackListener simulationCallback)
	{
		super(new DepthFirstTraverserEntitiesFirst(), new BaseVisitor());
		_sessionContext = sessionContext;
		_simulationCallback=simulationCallback;
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
		try
		{
			ISimulator simulator = _sessionContext.getSimulator(simulatorModel);
			SimulatorRuntime simulatorRuntime = _sessionContext.getSimulatorRuntime(simulatorModel);

			//we proceed only if the simulator is not already stepping 
			if(!simulatorRuntime.getStatus().equals(SimulatorRuntimeStatus.STEPPING))
			{
				// Load Model if it is still in initial conditions
				if(!simulator.isInitialized() || simulatorRuntime.isAtInitialConditions())
				{
					_simulationCallback.error(GeppettoErrorCodes.SIMULATOR, this.getClass().getName(),"The simulator is not initialised",null);

					// LoadSimulationVisitor loadSimulationVisitor = new LoadSimulationVisitor(_sessionContext);
					// _sessionContext.getSimulation().accept(loadSimulationVisitor);
				}

				if(simulatorRuntime.getNonConsumedSteps() < _sessionContext.getMaxBufferSize())
				{
					// we advance the simulation for this simulator only if we don't have already
					// too many steps in the buffer
					try
					{
						simulatorRuntime.setStatus(SimulatorRuntimeStatus.STEPPING);
						simulator.simulate(new TimeConfiguration(null, 1, 1));
					}
					catch(GeppettoExecutionException e)
					{
						_simulationCallback.error(GeppettoErrorCodes.SIMULATOR, this.getClass().getName(),"Error while stepping "+simulator.getName(),e);
					}
				}
			}

		}
		catch(GeppettoInitializationException e)
		{
			_simulationCallback.error(GeppettoErrorCodes.SIMULATOR, this.getClass().getName(),null,e);
		}
	}

}
