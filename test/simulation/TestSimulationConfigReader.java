package simulation;

import static org.junit.Assert.*;

import java.io.File;
import java.net.MalformedURLException;

import org.junit.Test;
import org.openworm.simulationengine.simulation.SimulationConfigReader;
import org.openworm.simulationengine.simulation.model.OutputFormat;
import org.openworm.simulationengine.simulation.model.Simulation;

public class TestSimulationConfigReader {

	@Test
	public void testReadConfig() throws MalformedURLException {
		Simulation sim = SimulationConfigReader.readConfig(new File("./test/simulation/sim-config.xml").toURI().toURL());
		
		assertTrue(sim != null);
		assertTrue(sim.getName().equals("sph"));
		assertTrue(sim.getConfiguration().getOutputFormat() == OutputFormat.RAW);
		assertTrue(sim.getAspects().size() == 1);
		assertTrue(sim.getAspects().get(0).getModelInterpreter().equals("sphModelInterpreter"));
		assertTrue(sim.getAspects().get(0).getModelURL().equals("someurl"));
		assertTrue(sim.getAspects().get(0).getSimulator().equals("sphSimulator"));
		assertTrue(sim.getAspects().get(0).getId().equals("sph"));
	}
}
