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

import org.geppetto.core.model.state.AspectTreeNode;

/**
 * @author matteocantarelli
 *
 */
public class SimulatorRuntime
{

	private AspectTreeNode _stateTree;
	private SimulatorRuntimeStatus _status=SimulatorRuntimeStatus.IDLE;
	
	//This is the number of steps this simulator has processed
	private int _processedSteps=0;
	//This is the number of steps that were processed by this simulator and that have been
	//sent to the client
	private int _stepsConsumed=0;

	
	/**
	 * @return
	 */
	public AspectTreeNode getStateTree()
	{
		return _stateTree;
	}
	
	/**
	 * @param stateTree
	 */
	public void setStateTree(AspectTreeNode stateTree)
	{
		this._stateTree = stateTree;
	}
	
	/**
	 * @param status
	 */
	public void setStatus(SimulatorRuntimeStatus status)
	{
		_status=status;
	}
	
	/**
	 * @return
	 */
	public SimulatorRuntimeStatus getStatus()
	{
		return _status;
	}
	
	
	/**
	 * @return
	 */
	public Integer getProcessedSteps()
	{
		return _processedSteps;
	}
	
	/**
	 * @param processedSteps
	 */
	public void setProcessedSteps(int processedSteps)
	{
		_processedSteps=processedSteps;
	}
	
	/**
	 * 
	 */
	public void incrementProcessedSteps()
	{
		_processedSteps++;
	}


	/**
	 * @return the number of steps which have been processed but not yet consumed
	 */
	public int getNonConsumedSteps()
	{
		return _processedSteps-_stepsConsumed;
	}
	
	/**
	 * @return
	 */
	public int getStepsConsumed()
	{
		return _stepsConsumed;
	}

	/**
	 * 
	 */
	public void incrementStepsConsumed()
	{
		_stepsConsumed++;
	}
	

	/**
	 * Revert the simulator to the initial conditions
	 */
	public void revertToInitialConditions(){
		if(_stateTree != null){
			_stateTree.getChildren().clear();
			_stateTree = null;
		}
		_stepsConsumed=0;
		_processedSteps=0;
	}
	
	/**
	 * @return true if the simulator is at initial conditions
	 */
	public boolean isAtInitialConditions()
	{
		return _stateTree == null;
	}


}
