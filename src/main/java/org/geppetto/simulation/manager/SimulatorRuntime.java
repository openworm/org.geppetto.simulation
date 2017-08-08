

package org.geppetto.simulation.manager;

import org.geppetto.simulation.SimulatorRuntimeStatus;

/**
 * @author matteocantarelli
 *
 */
public class SimulatorRuntime
{

	private SimulatorRuntimeStatus _status = SimulatorRuntimeStatus.IDLE;

	// This is the number of steps this simulator has processed
	private int _processedSteps = 0;
	// This is the number of steps that were processed by this simulator and that have been
	// sent to the client
	private int _stepsConsumed = 0;

	/**
	 * @param status
	 */
	public void setStatus(SimulatorRuntimeStatus status)
	{
		_status = status;
	}

	/**
	 * @return
	 */
	public SimulatorRuntimeStatus getStatus()
	{
		return _status;
	}

	/**
	 * @return
	 */
	public Integer getProcessedSteps()
	{
		return _processedSteps;
	}

	/**
	 * @param processedSteps
	 */
	public void setProcessedSteps(int processedSteps)
	{
		_processedSteps = processedSteps;
	}

	/**
	 * 
	 */
	public void incrementProcessedSteps()
	{
		_processedSteps++;
	}

	/**
	 * @return the number of steps which have been processed but not yet consumed
	 */
	public int getNonConsumedSteps()
	{
		return _processedSteps - _stepsConsumed;
	}

	/**
	 * @return
	 */
	public int getStepsConsumed()
	{
		return _stepsConsumed;
	}

	/**
	 * 
	 */
	public void incrementStepsConsumed()
	{
		_stepsConsumed++;
	}

	/**
	 * Revert the simulator to the initial conditions
	 */
	public void revertToInitialConditions()
	{
		_stepsConsumed = 0;
		_processedSteps = 0;
	}
}
