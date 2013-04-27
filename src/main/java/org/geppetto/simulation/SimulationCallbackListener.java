package org.geppetto.simulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.geppetto.core.model.IModel;
import org.geppetto.core.simulation.ISimulatorCallbackListener;

public class SimulationCallbackListener implements ISimulatorCallbackListener
{

	private String simulationAspectID;
	private SessionContext sessionContext;

	public SimulationCallbackListener(String aspectID, SessionContext context)
	{
		this.simulationAspectID = aspectID;
		this.sessionContext = context;
	}

	/**
	 * Callback to populate results buffers.
	 * 
	 * @see org.geppetto.core.simulation.ISimulatorCallbackListener
	 *      #resultReady(java.util.List)
	 */
	@Override
	public void resultReady(final List<IModel> models)
	{
		// when the callback is received results are appended
		appendResults(models);
	}

	/**
	 * Populates results in session context.
	 * 
	 * @param models
	 */
	private void appendResults(List<IModel> models)
	{
		if (!sessionContext.modelsByAspect.containsKey(simulationAspectID))
		{
			sessionContext.modelsByAspect.put(simulationAspectID, new HashMap<String, List<IModel>>());
		}
		
		String modelId = models.get(0).getId();
		if (sessionContext.modelsByAspect.get(simulationAspectID).containsKey(modelId))
		{
			// check if we have more steps stored than our fixed maximum
			if (sessionContext.modelsByAspect.get(simulationAspectID).get(modelId).size() >= sessionContext.maxBufferSize)
			{
				// if we have more steps that can be displayed - remove the difference
				for (int i = 0; i < models.size(); i++)
				{
					// always remove the first - when removing everything gets shifted
					sessionContext.modelsByAspect.get(simulationAspectID).get(modelId).remove(0);
				}
			}
			
			sessionContext.modelsByAspect.get(simulationAspectID).get(modelId).addAll(models);
		}
		else
		{
			sessionContext.modelsByAspect.get(simulationAspectID).put(modelId, new ArrayList<IModel>(models));
		}

		Integer processed = sessionContext.processedElementsByAspect.get(simulationAspectID);
		sessionContext.processedElementsByAspect.replace(simulationAspectID, processed + 1);
		
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
		for (String aspectID : sessionContext.aspectIDs)
		{	
			if (sessionContext.elementCountByAspect.get(aspectID) == sessionContext.processedElementsByAspect.get(aspectID))
			{
				// if not all elements have been processed cycle is still running
				processedAspects++;
			}
		}
		
		if(sessionContext.aspectIDs.size() == processedAspects)
		{
			sessionContext.runningCycleSemaphore = false;
		}
	}

}
