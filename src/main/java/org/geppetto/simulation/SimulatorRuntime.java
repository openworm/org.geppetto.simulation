/**
 * 
 */
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

	public StateTreeRoot getStateSet()
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
