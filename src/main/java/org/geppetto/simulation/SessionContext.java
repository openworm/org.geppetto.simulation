package org.geppetto.simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.simulator.ISimulator;

public class SessionContext
{

	private ConcurrentHashMap<String,SimulatorRuntime> _runtimeByAspect= new ConcurrentHashMap<String,SimulatorRuntime>();
	private ConcurrentHashMap<String,AspectConfiguration> _configurationByAspect= new ConcurrentHashMap<String,AspectConfiguration>();
	
	private List<String> _aspectIDs = new ArrayList<String>();

	/*
	 * simulation flags 
	 */
	private boolean _runningCycleSemaphore = false;
	private boolean _isRunning = false;
	
	private int _maxBufferSize = 100;
	
	/*
	 * Reverts the simulation state to initial conditions
	 * */
	public void revertToInitialConditions()
	{
		// for each aspect, revert runtime to initial conditions
		for(String aspectID : this.getAspectIds())
		{
			this.getSimulatorRuntimeByAspect(aspectID).revertToInitialConditions();
		}

		_runningCycleSemaphore = false;
		_isRunning = false;
	}
	
	/*
	 * Resets the simulation context
	 * NOTE: WIPES EVERYTHING
	 * */
	public void reset()
	{
		_runtimeByAspect.clear();
		_configurationByAspect.clear();
		_aspectIDs.clear();
		_runningCycleSemaphore = false;
		_isRunning = false;
	}

	public boolean isRunning()
	{
		return _isRunning;
	}
	
	public void setRunning(boolean isRunning)
	{
		_isRunning=isRunning;
	}

	public boolean isRunningCycleSemaphore()
	{
		return _runningCycleSemaphore;
	}
	
	public void setRunningCycleSemaphore(boolean runningCycleSemaphore)
	{
		_runningCycleSemaphore=runningCycleSemaphore;
	}
	
	public int getMaxBufferSize()
	{
		return _maxBufferSize;
	}

	public List<String> getAspectIds()
	{
		return _aspectIDs;
	}

	public SimulatorRuntime getSimulatorRuntimeByAspect(String aspectId)
	{
		return _runtimeByAspect.get(aspectId);
	}

	public AspectConfiguration getConfigurationByAspect(String aspectId)
	{
		return _configurationByAspect.get(aspectId);
	}

	public void setProcessedElements(String aspectId, int i)
	{
		_runtimeByAspect.get(aspectId).setProcessedElements(i);
	}

	public void setMaxBufferSize(int maxBufferSize)
	{
		_maxBufferSize=maxBufferSize;
	}

	public void addAspectId(String id, IModelInterpreter modelInterpreter, ISimulator simulator, String modelURL)
	{
		_aspectIDs.add(id);
		SimulatorRuntime simulatorRuntime=new SimulatorRuntime();
		AspectConfiguration aspectConfiguration=new AspectConfiguration();
		simulatorRuntime.setProcessedElements(0);
		aspectConfiguration.setModelInterpreter(modelInterpreter);
		aspectConfiguration.setSimulator(simulator);
		aspectConfiguration.setUrl(modelURL);
		_runtimeByAspect.put(id, simulatorRuntime);
		_configurationByAspect.put(id, aspectConfiguration);
	}
	
	
}
