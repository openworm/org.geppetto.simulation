
package org.geppetto.simulation.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.data.model.IAspectConfiguration;
import org.geppetto.core.data.model.ResultsFormat;
import org.geppetto.core.model.GeppettoModelAccess;
import org.geppetto.core.recordings.ConvertDATToRecording;
import org.geppetto.core.services.registry.ServicesRegistry;
import org.geppetto.core.simulation.ISimulatorCallbackListener;
import org.geppetto.core.simulator.ASimulator;
import org.geppetto.model.DomainModel;
import org.geppetto.model.ExperimentState;
import org.geppetto.model.ModelFormat;
import org.junit.Assert;

/**
 * @author matteocantarelli
 *
 */
public class TestSimulatorService extends ASimulator
{

	@Override
	public void initialize(DomainModel model, IAspectConfiguration aspectConfiguration, ExperimentState experimentState, ISimulatorCallbackListener listener, GeppettoModelAccess modelAccess) throws GeppettoInitializationException,
			GeppettoExecutionException
	{
		super.initialize(model, aspectConfiguration, experimentState, listener,modelAccess);
		Assert.assertNotNull(aspectConfiguration);
		Assert.assertNotNull(experimentState);
		Assert.assertNotNull(listener);
		Assert.assertNotNull(modelAccess);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulator.ISimulator#simulate()
	 */
	@Override
	public void simulate() throws GeppettoExecutionException
	{
		Map<File, ResultsFormat> results = new HashMap<File, ResultsFormat>();
		ConvertDATToRecording converter=new ConvertDATToRecording("./src/test/resources/test/testResults.h5", this.geppettoModelAccess);
		String[] variables={
				"time(StateVariable)",
				"testVar(testType).c(StateVariable)",
				"testVar(testType).a(StateVariable)",
				"testVar(testType).b(StateVariable)"
		};
		converter.addDATFile("./src/test/resources/test/testResults.dat", variables);
		try
		{
			converter.convert(this.experimentState);
		}
		catch(Exception e)
		{
			throw new GeppettoExecutionException(e);
		}
		File geppettoResult = new File("./src/test/resources/test/testResults.h5");
		File rawResult = new File("./src/test/resources/test/testResults.dat");
		results.put(geppettoResult, ResultsFormat.GEPPETTO_RECORDING);
		results.put(rawResult, ResultsFormat.RAW);
		getListener().endOfSteps(this.aspectConfiguration, results);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulator.ISimulator#getName()
	 */
	@Override
	public String getName()
	{
		return "Test Simulator";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulator.ISimulator#getId()
	 */
	@Override
	public String getId()
	{
		return "testSimulator";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.services.IService#registerGeppettoService()
	 */
	@Override
	public void registerGeppettoService() throws Exception
	{
		List<ModelFormat> modelFormats = new ArrayList<ModelFormat>(Arrays.asList(ServicesRegistry.registerModelFormat("TEST_FORMAT")));
		ServicesRegistry.registerSimulatorService(this, modelFormats);
	}

}
