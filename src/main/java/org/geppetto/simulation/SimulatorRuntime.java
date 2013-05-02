/**
 * 
 */
package org.geppetto.simulation;

import org.geppetto.core.model.IModel;
import org.geppetto.core.model.StateSet;

/**
 * @author matteocantarelli
 *
 */
public class SimulatorRuntime
{

	private StateSet _stateSet;
	private Integer _processedElements;
	private Integer _elementCount;
	private IModel _model;
	
	public IModel getModel()
	{
		return _model;
	}

	public void setModel(IModel model)
	{
		this._model = model;
	}

	public StateSet getStateSet()
	{
		return _stateSet;
	}
	
	public void setStateSet(StateSet stateSet)
	{
		this._stateSet = stateSet;
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

	
}
