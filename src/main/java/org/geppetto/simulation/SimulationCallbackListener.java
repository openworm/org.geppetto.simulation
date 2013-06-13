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

import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.model.state.StateTreeRoot;
import org.geppetto.core.model.state.visitors.CountTimeStepsVisitor;
import org.geppetto.core.model.state.visitors.RemoveTimeStepsVisitor;
import org.geppetto.core.simulation.ISimulatorCallbackListener;

public class SimulationCallbackListener implements ISimulatorCallbackListener
{

	private String simulationAspectID;
	private SessionContext _sessionContext;

	public SimulationCallbackListener(String aspectID, SessionContext context)
	{
		this.simulationAspectID = aspectID;
		this._sessionContext = context;
	}

	/**
	 * Figures out value of running cycle flag given processed elements counts NOTE: when all elements have been processed running cycle is set to false so that the next cycle can start
	 */
	private void updateRunningCycleSemaphore()
	{
		int processedAspects = 0;

		// check that all elements have been processed on all the simulation aspects
		for(String aspectID : _sessionContext.getAspectIds())
		{
			if(_sessionContext.getSimulatorRuntimeByAspect(aspectID).allElementsProcessed())
			{
				// if not all elements have been processed cycle is still running
				processedAspects++;
			}
		}

		if(_sessionContext.getAspectIds().size() == processedAspects)
		{
			_sessionContext.setRunningCycleSemaphore(false);
		}
	}

	@Override
	public void stateTreeUpdated(StateTreeRoot stateTree) throws GeppettoExecutionException
	{
		StateTreeRoot sessionStateTree = _sessionContext.getSimulatorRuntimeByAspect(simulationAspectID).getStateTree();
		if(sessionStateTree == null)
		{
			sessionStateTree = stateTree;
			_sessionContext.getSimulatorRuntimeByAspect(simulationAspectID).setStateSet(sessionStateTree);
		}
		// we throw an exception if the tree is a different object, this should not happen.
		if(!sessionStateTree.equals(stateTree))
		{
			throw new GeppettoExecutionException("Out of sync! The state tree received is different from the one stored in the session context");
		}
		// if the tree starts having more elements than the max size of the buffers remove the oldest ones
		boolean wait = true;
		while(wait)
		{
			CountTimeStepsVisitor countTimeStepsVisitor = new CountTimeStepsVisitor();
			stateTree.apply(countTimeStepsVisitor);
			int timeStepsToRemove = countTimeStepsVisitor.getNumberOfTimeSteps() - _sessionContext.getMaxBufferSize();
			if(timeStepsToRemove<0)
			{
				wait=false;
			}
		}
		// This line is necessary because we have logic that checks that all models are processed before sending an update
		_sessionContext.getSimulatorRuntimeByAspect(simulationAspectID).increaseProcessedElements();
		updateRunningCycleSemaphore();
	}

}
