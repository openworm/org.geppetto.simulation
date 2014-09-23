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
import org.geppetto.core.model.state.visitors.SerializeTreeVisitor;
import org.geppetto.core.simulation.ISimulationCallbackListener;
import org.geppetto.core.simulation.ISimulationCallbackListener.SimulationEvents;
import org.geppetto.simulation.visitor.CheckSteppedSimulatorsVisitor;
import org.geppetto.simulation.visitor.ExitVisitor;
import org.geppetto.simulation.visitor.SimulationVisitor;

class SimulationThread extends Thread
{

	private static Log _logger = LogFactory.getLog(SimulationThread.class);
	private SessionContext _sessionContext = null;
	private ISimulationCallbackListener _simulationCallback;
	private int _updateCycles = 0;
	private long _timeElapsed;
	private boolean _simulationStarted = false;
	private String _requestID;

	/**
	 * @param context
	 * @param simulationListener 
	 */
	public SimulationThread(SessionContext context, ISimulationCallbackListener simulationListener, 
			String requestID, int cycle)
	{
		this._sessionContext = context;
		_simulationCallback=simulationListener;
		_updateCycles = cycle;
		_requestID = requestID;
		_timeElapsed = System.currentTimeMillis();
	}

	/**
	 * @return
	 */
	private SessionContext getSessionContext()
	{
		return _sessionContext;
	}


	public void run()
	{
		SimulationVisitor simulationVisitor=new SimulationVisitor(_sessionContext,_simulationCallback);
		while(getSessionContext().getStatus().equals(SimulationRuntimeStatus.RUNNING) )
		{
				_sessionContext.getRuntimeTreeRoot().apply(simulationVisitor);
				long calculateTime =System.currentTimeMillis() - _timeElapsed;
				
				//update only if time elapsed since last client update doesn't exceed
				//the update cycle of application.
				if( calculateTime >= _updateCycles){
					updateRuntimeTreeClient();

					_timeElapsed = System.currentTimeMillis();
					_logger.info("Updating after " + calculateTime + " ms");
				}
		}
	}
	
	/**
	 * Send update to client with new run time tree
	 */
	public void updateRuntimeTreeClient(){
		CheckSteppedSimulatorsVisitor checkSteppedSimulatorsVisitor = new CheckSteppedSimulatorsVisitor(_sessionContext);
		_sessionContext.getSimulation().accept(checkSteppedSimulatorsVisitor);

		if(checkSteppedSimulatorsVisitor.allStepped() && getSessionContext().getStatus().equals(SimulationRuntimeStatus.RUNNING))
		{
			SerializeTreeVisitor updateClientVisitor = new SerializeTreeVisitor();
			_sessionContext.getRuntimeTreeRoot().apply(updateClientVisitor);

			ExitVisitor exitVisitor = new ExitVisitor(_simulationCallback);
			_sessionContext.getRuntimeTreeRoot().apply(exitVisitor);
			
			String scene = updateClientVisitor.getSerializedTree();
			if(scene!=null){
				if(!this._simulationStarted){
					_simulationCallback.updateReady(SimulationEvents.START_SIMULATION, _requestID,scene);
					_logger.info("First step of simulation sent to Simulation Callback Listener");
					this._simulationStarted = true;
				}else{
					_simulationCallback.updateReady(SimulationEvents.SCENE_UPDATE, null, scene);
					_logger.info("Update sent to Simulation Callback Listener");
				}
			}
		}
	}
}