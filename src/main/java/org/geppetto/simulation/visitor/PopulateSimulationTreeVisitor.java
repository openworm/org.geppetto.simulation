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

import java.util.HashMap;
import java.util.Map;

import org.geppetto.core.common.GeppettoErrorCodes;
import org.geppetto.core.features.IWatchableVariableListFeature;
import org.geppetto.core.model.IModel;
import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.model.ModelInterpreterException;
import org.geppetto.core.model.ModelWrapper;
import org.geppetto.core.model.runtime.AspectNode;
import org.geppetto.core.model.runtime.AspectSubTreeNode;
import org.geppetto.core.model.runtime.AspectSubTreeNode.AspectTreeType;
import org.geppetto.core.model.runtime.EntityNode;
import org.geppetto.core.model.runtime.VariableNode;
import org.geppetto.core.model.state.visitors.DefaultStateVisitor;
import org.geppetto.core.services.GeppettoFeature;
import org.geppetto.core.simulation.IGeppettoManagerCallbackListener;

/**
 * Visitor used for retrieving simulator from aspect node's and sending call to simulator
 * for populating the visualization tree
 * 
 * @author  Adrian Quintana (adrian.perez@ucl.ac.uk)
 *
 */
public class PopulateSimulationTreeVisitor extends DefaultStateVisitor{
	
	private IGeppettoManagerCallbackListener _simulationCallBack;
	//The id of aspect we will be populating
	private String _instancePath;
	
	//This is not being used at the moment
	private HashMap<String, AspectSubTreeNode> _populateSimulationTree;

	public PopulateSimulationTreeVisitor(IGeppettoManagerCallbackListener simulationListener, String instancePath)
	{
		this._simulationCallBack = simulationListener;
		this._instancePath = instancePath;
	}

	/* (non-Javadoc)
	 * @see org.geppetto.core.model.state.visitors.DefaultStateVisitor#inCompositeStateNode(org.geppetto.core.model.state.CompositeStateNode)
	 */
	@Override
	public boolean inAspectNode(AspectNode node)
	{
		if(this._instancePath.equals(node.getInstancePath())){
			IModelInterpreter model = node.getModelInterpreter();
			try
			{
				if(model!=null){
					((IWatchableVariableListFeature) model.getFeature(GeppettoFeature.WATCHABLE_VARIABLE_LIST_FEATURE)).listWatchableVariables(node);
				}
			}
			catch(ModelInterpreterException e)
			{
				_simulationCallBack.error(GeppettoErrorCodes.INITIALIZATION, this.getClass().getName(),null,e);
			}
			
			//FIXME: It it is possible to call it from the js api we should populate as in PopulateModelTree, depending if it is an entity or a subentity
			this._populateSimulationTree = new HashMap<String, AspectSubTreeNode>();
			this._populateSimulationTree.put(node.getInstancePath(),((AspectSubTreeNode) node.getSubTree(AspectTreeType.SIMULATION_TREE)));
						
			IModel imodel =  node.getModel();
			if(imodel instanceof ModelWrapper){
				ModelWrapper wrapper = (ModelWrapper)imodel;
				Map<String, EntityNode> mapping = (Map<String, EntityNode>) wrapper.getModel("entitiesMapping");
				EntityNode entityNode = mapping.get(node.getParent().getId());
				if (entityNode == null){
					for (Map.Entry<String, EntityNode> entry : mapping.entrySet()) {
						String key = entry.getKey();

						for (AspectNode aspectNode : entry.getValue().getAspects()){
							if (aspectNode.getId() == node.getId()){
								this._populateSimulationTree.put(aspectNode.getInstancePath(),(AspectSubTreeNode) aspectNode.getSubTree(AspectTreeType.SIMULATION_TREE));
							}
						}	
					}
				}
			}
		}
		
		
		return super.inAspectNode(node);
	}

	/* (non-Javadoc)
	 * @see org.geppetto.core.model.state.visitors.DefaultStateVisitor#outCompositeStateNode(org.geppetto.core.model.state.CompositeStateNode)
	 */
	@Override
	public boolean outAspectNode(AspectNode node)
	{
		return super.outAspectNode(node);
	}

	/* (non-Javadoc)
	 * @see org.geppetto.core.model.state.visitors.DefaultStateVisitor#visitSimpleStateNode(org.geppetto.core.model.state.SimpleStateNode)
	 */
	@Override
	public boolean visitVariableNode(VariableNode node)
	{
		return super.visitVariableNode(node);
	}
	
	public HashMap<String, AspectSubTreeNode> getPopulatedSimulationTree(){
		return this._populateSimulationTree;
	}
}
