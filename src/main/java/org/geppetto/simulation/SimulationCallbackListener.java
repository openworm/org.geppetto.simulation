package org.geppetto.simulation;

import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.model.state.StateTreeRoot;
import org.geppetto.core.model.state.visitors.CountTimeStepsVisitor;
import org.geppetto.core.model.state.visitors.RemoveTimeStepsVisitor;
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
	public void stateTreeUpdated(StateTreeRoot stateTree) throws GeppettoExecutionException
	{
		StateTreeRoot sessionStateTree=_sessionContext.getSimulatorRuntimeByAspect(simulationAspectID).getStateSet();
		if(sessionStateTree==null)
		{
			sessionStateTree=stateTree;
			_sessionContext.getSimulatorRuntimeByAspect(simulationAspectID).setStateSet(sessionStateTree);
		}
		//we throw an exception if the tree is a different object, this should not happen.
		if(!sessionStateTree.equals(stateTree))
		{
			throw new GeppettoExecutionException("Out of sync! The state tree received is different from the one stored in the session context");
		}
		//if the tree starts having more elements than the max size of the buffers remove the oldest ones
		CountTimeStepsVisitor countTimeStepsVisitor=new CountTimeStepsVisitor();
		stateTree.apply(countTimeStepsVisitor);
		int timeStepsToRemove=countTimeStepsVisitor.getNumberOfTimeSteps()-_sessionContext.getMaxBufferSize();
		if (timeStepsToRemove>0)
		{
			RemoveTimeStepsVisitor removeTimeStepsVisitor=new RemoveTimeStepsVisitor(timeStepsToRemove);
			sessionStateTree.apply(removeTimeStepsVisitor);
		}
		//This line is necessary because we have logic that checks that all models are processed before sending an update
		_sessionContext.getSimulatorRuntimeByAspect(simulationAspectID).increaseProcessedElements();
		updateRunningCycleSemaphore();
	}

}
