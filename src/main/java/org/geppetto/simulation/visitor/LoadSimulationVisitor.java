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
package org.geppetto.simulation.visitor;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geppetto.core.common.GeppettoErrorCodes;
import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.conversion.ConversionException;
import org.geppetto.core.conversion.IConversion;
import org.geppetto.core.model.IModel;
import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.model.ModelInterpreterException;
import org.geppetto.core.model.simulation.Model;
import org.geppetto.core.model.simulation.Simulator;
import org.geppetto.core.model.simulation.visitor.BaseVisitor;
import org.geppetto.core.model.simulation.visitor.TraversingVisitor;
import org.geppetto.core.services.ModelFormat;
import org.geppetto.core.simulation.ISimulationCallbackListener;
import org.geppetto.core.simulator.ISimulator;
import org.geppetto.simulation.SessionContext;
import org.geppetto.simulation.SimulatorCallbackListener;

/**
 * This visitor loads a simulation
 * 
 * @author matteocantarelli
 * 
 */
public class LoadSimulationVisitor extends TraversingVisitor
{

	private SessionContext _sessionContext;
	private ISimulationCallbackListener _simulationCallback;
	private static Log _logger = LogFactory.getLog(LoadSimulationVisitor.class);

	public LoadSimulationVisitor(SessionContext sessionContext, ISimulationCallbackListener simulationListener)
	{
		super(new DepthFirstTraverserEntitiesFirst(), new BaseVisitor());
		_sessionContext = sessionContext;
		_simulationCallback = simulationListener;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.massfords.humantask.TraversingVisitor#visit(org.geppetto.simulation.model.Model)
	 */
	@Override
	public void visit(Model pModel)
	{
		super.visit(pModel);
		try
		{
			IModelInterpreter modelInterpreter = _sessionContext.getModelInterpreter(pModel);
			IModel model = _sessionContext.getIModel(pModel.getInstancePath());
			if(model == null)
			{
				List<URL> recordings = new ArrayList<URL>();
				if(pModel.getRecordingURL() != null)
				{
					// add all the recordings found
					for(String recording : pModel.getRecordingURL())
					{
						URL url = null;
						url = this.getClass().getResource(recording);
						recordings.add(url);
					}
				}

				long start = System.currentTimeMillis();

				URL modelUrl = null;
				if(pModel.getModelURL() != null)
				{
					modelUrl = new URL(pModel.getModelURL());
				}
				model = modelInterpreter.readModel(modelUrl, recordings, pModel.getParentAspect().getInstancePath());
				model.setInstancePath(pModel.getInstancePath());
				_sessionContext.getModels().put(pModel.getInstancePath(), model);

				long end = System.currentTimeMillis();
				_logger.info("Finished reading model, took " + (end - start) + " ms ");

			}
			else
			{
				// the model is already loaded, we are coming here after a stop simulation which doesn't delete the model, do nothing.
			}

		}
		catch(GeppettoInitializationException e)
		{
			_logger.error("Error: ", e);
			_simulationCallback.error(GeppettoErrorCodes.SIMULATION, this.getClass().getName(), null, e);
		}
		catch(MalformedURLException e)
		{
			_logger.error("Malformed URL for model", e);
			_simulationCallback.error(GeppettoErrorCodes.SIMULATION, this.getClass().getName(), "Unable to load model for " + pModel.getInstancePath(), e);

		}
		catch(ModelInterpreterException e)
		{
			_logger.error("Error Reading Model", e);
			_simulationCallback.error(GeppettoErrorCodes.SIMULATION, this.getClass().getName(), "Unable to load model for " + pModel.getInstancePath(), e);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.massfords.humantask.TraversingVisitor#visit(org.geppetto.simulation.model.Simulator)
	 */
	@Override
	public void visit(Simulator simulatorModel)
	{
		super.visit(simulatorModel);
		try
		{
			IConversion conversion = _sessionContext.getConversion(simulatorModel);
			ISimulator simulator = _sessionContext.getSimulator(simulatorModel);
			if(conversion != null || simulator != null)
			{
				// initialize simulator
				GetModelsForSimulatorVisitor getModelsForSimulatorVisitor = new GetModelsForSimulatorVisitor(simulatorModel);
				simulatorModel.getParentAspect().getParentEntity().accept(getModelsForSimulatorVisitor);
				List<Model> models = getModelsForSimulatorVisitor.getModels();

				_sessionContext.mapSimulatorToModels(simulatorModel, models);

				// Builds a list with the IModel corresponding to the discovered models.
				List<IModel> iModels = new ArrayList<IModel>();
				for(Model m : models)
				{
					iModels.add(_sessionContext.getIModel(m.getInstancePath()));
					// store in the session context what simulator is in charge of a given model
					_sessionContext.mapModelToSimulator(m, simulatorModel);
				}
				
				if(conversion != null)
				{
					// TODO Refactor simulators to deal with more than one model!
					IModel iModelConverted = conversion.convert(iModels.get(0), new ModelFormat("NeuroML"), new ModelFormat("Neuron"));
					
					//Replace model
					_sessionContext.getModels().put(iModelConverted.getInstancePath(), iModelConverted);
					iModels = new ArrayList<IModel>();
					for(Model m : models)
					{
						iModels.add(_sessionContext.getIModel(m.getInstancePath()));
					}
				}

				if(simulator != null)
				{

					long start = System.currentTimeMillis();

					SimulatorCallbackListener callbackListener = new SimulatorCallbackListener(simulatorModel, _sessionContext, _simulationCallback);
					simulator.initialize(iModels, callbackListener);
					long end = System.currentTimeMillis();
					_logger.info("Finished initializing simulator, took " + (end - start) + " ms ");

				}
				else
				{
					_simulationCallback.error(GeppettoErrorCodes.SIMULATION, this.getClass().getName(), "A simulator for " + simulatorModel.getInstancePath()
							+ " already exists, something did not get cleared", null);
				}
			}
		}
		catch(GeppettoInitializationException e)
		{
			_logger.error("Error: ", e);
			_simulationCallback.error(GeppettoErrorCodes.SIMULATION, this.getClass().getName(), null, e);
		}
		catch(GeppettoExecutionException e)
		{
			_logger.error("Error: ", e);
			_simulationCallback.error(GeppettoErrorCodes.SIMULATION, this.getClass().getName(), null, e);
		}
		catch(ConversionException e)
		{
			_logger.error("Error: ", e);
			_simulationCallback.error(GeppettoErrorCodes.SIMULATION, this.getClass().getName(), null, e);
		}
	}

}
