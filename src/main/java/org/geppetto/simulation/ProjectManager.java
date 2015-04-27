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

import java.util.LinkedHashMap;
import java.util.Map;

import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.data.DataManagerHelper;
import org.geppetto.core.data.model.IGeppettoProject;
import org.geppetto.core.simulation.IProjectManager;

public class ProjectManager implements IProjectManager
{

	private Map<IGeppettoProject, RuntimeProject> projects = new LinkedHashMap<>();

	public void loadProject(IGeppettoProject project)
	{
		// RuntimeProject is created and populated when loadProject is called
		RuntimeProject runtimeProject = new RuntimeProject(project);
		projects.put(project, runtimeProject);
	}

	public void closeProject(IGeppettoProject project) throws GeppettoExecutionException
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

	public String getGeppettoModelUrl(IGeppettoProject project)
	{
		if(project != null)
		{
			// this could be null when loaded from a json that may not include the model part
			if(project.getGeppettoModel() == null)
			{
				project = DataManagerHelper.getDataManager().getGeppettoProjectById(project.getId());
			}
			if(project.getGeppettoModel() != null)
			{
				return project.getGeppettoModel().getUrl();
			}
		}
		return "";
	}

}
