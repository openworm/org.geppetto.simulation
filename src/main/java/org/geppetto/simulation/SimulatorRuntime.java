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

package org.geppetto.simulation;

import org.geppetto.core.model.IModel;
import org.geppetto.core.model.state.StateTreeRoot;

/**
 * @author matteocantarelli
 *
 */
public class SimulatorRuntime
{

	private StateTreeRoot _stateTree;
	private Integer _processedElements;
	private Integer _elementCount;
	private IModel _model;
	private int _updatesSent=0;
	
	public IModel getModel()
	{
		return _model;
	}

	public void setModel(IModel model)
	{
		this._model = model;
	}

	public StateTreeRoot getStateTree()
	{
		return _stateTree;
	}
	
	public void setStateSet(StateTreeRoot stateTree)
	{
		this._stateTree = stateTree;
	}
	
	public Integer getProcessedElements()
	{
		return _processedElements;
	}
	
	public void setProcessedElements(Integer processedElements)
	{
		this._processedElements = processedElements;
	}
	
	public Integer getElementCount()
	{
		return _elementCount;
	}
	
	public void setElementCount(Integer elementCount)
	{
		this._elementCount = elementCount;
	}

	public void increaseProcessedElements()
	{
		_processedElements++;
	}

	public boolean allElementsProcessed()
	{
		return _elementCount==_processedElements;
	}

	public int getUpdatesProcessed()
	{
		return _updatesSent;
	}

	public void updateProcessed()
	{
		_updatesSent++;
	}
	
	/*
	 * reset everything - only thing untouched is _model, representing initial conditions
	 * */
	public void revertToInitialConditions(){
		_stateTree.getChildren().clear();
		_stateTree = null;
	}
	
	/*
	 * Check if the runtime is at initial conditions
	 * */
	public boolean isAtInitialConditions()
	{
		return _stateTree == null;
	}
}
