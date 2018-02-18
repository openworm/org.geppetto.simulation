
package org.geppetto.simulation.manager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geppetto.core.beans.PathConfiguration;
import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.conversion.AConversion;
import org.geppetto.core.conversion.ConversionException;
import org.geppetto.core.conversion.IConversion;
import org.geppetto.core.data.DataManagerHelper;
import org.geppetto.core.data.model.ExperimentStatus;
import org.geppetto.core.data.model.IAspectConfiguration;
import org.geppetto.core.data.model.IExperiment;
import org.geppetto.core.data.model.IPersistedData;
import org.geppetto.core.data.model.ISimulationResult;
import org.geppetto.core.data.model.ISimulatorConfiguration;
import org.geppetto.core.data.model.PersistedDataType;
import org.geppetto.core.data.model.ResultsFormat;
import org.geppetto.core.manager.Scope;
import org.geppetto.core.model.GeppettoModelAccess;
import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.s3.S3Manager;
import org.geppetto.core.services.ServiceCreator;
import org.geppetto.core.services.registry.ServicesRegistry;
import org.geppetto.core.services.registry.ServicesRegistry.ConversionServiceKey;
import org.geppetto.core.simulation.ISimulatorCallbackListener;
import org.geppetto.core.simulator.ASimulator;
import org.geppetto.core.simulator.ISimulator;
import org.geppetto.core.utilities.URLReader;
import org.geppetto.core.utilities.Zipper;
import org.geppetto.model.DomainModel;
import org.geppetto.model.ExperimentState;
import org.geppetto.model.ModelFormat;
import org.geppetto.model.util.GeppettoModelException;
import org.geppetto.model.util.PointerUtility;
import org.geppetto.model.values.Pointer;
import org.geppetto.simulation.AppConfig;
import org.geppetto.simulation.IExperimentListener;
import org.geppetto.simulation.SimulatorRuntimeStatus;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The ExperimentRun is created when an experiment can be executed, during the init phase all the needed services are created.
 * 
 * @author dandromereschi
 * @author matteocantarelli
 * 
 */
public class ExperimentRunThread extends Thread implements ISimulatorCallbackListener
{

	private static Log logger = LogFactory.getLog(ExperimentRunThread.class);

	@Autowired
	public AppConfig appConfig;

	private IExperiment experiment;

	private Map<String, ISimulator> simulatorServices = new ConcurrentHashMap<>();

	private Map<String, IConversion> conversionServices = new ConcurrentHashMap<>();

	// This map contains the simulator runtime for each one of the simulators
	private Map<String, SimulatorRuntime> simulatorRuntimes = new ConcurrentHashMap<String, SimulatorRuntime>();

	private IExperimentListener listener;

	private RuntimeProject runtimeProject;

	/**
	 * @param experiment
	 * @param runtimeExperiment
	 * @param project
	 * @param geppettoCallbackListener
	 * @param listener
	 */
	public ExperimentRunThread(IExperiment experiment, RuntimeProject runtimeProject, IExperimentListener listener)
	{
		this.experiment = experiment;
		this.runtimeProject = runtimeProject;
		this.listener = listener;
	}

	/**
	 * @param experiment
	 * @throws GeppettoInitializationException
	 */
	private void init(IExperiment experiment) throws GeppettoInitializationException
	{
		try
		{
			GeppettoModelAccess modelAccess = new GeppettoModelAccess(runtimeProject.getGeppettoModel());
			List<? extends IAspectConfiguration> aspectConfigs = experiment.getAspectConfigurations();
			for(IAspectConfiguration aspectConfig : aspectConfigs)
			{
				ISimulatorConfiguration simConfig = aspectConfig.getSimulatorConfiguration();
				String simulatorId = simConfig.getSimulatorId();
				String instancePath = aspectConfig.getInstance();
				Pointer pointer = PointerUtility.getPointer(runtimeProject.getGeppettoModel(), instancePath);

				// We are taking the domain model for the last element of the pointer
				DomainModel model = PointerUtility.getType(pointer).getDomainModel();

				if(simConfig.getConversionServiceId() != null && !simConfig.getConversionServiceId().isEmpty())
				{
					AConversion conversionService = (AConversion) ServiceCreator.getNewServiceInstance(simConfig.getConversionServiceId());
					conversionService.setScope(Scope.RUN);
					conversionService.setProjectId(experiment.getParentProject().getId());
					conversionService.setExperiment(experiment);
					conversionServices.put(instancePath, conversionService);
				}
				ASimulator simulator = (ASimulator) ServiceCreator.getNewServiceInstance(simulatorId);
				simulator.setProjectId(experiment.getParentProject().getId());
				simulator.setExperiment(experiment);
				simulatorServices.put(instancePath, simulator);
				simulatorRuntimes.put(instancePath, new SimulatorRuntime());

				// get conversion service
				IConversion conversionService = null;
				if(simConfig.getConversionServiceId() != null && !simConfig.getConversionServiceId().isEmpty())
				{
					conversionService = this.conversionServices.get(simConfig.getConversionServiceId());
				}
				IModelInterpreter modelService = runtimeProject.getModelInterpreter(pointer);

				// TODO: Extract formats from model interpreters from within here somehow
				List<ModelFormat> inputFormats = ServicesRegistry.getModelInterpreterServiceFormats(modelService);
				List<ModelFormat> outputFormats = ServicesRegistry.getSimulatorServiceFormats(simulator);
				if(inputFormats == null || inputFormats.isEmpty())
				{
					throw new GeppettoInitializationException("No supported formats for the model interpreter " + modelService.getName());
				}
				if(outputFormats == null || outputFormats.isEmpty())
				{
					throw new GeppettoInitializationException("No supported formats for the simulator " + simulator.getName());
				}
				DomainModel iConvertedModel = null;

				if(conversionService != null)
				{
					// Read conversion supported model formats
					List<ModelFormat> supportedInputFormats = conversionService.getSupportedInputs();
					// FIXME: We can pass the model and the input format so it brings back a filtered list of outputs format
					List<ModelFormat> supportedOutputFormats = conversionService.getSupportedOutputs();

					// Check if real model formats and conversion supported model formats match
					supportedInputFormats.retainAll(inputFormats);
					supportedOutputFormats.retainAll(outputFormats);

					// Try to convert until a input-output format combination works
					for(ModelFormat inputFormat : supportedInputFormats)
					{
						if(iConvertedModel == null)
						{
							for(ModelFormat outputFormat : supportedOutputFormats)
							{
								try
								{

									iConvertedModel = conversionService.convert(model, outputFormat, aspectConfig, modelAccess);
									break;
								}
								catch(ConversionException e)
								{
									throw new GeppettoInitializationException(e);
								}
							}
						}
					}
				}
				else
				{
					// Check format returned by the model interpreter matches with the one accepted by the simulator
					if(Collections.disjoint(inputFormats, outputFormats) && inputFormats != null && outputFormats != null)
					{
						Map<ConversionServiceKey, List<IConversion>> conversionServices = ServicesRegistry.getConversionService(inputFormats, outputFormats);

						for(Map.Entry<ConversionServiceKey, List<IConversion>> entry : conversionServices.entrySet())
						{
							if(iConvertedModel == null)
							{
								// FIXME: Assuming we will only have one conversion service
								ConversionServiceKey conversionServiceKey = entry.getKey();
								for(ModelFormat supportedModelFormat : entry.getValue().get(0).getSupportedOutputs(model))
								{
									// Verify supported outputs for this model
									if(supportedModelFormat.getModelFormat().equalsIgnoreCase(conversionServiceKey.getOutputModelFormat().getModelFormat()))
									{
										((AConversion) entry.getValue().get(0)).setScope(Scope.RUN);
										((AConversion) entry.getValue().get(0)).setProjectId(experiment.getParentProject().getId());
										((AConversion) entry.getValue().get(0)).setExperiment(experiment);
										iConvertedModel = entry.getValue().get(0).convert(model, conversionServiceKey.getOutputModelFormat(), aspectConfig, modelAccess);
										break;
									}
								}
							}
						}
					}
				}

				// code to initialize simulator
				if(simulator != null)
				{

					long start = System.currentTimeMillis();
					ExperimentState experimentState = runtimeProject.getRuntimeExperiment(experiment).getExperimentState();
					if(iConvertedModel == null)
					{
						simulator.initialize(model, aspectConfig, experimentState, this, modelAccess);
					}
					else
					{
						simulator.initialize(iConvertedModel, aspectConfig, experimentState, this, modelAccess);
					}
					long end = System.currentTimeMillis();
					logger.info("Finished initializing simulator, took " + (end - start) + " ms ");
				}
			}
		}
		catch(Exception e)
		{
			throw new GeppettoInitializationException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#run()
	 */
	public synchronized void run()
	{
		try
		{
			init(experiment);

			while(experiment.getStatus().equals(ExperimentStatus.RUNNING))
			{

				List<? extends IAspectConfiguration> aspectConfigs = experiment.getAspectConfigurations();

				for(IAspectConfiguration aspectConfig : aspectConfigs)
				{

					String instancePath = aspectConfig.getInstance();
					Pointer pointer = PointerUtility.getPointer(runtimeProject.getGeppettoModel(), instancePath);
					SimulatorRuntime simulatorRuntime = simulatorRuntimes.get(instancePath);
					ISimulator simulator = simulatorServices.get(instancePath);

					// if it's still stepping or completed we don't step again
					if(!simulatorRuntime.getStatus().equals(SimulatorRuntimeStatus.STEPPING) && !simulatorRuntime.getStatus().equals(SimulatorRuntimeStatus.DONE))
					{
						// we advance the simulation for this simulator only if we are not already stepping
						// note that some simulators might perform more than one step at the time (i.e. NEURON
						// so the status will be STEPPING until they are all completed)

						SimulatorRunThread simulatorRunThread = new SimulatorRunThread(experiment, simulator);
						simulatorRunThread.start();
						simulatorRuntime.setStatus(SimulatorRuntimeStatus.STEPPING);
					}

				}

				if(checkAllSimulatorsAreDone())
				{
					experiment.setStatus(ExperimentStatus.COMPLETED);
					experiment.updateEndDate();
					DataManagerHelper.getDataManager().saveEntity(experiment.getParentProject());
					logger.info("All simulators are done, experiment " + experiment.getId() + " was completed.");
					break;
				}
			}
		}
		catch(GeppettoInitializationException | GeppettoModelException e)
		{
			// TODO How to make the error surface in some description?
			externalProcessFailed("", e);
			logger.error(e);
		}
		try
		{
			Thread.sleep(500);
		}
		catch(InterruptedException e)
		{
			String errorMessage = "Error running experiment with name: (" + experiment.getName() + ") and id: " + experiment.getId();
			externalProcessFailed(errorMessage, e);
			throw new RuntimeException(e);

		}

		// and when done, notify about it
		try
		{
			listener.experimentRunDone(this, experiment, runtimeProject);

		}
		catch(GeppettoExecutionException e)
		{
			String errorMessage = "Error running experiment with name: (" + experiment.getName() + ") and id: " + experiment.getId();
			externalProcessFailed(errorMessage, e);
			throw new RuntimeException("Post run experiment error", e);
		}

	}

	/**
	 * @return true if all the simulators associated with this experiment have completed their execution
	 */
	private synchronized boolean checkAllSimulatorsAreDone()
	{
		List<? extends IAspectConfiguration> aspectConfigs = experiment.getAspectConfigurations();
		for(IAspectConfiguration aspectConfig : aspectConfigs)
		{
			try
			{
				String instancePath = aspectConfig.getInstance();
				SimulatorRuntime simulatorRuntime = simulatorRuntimes.get(instancePath);
				if(!simulatorRuntime.getStatus().equals(SimulatorRuntimeStatus.DONE))
				{
					return false;
				}
			}
			catch(NullPointerException npe)
			{
				logger.error(npe);
			}

		}
		return true;
	}

	/**
	 * @throws GeppettoExecutionException
	 */
	protected void cancelRun() throws GeppettoExecutionException
	{

		logger.info("Canceling ExperimentRun");
		experiment.setStatus(ExperimentStatus.CANCELED);

		// iterate through aspects and instruct them to stop
		// TODO Check
		for(ISimulator simulator : simulatorServices.values())
		{
			if(simulator != null)
			{
				simulator.setInitialized(false);
			}
		}
	}

	/**
	 * 
	 */
	public void release()
	{
		simulatorServices.clear();
		conversionServices.clear();
		simulatorRuntimes.clear();
		// listener = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulation.ISimulatorCallbackListener#endOfSteps(java.lang.String)
	 */
	@Override
	public void endOfSteps(IAspectConfiguration aspectConfiguration, Map<File, ResultsFormat> results) throws GeppettoExecutionException
	{
		String instancePath = aspectConfiguration.getInstance();
		SimulatorRuntime simulatorRuntime = simulatorRuntimes.get(instancePath);

		try
		{
			List<File> rawToZip = new ArrayList<File>();

			// where we store the results in S3
			for(File result : results.keySet())
			{
				switch(results.get(result))
				{
					case GEPPETTO_RECORDING:
					{
						String fileName = URLReader.getFileName(result.toURI().toURL());
						String newPath = "projects/" + Long.toString(runtimeProject.getGeppettoProject().getId()) + "/experiment/" + experiment.getId() + "/" + fileName;
						IPersistedData recording;
						if(!DataManagerHelper.getDataManager().isDefault())
						{
							S3Manager.getInstance().saveFileToS3(result, newPath);
							recording = DataManagerHelper.getDataManager().newPersistedData(S3Manager.getInstance().getURL(newPath), PersistedDataType.RECORDING);
						}
						else
						{
							recording = DataManagerHelper.getDataManager().newPersistedData(result.toURI().toURL(), PersistedDataType.RECORDING);
						}
						ISimulationResult simulationResults = DataManagerHelper.getDataManager().newSimulationResult(instancePath, recording, ResultsFormat.GEPPETTO_RECORDING);
						experiment.addSimulationResult(simulationResults);
						break;
					}
					case RAW:
					{
						rawToZip.add(result);
						break;
					}
				}
			}

			String fileName = "rawRecording.zip";
			Zipper zipper = new Zipper(PathConfiguration.createExperimentTmpPath(Scope.RUN, runtimeProject.getGeppettoProject().getId(), experiment.getId(), instancePath, fileName));

			for(File raw : rawToZip)
			{
				zipper.addToZip(raw.toURI().toURL());
			}
			Path zipped = zipper.processAddedFilesAndZip();
			String newPath = "projects/" + Long.toString(runtimeProject.getGeppettoProject().getId()) + "/experiment/" + experiment.getId() + "/" + fileName;
			if(!DataManagerHelper.getDataManager().isDefault())
			{
				S3Manager.getInstance().saveFileToS3(zipped.toFile(), newPath);
			}
			IPersistedData rawResults = DataManagerHelper.getDataManager().newPersistedData(S3Manager.getInstance().getURL(newPath), PersistedDataType.RECORDING);
			ISimulationResult simulationResults = DataManagerHelper.getDataManager().newSimulationResult(instancePath, rawResults, ResultsFormat.RAW);
			experiment.addSimulationResult(simulationResults);

			DataManagerHelper.getDataManager().saveEntity(experiment.getParentProject());
		}
		catch(IOException e)
		{
			throw new GeppettoExecutionException(e);
		}

		simulatorRuntime.setStatus(SimulatorRuntimeStatus.DONE);
	}

	@Override
	public void externalProcessFailed(String message, Exception e)
	{
		String errorMessage = "Experiment with name: " + experiment.getName() + " and id: " + experiment.getId() + " has failed." + '\n';

		experiment.setStatus(ExperimentStatus.ERROR);
		this.listener.experimentError(errorMessage, message + e.getMessage(), e, experiment);
		DataManagerHelper.getDataManager().saveEntity(experiment);
	}
}
