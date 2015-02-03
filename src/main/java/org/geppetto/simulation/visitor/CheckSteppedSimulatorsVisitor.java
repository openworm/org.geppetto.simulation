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
package org.geppetto.simulation.visitor;

import org.geppetto.core.model.simulation.Simulator;
import org.geppetto.core.model.simulation.visitor.BaseVisitor;
import org.geppetto.core.model.simulation.visitor.TraversingVisitor;
import org.geppetto.simulation.SessionContext;
import org.geppetto.simulation.SimulatorRuntime;

/**
 * This visitor checks if all the simulators have stepped Note: This visitor at the moment doesn't take into account that different simulators could have different time steps and in this case the
 * check will have to be more sophisticated
 * 
 * @author matteocantarelli
 * 
 */
public class CheckSteppedSimulatorsVisitor extends TraversingVisitor
{

	private SessionContext _sessionContext = null;
	private boolean _allStepped = true;
	private boolean _noneEverStepped = true;

	public CheckSteppedSimulatorsVisitor(SessionContext sessionContext)
	{
		super(new DepthFirstTraverserEntitiesFirst(), new BaseVisitor());
		_sessionContext = sessionContext;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.massfords.humantask.TraversingVisitor#visit(org.geppetto.simulation.model.Simulator)
	 */
	@Override
	public void visit(Simulator simulator)
	{
		
		if(_allStepped || _noneEverStepped)
		{
			SimulatorRuntime simulatorRuntime = _sessionContext.getSimulatorRuntime(simulator.getSimulatorId());
			
			if(simulatorRuntime.getNonConsumedSteps() < 0)
			{
				// this simulator has no steps to consume
				_allStepped = false;
			}
			
			if(simulatorRuntime.getProcessedSteps() != 0)
			{
				// this simulator has steps to consume
				_noneEverStepped = false;
			}
		}

	}

	/**
	 * @return
	 */
	public boolean allStepped()
	{
		return _allStepped;
	}

	/**
	 * @return
	 */
	public boolean noneEverStepped()
	{
		return _noneEverStepped;
	}

}
