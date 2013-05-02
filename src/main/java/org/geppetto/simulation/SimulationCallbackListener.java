package org.geppetto.simulation;

import org.geppetto.core.model.StateSet;
import org.geppetto.core.simulation.ISimulatorCallbackListener;

public class SimulationCallbackListener implements ISimulatorCallbackListener
{

	private String simulationAspectID;
	private SessionContext _sessionContext;

	public SimulationCallbackListener(String aspectID, SessionContext context)
	{
		this.simulationAspectID = aspectID;
		this._sessionContext = context;
	}


	/**
	 * Populates results in session context.
	 * 
	 * @param results
	 */
	private void appendResults(StateSet results)
	{
		StateSet currentStateSet=_sessionContext.getSimulatorRuntimeByAspect(simulationAspectID).getStateSet();
		if(currentStateSet==null)
		{
			currentStateSet=new StateSet(results.getModelId());
			_sessionContext.getSimulatorRuntimeByAspect(simulationAspectID).setStateSet(currentStateSet);
		}
		
		if (currentStateSet.getNumberOfStates()>=_sessionContext.getMaxBufferSize())
		{
			currentStateSet.removeOldestStates(results.getNumberOfStates());
		}
		currentStateSet.appendStateSet(results);
		_sessionContext.getSimulatorRuntimeByAspect(simulationAspectID).increaseProcessedElements();

		updateRunningCycleSemaphore();
	}
	
	/**
	 * Figures out value of running cycle flag given processed elements counts
	 * NOTE: when all elements have been processed running cycle is set to false so that the next cycle can start
	 */
	private void updateRunningCycleSemaphore()
	{
		int processedAspects = 0; 
		
		// check that all elements have been processed on all the simulation aspects
		for (String aspectID : _sessionContext.getAspectIds())
		{	
			if (_sessionContext.getSimulatorRuntimeByAspect(aspectID).allElementsProcessed())
			{
				// if not all elements have been processed cycle is still running
				processedAspects++;
			}
		}
		
		if(_sessionContext.getAspectIds().size() == processedAspects)
		{
			_sessionContext.setRunningCycleSemaphore(false);
		}
	}

	@Override
	public void stateSetReady(StateSet results)
	{
		// when the callback is received results are appended
		appendResults(results);
	}

}
