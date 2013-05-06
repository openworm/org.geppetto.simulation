package org.geppetto.simulation;

import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.model.ModelInterpreterException;
import org.geppetto.core.model.StateSet;
import org.geppetto.core.simulation.ISimulation;
import org.geppetto.core.simulation.ISimulationCallbackListener;
import org.geppetto.core.simulator.ISimulator;
import org.geppetto.core.visualisation.model.Scene;
import org.geppetto.simulation.model.Aspect;
import org.geppetto.simulation.model.Simulation;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
class SimulationService implements ISimulation
{

	@Autowired
	public AppConfig appConfig;

	BundleContext _bc = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

	private static Log logger = LogFactory.getLog(SimulationService.class);

	private final SessionContext _sessionContext = new SessionContext();

	private Timer _clientUpdateTimer;

	private SimulationThread _simThread;

	private ISimulationCallbackListener _simulationListener;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulation.ISimulation#init(java.net.URL, org.geppetto.core.simulation.ISimulationCallbackListener)
	 */
	@Override
	public void init(URL simConfigURL, ISimulationCallbackListener simulationListener) throws GeppettoInitializationException
	{
		Simulation sim = SimulationConfigReader.readConfig(simConfigURL);
		// grab config and retrieve model interpreters and simulators

		populateDiscoverableServices(sim);

		_simulationListener = simulationListener;
		_sessionContext.setMaxBufferSize(appConfig.getMaxBufferSize());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulation.ISimulation#start()
	 */
	@Override
	public void start()
	{
		_sessionContext.setRunning(true);
		startSimulationThread();
		startClientUpdateTimer();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulation.ISimulation#stop()
	 */
	@Override
	public void stop()
	{
		// tell the thread to stop running the simulation
		_sessionContext.setRunning(false);
		// also need to stop the timer that updates the client
		_clientUpdateTimer.cancel();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulation.ISimulation#stop()
	 */
	@Override
	public void reset()
	{
		// stop simulation if it's running
		if(_sessionContext.isRunning())
		{
			_sessionContext.setRunning(false);
			_clientUpdateTimer.cancel();
		}

		_sessionContext.reset();
	}

	/**
	 * 
	 */
	private void startSimulationThread()
	{
		_simThread = new SimulationThread(_sessionContext);
		_simThread.start();
	}

	/**
	 * 
	 */
	private void startClientUpdateTimer()
	{
		_clientUpdateTimer = new Timer(SimulationService.class.getSimpleName() + " - Timer - " + new java.util.Date().getTime());
		_clientUpdateTimer.scheduleAtFixedRate(new TimerTask()
		{
			@Override
			public void run()
			{
				try
				{
					update();
				}
				catch(GeppettoExecutionException e)
				{
					throw new RuntimeException(e);
				}

			}
		}, appConfig.getUpdateCycle(), appConfig.getUpdateCycle());
	}

	/**
	 * Method that takes the oldest model in the buffer and send it to the client
	 * 
	 * @throws JsonProcessingException
	 */
	private void update() throws GeppettoExecutionException
	{
		logger.info("Update frontend called");
		StringBuilder sb = new StringBuilder();
		boolean updateAvailable = false;

		for(String aspectID : _sessionContext.getAspectIds())
		{
			// get models Map for the given aspect String = modelId / List<IModel> = a given model at different time steps
			if(_sessionContext.getSimulatorRuntimeByAspect(aspectID).getStateSet() != null)
			{
				StateSet oldestSet = _sessionContext.getSimulatorRuntimeByAspect(aspectID).getStateSet().pullOldestStateSet();
				//we send data to the frontend if it' either the first cycle or if there is a change in the state, i.e. something that might produce a frontend update
				if(!oldestSet.isEmpty() || _sessionContext.getSimulatorRuntimeByAspect(aspectID).getUpdatesProcessed()==0)
				{
					logger.info("Available update found");
					updateAvailable = true;
					// create scene
					Scene scene;
					try
					{
						scene = _sessionContext.getConfigurationByAspect(aspectID).getModelInterpreter().getSceneFromModel(_sessionContext.getSimulatorRuntimeByAspect(aspectID).getModel(), oldestSet);
						ObjectMapper mapper = new ObjectMapper();
						sb.append(mapper.writer().writeValueAsString(scene));
						_sessionContext.getSimulatorRuntimeByAspect(aspectID).updateProcessed();
					}
					catch(ModelInterpreterException e)
					{
						throw new GeppettoExecutionException(e);
					}
					catch(JsonProcessingException e)
					{
						throw new GeppettoExecutionException(e);
					}
				}
			}
		}

		if(updateAvailable)
		{
			logger.info("Update sent to listener");
			_simulationListener.updateReady(sb.toString());			
		}
	}

	/**
	 * @param simConfig
	 * @throws InvalidSyntaxException
	 */
	private void populateDiscoverableServices(Simulation simConfig) throws GeppettoInitializationException
	{
		for(Aspect aspect : simConfig.getAspects())
		{
			String id = aspect.getId();
			String modelInterpreterId = aspect.getModelInterpreter();
			String simulatorId = aspect.getSimulator();
			String modelURL = aspect.getModelURL();

			IModelInterpreter modelInterpreter = this.<IModelInterpreter> getService(modelInterpreterId, IModelInterpreter.class.getName());
			ISimulator simulator = this.<ISimulator> getService(simulatorId, ISimulator.class.getName());

			// populate context
			_sessionContext.addAspectId(id, modelInterpreter, simulator, modelURL);
		}
	}

	/*
	 * A generic routine to encapsulate boiler-plate code for dynamic service discovery
	 */
	/**
	 * @param discoveryId
	 * @param type
	 * @return
	 * @throws InvalidSyntaxException
	 */
	private <T> T getService(String discoveryId, String type) throws GeppettoInitializationException
	{
		T service = null;

		String filter = String.format("(discoverableID=%s)", discoveryId);
		ServiceReference<?>[] sr;
		try
		{
			sr = _bc.getServiceReferences(type, filter);
		}
		catch(InvalidSyntaxException e)
		{
			throw new GeppettoInitializationException(e);
		}
		if(sr != null && sr.length > 0)
		{
			service = (T) _bc.getService(sr[0]);
		}

		if(service == null)
		{
			throw new GeppettoInitializationException("No service found for id:"+discoveryId);
		}
		return service;
	}

}