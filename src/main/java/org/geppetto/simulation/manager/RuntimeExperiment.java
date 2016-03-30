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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.common.HDF5Reader;
import org.geppetto.core.data.DataManagerHelper;
import org.geppetto.core.data.model.ExperimentStatus;
import org.geppetto.core.data.model.IAspectConfiguration;
import org.geppetto.core.data.model.IExperiment;
import org.geppetto.core.data.model.IParameter;
import org.geppetto.core.data.model.ISimulationResult;
import org.geppetto.core.data.model.ResultsFormat;
import org.geppetto.core.features.ISetParameterFeature;
import org.geppetto.core.manager.Scope;
import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.model.ModelInterpreterException;
import org.geppetto.core.model.Recording;
import org.geppetto.core.services.DropboxUploadService;
import org.geppetto.core.services.GeppettoFeature;
import org.geppetto.core.services.registry.ServicesRegistry;
import org.geppetto.core.simulator.RecordingReader;
import org.geppetto.core.utilities.URLReader;
import org.geppetto.model.ExperimentState;
import org.geppetto.model.GeppettoFactory;
import org.geppetto.model.ModelFormat;
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
		experimentState.setExperimentId(experiment.getId());
		init();
	}

	private void init() throws GeppettoExecutionException
	{
		try
		{
			// let's set the parameters if they exist
			if(experiment.getAspectConfigurations() != null)
			{
				for(IAspectConfiguration ac : experiment.getAspectConfigurations())
				{
					if(ac.getModelParameter() != null && !ac.getModelParameter().isEmpty())
					{
						setModelParameters(ac.getModelParameter());
					}
					if(ac.getWatchedVariables() != null && !ac.getWatchedVariables().isEmpty())
					{
						for(String instancePath : ac.getWatchedVariables())
						{
							VariableValue variableValue = GeppettoFactory.eINSTANCE.createVariableValue();
							variableValue.setPointer(PointerUtility.getPointer(runtimeProject.getGeppettoModel(), instancePath));
							experimentState.getRecordedVariables().add(variableValue);
						}
					}

				}
				VariableValue time = GeppettoFactory.eINSTANCE.createVariableValue();
				time.setPointer(PointerUtility.getPointer(runtimeProject.getGeppettoModel(), "time(StateVariable)"));
				experimentState.getRecordedVariables().add(time);
			}
		}
		catch(GeppettoModelException e)
		{
			throw new GeppettoExecutionException(e);
		}
	}

	/**
	 * @param recordedVariables
	 * @return
	 * @throws GeppettoExecutionException
	 */
	/**
	 * @param recordedVariables
	 * @param watch
	 * @return
	 * @throws GeppettoExecutionException
	 */
	private ExperimentState doSetWatchedVariables(List<String> recordedVariables, boolean watch) throws GeppettoExecutionException
	{
		logger.info("Setting watched variables in simulation tree");
		try
		{
			for(String recordedVariable : recordedVariables)
			{

				Pointer pointer = PointerUtility.getPointer(runtimeProject.getGeppettoModel(), recordedVariable);

				IAspectConfiguration aspectConfiguration = getAspectConfiguration(pointer);

				// first let's update the model state
				VariableValue variableValue = null;
				for(VariableValue vv : experimentState.getRecordedVariables())
				{
					if(PointerUtility.equals(vv.getPointer(), pointer))
					{
						variableValue = vv;
						break;
					}
				}
				if(!watch)
				{
					if(variableValue != null)
					{
						// it already existed, we remove it, it means we are stop watching it
						// Matteo: I don't like this but not changing it
						experimentState.getRecordedVariables().remove(variableValue);

						// now let's update the DB
						String instancePath = null;
						for(String variable : aspectConfiguration.getWatchedVariables())
						{
							if(variable.equals(recordedVariable))
							{
								instancePath = variable;
								break;
							}
							if(PointerUtility.getPathWithoutTypes(variable).equals(recordedVariable))
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
				}
				else
				{
					if(variableValue == null)
					{
						// we add it
						variableValue = GeppettoFactory.eINSTANCE.createVariableValue();
						variableValue.setPointer(pointer);
						experimentState.getRecordedVariables().add(variableValue);

						// now let's update the DB
						DataManagerHelper.getDataManager().addWatchedVariable(aspectConfiguration, recordedVariable);
					}
				}

			}

			DataManagerHelper.getDataManager().saveEntity(experiment);
		}
		catch(GeppettoModelException e)
		{
			throw new GeppettoExecutionException(e);
		}
		return experimentState;
	}

	/**
	 * @param recordedVariables
	 * @throws GeppettoModelException
	 * @throws GeppettoExecutionException
	 * @throws GeppettoInitializationException
	 */
	public ExperimentState setWatchedVariables(List<String> recordedVariables, boolean watch) throws GeppettoExecutionException
	{
		if(!experiment.getStatus().equals(ExperimentStatus.DESIGN))
		{
			throw new GeppettoExecutionException("Cannot set what variables to record for an experiment not in DESIGN");
		}
		return doSetWatchedVariables(recordedVariables, watch);
	}

	/**
	 * 
	 */
	public void release()
	{
		experimentState.getRecordedVariables().clear();
		experimentState.getSetParameters().clear();
		experimentState = null;
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
	public ExperimentState getRecordedVariables(List<String> filter) throws GeppettoExecutionException
	{
		for(ISimulationResult result : experiment.getSimulationResults())
		{
			if(result.getFormat().equals(ResultsFormat.GEPPETTO_RECORDING))
			{
				RecordingReader recordingReader = null;
				// after reading values out from recording, amp to the correct aspect given the watched variable

				if(recordingReader == null)
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

					recordingReader = new RecordingReader(new Recording(HDF5Reader.readHDF5File(url, experiment.getParentProject().getId())), result.getFormat());
				}

				for(VariableValue watchedVariableValue : experimentState.getRecordedVariables())
				{
					boolean removed = false;
					String watchedVariable = watchedVariableValue.getPointer().getInstancePath();
					if(filter != null)
					{
						if(!filter.contains(watchedVariable) && !watchedVariable.equals("time(StateVariable)"))
						{
							watchedVariableValue.setValue(null);
							removed = true;
						}
					}
					if(!removed && watchedVariableValue.getValue() == null)
					{
						// we add to the model state every variable that was recorded
						recordingReader.readRecording(watchedVariable, experimentState, true);
						logger.info("Finished reading results for " + watchedVariable);
					}
				}

				recordingReader.closeRecording();

			}

		}
		return experimentState;
	}

	/**
	 * @param experiment
	 * @param instancePath
	 * @return
	 * @throws GeppettoExecutionException
	 */
	private IAspectConfiguration getAspectConfiguration(Pointer pointer) throws GeppettoModelException
	{
		String instancePathString = pointer.getInstancePath();

		for(IAspectConfiguration aspectConfig : experiment.getAspectConfigurations())
		{
			if(instancePathString.startsWith(aspectConfig.getInstance()))
			{
				return aspectConfig;
			}
		}
		throw new GeppettoModelException("Cannot find an aspect configuration for the pointer " + pointer.getInstancePath());
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
			parameters.put(p.getVariable(), p.getValue());
		}
		return doSetModelParameters(parameters);
	}

	/**
	 * @param modelAspectPath
	 * @param parameters
	 * @return
	 * @throws GeppettoExecutionException
	 */
	public ExperimentState setModelParameters(Map<String, String> parameters) throws GeppettoExecutionException
	{
		if(!experiment.getStatus().equals(ExperimentStatus.DESIGN))
		{
			throw new GeppettoExecutionException("Cannot set the value of parameters for an experiment not in DESIGN");
		}
		return doSetModelParameters(parameters);
	}

	/**
	 * @param parameters
	 * @return
	 * @throws GeppettoExecutionException
	 */
	private ExperimentState doSetModelParameters(Map<String, String> parameters) throws GeppettoExecutionException
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
					if(PointerUtility.equals(vv.getPointer(), pointer))
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
						if(p.getVariable().equals(path))
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
						config.addModelParameter(DataManagerHelper.getDataManager().newParameter(parameter, parameters.get(path)));
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
			if(result.getSimulatedInstance().equals(aspectID))
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
		return experimentState;
	}

}
