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

class SimulationThread extends Thread implements ISimulation {

	private static Log logger = LogFactory.getLog(SimulationThread.class);
	private SessionContext sessionContext = null;

	public SimulationThread(SessionContext context) {
		this.sessionContext = context;
	}

	private SessionContext getSessionContext() {
		return sessionContext;
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
						simulator.initialize(new SimulationCallbackListener(aspectID, sessionContext));
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