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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.data.model.IExperiment;
import org.geppetto.core.data.model.IGeppettoProject;
import org.geppetto.core.data.model.IPersistedData;
import org.geppetto.core.model.simulation.GeppettoModel;

public class RuntimeProject
{

	private IExperiment activeExperiment;

	private Map<IExperiment, RuntimeExperiment> experimentRuntime = new HashMap<IExperiment, RuntimeExperiment>();

	private IPersistedData geppettoModel;

	private GeppettoModel simulation;

	public RuntimeProject(IGeppettoProject project) throws MalformedURLException, GeppettoInitializationException
	{
		geppettoModel = project.getGeppettoModel();
		URL url = new URL(geppettoModel.getUrl());
		simulation = SimulationConfigReader.readConfig(url);
	}

	public void openExperiment(IExperiment experiment) throws MalformedURLException, GeppettoInitializationException
	{
		// You need a RuntimeExperiment inside the RuntimeProject for each experiment we are doing something with, i.e. we are either running a simulation or the user is connected and working with it.
		RuntimeExperiment runtimeExperiment = new RuntimeExperiment(simulation, geppettoModel);
		experimentRuntime.put(experiment, runtimeExperiment);
	}

	public void closeExperiment(IExperiment experiment) throws GeppettoExecutionException
	{
		// When an experiment is closed we release it (all the services are cleared and destroyed) and we remove it from the map
		if(!experimentRuntime.containsKey(experiment) && experimentRuntime.get(experiment) != null)
		{
			experimentRuntime.get(experiment).release();
			experimentRuntime.remove(experiment);
		}
		else
		{
			throw new GeppettoExecutionException("An experiment not having a runtime experiment cannot be closed");
		}
	}

	public RuntimeExperiment getRuntimeExperiment(IExperiment experiment)
	{
		return experimentRuntime.get(experiment);
	}

	public IExperiment getActiveExperiment()
	{
		return activeExperiment;
	}

	public void setActiveExperiment(IExperiment experiment) throws GeppettoExecutionException
	{
		if(activeExperiment != null)
		{
			// switching the active experiment requires us to close the currently active one
			closeExperiment(activeExperiment);
		}
		activeExperiment = experiment;
	}

}
