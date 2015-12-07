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

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.data.DataManagerHelper;
import org.geppetto.core.data.model.IExperiment;
import org.geppetto.core.data.model.IGeppettoProject;
import org.geppetto.core.data.model.IPersistedData;
import org.geppetto.core.manager.IGeppettoManager;
import org.geppetto.core.manager.SharedLibraryManager;
import org.geppetto.core.model.GeppettoCommonLibraryAccess;
import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.utilities.URLReader;
import org.geppetto.model.GeppettoLibrary;
import org.geppetto.model.GeppettoModel;
import org.geppetto.model.util.GeppettoModelTraversal;
import org.geppetto.model.util.GeppettoVisitingException;
import org.geppetto.model.util.PointerUtility;
import org.geppetto.model.values.Pointer;
import org.geppetto.simulation.visitor.CreateModelInterpreterServicesVisitor;
import org.geppetto.simulation.visitor.ImportTypesVisitor;
import org.geppetto.simulation.visitor.PopulateExperimentVisitor;

/**
 * The Runtime project holds the runtime state for an open project.
 * 
 * @author dandromereschi
 * @author matteocantarelli
 *
 */
public class RuntimeProject
{

	private IExperiment activeExperiment;
	
	private Map<GeppettoLibrary, IModelInterpreter> modelInterpreters = new HashMap<GeppettoLibrary, IModelInterpreter>();

	private Map<IExperiment, RuntimeExperiment> experimentRuntime = new HashMap<IExperiment, RuntimeExperiment>();

	private GeppettoModel geppettoModel;

	private IGeppettoManager geppettoManager;

	private IGeppettoProject geppettoProject;

	public GeppettoModel getGeppettoModel()
	{
		return geppettoModel;
	}

	public IGeppettoProject getGeppettoProject()
	{
		return geppettoProject;
	}

	/**
	 * @param project
	 * @param geppettoManagerCallbackListener
	 * @throws MalformedURLException
	 * @throws GeppettoInitializationException
	 */
	public RuntimeProject(IGeppettoProject project, IGeppettoManager geppettoManager) throws MalformedURLException, GeppettoInitializationException
	{
		this.geppettoManager = geppettoManager;
		this.geppettoProject = project;
		IPersistedData geppettoModelData = project.getGeppettoModel();

		try
		{
			//reading and parsing the model
			geppettoModel = GeppettoModelReader.readGeppettoModel(URLReader.getURL(geppettoModelData.getUrl()));
			
			//loading the Geppetto common library, we create a clone of what's loaded in the shared common library
			//since every geppetto model will have his
			geppettoModel.getLibraries().add(EcoreUtil.copy(SharedLibraryManager.getSharedCommonLibrary()));
			GeppettoCommonLibraryAccess commonLibraryAccess = new GeppettoCommonLibraryAccess(geppettoModel);

			// create model interpreters
			CreateModelInterpreterServicesVisitor createServicesVisitor = new CreateModelInterpreterServicesVisitor(modelInterpreters, project.getId(),
					geppettoManager.getScope());
			GeppettoModelTraversal.apply(geppettoModel, createServicesVisitor);

			//importing the types defined in the geppetto model using the model interpreters
			ImportTypesVisitor importTypesVisitor = new ImportTypesVisitor(modelInterpreters, commonLibraryAccess);
			GeppettoModelTraversal.apply(geppettoModel, importTypesVisitor);
		}
		catch(IOException | GeppettoVisitingException e)
		{
			throw new GeppettoInitializationException(e);
		}

	}

	/**
	 * @param requestId
	 * @param experiment
	 * @throws MalformedURLException
	 * @throws GeppettoInitializationException
	 * @throws GeppettoExecutionException
	 */
	public void openExperiment(String requestId, IExperiment experiment) throws MalformedURLException, GeppettoInitializationException, GeppettoExecutionException
	{
		// You need a RuntimeExperiment inside the RuntimeProject for each experiment we are doing something with, i.e. we are either running a simulation or the user is connected and working with it.
		RuntimeExperiment runtimeExperiment = new RuntimeExperiment(this, experiment);
		experimentRuntime.put(experiment, runtimeExperiment);
		activeExperiment = experiment;
	}

	/**
	 * @param experiment
	 * @throws GeppettoExecutionException
	 */
	public void closeExperiment(IExperiment experiment) throws GeppettoExecutionException
	{
		// When an experiment is closed we release it (all the services are cleared and destroyed) and we remove it from the map
		if(experimentRuntime.containsKey(experiment) && experimentRuntime.get(experiment) != null)
		{
			experimentRuntime.get(experiment).release();
			experimentRuntime.remove(experiment);
			if(activeExperiment == experiment)
			{
				activeExperiment = null;
			}
		}
		else
		{
			throw new GeppettoExecutionException("An experiment not having a runtime experiment cannot be closed");
		}
	}

	/**
	 * @param experiment
	 * @return
	 */
	public RuntimeExperiment getRuntimeExperiment(IExperiment experiment)
	{
		return experimentRuntime.get(experiment);
	}

	/**
	 * @return
	 */
	public IExperiment getActiveExperiment()
	{
		return activeExperiment;
	}

	/**
	 * @param experiment
	 * @throws GeppettoExecutionException
	 */
	public void setActiveExperiment(IExperiment experiment) throws GeppettoExecutionException
	{
		if(getRuntimeExperiment(experiment) != null)
		{
			activeExperiment = experiment;
			// if the experiment we are loading is not already the active one we set it as such in the parent project
			if(!experiment.getParentProject().isVolatile())
			{
				if(experiment.getParentProject().getActiveExperimentId() == -1 || !(experiment.getId() == experiment.getParentProject().getActiveExperimentId()))
				{
					experiment.getParentProject().setActiveExperimentId(experiment.getId());
					DataManagerHelper.getDataManager().saveEntity(experiment.getParentProject());
				}
			}
		}
		else
		{
			throw new GeppettoExecutionException("An experiment not yet opened cannot be made active");
		}
	}

	/**
	 * 
	 */
	public void release()
	{
		for(IExperiment e : experimentRuntime.keySet())
		{
			getRuntimeExperiment(e).release();
		}
		activeExperiment = null;
		geppettoManager = null;
		experimentRuntime.clear();
	}

	/**
	 * @param experiment
	 */
	public void populateNewExperiment(IExperiment experiment)
	{
		PopulateExperimentVisitor populateExperimentVisitor = new PopulateExperimentVisitor(experiment);
		populateExperimentVisitor.doSwitch(geppettoModel);
	}

	/**
	 * @return
	 */
	public IGeppettoManager getGeppettoManager()
	{
		return geppettoManager;
	}

	/**
	 * @param pointer
	 * @return
	 */
	public IModelInterpreter getModelInterpreter(Pointer pointer)
	{
		return modelInterpreters.get(PointerUtility.getGeppettoLibrary(pointer));
	}

}
