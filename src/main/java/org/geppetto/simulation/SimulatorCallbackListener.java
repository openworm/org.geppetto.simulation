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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.model.runtime.AspectNode;
import org.geppetto.core.model.simulation.Simulator;
import org.geppetto.core.simulation.ISimulatorCallbackListener;

public class SimulatorCallbackListener implements ISimulatorCallbackListener
{

	private Simulator _simulatorModel;
	private SimulatorRuntime _simulatorRuntime;
	private SessionContext _sessionContext;

	private static Log _logger = LogFactory.getLog(SimulatorCallbackListener.class);

	public SimulatorCallbackListener(Simulator simulatorModel, SessionContext context)
	{
		_simulatorModel = simulatorModel;
		_sessionContext = context;
		_simulatorRuntime=_sessionContext.getSimulatorRuntime(_simulatorModel.getSimulatorId());
	}

	@Override
	public void stateTreeUpdated(AspectNode stateTree) throws GeppettoExecutionException
	{
		_simulatorRuntime.incrementProcessedSteps();
		_simulatorRuntime.setStatus(SimulatorRuntimeStatus.STEPPED);
				
		//A scheduled event could have taken place ms prior to simulation being stopped, make sure 
		//to revert tree to initial conditions is simulation has been stopped
		if(_sessionContext.getStatus().equals(SimulationRuntimeStatus.STOPPED)){
			_sessionContext.revertToInitialConditions();
		}
	}

}
