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

import java.io.File;
import java.net.MalformedURLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.data.model.ExperimentStatus;
import org.geppetto.core.data.model.IExperiment;
import org.geppetto.core.data.model.IGeppettoProject;
import org.geppetto.core.data.model.IUser;
import org.geppetto.core.manager.IGeppettoManager;
import org.geppetto.core.model.runtime.AspectSubTreeNode;
import org.geppetto.core.services.IModelFormat;
import org.geppetto.core.simulation.IExperimentRunManager;
import org.geppetto.core.simulation.IGeppettoManagerCallbackListener;
import org.geppetto.core.simulation.ResultsFormat;
import org.springframework.beans.factory.annotation.Autowired;

public class GeppettoManager implements IGeppettoManager
{

	private static Log logger = LogFactory.getLog(GeppettoManager.class);
	
	private Map<IGeppettoProject, RuntimeProject> projects = new LinkedHashMap<>();
	
	private IGeppettoManagerCallbackListener geppettoManagerCallbackListener;

	@Autowired
	private IExperimentRunManager experimentRunManager;

	private IUser user;

	public void loadProject(String requestId, IGeppettoProject project) throws MalformedURLException, GeppettoInitializationException,
			GeppettoExecutionException
	{
		// RuntimeProject is created and populated when loadProject is called
		RuntimeProject runtimeProject = new RuntimeProject(project, geppettoManagerCallbackListener);
		projects.put(project, runtimeProject);
		// load the active experiment if there is one
		if(project.getExperiments().size() > 0)
		{
			//loadExperiment(requestId, user, project.getExperiments().get(0));
		}
	}

	public void closeProject(String requestId, IGeppettoProject project) throws GeppettoExecutionException
	{
		if(!projects.containsKey(project) && projects.get(project) == null)
		{
			throw new GeppettoExecutionException("A project without a runtime project cannot be closed");
		}
		if(projects.get(project).getActiveExperiment() != null)
		{
			throw new GeppettoExecutionException("A project with an active experiment cannot be closed");
		}

		projects.remove(project);
	}

	public RuntimeProject getRuntimeProject(IGeppettoProject project)
	{
		return projects.get(project);
	}

	@Override
	public void loadExperiment(String requestId, IExperiment experiment, IGeppettoProject project) throws GeppettoExecutionException
	{
//		for(IGeppettoProject proj : projects.keySet())
//		{
//			if(proj.getExperiments().contains(experiment))
//			{
//				project = proj;
//			}
//		}
//		if(!projects.containsKey(project) && projects.get(project) == null)
//		{
//			throw new GeppettoExecutionException("A project without a runtime project cannot be closed");
//		}
//		experimentRunManager.queueExperiment(user, experiment);
	}

	@Override
	public void runExperiment(String requestId, IExperiment experiment, IGeppettoProject project) throws GeppettoInitializationException
	{
		if(experiment.getStatus().equals(ExperimentStatus.DESIGN))
		{
		experimentRunManager.queueExperiment(user, experiment);
		}
		else
		{
			//TODO Send an error
			//managerListener.error(error, classSource, errorMessage, e);
		}
	}


	@Override
	public void deleteProject(String requestId, IGeppettoProject project) throws GeppettoExecutionException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void persistProject(String requestId, IGeppettoProject project)
	{
		// TODO Auto-generated method stub
		
	}



	@Override
	public IExperiment newExperiment(String requestId, IGeppettoProject project)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteExperiment(String requestId, IExperiment experiment, IGeppettoProject project)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void linkDropBoxAccount()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void uploadModelToDropBox(String aspectID, IModelFormat format)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void uploadResultsToDropBox(String aspectID, ResultsFormat format)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public Map<String, AspectSubTreeNode> getModelTree(String aspectInstancePath)
	{
		return null;
	}

	@Override
	public Map<String, AspectSubTreeNode> getSimulationTree(String aspectInstancePath)
	{
		return null;
	}

	@Override
	public Map<String, String> setModelParameters(String aspectInstancePath, Map<String, String> parameters)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, String> setSimulatorConfiguration(String aspectInstancePath, Map<String, String> parameters)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setWatchedVariables(List<String> watchedVariables) throws GeppettoExecutionException, GeppettoInitializationException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clearWatchLists()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public File downloadModel(String aspectID, IModelFormat format)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File downloadResults(ResultsFormat resultsFormat)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setUser(IUser user)
	{
		this.user=user;
	}

	@Override
	public void setCallback(IGeppettoManagerCallbackListener geppettoManagerCallbackListener)
	{
		this.geppettoManagerCallbackListener=geppettoManagerCallbackListener;
	
	}


}
