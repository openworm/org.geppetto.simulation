package org.openworm.simulationengine.simulation;

import java.net.URL;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openworm.simulationengine.core.model.IModel;
import org.openworm.simulationengine.core.model.IModelInterpreter;
import org.openworm.simulationengine.core.simulation.ISimulation;
import org.openworm.simulationengine.core.simulation.ISimulationCallbackListener;
import org.openworm.simulationengine.core.simulator.ISimulator;

class SimulationThread extends Thread implements ISimulation, ISimulationCallbackListener {

	private static Log logger = LogFactory.getLog(SimulationThread.class);
	private SessionContext sessionContext = null;

	public SimulationThread(SessionContext context) {
		this.sessionContext = context;
	}

	private SessionContext getSessionContext() {
		return sessionContext;
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
	 * @param models
	 * TODO: figure out how to pass aspects IDs to this
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

	public void run() {
		try {
			getSessionContext().runSimulation = true;
	
			while (getSessionContext().runSimulation) {
				if (!getSessionContext().runningCycle) {
					getSessionContext().runningCycle = true;
					getSessionContext().processedAspects = 0;
				
					for(String aspectID : sessionContext.aspectIDs)
					{	
						IModelInterpreter modelInterpreter = sessionContext.modelInterpretersByAspect.get(aspectID);
						ISimulator simulator = sessionContext.simulatorsByAspect.get(aspectID);
						List<IModel> models = modelInterpreter.readModel(new URL(sessionContext.modelURLByAspect.get(aspectID)));
						
						// inject listener into the simulator (in this case the thread is the listener
						// NOTE: might have to change this to inject a listener that is not the thread for multiple simulations
						simulator.initialize(this);
						simulator.startSimulatorCycle();
						// add models to simulate
						for(IModel model : models){
							// TODO: figure out how to generalize time configuration
							simulator.simulate(model, null);
						}
						simulator.endSimulatorCycle();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}