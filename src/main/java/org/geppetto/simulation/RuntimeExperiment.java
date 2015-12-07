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
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.common.HDF5Reader;
import org.geppetto.core.data.DataManagerHelper;
import org.geppetto.core.data.model.IAspectConfiguration;
import org.geppetto.core.data.model.IExperiment;
import org.geppetto.core.data.model.IInstancePath;
import org.geppetto.core.data.model.IParameter;
import org.geppetto.core.data.model.ISimulationResult;
import org.geppetto.core.data.model.ISimulatorConfiguration;
import org.geppetto.core.data.model.ResultsFormat;
import org.geppetto.core.features.ISetParameterFeature;
import org.geppetto.core.manager.Scope;
import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.model.ModelInterpreterException;
import org.geppetto.core.model.RecordingModel;
import org.geppetto.core.services.DropboxUploadService;
import org.geppetto.core.services.GeppettoFeature;
import org.geppetto.core.services.ModelFormat;
import org.geppetto.core.services.registry.ServicesRegistry;
import org.geppetto.core.simulator.RecordingReader;
import org.geppetto.core.utilities.URLReader;
import org.geppetto.model.ExperimentState;
import org.geppetto.model.GeppettoFactory;
import org.geppetto.model.VariableValue;
import org.geppetto.model.util.GeppettoModelException;
import org.geppetto.model.util.PointerUtility;
import org.geppetto.model.values.Pointer;
import org.geppetto.model.values.Quantity;
import org.geppetto.model.values.ValuesFactory;

public class RuntimeExperiment
{

	private ExperimentState experimentState;

	private IExperiment experiment;

	private RuntimeProject runtimeProject;

	private static Log logger = LogFactory.getLog(RuntimeExperiment.class);

	public RuntimeExperiment(RuntimeProject runtimeProject, IExperiment experiment) throws GeppettoExecutionException
	{
		this.experiment = experiment;
		this.runtimeProject = runtimeProject;
		// every experiment has a state
		experimentState = GeppettoFactory.eINSTANCE.createExperimentState();
		init();
	}

	private void init() throws GeppettoExecutionException
	{
		// let's set the parameters if they exist
		for(IAspectConfiguration ac : experiment.getAspectConfigurations())
		{
			if(ac.getModelParameter() != null && !ac.getModelParameter().isEmpty())
			{
				setModelParameters(ac.getModelParameter());
			}
		}

	}

	/**
	 * @param recordedVariables
	 * @throws GeppettoModelException 
	 * @throws GeppettoExecutionException
	 * @throws GeppettoInitializationException
	 */
	public void setWatchedVariables(List<String> recordedVariables) throws GeppettoModelException
	{

		try
		{
			logger.info("Setting watched variables in simulation tree");

			for(String recordedVariable : recordedVariables)
			{

				Pointer pointer = PointerUtility.getPointer(runtimeProject.getGeppettoModel(), recordedVariable);

				IAspectConfiguration aspectConfiguration = getAspectConfiguration(pointer);

				// first let's update the model state
				VariableValue variableValue = null;
				for(VariableValue vv : experimentState.getRecordedVariables())
				{
					if(vv.getPointer().equals(pointer)) // IT FIXME implement equals or add utility method
					{
						variableValue = vv;
						break;
					}
				}
				if(variableValue != null)
				{
					// it already existed, we remove it, it means we are stop watching it
					// Matteo: I don't like this but not changing it
					experimentState.getRecordedVariables().remove(variableValue);

					// now let's update the DB
					IInstancePath instancePath = null;
					for(IInstancePath variable : aspectConfiguration.getWatchedVariables())
					{
						if(variable.getInstancePath().equals(recordedVariable))
						{
							instancePath = variable;
							break;
						}
					}
					if(instancePath != null)
					{
						aspectConfiguration.getWatchedVariables().remove(instancePath);
					}
				}
				else
				{
					// we add it
					variableValue = GeppettoFactory.eINSTANCE.createVariableValue();
					variableValue.setPointer(pointer);
					experimentState.getRecordedVariables().add(variableValue);

					// now let's update the DB
					IInstancePath instancePath = DataManagerHelper.getDataManager().newInstancePath(pointer.getInstancePath());
					DataManagerHelper.getDataManager().addWatchedVariable(aspectConfiguration, instancePath);
				}

			}

			DataManagerHelper.getDataManager().saveEntity(experiment);
		}
		catch(GeppettoModelException e)
		{
			throw new GeppettoModelException(e);
		}

	}

	/**
	 * 
	 */
	public void release()
	{
		// IT FIXME Release ExperimentState
	}

	/**
	 * @param instancePath
	 * @return
	 * @throws GeppettoExecutionException
	 */
	public File downloadModel(String instancePath, ModelFormat format) throws GeppettoExecutionException
	{
		try
		{
			logger.info("Downloading Model for " + instancePath + " in format " + format);

			// find model interpreter
			Pointer pointer = PointerUtility.getPointer(runtimeProject.getGeppettoModel(), instancePath);
			IModelInterpreter modelInterpreter = runtimeProject.getModelInterpreter(pointer);
			ModelFormat modelFormat = format;
			if(format == null)
			{
				// FIXME: We are assuming there is only one format
				List<ModelFormat> supportedOutputs = ServicesRegistry.getModelInterpreterServiceFormats(modelInterpreter);
				modelFormat = supportedOutputs.get(0);
			}

			return modelInterpreter.downloadModel(pointer, modelFormat, getAspectConfiguration(pointer));
		}
		catch(ModelInterpreterException | GeppettoModelException e)
		{
			throw new GeppettoExecutionException(e);
		}
	}

	/**
	 * @param aspectInstancePath
	 * @return
	 * @throws GeppettoExecutionException
	 */
	public List<ModelFormat> supportedOuputs(String instancePath) throws GeppettoExecutionException
	{
		try
		{
			logger.info("Getting supported outputs for " + instancePath);

			Pointer pointer = PointerUtility.getPointer(runtimeProject.getGeppettoModel(), instancePath);
			IModelInterpreter modelInterpreter = runtimeProject.getModelInterpreter(pointer);
			return modelInterpreter.getSupportedOutputs(pointer);
		}
		catch(ModelInterpreterException | GeppettoModelException e)
		{
			throw new GeppettoExecutionException(e);
		}
	}

	/**
	 * @return
	 * @throws GeppettoExecutionException
	 */
	public ExperimentState getRecordedVariables() throws GeppettoExecutionException
	{
		for(ISimulationResult result : experiment.getSimulationResults())
		{
			if(result.getFormat().equals(ResultsFormat.GEPPETTO_RECORDING))
			{
				URL url;
				try
				{
					url = URLReader.getURL(result.getResult().getUrl());
				}
				catch(IOException e)
				{
					throw new GeppettoExecutionException(e);
				}

				RecordingReader recordingReader = new RecordingReader(new RecordingModel(HDF5Reader.readHDF5File(url, experiment.getParentProject().getId())), result.getFormat());

				// get all aspect configurations
				List<IAspectConfiguration> aspectConfigs = (List<IAspectConfiguration>) experiment.getAspectConfigurations();

				// get all watched variables from all aspect configurations
				List<IInstancePath> watchedVariables = new ArrayList<IInstancePath>();
				for(IAspectConfiguration aspectConfig : aspectConfigs)
				{
					for(IInstancePath ip : aspectConfig.getWatchedVariables())
					{
						watchedVariables.add(ip);
					}
				}

				if(watchedVariables.size() > 0)
				{
					// after reading values out from recording, amp to the correct aspect given the watched variable
					for(IInstancePath watchedVariable : watchedVariables)
					{
						logger.info("Reading results for " + watchedVariable.getInstancePath());

						// we add to the model state every variable that was recorded
						recordingReader.readRecording(watchedVariable, experimentState, true);

						logger.info("Finished reading results for " + watchedVariable.getInstancePath());
					}

				}
			}
		}
		return experimentState;
	}

	/**
	 * @param experiment
	 * @param instancePath
	 * @return
	 */
	private IAspectConfiguration getAspectConfiguration(Pointer pointer)
	{
		// Check if it is a subAspect Instance Path and extract the base one
		String instancePathString = pointer.getInstancePath();
		// IT FIXME This algorithm is not valid anymore
		String[] instancePathSplit = instancePathString.split("\\.");
		if(instancePathSplit.length > 2)
		{
			instancePathString = instancePathSplit[0] + "." + instancePathSplit[2];
		}

		for(IAspectConfiguration aspectConfig : experiment.getAspectConfigurations())
		{
			if(aspectConfig.getAspect().getInstancePath().equals(instancePathString))
			{
				return aspectConfig;
			}
		}
		// IT FIXME Moved from SetWatchedVariablesVisitor
		// if an aspect configuration doesn't already exist we create it
		IInstancePath instancePath = DataManagerHelper.getDataManager().newInstancePath(pointer.getInstancePath());
		ISimulatorConfiguration simulatorConfiguration = DataManagerHelper.getDataManager().newSimulatorConfiguration("", "", 0l, 0l);
		return DataManagerHelper.getDataManager().newAspectConfiguration(experiment, instancePath, simulatorConfiguration);
	}

	/**
	 * @param instancePath
	 * @param modelParameter
	 * @return
	 * @throws GeppettoExecutionException
	 */
	private ExperimentState setModelParameters(List<? extends IParameter> modelParameter) throws GeppettoExecutionException
	{
		Map<String, String> parameters = new HashMap<String, String>();
		for(IParameter p : modelParameter)
		{
			parameters.put(p.getVariable().getInstancePath(), p.getValue());
		}
		return setModelParameters(parameters);
	}

	/**
	 * @param modelAspectPath
	 * @param parameters
	 * @return
	 * @throws GeppettoExecutionException
	 */
	public ExperimentState setModelParameters(Map<String, String> parameters) throws GeppettoExecutionException
	{
		for(String parameter : parameters.keySet())
		{
			try
			{
				Pointer pointer = PointerUtility.getPointer(runtimeProject.getGeppettoModel(), parameter);
				IModelInterpreter modelInterpreter = runtimeProject.getModelInterpreter(pointer);

				if(!modelInterpreter.isSupported(GeppettoFeature.SET_PARAMETERS_FEATURE))
				{
					throw new GeppettoExecutionException("The model interpreter for the parameter " + parameter + " does not support the setParameter Feature");

				}
				Map<String, String> parameterValue = new HashMap<String, String>();
				parameterValue.put(parameter, parameters.get(parameter));

				Quantity value = ValuesFactory.eINSTANCE.createQuantity();
				value.setValue(Double.valueOf(parameters.get(parameter)));
				VariableValue variableValue = null;
				// let's look if the same parameter has already been set, in that case we update the model
				for(VariableValue vv : experimentState.getSetParameters())
				{
					if(vv.getPointer().equals(pointer))// IT FIXME implement equals or add utility method
					{
						variableValue = vv;
					}
				}
				// it didn't exist, we create it
				if(variableValue == null)
				{
					variableValue = GeppettoFactory.eINSTANCE.createVariableValue();
					variableValue.setPointer(pointer);
					experimentState.getSetParameters().add(variableValue);
				}
				variableValue.setValue(value);

				((ISetParameterFeature) modelInterpreter.getFeature(GeppettoFeature.SET_PARAMETERS_FEATURE)).setParameter(variableValue);

				IAspectConfiguration config = getAspectConfiguration(pointer);
				for(String path : parameters.keySet())
				{
					IParameter existingParameter = null;
					for(IParameter p : config.getModelParameter())
					{
						if(p.getVariable().getInstancePath().equals(path))
						{
							existingParameter = p;
							break;
						}
					}
					if(existingParameter != null)
					{
						existingParameter.setValue(parameters.get(path));
					}
					else
					{
						IInstancePath instancePath = DataManagerHelper.getDataManager().newInstancePath(pointer.getInstancePath());
						config.addModelParameter(DataManagerHelper.getDataManager().newParameter(instancePath, parameters.get(path)));
					}
				}
			}
			catch(ModelInterpreterException | GeppettoModelException e)
			{
				throw new GeppettoExecutionException(e);
			}

		}

		return experimentState;
	}

	/**
	 * @param aspectID
	 * @param format
	 * @param dropboxService
	 * @throws GeppettoExecutionException
	 */
	public void uploadResults(String aspectID, ResultsFormat format, DropboxUploadService dropboxService) throws GeppettoExecutionException
	{
		for(ISimulationResult result : experiment.getSimulationResults())
		{
			if(result.getAspect().getInstancePath().equals(aspectID))
			{
				if(result.getFormat().equals(format))
				{
					URL url;
					try
					{
						url = URLReader.getURL(result.getResult().getUrl());
						dropboxService.upload(new File(URLReader.createLocalCopy(Scope.CONNECTION, experiment.getParentProject().getId(), url).toURI()));
					}
					catch(Exception e)
					{
						throw new GeppettoExecutionException(e);
					}
				}
			}
		}
	}

	public ExperimentState getExperimentState()
	{
		// TODO Auto-generated method stub
		return null;
	}

}
