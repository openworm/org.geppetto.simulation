package org.openworm.simulationengine.simulation;

import java.util.HashMap;
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
	
	/**
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
		
		if(!sessionContext.modelsByAspect.containsKey(simulationAspectID))
		{
			sessionContext.modelsByAspect.put(simulationAspectID, new HashMap<String, List<IModel>>());
		}

		String modelId = models.get(0).getId();
		if (sessionContext.modelsByAspect.get(simulationAspectID).containsKey(modelId)) {
			
			// NOTE: not sure how the segment below applies to the generic simulation
			// NOTE: it was originally designed for dealing with plots (multiple timesteps)
			// check if we have more steps than can be displayed
			/*if (getSessionContext()._models.get(receivedId).size() >= (config.getViewport() / config.getDt()) / config.getSamplingPeriod()) {
				// if we have more steps that can be displayed - remove the difference
				for (int i = 0; i < models.size(); i++) {
					// always remove the first - when removing everything gets
					// shifted
					getSessionContext()._models.get(receivedId).remove(0);
				}
			}*/
			
			sessionContext.modelsByAspect.get(simulationAspectID).get(modelId).addAll(models);
		} else {
			sessionContext.modelsByAspect.get(simulationAspectID).put(modelId, models);
		}
		
		Integer processed = sessionContext.processedElementsByAspect.get(simulationAspectID);
		sessionContext.processedElementsByAspect.replace(simulationAspectID, processed + 1);
	}

}
