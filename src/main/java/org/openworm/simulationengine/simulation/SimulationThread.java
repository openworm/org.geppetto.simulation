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

	public Simulation simConfig;

	public SimulationThread(SessionContext context, Simulation config) {
		this.sessionContext = context;
		this.simConfig = config;
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
		if (getSessionContext()._modelsByAspect == null) {
			getSessionContext()._modelsByAspect = new ConcurrentHashMap<String, List<IModel>>();
		}

		if (getSessionContext()._modelsByAspect.containsKey(receivedId)) {
			getSessionContext()._modelsByAspect.get(receivedId).addAll(models);
		} else {
			getSessionContext()._modelsByAspect.put(receivedId, models);
		}

		getSessionContext()._processedAspects = getSessionContext()._processedAspects + 1;

		// NOTE: this needs to be set only when all the elements have been processed
		if (getSessionContext()._processedAspects == simConfig.getAspects().size()) {
			getSessionContext()._runningCycle = false;
		}
	}

	public void run() {
		try {
			getSessionContext()._runSimulation = true;
	
			while (getSessionContext()._runSimulation) {
				if (!getSessionContext()._runningCycle) {
					getSessionContext()._runningCycle = true;
					getSessionContext()._processedAspects = 0;
				
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