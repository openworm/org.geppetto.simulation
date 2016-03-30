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
package org.geppetto.simulation.manager;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.data.DataManagerHelper;
import org.geppetto.core.data.model.IExperiment;
import org.geppetto.core.data.model.IGeppettoProject;
import org.geppetto.core.data.model.IPersistedData;
import org.geppetto.core.data.model.ISimulatorConfiguration;
import org.geppetto.core.datasources.GeppettoDataSourceException;
import org.geppetto.core.datasources.IDataSourceService;
import org.geppetto.core.manager.IGeppettoManager;
import org.geppetto.core.manager.SharedLibraryManager;
import org.geppetto.core.model.GeppettoModelAccess;
import org.geppetto.core.model.GeppettoModelReader;
import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.services.ServiceCreator;
import org.geppetto.core.utilities.URLReader;
import org.geppetto.model.DataSource;
import org.geppetto.model.GeppettoLibrary;
import org.geppetto.model.GeppettoModel;
import org.geppetto.model.types.Type;
import org.geppetto.model.types.TypesPackage;
import org.geppetto.model.util.GeppettoModelException;
import org.geppetto.model.util.GeppettoModelTraversal;
import org.geppetto.model.util.GeppettoVisitingException;
import org.geppetto.model.util.PointerUtility;
import org.geppetto.model.values.PhysicalQuantity;
import org.geppetto.model.values.Pointer;
import org.geppetto.model.values.Unit;
import org.geppetto.model.values.ValuesFactory;
import org.geppetto.model.variables.Variable;
import org.geppetto.model.variables.VariablesFactory;
import org.geppetto.simulation.visitor.CreateModelInterpreterServicesVisitor;
import org.geppetto.simulation.visitor.ImportTypesVisitor;

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

	private GeppettoModelAccess geppettoModelAccess;

	private IGeppettoManager geppettoManager;

	private IGeppettoProject geppettoProject;

	private Map<String, IDataSourceService> dataSourceServices;

	private static Log logger = LogFactory.getLog(RuntimeProject.class);

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
		this.dataSourceServices = new HashMap<String, IDataSourceService>();
		IPersistedData geppettoModelData = project.getGeppettoModel();

		try
		{
			long start = System.currentTimeMillis();
			// reading and parsing the model
			geppettoModel = GeppettoModelReader.readGeppettoModel(URLReader.getURL(geppettoModelData.getUrl()));

			// loading the Geppetto common library, we create a clone of what's loaded in the shared common library
			// since every geppetto model will have his
			geppettoModel.getLibraries().add(EcoreUtil.copy(SharedLibraryManager.getSharedCommonLibrary()));
			geppettoModelAccess = new GeppettoModelAccess(geppettoModel);
			logger.info("Model reading took " + (System.currentTimeMillis() - start) + "ms");
			// create model interpreters
			CreateModelInterpreterServicesVisitor createServicesVisitor = new CreateModelInterpreterServicesVisitor(modelInterpreters, project.getId(), geppettoManager.getScope());
			GeppettoModelTraversal.apply(geppettoModel, createServicesVisitor);
			start = System.currentTimeMillis();

			// importing the types defined in the geppetto model using the model interpreters
			ImportTypesVisitor importTypesVisitor = new ImportTypesVisitor(modelInterpreters, geppettoModelAccess);
			GeppettoModelTraversal.apply(geppettoModel, importTypesVisitor);
			importTypesVisitor.removeProcessedImportType();
			logger.info("Importing types took " + (System.currentTimeMillis() - start) + "ms");

			// create time (puhrrrrr)
			Variable time = VariablesFactory.eINSTANCE.createVariable();
			time.setId("time");
			time.setName("time");
			time.getTypes().add(geppettoModelAccess.getType(TypesPackage.Literals.STATE_VARIABLE_TYPE));
			PhysicalQuantity initialValue = ValuesFactory.eINSTANCE.createPhysicalQuantity();
			Unit seconds = ValuesFactory.eINSTANCE.createUnit();
			seconds.setUnit("s");
			initialValue.setUnit(seconds);
			time.getInitialValues().put(geppettoModelAccess.getType(TypesPackage.Literals.STATE_VARIABLE_TYPE), initialValue);
			geppettoModel.getVariables().add(time);
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
					DataManagerHelper.getDataManager().saveEntity(geppettoProject);
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
	 * @throws GeppettoVisitingException
	 */
	public void populateNewExperiment(IExperiment experiment) throws GeppettoVisitingException
	{
		// Create one aspect configuration for each variable in the root
		for(Variable variable : geppettoModel.getVariables())
		{
			if(!variable.getId().equals("time"))
			{
				for(Type type : variable.getTypes())
				{
					String instancePath = PointerUtility.getInstancePath(variable, type);
					ISimulatorConfiguration simulatorConfiguration = DataManagerHelper.getDataManager().newSimulatorConfiguration("", "", 0l, 0l);
					DataManagerHelper.getDataManager().newAspectConfiguration(experiment, instancePath, simulatorConfiguration);
				}
			}
		}
	}

	/**
	 * @param typePath
	 * @return
	 * @throws GeppettoModelException
	 */
	public GeppettoModel resolveImportType(String typePath) throws GeppettoExecutionException
	{
		try
		{
			// let's find the importType
			Type importType = PointerUtility.getType(geppettoModel, typePath);

			CreateModelInterpreterServicesVisitor createServicesVisitor = new CreateModelInterpreterServicesVisitor(modelInterpreters, geppettoProject.getId(), geppettoManager.getScope());
			GeppettoModelTraversal.apply(importType, createServicesVisitor);

			ImportTypesVisitor importTypesVisitor = new ImportTypesVisitor(modelInterpreters, geppettoModelAccess);
			GeppettoModelTraversal.apply(importType, importTypesVisitor);

			importTypesVisitor.removeProcessedImportType();
		}
		catch(GeppettoVisitingException e)
		{
			throw new GeppettoExecutionException(e);
		}
		catch(GeppettoModelException e)
		{
			throw new GeppettoExecutionException(e);
		}

		return geppettoModel;
	}

	/**
	 * @param dataSourceId
	 * @param variableId
	 * @return
	 * @throws GeppettoModelException
	 * @throws GeppettoDataSourceException
	 */
	public GeppettoModel fetchVariable(String dataSourceId, String variableId) throws GeppettoModelException, GeppettoDataSourceException
	{
		// the data source service has already been initialized with the GeppettoModelAccess
		// the variable will be added to the GeppettoModel
		IDataSourceService dataSourceService = getDataSourceService(dataSourceId);
		dataSourceService.fetchVariable(variableId);
		return geppettoModel;
	}

	/**
	 * @param dataSourceId
	 * @return
	 * @throws GeppettoInitializationException
	 * @throws GeppettoModelException
	 */
	private IDataSourceService getDataSourceService(String dataSourceId) throws GeppettoModelException
	{
		try
		{
			IDataSourceService dataSourceService = null;
			if(!dataSourceServices.containsKey(dataSourceId))
			{
				for(DataSource dataSource : geppettoModel.getDataSources())
				{
					if(dataSource.getId().equals(dataSourceId))
					{
						dataSourceService = (IDataSourceService) ServiceCreator.getNewServiceInstance(dataSource.getDataSourceService());
						dataSourceService.initialize(dataSource, geppettoModelAccess);
						dataSourceServices.put(dataSourceId, dataSourceService);
						break;
					}
				}
				if(dataSourceService == null)
				{
					throw new GeppettoModelException("The datasource service for " + dataSourceId + " was not found");
				}
			}
		}
		catch(GeppettoInitializationException e)
		{
			throw new GeppettoModelException(e);
		}
		return dataSourceServices.get(dataSourceId);
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
		return getModelInterpreter(PointerUtility.getGeppettoLibrary(pointer));
	}

	/**
	 * @param library
	 * @return
	 */
	public IModelInterpreter getModelInterpreter(GeppettoLibrary library)
	{
		return modelInterpreters.get(library);
	}

	/**
	 * @return
	 */
	public GeppettoModel getGeppettoModel()
	{
		return geppettoModel;
	}

	/**
	 * @return
	 */
	public IGeppettoProject getGeppettoProject()
	{
		return geppettoProject;
	}

}
