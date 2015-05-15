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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.geppetto.core.common.GeppettoErrorCodes;
import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.conversion.IConversion;
import org.geppetto.core.data.IGeppettoDataManager;
import org.geppetto.core.data.IGeppettoS3Manager;
import org.geppetto.core.data.model.IAspectConfiguration;
import org.geppetto.core.data.model.IExperiment;
import org.geppetto.core.data.model.ISimulatorConfiguration;
import org.geppetto.core.simulation.IGeppettoManagerCallbackListener;
import org.geppetto.core.simulation.ISimulatorCallbackListener;
import org.geppetto.core.simulator.ISimulator;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author matteocantarelli
 *
 */
public class ExperimentRun implements ISimulatorCallbackListener
{

	private IGeppettoDataManager dataManager;

	@Autowired
	private IGeppettoS3Manager s3Manager;

	private IExperiment experiment;

	private Map<String, ISimulator> simulatorServices = new ConcurrentHashMap<>();

	private Map<String, IConversion> conversionServices = new ConcurrentHashMap<>();

	private List<IExperimentListener> experimentListeners = new ArrayList<>();

	// This map contains the simulator runtime for each one of the simulators
	private Map<String, SimulatorRuntime> simulatorRuntimes = new ConcurrentHashMap<String, SimulatorRuntime>();

	private IGeppettoManagerCallbackListener simulationCallbackListener;

	/**
	 * @param dataManager
	 * @param experiment
	 * @param simulationCallbackListener
	 */
	public ExperimentRun(IGeppettoDataManager dataManager, IExperiment experiment, IGeppettoManagerCallbackListener simulationCallbackListener)
	{
		this.dataManager = dataManager;
		this.experiment = experiment;
		this.simulationCallbackListener = simulationCallbackListener;
		init(experiment);
	}

	protected void addExperimentListener(IExperimentListener listener)
	{
		experimentListeners.add(listener);
	}

	protected void removeExperimentListener(IExperimentListener listener)
	{
		experimentListeners.remove(listener);
	}

	private void init(IExperiment experiment)
	{
		List<? extends IAspectConfiguration> aspectConfigs = experiment.getAspectConfigurations();
		for(IAspectConfiguration aspectConfig : aspectConfigs)
		{
			ISimulatorConfiguration simConfig = aspectConfig.getSimulatorConfiguration();
			String simulatorId = simConfig.getSimulatorId();
			String instancePath = aspectConfig.getAspect().getInstancePath();

			if(simConfig.getConversionServiceId() != null)
			{
				ServiceCreator<String, IConversion> scc = new ServiceCreator<String, IConversion>(simConfig.getConversionServiceId(), IConversion.class.getName(), instancePath, conversionServices,
						simulationCallbackListener);
				scc.run();
			}

			ServiceCreator<String, ISimulator> scs = new ServiceCreator<String, ISimulator>(simulatorId, ISimulator.class.getName(), instancePath, simulatorServices, simulationCallbackListener);
			Thread tscs = new Thread(scs);
			tscs.start();
			try
			{
				tscs.join();
			}
			catch(InterruptedException e)
			{
				simulationCallbackListener.error(GeppettoErrorCodes.INITIALIZATION, this.getClass().getName(), null, e);
			}
			if(simulatorId != null)
			{
				SimulatorRuntime simRuntime = new SimulatorRuntime();
				simulatorRuntimes.put(instancePath, simRuntime);
			}
		}
	}

	protected void run() throws GeppettoExecutionException
	{
		// TODO: run and run

		// and when done, notify about it
		for(IExperimentListener listener : experimentListeners)
		{
			listener.experimentRunDone(this, experiment);
		}
	}

	private void storeResults()
	{
		// TODO: need to figure out the logic here
		if(s3Manager != null)
		{
			// s3Manager.saveTextToS3("text to be stored", "path/inside/the/bucket");
		}
	}

	public void release()
	{
		simulatorServices.clear();
		conversionServices.clear();
		simulatorRuntimes.clear();
		experimentListeners.clear();
	}

	@Override
	public void endOfSteps(String message)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void stateTreeUpdated() throws GeppettoExecutionException
	{
		// TODO Auto-generated method stub

	}

}
