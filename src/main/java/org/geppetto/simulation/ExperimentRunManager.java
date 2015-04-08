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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.data.IGeppettoDataManager;
import org.geppetto.core.data.model.ExperimentStatus;
import org.geppetto.core.data.model.IExperiment;
import org.geppetto.core.data.model.IGeppettoProject;
import org.geppetto.core.data.model.IUser;
import org.geppetto.core.simulation.IExperimentRunManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class ExperimentRunManager implements IExperimentRunManager
{
	private Map<IUser, List<IExperiment>> queue = new LinkedHashMap<>();

	public ExperimentRunManager()
	{
		try
		{
			loadExperiments();
		}
		catch(GeppettoInitializationException e)
		{
			e.printStackTrace();
		}
	}

	public void queueExperiment(IExperiment experiment)
	{

	}

	private void loadExperiments() throws GeppettoInitializationException
	{
		IGeppettoDataManager dataManager = getService(IGeppettoDataManager.class.getName());
		List<? extends IUser> users = dataManager.getAllUsers();
		for (IUser user : users) {
			List<? extends IGeppettoProject> projects = dataManager.getGeppettoProjectsForUser(user.getLogin());
			for (IGeppettoProject project : projects) {
				List<? extends IExperiment> experiments = dataManager.getExperimentsForProject(project.getId());
				addExperimentsToQueue(user, experiments, ExperimentStatus.RUNNING);
				addExperimentsToQueue(user, experiments, ExperimentStatus.QUEUED);
			}
		}
	}
	
	private void addExperimentsToQueue(IUser user, List<? extends IExperiment> experiments, ExperimentStatus status) {
		List<IExperiment> userExperiments = queue.get(user);
		if (userExperiments == null) {
			userExperiments = new ArrayList<>();
			queue.put(user, userExperiments);
		}
		for (IExperiment experiment : experiments) {
			if (experiment.getStatus() == status) {
				userExperiments.add(experiment);
			}
		}
	}

	/**
	 * A method to get a service of a given type
	 * 
	 * @param type
	 * @return
	 * @throws InvalidSyntaxException
	 */
	private IGeppettoDataManager getService(String type) throws GeppettoInitializationException
	{
		BundleContext bc = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

		IGeppettoDataManager service = null;
		ServiceReference<?>[] sr;
		try
		{
			sr = bc.getServiceReferences(type, null);
		}
		catch(InvalidSyntaxException e)
		{
			throw new GeppettoInitializationException(e);
		}
		if(sr != null && sr.length > 0)
		{
			service = (IGeppettoDataManager) bc.getService(sr[0]);
			for(ServiceReference<?> s : sr)
			{
				if(!((IGeppettoDataManager) bc.getService(s)).isDefault())
				{
					service = (IGeppettoDataManager) bc.getService(s);
				}
			}
		}

		return service;
	}

}
