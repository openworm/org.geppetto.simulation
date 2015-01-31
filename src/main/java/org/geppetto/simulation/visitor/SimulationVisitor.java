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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geppetto.core.common.GeppettoErrorCodes;
import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.model.runtime.AspectNode;
import org.geppetto.core.model.runtime.VariableNode;
import org.geppetto.core.model.state.visitors.DefaultStateVisitor;
import org.geppetto.core.simulation.ISimulationCallbackListener;
import org.geppetto.core.simulation.TimeConfiguration;
import org.geppetto.core.simulator.ISimulator;
import org.geppetto.simulation.SessionContext;
import org.geppetto.simulation.SimulatorCallbackListener;
import org.geppetto.simulation.SimulatorRuntime;
import org.geppetto.simulation.SimulatorRuntimeStatus;
import org.geppetto.core.simulation.ISimulationCallbackListener.SimulationEvents;

/**
 * This is the simulation visitor which traverse the simulation tree and orchestrates the simulation of the different models.
 * 
 * 
 * @author matteocantarelli
 * 
 */
public class SimulationVisitor extends DefaultStateVisitor
{
	private ISimulationCallbackListener _simulationCallBack;
	private SessionContext _sessionContext;
	private String _requestID;

	private static Log _logger = LogFactory.getLog(SimulatorCallbackListener.class);

	public SimulationVisitor(SessionContext _sessionContext, ISimulationCallbackListener simulationListener, String _requestID)
	{
		this._simulationCallBack = simulationListener;
		this._sessionContext = _sessionContext;
		this._requestID = _requestID;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.model.state.visitors.DefaultStateVisitor#inCompositeStateNode(org.geppetto.core.model.state.CompositeStateNode)
	 */
	@Override
	public boolean inAspectNode(AspectNode node)
	{
		return super.inAspectNode(node);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.model.state.visitors.DefaultStateVisitor#outCompositeStateNode(org.geppetto.core.model.state.CompositeStateNode)
	 */
	@Override
	public boolean outAspectNode(AspectNode node)
	{
		ISimulator simulator = node.getSimulator();
		if(simulator != null)
		{
			SimulatorRuntime simulatorRuntime = this._sessionContext.getSimulatorRuntime(simulator.getId());

			if(simulatorRuntime.getStatus().equals(SimulatorRuntimeStatus.OVER)){
				_simulationCallBack.updateReady(SimulationEvents.SIMULATION_OVER, _requestID, null);
				simulatorRuntime.setStatus(SimulatorRuntimeStatus.IDLE);
			}
			
			// we proceed only if the simulator is not already stepping
			else if(!simulatorRuntime.getStatus().equals(SimulatorRuntimeStatus.STEPPING))
			{
				// Load Model if it is at the initial conditions, this happens if the simulation was stopped
				if(!simulator.isInitialized())
				{
					LoadSimulationVisitor loadSimulationVisitor = 
							new LoadSimulationVisitor(_sessionContext, _simulationCallBack);
					_sessionContext.getSimulation().accept(loadSimulationVisitor);

					// populate visual tree
					PopulateVisualTreeVisitor populateVisualVisitor = new PopulateVisualTreeVisitor(_simulationCallBack);
					node.apply(populateVisualVisitor);
				}

				if(simulatorRuntime.getNonConsumedSteps() < _sessionContext.getMaxBufferSize())
				{
					// we advance the simulation for this simulator only if we don't have already
					// too many steps in the buffer
					try
					{
						simulatorRuntime.setStatus(SimulatorRuntimeStatus.STEPPING);
						simulator.simulate(new TimeConfiguration(null, 1, 1), node);
						simulatorRuntime.incrementStepsConsumed();
					}
					catch(GeppettoExecutionException e)
					{
						_logger.error("Error: ", e);
						_simulationCallBack.error(GeppettoErrorCodes.SIMULATOR, this.getClass().getName(), "Error while stepping " + simulator.getName(), e);
					}
				}
			}
		}
		
		return super.outAspectNode(node);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.model.state.visitors.DefaultStateVisitor#visitSimpleStateNode(org.geppetto.core.model.state.SimpleStateNode)
	 */
	@Override
	public boolean visitVariableNode(VariableNode node)
	{
		return super.visitVariableNode(node);
	}
}
