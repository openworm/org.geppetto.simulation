package org.openworm.simulationengine.simulation;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openworm.simulationengine.core.model.IModel;
import org.openworm.simulationengine.core.model.IModelInterpreter;
import org.openworm.simulationengine.core.simulation.ISimulation;
import org.openworm.simulationengine.core.simulation.ISimulationCallbackListener;
import org.openworm.simulationengine.core.simulator.ISimulator;
import org.openworm.simulationengine.core.visualisation.model.Scene;
import org.openworm.simulationengine.simulation.model.Aspect;
import org.openworm.simulationengine.simulation.model.Simulation;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
class SimulationService implements ISimulation
{

	@Autowired
	public AppConfig appConfig;

	BundleContext _bc = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

	private static Log logger = LogFactory.getLog(SimulationService.class);
	
	private final Timer _simTimer = new Timer(SimulationThread.class.getSimpleName() + " Timer");

	private final SessionContext _sessionContext = new SessionContext();

	private ISimulationCallbackListener _simulationListener;


	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.openworm.simulationengine.core.simulation.ISimulation#init(java.net
	 * .URL,
	 * org.openworm.simulationengine.core.simulation.ISimulationCallbackListener
	 * )
	 */
	@Override
	public void init(URL simConfigURL, ISimulationCallbackListener simulationListener)
	{
		SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
		
		Simulation sim = SimulationConfigReader.readConfig(simConfigURL);
		// grab config and retrieve model interpreters and simulators
		try
		{
			populateDiscoverableServices(sim);
		}
		catch (InvalidSyntaxException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		_simulationListener = simulationListener;
		_sessionContext.maxBufferSize = appConfig.getMaxBufferSize();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openworm.simulationengine.core.simulation.ISimulation#start()
	 */
	@Override
	public void start()
	{
		startClientUpdateTimer();
		startSimulationTimer();
	}

	/**
	 * 
	 */
	private void startSimulationTimer()
	{
		new SimulationThread(_sessionContext).start();
	}

	/**
	 * 
	 */
	private void startClientUpdateTimer()
	{
		_simTimer.scheduleAtFixedRate(new TimerTask()
		{
			@Override
			public void run()
			{
				try
				{
					update();
				}
				catch (JsonProcessingException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}, appConfig.getUpdateCycle(), appConfig.getUpdateCycle());
	}
	
	/**
	 * Method that takes the oldest model in the buffer and send it to the
	 * client
	 * 
	 * @throws JsonProcessingException
	 */
	private void update() throws JsonProcessingException
	{
		StringBuilder sb = new StringBuilder();

		for (String aspectID : _sessionContext.aspectIDs)
		{
			// TODO: how do we allow for multiple timesteps to be returned?

			// get models Map for the given aspect String = modelId /
			// List<IModel> = a given model at different timesteps
			HashMap<String, List<IModel>> modelsMap = _sessionContext.modelsByAspect.get(aspectID);

			List<IModel> models = new ArrayList<IModel>();
			// traverse models
			for (String modelId : modelsMap.keySet())
			{
				if (modelsMap.get(modelId).size() > 0)
				{
					// get oldest and add it to the models list to be sent to
					// the client
					models.add(modelsMap.get(modelId).get(0));
					// remove oldest from the original buffer
					modelsMap.get(modelId).remove(0);
				}
			}

			// create scene
			Scene scene = _sessionContext.modelInterpretersByAspect.get(aspectID).getSceneFromModel(models);
			ObjectMapper mapper = new ObjectMapper();
			sb.append(mapper.writer().writeValueAsString(scene));

			// TODO: figure out how to separate aspects in the representation
		}

		_simulationListener.updateReady(sb.toString());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openworm.simulationengine.core.simulation.ISimulation#stop()
	 */
	@Override
	public void stop()
	{
		_sessionContext.runSimulation = false;
		_sessionContext.runningCycle = false;
	}

	/**
	 * @param simConfig
	 * @throws InvalidSyntaxException
	 */
	private void populateDiscoverableServices(Simulation simConfig) throws InvalidSyntaxException
	{
		for (Aspect aspect : simConfig.getAspects())
		{
			String id = aspect.getId();
			String modelInterpreterId = aspect.getModelInterpreter();
			String simulatorId = aspect.getSimulator();
			String modelURL = aspect.getModelURL();

			IModelInterpreter modelInterpreter = this.<IModelInterpreter> getService(modelInterpreterId, IModelInterpreter.class.getName());
			ISimulator simulator = this.<ISimulator> getService(simulatorId, ISimulator.class.getName());

			_sessionContext.aspectIDs.add(id);
			_sessionContext.modelInterpretersByAspect.put(id, modelInterpreter);
			_sessionContext.simulatorsByAspect.put(id, simulator);
			_sessionContext.modelURLByAspect.put(id, modelURL);
		}
	}

	/*
	 * A generic routine to encapsulate boiler-plate code for dynamic service
	 * discovery
	 */
	/**
	 * @param discoveryId
	 * @param type
	 * @return
	 * @throws InvalidSyntaxException
	 */
	private <T> T getService(String discoveryId, String type) throws InvalidSyntaxException
	{
		T service = null;

		String filter = String.format("(discoverableID=%s)", discoveryId);
		ServiceReference[] sr = _bc.getServiceReferences(type, filter);
		if (sr != null && sr.length > 0)
		{
			service = (T) _bc.getService(sr[0]);
		}

		return service;
	}

}