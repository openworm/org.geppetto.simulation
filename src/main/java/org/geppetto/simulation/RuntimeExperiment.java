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
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.common.HDF5Reader;
import org.geppetto.core.data.DataManagerHelper;
import org.geppetto.core.data.model.ExperimentStatus;
import org.geppetto.core.data.model.IAspectConfiguration;
import org.geppetto.core.data.model.IExperiment;
import org.geppetto.core.data.model.IInstancePath;
import org.geppetto.core.data.model.ISimulationResult;
import org.geppetto.core.model.IModel;
import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.model.runtime.ACompositeNode;
import org.geppetto.core.model.runtime.ANode;
import org.geppetto.core.model.runtime.AspectNode;
import org.geppetto.core.model.runtime.AspectSubTreeNode;
import org.geppetto.core.model.runtime.CompositeNode;
import org.geppetto.core.model.runtime.VariableNode;
import org.geppetto.core.model.runtime.AspectSubTreeNode.AspectTreeType;
import org.geppetto.core.model.runtime.RuntimeTreeRoot;
import org.geppetto.core.model.simulation.GeppettoModel;
import org.geppetto.core.model.state.visitors.SetWatchedVariablesVisitor;
import org.geppetto.core.services.ModelFormat;
import org.geppetto.core.simulation.IGeppettoManagerCallbackListener;
import org.geppetto.core.simulator.RecordingReader;
import org.geppetto.core.utilities.URLReader;
import org.geppetto.simulation.visitor.CreateModelInterpreterServicesVisitor;
import org.geppetto.simulation.visitor.CreateRuntimeTreeVisitor;
import org.geppetto.simulation.visitor.DownloadModelVisitor;
import org.geppetto.simulation.visitor.FindAspectNodeVisitor;
import org.geppetto.simulation.visitor.LoadSimulationVisitor;
import org.geppetto.simulation.visitor.PopulateModelTreeVisitor;
import org.geppetto.simulation.visitor.PopulateSimulationTreeVisitor;
import org.geppetto.simulation.visitor.PopulateVisualTreeVisitor;
import org.geppetto.simulation.visitor.SetParametersVisitor;
import org.geppetto.simulation.visitor.SupportedOutputsVisitor;

public class RuntimeExperiment
{

	private Map<String, IModelInterpreter> modelInterpreters = new HashMap<String, IModelInterpreter>();

	private Map<String, IModel> instancePathToIModelMap = new HashMap<>();

	private IGeppettoManagerCallbackListener geppettoManagerCallbackListener;

	// Head node that holds the entities
	private RuntimeTreeRoot runtimeTreeRoot = new RuntimeTreeRoot("scene");

	private IExperiment experiment;

	private static Log logger = LogFactory.getLog(RuntimeExperiment.class);

	public RuntimeExperiment(RuntimeProject runtimeProject, IExperiment experiment, IGeppettoManagerCallbackListener geppettoManagerCallbackListener)
	{
		this.experiment = experiment;
		this.geppettoManagerCallbackListener = geppettoManagerCallbackListener;
		init(runtimeProject.getGeppettoModel());
	}

	private void init(GeppettoModel geppettoModel)
	{
		this.clearWatchLists();

		// retrieve model interpreters and simulators
		CreateModelInterpreterServicesVisitor createServicesVisitor = new CreateModelInterpreterServicesVisitor(modelInterpreters, geppettoManagerCallbackListener);
		geppettoModel.accept(createServicesVisitor);

		LoadSimulationVisitor loadSimulationVisitor = new LoadSimulationVisitor(modelInterpreters, instancePathToIModelMap, geppettoManagerCallbackListener);
		geppettoModel.accept(loadSimulationVisitor);

		CreateRuntimeTreeVisitor runtimeTreeVisitor = new CreateRuntimeTreeVisitor(modelInterpreters, instancePathToIModelMap, runtimeTreeRoot, geppettoManagerCallbackListener);
		geppettoModel.accept(runtimeTreeVisitor);

		runtimeTreeRoot = runtimeTreeVisitor.getRuntimeModel();

		PopulateVisualTreeVisitor populateVisualVisitor = new PopulateVisualTreeVisitor(geppettoManagerCallbackListener);
		runtimeTreeRoot.apply(populateVisualVisitor);
		
		//create variables for each aspect node's simulation tree
		for(IAspectConfiguration a : experiment.getAspectConfigurations()){
			List<String> variables = new ArrayList<String>();
			List<? extends IInstancePath> vars = a.getWatchedVariables();
			if (vars != null){
				for(IInstancePath i : vars){
					String var =i.getInstancePath();
					variables.add(var);
				}
			}
			String aspect = a.getAspect().getInstancePath();
			FindAspectNodeVisitor findAspectNodeVisitor = new FindAspectNodeVisitor(aspect);
			runtimeTreeRoot.apply(findAspectNodeVisitor);
			AspectNode node = findAspectNodeVisitor.getAspectNode();
			this.createVariables(variables, node.getSubTree(AspectTreeType.SIMULATION_TREE));
		}


	}

	public Map<String, IModel> getInstancePathToIModelMap()
	{
		return instancePathToIModelMap;
	}

	public Map<String, IModelInterpreter> getModelInterpreters()
	{
		return modelInterpreters;
	}

	/**
	 * 
	 */
	public void clearWatchLists()
	{
		logger.info("Clearing watched variables in simulation tree");

		// Update the RunTimeTreeModel setting watched to false for every node
		SetWatchedVariablesVisitor clearWatchedVariablesVisitor = new SetWatchedVariablesVisitor();
		runtimeTreeRoot.apply(clearWatchedVariablesVisitor);

		if(experiment.getStatus().equals(ExperimentStatus.DESIGN))
		{
			// if we are still in design we ask the DataManager to change what we are watching
			// TODO Do we need "recordedVariables"? Thinking of the scenario that we recorded many and we
			// only want to get a portion of them in the client
			List<? extends IAspectConfiguration> aspectConfigs = experiment.getAspectConfigurations();
			for(IAspectConfiguration aspectConfig : aspectConfigs)
			{
				DataManagerHelper.getDataManager().clearWatchedVariables(aspectConfig);
			}
		}
		else
		{
			// TODO Exception or we change the "watched" and keep the "recorded"?
		}

		// SIM TODO instruct aspects to clear watch variables, this allows to change what a dynamic simulator
		// is recording while they are doing it, do we keep this?
		// for(ISimulator simulator : _sessionContext.getSimulators().values())
		// {
		// if(simulator != null)
		// {
		// IVariableWatchFeature watchFeature = ((IVariableWatchFeature) simulator.getFeature(GeppettoFeature.VARIABLE_WATCH_FEATURE));
		// if(watchFeature != null)
		// {
		// watchFeature.clearWatchVariables();
		// }
		// }
		// }

	}

	/**
	 * @param watchedVariables
	 * @throws GeppettoExecutionException
	 * @throws GeppettoInitializationException
	 */
	public void setWatchedVariables(List<String> watchedVariables)
	{
		logger.info("Setting watched variables in simulation tree");

		// Update the RunTimeTreeModel
		SetWatchedVariablesVisitor setWatchedVariablesVisitor = new SetWatchedVariablesVisitor(watchedVariables);
		runtimeTreeRoot.apply(setWatchedVariablesVisitor);

		if(experiment.getStatus().equals(ExperimentStatus.DESIGN))
		{
			// if we are still in design we ask the DataManager to change what we are watching
			// TODO Do we need "recordedVariables"? Thinking of the scenario that we recorded many and we
			// only want to get a portion of them in the client
			List<? extends IAspectConfiguration> aspectConfigs = experiment.getAspectConfigurations();
			for(IAspectConfiguration aspectConfig : aspectConfigs)
			{
				// TODO When do we create the aspect config? How do we map them to the variables?
				// DataManagerHelper.getDataManager().setWatchedVariables(aspectConfig, watchedVariables);
			}
		}
		else
		{
			// TODO Exception or we change the "watched" and keep the "recorded"?
		}

		// SIM TODO
		// Call the function for each simulator, , this allows to change what a dynamic simulator
		// is recording while they are doing it, do we keep this?
		// for(Simulator simulatorModel : _sessionContext.getSimulators().keySet())
		// {
		// ISimulator simulator = _sessionContext.getSimulator(simulatorModel);
		// IVariableWatchFeature watchFeature = ((IVariableWatchFeature) simulator.getFeature(GeppettoFeature.VARIABLE_WATCH_FEATURE));
		// if(watchFeature != null)
		// {
		// watchFeature.setWatchedVariables(watchedVariables);
		// }
		// }
	}

	/**
	 * 
	 */
	public void release()
	{
		modelInterpreters.clear();
		instancePathToIModelMap.clear();
		runtimeTreeRoot = null;
	}

	/**
	 * @param aspectInstancePath
	 * @return
	 */
	public Map<String, AspectSubTreeNode> populateModelTree(String aspectInstancePath)
	{
		logger.info("Populating Model Tree for " + aspectInstancePath);
		PopulateModelTreeVisitor populateModelVisitor = new PopulateModelTreeVisitor(geppettoManagerCallbackListener, aspectInstancePath);
		runtimeTreeRoot.apply(populateModelVisitor);
		return populateModelVisitor.getPopulatedModelTree();
	}

	/**
	 * @param aspectInstancePath
	 * @return
	 */
	public Map<String, AspectSubTreeNode> populateSimulationTree(String aspectInstancePath)
	{
		logger.info("Populating Simulation Tree for " + aspectInstancePath);
		PopulateSimulationTreeVisitor populateSimulationVisitor = new PopulateSimulationTreeVisitor(geppettoManagerCallbackListener, aspectInstancePath);
		runtimeTreeRoot.apply(populateSimulationVisitor);
		
		return populateSimulationVisitor.getPopulatedSimulationTree();
	}

	/**
	 * @return
	 */
	public RuntimeTreeRoot getRuntimeTree()
	{
		return runtimeTreeRoot;
	}

	/**
	 * @param aspectInstancePath
	 * @return
	 */
	public File downloadModel(String aspectInstancePath, ModelFormat format)
	{
		logger.info("Downloading Model for " + aspectInstancePath + " in format " + format);
		DownloadModelVisitor downloadModelVistor = new DownloadModelVisitor(geppettoManagerCallbackListener, aspectInstancePath, format);
		runtimeTreeRoot.apply(downloadModelVistor);
		return downloadModelVistor.getModelFile();
	}
	
	/**
	 * @param aspectInstancePath
	 * @return
	 */
	public List<ModelFormat> supportedOuputs(String aspectInstancePath)
	{
		logger.info("Getting supported outputs for " + aspectInstancePath);
		SupportedOutputsVisitor supportedOutputsModelVisitor = new SupportedOutputsVisitor(geppettoManagerCallbackListener, aspectInstancePath);
		runtimeTreeRoot.apply(supportedOutputsModelVisitor);
		return supportedOutputsModelVisitor.getSupportedOutputs();
	}

	/**
	 * @return
	 * @throws GeppettoExecutionException 
	 */
	public Map<String, AspectSubTreeNode> updateSimulationTreeWithResults() throws GeppettoExecutionException
	{
		Map<String, AspectSubTreeNode> loadedResults=new HashMap<String, AspectSubTreeNode>();
		for(ISimulationResult result : experiment.getSimulationResults())
		{
			String instancePath = result.getAspect().getInstancePath();

			//We first need to populate the simulation tree for the given aspect
			populateSimulationTree(instancePath);
			
			logger.info("Reading results for " + instancePath);
			FindAspectNodeVisitor findAspectNodeVisitor = new FindAspectNodeVisitor(instancePath);
			getRuntimeTree().apply(findAspectNodeVisitor);
			AspectNode aspect = findAspectNodeVisitor.getAspectNode();
			AspectSubTreeNode simulationTree = (AspectSubTreeNode) aspect.getSubTree(AspectTreeType.SIMULATION_TREE);
			simulationTree.setModified(true);
			aspect.setModified(true);
			aspect.getParentEntity().setModified(true);

			URL url;
			try
			{
				url = URLReader.getURL(result.getResult().getUrl());
			}
			catch(IOException e)
			{
				throw new GeppettoExecutionException(e);
			}
			
			RecordingReader recordingReader = new RecordingReader();
			IAspectConfiguration aspectConfig = getAspectConfiguration(experiment, instancePath);
			
			List<String> watchedVariables = new ArrayList<String>();
			for(IInstancePath ip : aspectConfig.getWatchedVariables())
			{
				watchedVariables.add(ip.getInstancePath());
			}

			recordingReader.readRecording(HDF5Reader.readHDF5File(url), watchedVariables, simulationTree, true);
			loadedResults.put(instancePath, simulationTree);
			logger.info("Finished populating Simulation Tree " + simulationTree.getInstancePath() + " with recordings");
		}
		return loadedResults;
	}

	/**
	 * @param experiment
	 * @param instancePath
	 * @return
	 */
	private IAspectConfiguration getAspectConfiguration(IExperiment experiment, String instancePath)
	{
		for(IAspectConfiguration aspectConfig : experiment.getAspectConfigurations())
		{
			if(aspectConfig.getAspect().getInstancePath().equals(instancePath))
			{
				return aspectConfig;
			}
		}
		return null;
	}

	/**
	 * Creates variables to store in simulation tree
	 * 
	 * @param variables
	 * @param simulationTree
	 */
	public void createVariables(List<String> variables, AspectSubTreeNode simulationTree){
		for(String watchedVariable : variables)
		{
			String path = "/" + watchedVariable.replace(simulationTree.getInstancePath() + ".", "");
			path = path.replace(".", "/");

			path = path.replaceFirst("/", "");
			StringTokenizer tokenizer = new StringTokenizer(path, "/");
			ACompositeNode node = simulationTree;
			VariableNode newVariableNode = null;
			while(tokenizer.hasMoreElements())
			{
				String current = tokenizer.nextToken();
				boolean found = false;
				for(ANode child : node.getChildren())
				{
					if(child.getId().equals(current))
					{
						if(child instanceof ACompositeNode)
						{
							node = (ACompositeNode) child;
						}
						if(child instanceof VariableNode)
						{
							newVariableNode = (VariableNode) child;
						}
						found = true;
						break;
					}
				}
				if(found)
				{
					continue;
				}
				else
				{
					if(tokenizer.hasMoreElements())
					{
						// not a leaf, create a composite state node
						ACompositeNode newNode = new CompositeNode(current);
						node.addChild(newNode);
						node = newNode;
					}
					else
					{
						// it's a leaf node
						VariableNode newNode = new VariableNode(current);
						newVariableNode = newNode;
						node.addChild(newNode);

					}
				}
			}
		}
	}

	public boolean setModelParameters(String modelAspectPath, Map<String, String> parameters) {
		SetParametersVisitor parameterVisitor = new SetParametersVisitor(geppettoManagerCallbackListener,parameters, modelAspectPath);
		IAspectConfiguration config = this.getAspectConfiguration(experiment, modelAspectPath);
		//TODO ; Add method in data manager to store new parameters in aspect configuration
		//DataManagerHelper.getDataManager().setModelParameters(parameters, config);
		return runtimeTreeRoot.apply(parameterVisitor);
	}
}
