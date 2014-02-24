/**
 * 
 */
package org.geppetto.simulation.test;

import java.io.File;
import java.net.MalformedURLException;

import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.simulation.SimulationConfigReader;
import org.geppetto.simulation.model.Simulation;
import org.geppetto.simulation.visitor.InstancePathDecoratorVisitor;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author matteocantarelli
 *
 */
public class SimulationDecoratorVisitorTest
{

	@Test
	public void testInstancePaths() throws MalformedURLException, GeppettoInitializationException
	{
		Simulation sim=SimulationConfigReader.readConfig(new File("./src/test/resources/hierarchicalSimulationSample1.xml").toURI().toURL());
		InstancePathDecoratorVisitor decoratorVisitor=new InstancePathDecoratorVisitor();
		sim.accept(decoratorVisitor);
		
		//Test instancepath on entity nodes
		Assert.assertEquals("network",sim.getEntities().get(0).getInstancePath());
		Assert.assertEquals("network.neuron1",sim.getEntities().get(0).getEntities().get(0).getInstancePath());
		Assert.assertEquals("network.neuron2",sim.getEntities().get(0).getEntities().get(1).getInstancePath());
		
		//Test instancepath on aspect nodes
		Assert.assertEquals("network:electrical",sim.getEntities().get(0).getAspects().get(0).getInstancePath());
		Assert.assertEquals("network:mechanical",sim.getEntities().get(0).getAspects().get(1).getInstancePath());
		
		Assert.assertEquals("network.neuron1:electrical",sim.getEntities().get(0).getEntities().get(0).getAspects().get(0).getInstancePath());
		Assert.assertEquals("network.neuron1:mechanical",sim.getEntities().get(0).getEntities().get(0).getAspects().get(1).getInstancePath());
		
		Assert.assertEquals("network.neuron2:mechanical",sim.getEntities().get(0).getEntities().get(1).getAspects().get(0).getInstancePath());
		Assert.assertEquals("network.neuron2:electrical",sim.getEntities().get(0).getEntities().get(1).getAspects().get(1).getInstancePath());
		
		//Test instancepath on model nodes
		Assert.assertEquals("network.neuron1:electrical",sim.getEntities().get(0).getEntities().get(0).getAspects().get(0).getModel().getInstancePath());
		Assert.assertEquals("network.neuron1:mechanical",sim.getEntities().get(0).getEntities().get(0).getAspects().get(1).getModel().getInstancePath());
		
		Assert.assertEquals("network.neuron2:mechanical",sim.getEntities().get(0).getEntities().get(1).getAspects().get(0).getModel().getInstancePath());
		Assert.assertEquals("network.neuron2:electrical",sim.getEntities().get(0).getEntities().get(1).getAspects().get(1).getModel().getInstancePath());

	}

}
