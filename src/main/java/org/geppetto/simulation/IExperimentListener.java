
package org.geppetto.simulation;

import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.data.model.IExperiment;
import org.geppetto.simulation.manager.ExperimentRunThread;
import org.geppetto.simulation.manager.RuntimeProject;

public interface IExperimentListener
{

	void experimentRunDone(ExperimentRunThread experimentRun, IExperiment experiment, RuntimeProject project) throws GeppettoExecutionException;
	
	void experimentError(String titleMessage, String errorMessage, Exception exception, IExperiment experiment);

}
