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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geppetto.core.common.GeppettoErrorCodes;
import org.geppetto.core.common.GeppettoExecutionException;
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
import org.geppetto.core.model.IModel;
import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.model.quantities.Quantity;
import org.geppetto.core.model.quantities.Unit;
import org.geppetto.core.model.runtime.AspectNode;
import org.geppetto.core.model.runtime.RuntimeTreeRoot;
import org.geppetto.core.model.runtime.VariableNode;
import org.geppetto.core.model.values.ValuesFactory;
import org.geppetto.core.s3.S3Manager;
import org.geppetto.core.services.ModelFormat;
import org.geppetto.core.services.registry.ServicesRegistry;
import org.geppetto.core.services.registry.ServicesRegistry.ConversionServiceKey;
import org.geppetto.core.simulation.IGeppettoManagerCallbackListener;
import org.geppetto.core.simulation.IGeppettoManagerCallbackListener.GeppettoEvents;
import org.geppetto.core.simulation.ISimulatorCallbackListener;
import org.geppetto.core.simulator.ISimulator;
import org.geppetto.simulation.visitor.ExitVisitor;
import org.geppetto.simulation.visitor.FindAspectNodeVisitor;
import org.geppetto.simulation.visitor.TimeVisitor;
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

	private IGeppettoManagerCallbackListener geppettoManagerCallbackListener;

	private IExperimentListener listener;

	private int updateCycles = 0;

	private long timeElapsed;

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
	public ExperimentRunThread(IExperiment experiment, RuntimeExperiment runtimeExperiment, IGeppettoProject project, IGeppettoManagerCallbackListener geppettoCallbackListener,
			IExperimentListener listener)
	{
		this.experiment = experiment;
		this.runtimeExperiment = runtimeExperiment;
		this.geppettoManagerCallbackListener = geppettoCallbackListener;
		this.listener = listener;
		this.project = project;
		init(experiment);
	}

	/**
	 * @param experiment
	 */
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
				ServiceCreator<String, IConversion> scc = new ServiceCreator<String, IConversion>(simConfig.getConversionServiceId(), IConversion.class.getName(), instancePath, conversionServices);
				scc.run();
			}

			ServiceCreator<String, ISimulator> scs = new ServiceCreator<String, ISimulator>(simulatorId, ISimulator.class.getName(), instancePath, simulatorServices);
			Thread tscs = new Thread(scs);
			tscs.start();
			try
			{
				tscs.join();
			}
			catch(InterruptedException e)
			{
				geppettoManagerCallbackListener.error(GeppettoErrorCodes.INITIALIZATION, this.getClass().getName(), null, e);
			}
			if(simulatorId != null)
			{
				SimulatorRuntime simRuntime = new SimulatorRuntime();
				simulatorRuntimes.put(instancePath, simRuntime);
			}
			try
			{
				tscs.join();

				// retrieve models from runtime experiment
				IModel model = this.runtimeExperiment.getInstancePathToIModelMap().get(instancePath);
				List<IModel> models = new ArrayList<IModel>();
				models.add(model);

				ISimulator simulator = simulatorServices.get(instancePath);
				// get conversion service
				IConversion conversionService = this.conversionServices.get(simConfig.getConversionServiceId());
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
									logger.error("Error: ", e);
									geppettoManagerCallbackListener.error(GeppettoErrorCodes.SIMULATION, this.getClass().getName(), null, e);
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
					geppettoManagerCallbackListener.error(GeppettoErrorCodes.SIMULATION, this.getClass().getName(), "A simulator for " + instancePath
							+ " already exists, something did not get cleared", null);
				}
			}
			catch(Exception e)
			{
				geppettoManagerCallbackListener.error(GeppettoErrorCodes.INITIALIZATION, this.getClass().getName(), null, e);
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

						FindAspectNodeVisitor findAspectNodeVisitor = new FindAspectNodeVisitor(instancePath);
						runtimeExperiment.getRuntimeTree().apply(findAspectNodeVisitor);
						SimulatorRunThread simulatorRunThread = new SimulatorRunThread(experiment, simulator, aspectConfig, findAspectNodeVisitor.getAspectNode());
						simulatorRunThread.start();
						simulatorRuntime.setStatus(SimulatorRuntimeStatus.STEPPING);
					}


			}

			if(checkAllSimulatorsAreDone())
			{
				experiment.setStatus(ExperimentStatus.COMPLETED);
				DataManagerHelper.getDataManager().saveEntity(experiment.getParentProject());
				logger.info("All simulators are done, experiment " + experiment.getId() + " was completed.");
			}

		}

		if(geppettoManagerCallbackListener != null)
		{
			// if it is null the client is not connected
			sendSimulationCallback();
		}

		// and when done, notify about it
		try
		{
			listener.experimentRunDone(this, experiment, project);

		}
		catch(GeppettoExecutionException e)
		{
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
	 * Send update to client with new run time tree
	 */
	private void sendSimulationCallback()
	{

		// Visit simulators to extract time from them
		TimeVisitor timeVisitor = new TimeVisitor();
		runtimeExperiment.getRuntimeTree().apply(timeVisitor);

		// set global time
		this.setGlobalTime(timeVisitor.getTime(), runtimeExperiment.getRuntimeTree());

		ExitVisitor exitVisitor = new ExitVisitor();
		runtimeExperiment.getRuntimeTree().apply(exitVisitor);

		geppettoManagerCallbackListener.updateReady(GeppettoEvents.EXPERIMENT_UPDATE, runtimeExperiment.getRuntimeTree());
	}

	/**
	 * Updates the time node in the run time tree root node
	 * 
	 * @param newTimeValue
	 *            - New time
	 * @param tree
	 *            -Tree root node
	 */
	private void setGlobalTime(double newTimeValue, RuntimeTreeRoot tree)
	{
		runtime += newTimeValue;
		VariableNode time = new VariableNode("time");
		time.setUnit(new Unit(timeStepUnit));
		Quantity t = new Quantity();
		t.setValue(ValuesFactory.getDoubleValue(runtime));
		time.addQuantity(t);
		time.setParent(tree);
		tree.setTime(time);
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
		listener=null;
		geppettoManagerCallbackListener = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulation.ISimulatorCallbackListener#endOfSteps(java.lang.String)
	 */
	@Override
	public void endOfSteps(AspectNode aspectNode, File recordingsLocation) throws GeppettoExecutionException
	{
		SimulatorRuntime simulatorRuntime = simulatorRuntimes.get(aspectNode.getInstancePath());

		if(!DataManagerHelper.getDataManager().isDefault())
		{
			try
			{
				// where we store the results in S3
				String fileName = recordingsLocation.getPath().substring(recordingsLocation.getPath().lastIndexOf("/") + 1);
				String newPath = "projects/" + Long.toString(project.getId()) + "/" + experiment.getId() + "/" + aspectNode.getInstancePath() + "/" + fileName;
				S3Manager.getInstance().saveFileToS3(recordingsLocation, newPath);

				IInstancePath aspect = DataManagerHelper.getDataManager().newInstancePath(aspectNode);
				IPersistedData recording = DataManagerHelper.getDataManager().newPersistedData(S3Manager.getInstance().getURL(newPath), PersistedDataType.RECORDING);
				ISimulationResult results = DataManagerHelper.getDataManager().newSimulationResult(aspect, recording);

				experiment.addSimulationResult(results);
				DataManagerHelper.getDataManager().saveEntity(experiment.getParentProject());
			}
			catch(MalformedURLException e)
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
	public void stepped(AspectNode aspect) throws GeppettoExecutionException
	{
		String instancePath = aspect.getInstancePath();
		SimulatorRuntime simulatorRuntime = simulatorRuntimes.get(instancePath);
		simulatorRuntime.incrementProcessedSteps();
		simulatorRuntime.setStatus(SimulatorRuntimeStatus.STEPPED);
		// TODO What else?
	}

}
