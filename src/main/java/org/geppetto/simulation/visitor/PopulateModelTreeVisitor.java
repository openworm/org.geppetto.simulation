/*******************************************************************************
 * The MIT License (MIT)
 *
 * Copyright (c) 2011, 2013 OpenWorm.
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
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geppetto.core.common.GeppettoErrorCodes;
import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.model.ModelInterpreterException;
import org.geppetto.core.model.ModelWrapper;
import org.geppetto.core.model.runtime.ANode;
import org.geppetto.core.model.runtime.AspectNode;
import org.geppetto.core.model.runtime.AspectSubTreeNode;
import org.geppetto.core.model.runtime.EntityNode;
import org.geppetto.core.model.runtime.AspectSubTreeNode.AspectTreeType;
import org.geppetto.core.model.runtime.VariableNode;
import org.geppetto.core.model.state.visitors.DefaultStateVisitor;
import org.geppetto.core.simulation.ISimulationCallbackListener;

/**
 * Visitor used for retrieving model interpreter from aspect node's and sending call to interpreter
 * for populating the model tree
 * 
 * @author  Jesus R. Martinez (jesus@metacell.us)
 *
 */
public class PopulateModelTreeVisitor extends DefaultStateVisitor{

	private static Log _logger = LogFactory.getLog(PopulateModelTreeVisitor.class);

	//Listener used to send back errors 
	private ISimulationCallbackListener _simulationCallBack;
	//The id of aspect we will be populating
	private String _instancePath;
	private HashMap<String, AspectSubTreeNode> _populateModelTree;

	public PopulateModelTreeVisitor(ISimulationCallbackListener simulationListener,String instancePath)
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
			IModelInterpreter modelInterpreter = node.getModelInterpreter();
			try
			{
				modelInterpreter.populateModelTree(node);
			}
			catch(ModelInterpreterException e)
			{
				_simulationCallBack.error(GeppettoErrorCodes.INITIALIZATION, this.getClass().getName(),null,e);
			}		
			
			this._populateModelTree = new HashMap<String, AspectSubTreeNode>();
			this._populateModelTree.put(node.getInstancePath(),((AspectSubTreeNode) node.getSubTree(AspectTreeType.MODEL_TREE)));
			
			Map<String, EntityNode> mapping = (Map<String, EntityNode>) ((ModelWrapper) node.getModel()).getModel("entitiesMapping");
			EntityNode entityNode = mapping.get(node.getParent().getId());
			if (entityNode == null){
				for (Map.Entry<String, EntityNode> entry : mapping.entrySet()) {
		 		    String key = entry.getKey();
		 		    
		 		   for (AspectNode aspectNode : entry.getValue().getAspects()){
						if (aspectNode.getId() == node.getId()){
							this._populateModelTree.put(aspectNode.getInstancePath(),(AspectSubTreeNode) aspectNode.getSubTree(AspectTreeType.MODEL_TREE));
						}
		 		   }	
				}
			}
		}

		return super.inAspectNode(node);
	}
	
	public HashMap<String, AspectSubTreeNode> getPopulatedModelTree(){
		return this._populateModelTree;
	}
}
