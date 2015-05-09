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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.simulation.IGeppettoManagerCallbackListener;
import org.geppetto.core.simulation.ISimulatorCallbackListener;
import org.springframework.stereotype.Service;

public class SimulatorCallbackListener implements ISimulatorCallbackListener
{

	// SIM TODO
//	private Simulator _simulatorModel;
	private SimulatorRuntime _simulatorRuntime;
	private SessionContext _sessionContext;
	private IGeppettoManagerCallbackListener _simulationCallback;

	private static Log _logger = LogFactory.getLog(SimulatorCallbackListener.class);

	// SIM TODO
	public SimulatorCallbackListener(//Simulator simulatorModel, 
			SessionContext context, IGeppettoManagerCallbackListener simulationCallback)
	{
		// SIM TODO
//		_simulatorModel = simulatorModel;
		_sessionContext = context;
		// SIM TODO
//		_simulatorRuntime=_sessionContext.getSimulatorRuntime(_simulatorModel.getSimulatorId());
		_simulationCallback = simulationCallback;
	}

	@Override
	public void stateTreeUpdated() throws GeppettoExecutionException
	{
		if(!_simulatorRuntime.getStatus().equals(SimulatorRuntimeStatus.OVER)){
			_simulatorRuntime.incrementProcessedSteps();
			_simulatorRuntime.setStatus(SimulatorRuntimeStatus.STEPPED);
		}
	}

	@Override
	public void endOfSteps(String message) {		
		_simulatorRuntime.setStatus(SimulatorRuntimeStatus.OVER);
		this._simulationCallback.message(message);
	}
}
