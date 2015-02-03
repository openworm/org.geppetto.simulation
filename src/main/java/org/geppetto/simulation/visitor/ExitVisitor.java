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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geppetto.core.model.runtime.AspectNode;
import org.geppetto.core.model.runtime.AspectSubTreeNode;
import org.geppetto.core.model.runtime.ConnectionNode;
import org.geppetto.core.model.runtime.EntityNode;
import org.geppetto.core.model.state.visitors.DefaultStateVisitor;
import org.geppetto.core.simulation.ISimulationCallbackListener;

/**
 * Visitor used for setting modified flag on subtree to flase after sending
 * update
 * 
 * @author Jesus R. Martinez (jesus@metacell.us)
 * 
 */
public class ExitVisitor extends DefaultStateVisitor {

	private static Log _logger = LogFactory.getLog(ExitVisitor.class);

	// Listener used to send back errors
	private ISimulationCallbackListener _simulationCallBack;

	public ExitVisitor(ISimulationCallbackListener simulationListener) {
		this._simulationCallBack = simulationListener;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.model.state.visitors.DefaultStateVisitor#
	 * inCompositeStateNode(org.geppetto.core.model.state.CompositeStateNode)
	 */
	@Override
	public boolean outAspectSubTreeNode(AspectSubTreeNode node) {
		node.setModified(false);
		return super.inAspectSubTreeNode(node);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.model.state.visitors.DefaultStateVisitor#
	 * inCompositeStateNode(org.geppetto.core.model.state.CompositeStateNode)
	 */
	@Override
	public boolean outEntityNode(EntityNode node) {
		node.setModified(false);
		return super.inEntityNode(node);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.model.state.visitors.DefaultStateVisitor#
	 * inCompositeStateNode(org.geppetto.core.model.state.CompositeStateNode)
	 */
	@Override
	public boolean outAspectNode(AspectNode node) {
		node.setModified(false);
		return super.inAspectNode(node);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.model.state.visitors.DefaultStateVisitor#
	 */
	@Override
	public boolean outConnectionNode(ConnectionNode node) {
		node.setModified(false);
		return super.inConnectionNode(node);
	}
}
