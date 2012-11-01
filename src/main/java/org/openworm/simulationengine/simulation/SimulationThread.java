package org.openworm.simulationengine.simulation;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openworm.simulationengine.core.model.IModel;
import org.openworm.simulationengine.core.model.IModelInterpreter;
import org.openworm.simulationengine.core.simulation.ISimulation;
import org.openworm.simulationengine.core.simulation.ISimulationCallbackListener;
import org.openworm.simulationengine.core.simulator.ISimulator;

class SimulationThread extends Thread implements ISimulation
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
			getSessionContext().runSimulation = true;

			while (getSessionContext().runSimulation)
			{
				updateRunningCycleSemaphore();
				if (!getSessionContext().runningCycle)
				{
					getSessionContext().runningCycle = true;

					for (String aspectID : sessionContext.aspectIDs)
					{
						// reset processed elements counters
						getSessionContext().processedElementsByAspect.replace(aspectID, 0);

						IModelInterpreter modelInterpreter = sessionContext.modelInterpretersByAspect.get(aspectID);
						ISimulator simulator = sessionContext.simulatorsByAspect.get(aspectID);

						List<IModel> models = new ArrayList<IModel>();
						if (!sessionContext.modelsByAspect.containsKey(aspectID))
						{
							// initial conditions
							File file = new File(sessionContext.modelURLByAspect.get(aspectID));
							models = modelInterpreter.readModel(file.toURI().toURL());
						}
						else
						{
							// loop through keys and take last item available from previous cycle as initial condition for the new cycle
							for (String modelID : sessionContext.modelsByAspect.get(aspectID).keySet())
							{
								int listSize = sessionContext.modelsByAspect.get(aspectID).get(modelID).size();
								models.add(sessionContext.modelsByAspect.get(aspectID).get(modelID).get(listSize - 1));
							}
						}

						// set model count
						sessionContext.elementCountByAspect.put(aspectID, models.size());

						// inject listener into the simulator (in this case the thread is the listener
						simulator.initialize(new SimulationCallbackListener(aspectID, sessionContext));
						simulator.startSimulatorCycle();

						// add models to simulate
						for (IModel model : models)
						{
							// TODO: figure out how to generalize time configuration - where is it coming from?
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

	/**
	 * Figures out value of running cycle flag given processed elements counts
	 * NOTE: when all elements have been processed running cycle is set to false
	 * so that the next cycle can start
	 */
	private void updateRunningCycleSemaphore()
	{
		boolean runningCycle = false;

		// check if simulation has started
		if (sessionContext.elementCountByAspect.size() > 0)
		{
			for (String aspectID : sessionContext.aspectIDs)
			{
				// check that total number of time steps stored in buffers does not exceed given max number
				// NOTE: this is to avoid running out of memory - need to test out the max
				for (List<IModel> models : sessionContext.modelsByAspect.get(aspectID).values())
				{
					if (models.size() > sessionContext.maxBufferSize)
					{
						runningCycle = true;
						break;
					}
				}

				if (sessionContext.elementCountByAspect.get(aspectID) != sessionContext.processedElementsByAspect.get(aspectID))
				{
					// if not all elements have been processed cycle is still running
					runningCycle = true;
					break;
				}
			}
		}

		sessionContext.runningCycle = runningCycle;
	}
}