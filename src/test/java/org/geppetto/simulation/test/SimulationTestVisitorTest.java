/**
 * 
 */
package org.geppetto.simulation.test;

import java.io.File;
import java.net.MalformedURLException;

import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.model.simulation.Simulation;
import org.geppetto.simulation.SimulationConfigReader;
import org.junit.Test;

/**
 * @author matteocantarelli
 *
 */
public class SimulationTestVisitorTest
{

	@Test
	public void testHierarchical() throws MalformedURLException, GeppettoInitializationException
	{
		Simulation sim=SimulationConfigReader.readConfig(new File("./src/test/resources/hierarchicalSimulationSample1.xml").toURI().toURL());
		sim.accept(new SimulationTestVisitor());
	}

}
