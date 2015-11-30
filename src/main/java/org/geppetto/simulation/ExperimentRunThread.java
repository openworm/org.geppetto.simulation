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
import org.geppetto.core.data.model.IGeppettoProject;
import org.geppetto.core.data.model.IInstancePath;
import org.geppetto.core.data.model.IPersistedData;
import org.geppetto.core.data.model.ISimulationResult;
import org.geppetto.core.data.model.ISimulatorConfiguration;
import org.geppetto.core.data.model.PersistedDataType;
import org.geppetto.core.data.model.ResultsFormat;
import org.geppetto.core.manager.Scope;
import org.geppetto.core.model.IModel;
import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.s3.S3Manager;
import org.geppetto.core.services.ModelFormat;
import org.geppetto.core.services.ServiceCreator;
import org.geppetto.core.services.registry.ServicesRegistry;
import org.geppetto.core.services.registry.ServicesRegistry.ConversionServiceKey;
import org.geppetto.core.simulation.ISimulatorCallbackListener;
import org.geppetto.core.simulator.ASimulator;
import org.geppetto.core.simulator.ISimulator;
import org.geppetto.core.utilities.Zipper;
import org.geppetto.model.values.Pointer;
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

	private double runtime;

	private String timeStepUnit;

	private RuntimeExperiment runtimeExperiment;

	private IGeppettoProject project;

	/**
	 * @param experiment
	 * @param runtimeExperiment
	 * @param project
	 * @param geppettoCallbackListener
	 * @param listener
	 */
	public ExperimentRunThread(IExperiment experiment, RuntimeExperiment runtimeExperiment, IGeppettoProject project, IExperimentListener listener)
	{
		this.experiment = experiment;
		this.runtimeExperiment = runtimeExperiment;
		this.listener = listener;
		this.project = project;
	}

	/**
	 * @param experiment
	 * @throws GeppettoInitializationException
	 */
	private void init(IExperiment experiment) throws GeppettoInitializationException
	{
		List<? extends IAspectConfiguration> aspectConfigs = experiment.getAspectConfigurations();
		for(IAspectConfiguration aspectConfig : aspectConfigs)
		{
			ISimulatorConfiguration simConfig = aspectConfig.getSimulatorConfiguration();
			String simulatorId = simConfig.getSimulatorId();
			String instancePath = aspectConfig.getAspect().getInstancePath();

			if(simConfig.getConversionServiceId() != null && !simConfig.getConversionServiceId().isEmpty())
			{
				AConversion conversionService = (AConversion) ServiceCreator.getNewServiceInstance(simConfig.getConversionServiceId());
				conversionService.setScope(Scope.RUN);
				conversionService.setProjectId(experiment.getParentProject().getId());
				conversionServices.put(instancePath, conversionService);
			}
			ASimulator simulator=(ASimulator) ServiceCreator.getNewServiceInstance(simulatorId);
			simulator.setProjectId(experiment.getParentProject().getId());
			simulatorServices.put(instancePath, simulator);
			simulatorRuntimes.put(instancePath, new SimulatorRuntime());

			try
			{

				// retrieve models from runtime experiment
				IModel model = this.runtimeExperiment.getInstancePathToIModelMap().get(instancePath);
				List<IModel> models = new ArrayList<IModel>();
				models.add(model);

				// get conversion service
				IConversion conversionService = null;
				if(simConfig.getConversionServiceId() != null && !simConfig.getConversionServiceId().isEmpty())
				{
					conversionService = this.conversionServices.get(simConfig.getConversionServiceId());
				}
				IModelInterpreter modelService = runtimeExperiment.getModelInterpreters().get(instancePath);

				// TODO: Extract formats from model interpreters from within here somehow
				List<ModelFormat> inputFormats = ServicesRegistry.getModelInterpreterServiceFormats(modelService);
				List<ModelFormat> outputFormats = ServicesRegistry.getSimulatorServiceFormats(simulator);
				List<IModel> iModelsConverted = new ArrayList<IModel>();

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
						if(iModelsConverted.size() == 0)
						{
							for(ModelFormat outputFormat : supportedOutputFormats)
							{
								try
								{
									iModelsConverted.add(conversionService.convert(models.get(0), inputFormat, outputFormat, aspectConfig));
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
							if(iModelsConverted.size() == 0)
							{
								// FIXME: Assuming we will only have one conversion service
								ConversionServiceKey conversionServiceKey = entry.getKey();
								for(ModelFormat supportedModelFormat : entry.getValue().get(0).getSupportedOutputs(models.get(0), conversionServiceKey.getInputModelFormat()))
								{
									// Verify supported outputs for this model
									if(supportedModelFormat.equals(conversionServiceKey.getOutputModelFormat()))
									{
										((AConversion)entry.getValue().get(0)).setScope(Scope.RUN);
										((AConversion)entry.getValue().get(0)).setProjectId(experiment.getParentProject().getId());
										iModelsConverted.add(entry.getValue().get(0)
												.convert(models.get(0), conversionServiceKey.getInputModelFormat(), conversionServiceKey.getOutputModelFormat(), aspectConfig));
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

					if(iModelsConverted.size() == 0)
					{
						simulator.initialize(models, this);
					}
					else
					{
						simulator.initialize(iModelsConverted, this);
					}
					long end = System.currentTimeMillis();
					logger.info("Finished initializing simulator, took " + (end - start) + " ms ");
				}
				else
				{
					throw new GeppettoInitializationException("A simulator for " + instancePath + " already exists, something did not get cleared");
				}
			}
			catch(Exception e)
			{
				throw new GeppettoInitializationException(e);
			}
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
		}
		catch(GeppettoInitializationException e)
		{
			simulationError();
			logger.error(e);
		}
		while(experiment.getStatus().equals(ExperimentStatus.RUNNING))
		{
			List<? extends IAspectConfiguration> aspectConfigs = experiment.getAspectConfigurations();

			for(IAspectConfiguration aspectConfig : aspectConfigs)
			{

				String instancePath = aspectConfig.getAspect().getInstancePath();
				SimulatorRuntime simulatorRuntime = simulatorRuntimes.get(instancePath);
				ISimulator simulator = simulatorServices.get(instancePath);

				// if it's still stepping or completed we don't step again
				if(!simulatorRuntime.getStatus().equals(SimulatorRuntimeStatus.STEPPING) && !simulatorRuntime.getStatus().equals(SimulatorRuntimeStatus.DONE))
				{
					// we advance the simulation for this simulator only if we are not already stepping
					// note that some simulators might perform more than one step at the time (i.e. NEURON
					// so the status will be STEPPING until they are all completed)

					//IT FIXME pointer needs to be created same as it happens inside RuntimeExperiment, probably we need a utility function
					SimulatorRunThread simulatorRunThread = new SimulatorRunThread(experiment, simulator, aspectConfig, pointer);
					simulatorRunThread.start();
					simulatorRuntime.setStatus(SimulatorRuntimeStatus.STEPPING);
				}

			}

			if(checkAllSimulatorsAreDone())
			{
				experiment.setStatus(ExperimentStatus.COMPLETED);
				DataManagerHelper.getDataManager().saveEntity(experiment.getParentProject());
				logger.info("All simulators are done, experiment " + experiment.getId() + " was completed.");
				break;
			}

			try
			{
				Thread.sleep(500);
			}
			catch(InterruptedException e)
			{
				simulationError();
				throw new RuntimeException(e);
			}

		}

		// and when done, notify about it
		try
		{
			listener.experimentRunDone(this, experiment, project);

		}
		catch(GeppettoExecutionException e)
		{
			simulationError();
			throw new RuntimeException("Post run experiment error", e);
		}

	}

	/**
	 * 
	 */
	private void simulationError()
	{
		experiment.setStatus(ExperimentStatus.ERROR);
		DataManagerHelper.getDataManager().saveEntity(experiment);
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
				String instancePath = aspectConfig.getAspect().getInstancePath();
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
		listener = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulation.ISimulatorCallbackListener#endOfSteps(java.lang.String)
	 */
	@Override
	public void endOfSteps(Pointer pointer, Map<File, ResultsFormat> results) throws GeppettoExecutionException
	{
		SimulatorRuntime simulatorRuntime = simulatorRuntimes.get(pointer.getInstancePath());

		if(!DataManagerHelper.getDataManager().isDefault())
		{
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
							String fileName = result.getPath().substring(result.getPath().lastIndexOf("/") + 1);
							String newPath = PathConfiguration.getExperimentPath(Scope.RUN, project.getId(), experiment.getId(), pointer.getInstancePath(), fileName);
							S3Manager.getInstance().saveFileToS3(result, newPath);
							IInstancePath aspect = DataManagerHelper.getDataManager().newInstancePath(pointer.getInstancePath());
							IPersistedData recording = DataManagerHelper.getDataManager().newPersistedData(S3Manager.getInstance().getURL(newPath), PersistedDataType.RECORDING);
							ISimulationResult simulationResults = DataManagerHelper.getDataManager().newSimulationResult(aspect, recording, ResultsFormat.GEPPETTO_RECORDING);
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
				Zipper zipper = new Zipper(PathConfiguration.createExperimentTmpPath(Scope.RUN, project.getId(), experiment.getId(), pointer.getInstancePath(), fileName));

				for(File raw : rawToZip)
				{
					zipper.addToZip(raw.toURI().toURL());
				}
				Path zipped = zipper.processAddedFilesAndZip();
				String newPath = PathConfiguration.getExperimentPath(Scope.RUN, project.getId(), experiment.getId(), pointer.getInstancePath(), fileName);
				S3Manager.getInstance().saveFileToS3(zipped.toFile(), newPath);
				IInstancePath aspect = DataManagerHelper.getDataManager().newInstancePath(pointer.getInstancePath());
				IPersistedData rawResults = DataManagerHelper.getDataManager().newPersistedData(S3Manager.getInstance().getURL(newPath), PersistedDataType.RECORDING);
				ISimulationResult simulationResults = DataManagerHelper.getDataManager().newSimulationResult(aspect, rawResults, ResultsFormat.RAW);
				experiment.addSimulationResult(simulationResults);

				DataManagerHelper.getDataManager().saveEntity(experiment.getParentProject());
			}
			catch(IOException e)
			{
				throw new GeppettoExecutionException(e);
			}
		}
		simulatorRuntime.setStatus(SimulatorRuntimeStatus.DONE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulation.ISimulatorCallbackListener#stateTreeUpdated()
	 */
	@Override
	public void stepped(Pointer pointer) throws GeppettoExecutionException
	{
		String instancePath = pointer.getInstancePath();
		SimulatorRuntime simulatorRuntime = simulatorRuntimes.get(instancePath);
		simulatorRuntime.incrementProcessedSteps();
		simulatorRuntime.setStatus(SimulatorRuntimeStatus.STEPPED);
	}

}
