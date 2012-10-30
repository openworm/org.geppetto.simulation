package org.openworm.simulationengine.simulation;

import java.net.URL;
import java.util.HashMap;
import java.util.List;

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
	
	BundleContext _bc = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

	private SessionContext sessionContext = null;

	public ISimulator sampleSimulatorService;

	public Simulation config;

	public SimulationThread(SessionContext context, Simulation config, ISimulator sampleSimulatorService) {
		this.sessionContext = context;
		this.config = config;
		this.sampleSimulatorService = sampleSimulatorService;
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
		if (getSessionContext()._models == null) {
			getSessionContext()._models = new HashMap<String, List<IModel>>();
		}

		if (getSessionContext()._models.containsKey(receivedId)) {
			// check if we have more steps than can be displayed
			/*if (getSessionContext()._models.get(receivedId).size() >= (config.getViewport() / config.getDt()) / config.getSamplingPeriod()) {
				// if we have more steps that can be displayed - remove the
				// difference
				for (int i = 0; i < models.size(); i++) {
					// always remove the first - when removing everything gets
					// shifted
					getSessionContext()._models.get(receivedId).remove(0);
				}
			}*/

			// add all the timesteps for the model
			getSessionContext()._models.get(receivedId).addAll(models);
		} else {
			getSessionContext()._models.put(receivedId, models);
		}

		getSessionContext()._processedElements = getSessionContext()._processedElements + 1;

		// NOTE: this needs to be set only when all the elements have been
		// processed
		/*if (getSessionContext()._processedElements == config.getElemCount()) {
			getSessionContext()._runningCycle = false;
		}*/
	}

	public void run() {
		getSessionContext()._runSimulation = true;

		while (getSessionContext()._runSimulation) {
			if (!getSessionContext()._runningCycle) {
				getSessionContext()._runningCycle = true;
				getSessionContext()._processedElements = 0;

				logger.debug("start simulation");
				//int ELEM_COUNT = config.getElemCount();

				sampleSimulatorService.initialize(this);
				sampleSimulatorService.startSimulatorCycle();

				//getSessionContext()._timeConfiguration = new TimeConfiguration((float) config.getDt(), config.getSteps(),config.getSamplingPeriod());

				// create the models to be simulated
				/*for (int j = 0; j < ELEM_COUNT; j++) {
					HHModel modelToSimulate;
					if (getSessionContext()._models == null) {
						// initial condition
						modelToSimulate = new HHModel(Integer.toString(j),config.getV(), config.getXn(), config.getXm(),config.getXh(),getSessionContext()._externalCurrent);
					} else {
						modelToSimulate = (HHModel) getSessionContext()._models.get(Integer.toString(j)).get(getSessionContext()._models.get(Integer.toString(j)).size() - 1);
						modelToSimulate.setI(getSessionContext()._externalCurrent);
					}

					// this is where the simulation hooks up with the solver
					sampleSimulatorService.simulate(modelToSimulate,getSessionContext()._timeConfiguration);
				}*/

				sampleSimulatorService.endSimulatorCycle();
				logger.debug("end simulation");
			}
		}
	}
	
	/*
	 * Pseudo-logic for discovering services and start simulations
	 */
	private void runSimulation()
	{	
		try {
			// TODO: this needs to be passed in
			URL configUrl = null;
			Simulation sim = SimulationConfigReader.readConfig(configUrl);
		
			for(Aspect aspect : sim.getAspects())
			{
				String id = aspect.getId();
				String modelInterpreterId = aspect.getModelInterpreter();
				String simulatorId = aspect.getSimulator();
				String modelURL = aspect.getModelURL();
				
				IModelInterpreter modelInterpreter = this.<IModelInterpreter>getService(modelInterpreterId, IModelInterpreter.class.getName());
				ISimulator simulator = this.<ISimulator>getService(simulatorId, ISimulator.class.getName());
				
				List<IModel> models = modelInterpreter.readModel(new URL(modelURL));
					
				// send down to the simulator the models that have been read
				// simulator.simulate()
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * A generic routine to encapsulate boiler-plate code for dynamic service discovery
	 */
	private <T> T getService(String discoveryId, String type) throws InvalidSyntaxException{
		T service = null;
		
		String filter = String.format("(discoverableID=%s)", discoveryId);
		ServiceReference[] sr  =  _bc.getServiceReferences(type, filter);
		if(sr != null && sr.length > 0)
		{
			service = (T) _bc.getService(sr[0]);
		}
		
		return service;
	}
}