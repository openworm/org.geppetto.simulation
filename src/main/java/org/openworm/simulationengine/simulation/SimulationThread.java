package org.openworm.simulationengine.simulation;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openworm.simulationengine.core.model.HHModel;
import org.openworm.simulationengine.core.model.IModel;
import org.openworm.simulationengine.core.model.IModelInterpreter;
import org.openworm.simulationengine.core.simulation.ISimulation;
import org.openworm.simulationengine.core.simulation.ISimulationCallbackListener;
import org.openworm.simulationengine.core.simulation.TimeConfiguration;
import org.openworm.simulationengine.core.simulator.ISimulator;
import org.openworm.simulationengine.simulation.model.Aspect;
import org.openworm.simulationengine.simulation.model.Simulation;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

class SimulationThread extends Thread implements ISimulation, ISimulationCallbackListener {

	private static Log logger = LogFactory.getLog(SimulationThread.class);
	private SessionContext sessionContext = null;

	public SimulationThread(SessionContext context, Simulation config) {
		this.sessionContext = context;
	}

	private SessionContext getSessionContext() {
		return sessionContext;
	}

	/*
	 * (non-Javadoc)
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
	 */
	private void appendResults(List<IModel> models) {
		String receivedId = models.get(0).getId();
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
		if (getSessionContext().processedAspects == sessionContext.aspectsSize) {
			getSessionContext().runningCycle = false;
		}
	}

	public void run() {
		try {
			getSessionContext().runSimulation = true;
	
			while (getSessionContext().runSimulation) {
				if (!getSessionContext().runningCycle) {
					getSessionContext().runningCycle = true;
					getSessionContext().processedAspects = 0;
				
					/*for(Aspect aspect : simConfig.getAspects())
					{
						String id = aspect.getId();
						String modelInterpreterId = aspect.getModelInterpreter();
						String simulatorId = aspect.getSimulator();
						String modelURL = aspect.getModelURL();
						
						IModelInterpreter modelInterpreter = this.<IModelInterpreter>getService(modelInterpreterId, IModelInterpreter.class.getName());
						ISimulator simulator = this.<ISimulator>getService(simulatorId, ISimulator.class.getName());
						
						List<IModel> models = modelInterpreter.readModel(new URL(modelURL));
						
						simulator.initialize(this);
						simulator.startSimulatorCycle();
						// TODO: add models to simulate
						simulator.endSimulatorCycle();
					}*/
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}