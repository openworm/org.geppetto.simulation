/**
 * 
 */
package org.geppetto.simulation.test;

import java.net.URL;

import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.simulation.SessionContext;
import org.geppetto.simulation.SimulationConfigReader;

/**
 * @author matteocantarelli
 *
 */
public class SimulationTestUtility
{

	public static SessionContext getSessionContext(URL simulationURL) throws GeppettoInitializationException
	{
		SessionContext sc=new SessionContext();
		sc.setSimulation(SimulationConfigReader.readConfig(simulationURL));
		return sc;
	}
}
