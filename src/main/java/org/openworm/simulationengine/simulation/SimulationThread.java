package org.openworm.simulationengine.simulation;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openworm.simulationengine.core.model.IModel;
import org.openworm.simulationengine.core.model.IModelInterpreter;
import org.openworm.simulationengine.core.simulator.ISimulator;

class SimulationThread extends Thread
{

	private static Log logger = LogFactory.getLog(SimulationThread.class);
	private SessionContext sessionContext = null;

	public SimulationThread(SessionContext context)
	{
		this.sessionContext = context;
	}

	private SessionContext getSessionContext()
	{
		return sessionContext;
	}

	public void run()
	{
		try
		{
			while (getSessionContext().runSimulation)
			{
				if (!getSessionContext().runningCycleSemaphore)
				{
					getSessionContext().runningCycleSemaphore = true;

					for (String aspectID : sessionContext.aspectIDs)
					{
						// reset processed elements counters
						getSessionContext().processedElementsByAspect.put(aspectID, 0);

						IModelInterpreter modelInterpreter = sessionContext.modelInterpretersByAspect.get(aspectID);
						ISimulator simulator = sessionContext.simulatorsByAspect.get(aspectID);

						List<IModel> models = new ArrayList<IModel>();
						if (!sessionContext.modelsByAspect.containsKey(aspectID))
						{
							// initial conditions
							models = modelInterpreter.readModel(new URL(sessionContext.modelURLByAspect.get(aspectID)));
						}
						else
						{
							// loop through keys and take last item available
							// from previous cycle as initial condition for the
							// new cycle
							for (String modelID : sessionContext.modelsByAspect.get(aspectID).keySet())
							{
								int listSize = sessionContext.modelsByAspect.get(aspectID).get(modelID).size();
								models.add(sessionContext.modelsByAspect.get(aspectID).get(modelID).get(listSize - 1));
							}
						}

						// set model count
						sessionContext.elementCountByAspect.put(aspectID, models.size());

						// inject listener into the simulator
						simulator.initialize(new SimulationCallbackListener(aspectID, sessionContext));
						simulator.startSimulatorCycle();

						// add models to simulate
						for (IModel model : models)
						{
							// TODO: figure out how to generalize time
							// configuration - where is it coming from?
							simulator.simulate(model, null);
						}

						simulator.endSimulatorCycle();
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}