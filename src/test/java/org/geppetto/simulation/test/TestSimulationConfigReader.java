package org.geppetto.simulation.test;

import java.io.File;
import java.net.MalformedURLException;

import org.geppetto.simulation.SimulationConfigReader;
import org.geppetto.simulation.model.OutputFormat;
import org.geppetto.simulation.model.Simulation;
import org.junit.Assert;
import org.junit.Test;

public class TestSimulationConfigReader {

	@Test
	public void testReadConfig() throws MalformedURLException {
		Simulation sim = SimulationConfigReader.readConfig(new File("./src/test/resources/sim-config.xml").toURI().toURL());
		
		Assert.assertTrue(sim != null);
		Assert.assertTrue(sim.getName().equals("sph"));
		Assert.assertTrue(sim.getConfiguration().getOutputFormat() == OutputFormat.RAW);
		Assert.assertTrue(sim.getAspects().size() == 1);
		Assert.assertTrue(sim.getAspects().get(0).getModelInterpreter().equals("sphModelInterpreter"));
		Assert.assertTrue(sim.getAspects().get(0).getModelURL().equals("someurl"));
		Assert.assertTrue(sim.getAspects().get(0).getSimulator().equals("sphSimulator"));
		Assert.assertTrue(sim.getAspects().get(0).getId().equals("sph"));
	}
}
