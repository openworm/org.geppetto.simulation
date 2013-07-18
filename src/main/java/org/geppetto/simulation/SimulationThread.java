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

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.model.IModel;
import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.model.ModelInterpreterException;
import org.geppetto.core.simulation.TimeConfiguration;
import org.geppetto.core.simulator.ISimulator;

class SimulationThread extends Thread
{

	private static Log logger = LogFactory.getLog(SimulationThread.class);
	private SessionContext _sessionContext = null;

	public SimulationThread(SessionContext context)
	{
		this._sessionContext = context;
	}

	private SessionContext getSessionContext()
	{
		return _sessionContext;
	}
	
	/**
	 * Initializes the simulator with the model. 
	 * Simulates one step only to load the model positions and state tree.
	 * 
	 */
	public void loadModel(){
		for(String aspectID : _sessionContext.getAspectIds())
		{		
			// reset processed elements counters
			getSessionContext().setProcessedElements(aspectID, 0);
			
			IModelInterpreter modelInterpreter = _sessionContext.getConfigurationByAspect(aspectID).getModelInterpreter();
			ISimulator simulator = _sessionContext.getConfigurationByAspect(aspectID).getSimulator();

			if(!simulator.isInitialized() || _sessionContext.getSimulatorRuntimeByAspect(aspectID).isAtInitialConditions())
			{
				IModel model = _sessionContext.getSimulatorRuntimeByAspect(aspectID).getModel();
				
				// if we don't have a model fish it out of configuration
				if(model == null)
				{
					try
					{
						model = modelInterpreter.readModel(new URL(_sessionContext.getConfigurationByAspect(aspectID).getUrl()));
					}
					catch(MalformedURLException e)
					{
						throw new RuntimeException(e);
					}
					catch(ModelInterpreterException e)
					{
						throw new RuntimeException(e);
					}
					
					// set initial conditions
					_sessionContext.getSimulatorRuntimeByAspect(aspectID).setModel(model);
					_sessionContext.getSimulatorRuntimeByAspect(aspectID).setElementCount(1);
				}
				
				try
				{
					//initialize simulator
					simulator.initialize(model, new SimulationCallbackListener(aspectID, _sessionContext));
					simulator.simulate(new TimeConfiguration(null,0,0));
				}
				catch(GeppettoInitializationException e)
				{
					throw new RuntimeException(e);
				} catch (GeppettoExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	public void run() 
	{
			while(getSessionContext().isRunning())
			{
				if(!getSessionContext().isRunningCycleSemaphore())
				{
					//logger.info("Simulation thread cycle");
					getSessionContext().setRunningCycleSemaphore(true);

					for(String aspectID : _sessionContext.getAspectIds())
					{
						// reset processed elements counters
						getSessionContext().setProcessedElements(aspectID, 0);

						ISimulator simulator = _sessionContext.getConfigurationByAspect(aspectID).getSimulator();

						// TODO this is just saying "advance one step" at the moment
						try
						{
							simulator.simulate(new TimeConfiguration(null, 1, 1));
						}
						catch(GeppettoExecutionException e)
						{
							throw new RuntimeException(e);
						}

					}
				}
			}
	}
}