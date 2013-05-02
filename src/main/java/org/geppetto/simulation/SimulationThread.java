package org.geppetto.simulation;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.model.IModel;
import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.model.ModelInterpreterException;
import org.geppetto.core.simulation.TimeConfiguration;
import org.geppetto.core.simulator.ISimulator;

class SimulationThread extends Thread
{

	private static Log logger = LogFactory.getLog(SimulationThread.class);
	private SessionContext _sessionContext = null;

	public SimulationThread(SessionContext context)
	{
		this._sessionContext = context;
	}

	private SessionContext getSessionContext()
	{
		return _sessionContext;
	}

	public void run() 
	{

		while(getSessionContext().isRunning())
		{
			if(!getSessionContext().isRunningCycleSemaphore())
			{
				getSessionContext().setRunningCycleSemaphore(true);

				for(String aspectID : _sessionContext.getAspectIds())
				{
					// reset processed elements counters
					getSessionContext().setProcessedElements(aspectID, 0);

					IModelInterpreter modelInterpreter = _sessionContext.getConfigurationByAspect(aspectID).getModelInterpreter();
					ISimulator simulator = _sessionContext.getConfigurationByAspect(aspectID).getSimulator();

					if(!simulator.isInitialized())
					{
						IModel model = _sessionContext.getSimulatorRuntimeByAspect(aspectID).getModel();
						if(model == null)
						{
							try
							{
								model = modelInterpreter.readModel(new URL(_sessionContext.getConfigurationByAspect(aspectID).getUrl()));
							}
							catch(MalformedURLException e)
							{
								throw new RuntimeException(e);
							}
							catch(ModelInterpreterException e)
							{
								throw new RuntimeException(e);
							}
							_sessionContext.getSimulatorRuntimeByAspect(aspectID).setModel(model);
							_sessionContext.getSimulatorRuntimeByAspect(aspectID).setElementCount(1);
						}
						try
						{
							simulator.initialize(model, new SimulationCallbackListener(aspectID, _sessionContext));
						}
						catch(GeppettoInitializationException e)
						{
							throw new RuntimeException(e);
						}
					}
					
					// TODO this is just saying "advance one step" at the moment
					try
					{
						simulator.simulate(new TimeConfiguration(null, 1, 1));
					}
					catch(GeppettoExecutionException e)
					{
						throw new RuntimeException(e);
					}

				}
			}
		}
	}
}