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
import org.geppetto.core.data.model.IParameter;
import org.geppetto.core.data.model.ISimulationResult;
import org.geppetto.core.data.model.ResultsFormat;
import org.geppetto.core.model.IModel;
import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.model.RecordingModel;
import org.geppetto.core.model.runtime.ACompositeNode;
import org.geppetto.core.model.runtime.ANode;
import org.geppetto.core.model.runtime.AspectNode;
import org.geppetto.core.model.runtime.AspectSubTreeNode;
import org.geppetto.core.model.runtime.AspectSubTreeNode.AspectTreeType;
import org.geppetto.core.model.runtime.CompositeNode;
import org.geppetto.core.model.runtime.ParameterSpecificationNode;
import org.geppetto.core.model.runtime.RuntimeTreeRoot;
import org.geppetto.core.model.runtime.SkeletonAnimationNode;
import org.geppetto.core.model.runtime.VariableNode;
import org.geppetto.core.model.simulation.GeppettoModel;
import org.geppetto.core.model.state.visitors.SetWatchedVariablesVisitor;
import org.geppetto.core.services.DropboxUploadService;
import org.geppetto.core.services.ModelFormat;
import org.geppetto.core.simulator.RecordingReader;
import org.geppetto.core.utilities.URLReader;
import org.geppetto.simulation.visitor.CreateModelInterpreterServicesVisitor;
import org.geppetto.simulation.visitor.CreateRuntimeTreeVisitor;
import org.geppetto.simulation.visitor.DownloadModelVisitor;
import org.geppetto.simulation.visitor.FindAspectNodeVisitor;
import org.geppetto.simulation.visitor.FindModelTreeVisitor;
import org.geppetto.simulation.visitor.FindParameterSpecificationNodeVisitor;
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

	// Head node that holds the entities
	private RuntimeTreeRoot runtimeTreeRoot = new RuntimeTreeRoot("scene");

	private IExperiment experiment;

	private static Log logger = LogFactory.getLog(RuntimeExperiment.class);

	public RuntimeExperiment(RuntimeProject runtimeProject, IExperiment experiment) throws GeppettoExecutionException
	{
		this.experiment = experiment;
		init(runtimeProject.getGeppettoModel());
	}

	private void init(GeppettoModel geppettoModel) throws GeppettoExecutionException
	{
		this.clearWatchLists();

		// retrieve model interpreters and simulators
		CreateModelInterpreterServicesVisitor createServicesVisitor = new CreateModelInterpreterServicesVisitor(modelInterpreters);
		geppettoModel.accept(createServicesVisitor);
		createServicesVisitor.postProcessVisit();

		LoadSimulationVisitor loadSimulationVisitor = new LoadSimulationVisitor(modelInterpreters, instancePathToIModelMap);
		geppettoModel.accept(loadSimulationVisitor);
		loadSimulationVisitor.postProcessVisit();
		
		CreateRuntimeTreeVisitor runtimeTreeVisitor = new CreateRuntimeTreeVisitor(modelInterpreters, instancePathToIModelMap, runtimeTreeRoot);
		geppettoModel.accept(runtimeTreeVisitor);
		runtimeTreeVisitor.postProcessVisit();

		runtimeTreeRoot = runtimeTreeVisitor.getRuntimeModel();

		PopulateVisualTreeVisitor populateVisualVisitor = new PopulateVisualTreeVisitor();
		runtimeTreeRoot.apply(populateVisualVisitor);
		populateVisualVisitor.postProcessVisit();

		// If it is queued the whole simulation tree will be populated in order to have the units
		if(!experiment.getStatus().equals(ExperimentStatus.QUEUED))
		{
			// create variables for each aspect node's simulation tree
			for(IAspectConfiguration a : experiment.getAspectConfigurations())
			{
				List<? extends IInstancePath> vars = a.getWatchedVariables();

				String aspect = a.getAspect().getInstancePath();
				FindAspectNodeVisitor findAspectNodeVisitor = new FindAspectNodeVisitor(aspect);
				runtimeTreeRoot.apply(findAspectNodeVisitor);
				AspectNode node = findAspectNodeVisitor.getAspectNode();

				for(IInstancePath var : vars)
				{
					AspectTreeType treeType = var.getAspect().contains(AspectTreeType.SIMULATION_TREE.toString()) ? AspectTreeType.SIMULATION_TREE : AspectTreeType.VISUALIZATION_TREE;
					this.createVariables(var, node.getSubTree(treeType));
				}
			}
		}

		// let's set the parameters if they exist
		for(IAspectConfiguration ac : experiment.getAspectConfigurations())
		{
			if(ac.getModelParameter() != null && !ac.getModelParameter().isEmpty())
			{
				populateModelTree(ac.getAspect().getInstancePath());
				setModelParameters(ac.getAspect().getInstancePath(), ac.getModelParameter());
			}
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
		SetWatchedVariablesVisitor setWatchedVariablesVisitor = new SetWatchedVariablesVisitor(experiment, watchedVariables);
		runtimeTreeRoot.apply(setWatchedVariablesVisitor);
		DataManagerHelper.getDataManager().saveEntity(experiment);

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
	 * @throws GeppettoExecutionException 
	 */
	public Map<String, AspectSubTreeNode> populateModelTree(String aspectInstancePath) throws GeppettoExecutionException
	{
		logger.info("Populating Model Tree for " + aspectInstancePath);
		PopulateModelTreeVisitor populateModelVisitor = new PopulateModelTreeVisitor(aspectInstancePath);
		runtimeTreeRoot.apply(populateModelVisitor);
		populateModelVisitor.postProcessVisit();
		return populateModelVisitor.getPopulatedModelTree();
	}

	/**
	 * @param aspectInstancePath
	 * @return
	 * @throws GeppettoExecutionException 
	 */
	public Map<String, AspectSubTreeNode> populateSimulationTree(String aspectInstancePath) throws GeppettoExecutionException
	{
		logger.info("Populating Simulation Tree for " + aspectInstancePath);
		PopulateSimulationTreeVisitor populateSimulationVisitor = new PopulateSimulationTreeVisitor(aspectInstancePath, getAspectConfiguration(experiment,
				aspectInstancePath)); 
		runtimeTreeRoot.apply(populateSimulationVisitor);
		populateSimulationVisitor.postProcessVisit();
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
	 * @throws GeppettoExecutionException 
	 */
	public File downloadModel(String aspectInstancePath, ModelFormat format) throws GeppettoExecutionException
	{
		logger.info("Downloading Model for " + aspectInstancePath + " in format " + format);

		DownloadModelVisitor downloadModelVistor = new DownloadModelVisitor(aspectInstancePath, format, getAspectConfiguration(experiment, aspectInstancePath));
		runtimeTreeRoot.apply(downloadModelVistor);
		downloadModelVistor.postProcessVisit();
		return downloadModelVistor.getModelFile();
	}

	/**
	 * @param aspectInstancePath
	 * @return
	 * @throws GeppettoExecutionException 
	 */
	public List<ModelFormat> supportedOuputs(String aspectInstancePath) throws GeppettoExecutionException
	{
		logger.info("Getting supported outputs for " + aspectInstancePath);
		SupportedOutputsVisitor supportedOutputsModelVisitor = new SupportedOutputsVisitor(aspectInstancePath);
		runtimeTreeRoot.apply(supportedOutputsModelVisitor);
		supportedOutputsModelVisitor.postProcessVisit();
		return supportedOutputsModelVisitor.getSupportedOutputs();
	}

	/**
	 * @return
	 * @throws GeppettoExecutionException
	 */
	public Map<String, AspectSubTreeNode> updateRuntimeTreesWithResults() throws GeppettoExecutionException
	{
		Map<String, AspectSubTreeNode> loadedResults = new HashMap<String, AspectSubTreeNode>();
		for(ISimulationResult result : experiment.getSimulationResults())
		{
			if(result.getFormat().equals(ResultsFormat.GEPPETTO_RECORDING))
			{
				String instancePath = result.getAspect().getInstancePath();
				logger.info("Reading results for " + instancePath);

				FindAspectNodeVisitor findAspectNodeVisitor = new FindAspectNodeVisitor(instancePath);
				getRuntimeTree().apply(findAspectNodeVisitor);
				AspectNode aspect = findAspectNodeVisitor.getAspectNode();
				aspect.setModified(true);
				aspect.getParentEntity().setModified(true);

				// We first need to populate the simulation tree for the given aspect
				// NOTE: it would seem that commenting this line out makes no difference - remove?
				populateSimulationTree(instancePath);

				AspectSubTreeNode simulationTree = (AspectSubTreeNode) aspect.getSubTree(AspectTreeType.SIMULATION_TREE);
				AspectSubTreeNode visualizationTree = (AspectSubTreeNode) aspect.getSubTree(AspectTreeType.VISUALIZATION_TREE);

				URL url;
				try
				{
					url = URLReader.getURL(result.getResult().getUrl());
				}
				catch(IOException e)
				{
					throw new GeppettoExecutionException(e);
				}

				RecordingReader recordingReader = new RecordingReader(new RecordingModel(HDF5Reader.readHDF5File(url)));

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
						AspectTreeType treeType = watchedVariable.getAspect().contains(AspectTreeType.SIMULATION_TREE.toString()) ? AspectTreeType.SIMULATION_TREE : AspectTreeType.VISUALIZATION_TREE;

						recordingReader.readRecording(watchedVariable, treeType == AspectTreeType.SIMULATION_TREE ? simulationTree : visualizationTree, true);

						String aspectPath = watchedVariable.getEntityInstancePath() + "."
								+ watchedVariable.getAspect().replace("." + AspectTreeType.SIMULATION_TREE.toString(), "").replace("." + AspectTreeType.VISUALIZATION_TREE.toString(), "");

						// map results to the appropriate tree
						loadedResults.put(aspectPath, treeType == AspectTreeType.SIMULATION_TREE ? simulationTree : visualizationTree);

						if(treeType == AspectTreeType.SIMULATION_TREE)
						{
							simulationTree.setModified(true);
						}
						else
						{
							visualizationTree.setModified(true);
						}
					}

					logger.info("Finished populating runtime trees " + instancePath + " with recordings");
				}
			}
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
		//Check if it is a subAspect Instance Path and extract the base one
		String[] instancePathSplit = instancePath.split("\\.");
		if (instancePathSplit.length > 2){
			instancePath = instancePathSplit[0] + "." + instancePathSplit[2];
		}
				
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
	 * @param tree
	 */
	public void createVariables(IInstancePath variable, AspectSubTreeNode tree)
	{
		String path = "/" + variable.getInstancePath().replace(tree.getInstancePath() + ".", "");
		path = path.replace(".", "/");

		path = path.replaceFirst("/", "");
		StringTokenizer tokenizer = new StringTokenizer(path, "/");
		ACompositeNode node = tree;
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
					if(tree.getType() == AspectTreeType.SIMULATION_TREE)
					{
						// for now leaf nodes in the Sim tree can only be variable nodes
						VariableNode newNode = new VariableNode(current);
						newNode.setWatched(true);
						node.addChild(newNode);
					}
					else if(tree.getType() == AspectTreeType.VISUALIZATION_TREE)
					{
						// for now leaf nodes in the Viz tree can only be skeleton animation nodes
						SkeletonAnimationNode newNode = new SkeletonAnimationNode(current);
						node.addChild(newNode);
					}
				}
			}
		}
	}

	/**
	 * @param instancePath
	 * @param modelParameter
	 * @return
	 * @throws GeppettoExecutionException
	 */
	private AspectSubTreeNode setModelParameters(String instancePath, List<? extends IParameter> modelParameter) throws GeppettoExecutionException
	{
		Map<String, String> parameters = new HashMap<String, String>();
		for(IParameter p : modelParameter)
		{
			parameters.put(p.getVariable().getInstancePath(), p.getValue());
		}
		return setModelParameters(instancePath, parameters);
	}

	/**
	 * @param modelAspectPath
	 * @param parameters
	 * @return
	 * @throws GeppettoExecutionException
	 */
	public AspectSubTreeNode setModelParameters(String modelAspectPath, Map<String, String> parameters) throws GeppettoExecutionException
	{
		SetParametersVisitor parameterVisitor = new SetParametersVisitor(parameters, modelAspectPath);
		IAspectConfiguration config = this.getAspectConfiguration(experiment, modelAspectPath);
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
				FindParameterSpecificationNodeVisitor findParameterVisitor = new FindParameterSpecificationNodeVisitor(path);
				runtimeTreeRoot.apply(findParameterVisitor);
				ParameterSpecificationNode p = findParameterVisitor.getParameterNode();
				if(p != null)
				{
					IInstancePath instancePath = DataManagerHelper.getDataManager().newInstancePath(p.getEntityInstancePath(), p.getAspectInstancePath(), p.getLocalInstancePath());
					config.addModelParameter(DataManagerHelper.getDataManager().newParameter(instancePath, parameters.get(path)));
				}
				else
				{
					throw new GeppettoExecutionException("Cannot find parameter " + path + "in the runtime tree.");
				}
			}
		}
		runtimeTreeRoot.apply(parameterVisitor);
		parameterVisitor.postProcessVisit();
		FindModelTreeVisitor findParameterVisitor = new FindModelTreeVisitor(modelAspectPath + ".ModelTree");
		runtimeTreeRoot.apply(findParameterVisitor);

		return findParameterVisitor.getModelTreeNode();

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
						dropboxService.upload(new File(URLReader.createLocalCopy(url).toURI()));
					}
					catch(Exception e)
					{
						throw new GeppettoExecutionException(e);
					}
				}
			}
		}
	}

}
