
package org.geppetto.simulation.manager;

import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.data.model.ExperimentStatus;
import org.geppetto.core.data.model.IExperiment;
import org.geppetto.core.simulator.ISimulator;

/**
 * This class helps incapsulating the execution of a simulator in a separate thread
 * 
 * @author matteocantarelli
 *
 */
public class SimulatorRunThread extends Thread
{

	private ISimulator simulator;
	private IExperiment experiment;

	public SimulatorRunThread(IExperiment experiment, ISimulator simulator)
	{
		this.experiment = experiment;
		this.simulator = simulator;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run()
	{
		try
		{
			simulator.simulate();
		}
		catch(GeppettoExecutionException e)
		{
			experiment.setStatus(ExperimentStatus.ERROR);
			throw new RuntimeException(e);
		}

	}

}
