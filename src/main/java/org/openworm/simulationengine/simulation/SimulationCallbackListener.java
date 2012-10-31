package org.openworm.simulationengine.simulation;

import java.util.List;

import org.openworm.simulationengine.core.model.IModel;
import org.openworm.simulationengine.core.simulation.ISimulationCallbackListener;

public class SimulationCallbackListener implements ISimulationCallbackListener {

	private String simulationAspectID;
	private SessionContext sessionContext;
	
	public SimulationCallbackListener(String aspectID, SessionContext context) {
		this.simulationAspectID = aspectID;
		this.sessionContext = context;
	}
	
	/*
	 * Callback to populate results buffers.
	 * 
	 * @see
	 * org.openworm.simulationengine.core.simulation.ISimulationCallbackListener
	 * #resultReady(java.util.List)
	 */
	@Override
	public void resultReady(final List<IModel> models) {
		// when the callback is received results are appended
		appendResults(models);
	}

	/**
	 * Populates results in session context.
	 * 
	 * @param models
	 */
	private void appendResults(List<IModel> models) {
		/*String receivedId = models.get(0).getId();
		if (getSessionContext().modelsByAspect == null) {
			getSessionContext().modelsByAspect = new ConcurrentHashMap<String, List<IModel>>();
		}

		if (getSessionContext().modelsByAspect.containsKey(receivedId)) {
			getSessionContext().modelsByAspect.get(receivedId).addAll(models);
		} else {
			getSessionContext().modelsByAspect.put(receivedId, models);
		}

		getSessionContext().processedAspects = getSessionContext().processedAspects + 1;

		// NOTE: this needs to be set only when all the elements have been processed
		if (getSessionContext().processedAspects == sessionContext.aspectIDs.size()) {
			getSessionContext().runningCycle = false;
		}*/
	}

}
