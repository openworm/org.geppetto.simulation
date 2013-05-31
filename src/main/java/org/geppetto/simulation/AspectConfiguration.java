/**
 * 
 */
package org.geppetto.simulation;

import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.simulator.ISimulator;

/**
 * @author matteocantarelli
 *
 */
public class AspectConfiguration
{
	
	private IModelInterpreter _modelInterpreter;
	private ISimulator _simulator;
	private String _url;
	
	public IModelInterpreter getModelInterpreter()
	{
		return _modelInterpreter;
	}
	
	public void setModelInterpreter(IModelInterpreter modelInterpreter)
	{
		this._modelInterpreter = modelInterpreter;
	}
	
	public ISimulator getSimulator()
	{
		return _simulator;
	}
	
	public void setSimulator(ISimulator simulator)
	{
		this._simulator = simulator;
	}
	
	public String getUrl()
	{
		return _url;
	}
	
	public void setUrl(String url)
	{
		this._url = url;
	}
	

}
