
package org.geppetto.simulation.manager;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
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
import org.geppetto.core.model.ModelInterpreterException;
import org.geppetto.core.services.ServiceCreator;
import org.geppetto.core.utilities.URLReader;
import org.geppetto.model.GeppettoLibrary;
import org.geppetto.model.GeppettoModel;
import org.geppetto.model.GeppettoPackage;
import org.geppetto.model.datasources.CompoundRefQuery;
import org.geppetto.model.datasources.DataSource;
import org.geppetto.model.datasources.Query;
import org.geppetto.model.datasources.QueryResults;
import org.geppetto.model.datasources.RunnableQuery;
import org.geppetto.model.types.Type;
import org.geppetto.model.types.TypesPackage;
import org.geppetto.model.util.GeppettoModelException;
import org.geppetto.model.util.GeppettoModelTraversal;
import org.geppetto.model.util.GeppettoVisitingException;
import org.geppetto.model.util.ModelUtility;
import org.geppetto.model.util.PointerUtility;
import org.geppetto.model.values.ImportValue;
import org.geppetto.model.values.PhysicalQuantity;
import org.geppetto.model.values.Pointer;
import org.geppetto.model.values.Unit;
import org.geppetto.model.values.Value;
import org.geppetto.model.values.ValuesFactory;
import org.geppetto.model.variables.Variable;
import org.geppetto.model.variables.VariablesFactory;
import org.geppetto.simulation.manager.ViewProcessor.JsonObjectExtensionConflictException;
import org.geppetto.simulation.visitor.CreateModelInterpreterServicesVisitor;
import org.geppetto.simulation.visitor.ImportTypesVisitor;

import com.google.gson.JsonObject;

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
			geppettoModel = GeppettoModelReader.readGeppettoModel(URLReader.getURL(geppettoModelData.getUrl(), project.getBaseURL()));

			// loading the Geppetto common library, we create a clone of what's loaded in the shared common library
			// since every geppetto model will have his
			geppettoModel.getLibraries().add(EcoreUtil.copy(SharedLibraryManager.getSharedCommonLibrary()));
			geppettoModelAccess = new GeppettoModelAccess(geppettoModel);
			logger.info("Model reading took " + (System.currentTimeMillis() - start) + "ms");
			// create model interpreters
			CreateModelInterpreterServicesVisitor createServicesVisitor = new CreateModelInterpreterServicesVisitor(modelInterpreters, project.getId(), geppettoManager.getScope());
			GeppettoModelTraversal.apply(geppettoModel, createServicesVisitor);
			start = System.currentTimeMillis();

			boolean gatherDefaultViews = false;
			if(geppettoProject.getView().getView() == null)
			{
				//We gather the default views only if a view is not already set, i.e. default views were already gathered and modified
				gatherDefaultViews = true;
			}
			// importing the types defined in the geppetto model using the model interpreters
			ImportTypesVisitor importTypesVisitor = new ImportTypesVisitor(modelInterpreters, geppettoModelAccess, gatherDefaultViews, geppettoProject.getBaseURL(), false);
			GeppettoModelTraversal.apply(geppettoModel, importTypesVisitor);
			
			if(gatherDefaultViews)
			{
				List<JsonObject> viewCustomisations = importTypesVisitor.getDefaultViewCustomisations();
				try
				{
					geppettoProject.getView().setView(ViewProcessor.getView(viewCustomisations));
				}
				catch(JsonObjectExtensionConflictException e)
				{
					throw new GeppettoInitializationException(e.getMessage());
				}
			}

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
	public RuntimeExperiment getRuntimeExperiment(IExperiment experiment) throws GeppettoExecutionException
	{
		if(!experimentRuntime.containsKey(experiment))
		{
			try
			{
				openExperiment(null, experiment);
			}
			catch(MalformedURLException | GeppettoInitializationException e)
			{
				throw new GeppettoExecutionException(e);
			}
		}
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
	public void release() throws GeppettoExecutionException
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
					ISimulatorConfiguration simulatorConfiguration = DataManagerHelper.getDataManager().newSimulatorConfiguration("", "", 0l, 0l, new HashMap<String, String>());
					DataManagerHelper.getDataManager().newAspectConfiguration(experiment, instancePath, simulatorConfiguration);
				}
			}
		}
	}

	/**
	 * @param typePaths
	 * @return
	 * @throws GeppettoModelException
	 */
	public GeppettoModel resolveImportType(List<String> typePaths) throws GeppettoExecutionException
	{
		try
		{
			// let's find the importType
			EList<Type> importTypes = new BasicEList<Type>();
			for(String typePath : typePaths)
			{
				importTypes.add(PointerUtility.getType(geppettoModel, typePath));
			}

			CreateModelInterpreterServicesVisitor createServicesVisitor = new CreateModelInterpreterServicesVisitor(modelInterpreters, geppettoProject.getId(), geppettoManager.getScope());
			GeppettoModelTraversal.apply(importTypes, createServicesVisitor);

			ImportTypesVisitor importTypesVisitor = new ImportTypesVisitor(modelInterpreters, geppettoModelAccess, false, geppettoProject.getBaseURL());
			GeppettoModelTraversal.apply(importTypes, importTypesVisitor);

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
	 * @param typePaths
	 * @return
	 * @throws GeppettoModelException
	 */
	public GeppettoModel resolveImportType(String typePath) throws GeppettoExecutionException
	{
		try
		{
			// let's find the importType
			EList<Type> importTypes = new BasicEList<Type>();
                        importTypes.add(PointerUtility.getType(geppettoModel, typePath));

			CreateModelInterpreterServicesVisitor createServicesVisitor = new CreateModelInterpreterServicesVisitor(modelInterpreters, geppettoProject.getId(), geppettoManager.getScope());
			GeppettoModelTraversal.apply(importTypes, createServicesVisitor);

			ImportTypesVisitor importTypesVisitor = new ImportTypesVisitor(modelInterpreters, geppettoModelAccess, false, geppettoProject.getBaseURL());
			GeppettoModelTraversal.apply(importTypes, importTypesVisitor);

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
	 * @param path
	 * @return
	 * @throws GeppettoExecutionException
	 */
	public GeppettoModel resolveImportValue(String path) throws GeppettoExecutionException
	{
		try
		{
			// let's find the importValue
			ImportValue importValue = (ImportValue) PointerUtility.getValue(geppettoModel, path, geppettoModelAccess.getType(TypesPackage.Literals.STATE_VARIABLE_TYPE));
			Type type = (Type) importValue.eContainer().eContainer().eContainer();
			// We probably don't want to create a new one that will have to reopen the NWB file. Validate this hypothesis.
			CreateModelInterpreterServicesVisitor createServicesVisitor = new CreateModelInterpreterServicesVisitor(modelInterpreters, geppettoProject.getId(), geppettoManager.getScope());
			GeppettoModelTraversal.apply(type, createServicesVisitor);

			if(type.eContainingFeature().getFeatureID() == GeppettoPackage.GEPPETTO_LIBRARY__TYPES)
			{
				// this import type is inside a library
				GeppettoLibrary library = (GeppettoLibrary) type.eContainer();
				IModelInterpreter modelInterpreter = modelInterpreters.get(library);
				Value importedValue = modelInterpreter.importValue(importValue);
				// Class<? extends EObject> a = importedValue.eContainer().getClass();
				if(importValue.eContainer() instanceof Type)
				{
					// it's the default value of a type
					// TODO: You can leave this for now Nitesh as it won't be your case

				}
				else if(importValue.eContainer().eContainer() instanceof Variable)
				{
					Type mapType = ((Variable) importValue.eContainer().eContainer()).getInitialValues().get(0).getKey();
					((Variable) importValue.eContainer().eContainer()).getInitialValues().put(mapType, importedValue);
					// TODO Do this through the GeppettoModelAccess
					type.setSynched(false);
					((GeppettoLibrary) type.eContainer()).setSynched(false);
					((Variable) importedValue.eContainer().eContainer()).setSynched(false);

				}
			}

		}
		catch(GeppettoVisitingException e)
		{
			throw new GeppettoExecutionException(e);
		}
		catch(GeppettoModelException e)
		{
			throw new GeppettoExecutionException(e);
		}
		catch(ModelInterpreterException e)
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
	 * @param queries
	 * @return
	 * @throws GeppettoModelException
	 */
	public QueryResults runQuery(List<RunnableQuery> queries) throws GeppettoModelException, GeppettoDataSourceException
	{
		Query query = ModelUtility.getQuery(queries.get(0).getQueryPath(), geppettoModel);

		// Use the first query of the chain to have the datasource we want to start from
		Query firstQueryOfChain = ((CompoundRefQuery) query).getQueryChain().get(0);
		DataSource dataSource = (DataSource) firstQueryOfChain.eContainer();
		IDataSourceService dataSourceService = getDataSourceService(dataSource.getId());

		return dataSourceService.execute(queries);
	}

	/**
	 * @param queries
	 * @return
	 * @throws GeppettoModelException
	 * @throws GeppettoDataSourceException
	 */
	public int runQueryCount(List<RunnableQuery> queries) throws GeppettoModelException, GeppettoDataSourceException
	{
		Query query = ModelUtility.getQuery(queries.get(0).getQueryPath(), geppettoModel);

		// Use the first query of the chain to have the datasource we want to start from
		Query firstQueryOfChain = ((CompoundRefQuery) query).getQueryChain().get(0);
		DataSource dataSource = (DataSource) firstQueryOfChain.eContainer();
		IDataSourceService dataSourceService = getDataSourceService(dataSource.getId());

		return dataSourceService.getNumberOfResults(queries);
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
