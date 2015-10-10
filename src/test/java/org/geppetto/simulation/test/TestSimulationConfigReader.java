/*******************************************************************************
 * The MIT License (MIT)
 *
 * Copyright (c) 2011 - 2015 OpenWorm.
 * http://openworm.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     	OpenWorm - http://openworm.org/people.html
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
 * USE OR OTHER DEALINGS IN THE SOFTWARE.
 *******************************************************************************/

package org.geppetto.simulation.test;

import java.io.File;
import java.net.MalformedURLException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.model.geppettomodel.Entity;
import org.geppetto.core.model.geppettomodel.GeppettoModel;
import org.geppetto.simulation.GeppettoModelReader;
import org.junit.Assert;
import org.junit.Test;

public class TestSimulationConfigReader {

	
	// SIM TODO
//	@Test
	public void testReadSample1() throws MalformedURLException, GeppettoInitializationException {
		GeppettoModel sim = GeppettoModelReader.readGeppettoModel(new File("./src/test/resources/sample1.xml").toURI().toURL());
		Assert.assertTrue(sim != null);
	}
	
	// SIM TODO
//	@Test
	public void testReadConfig() throws MalformedURLException, GeppettoInitializationException {
		GeppettoModel sim = GeppettoModelReader.readGeppettoModel(new File("./src/test/resources/sim-config.xml").toURI().toURL());
		
		Assert.assertTrue(sim != null);
		Assert.assertTrue(sim.getEntities().size() == 1);
		Assert.assertTrue(sim.getEntities().get(0).getId().equals("Entity1"));
		Assert.assertTrue(sim.getEntities().get(0).getAspects().size() == 1);
		Assert.assertTrue(sim.getEntities().get(0).getAspects().get(0).getModel().getModelInterpreterId().equals("sphModelInterpreter"));
		Assert.assertTrue(sim.getEntities().get(0).getAspects().get(0).getModel().getModelURL().equals("someurl"));
		Assert.assertTrue(sim.getEntities().get(0).getAspects().get(0).getId().equals("sph"));
		
		String xml = GeppettoModelReader.writeGeppettoModel(new File("./src/test/resources/sim-config.xml").toURI().toURL());
				
		Assert.assertNotNull(xml);
								
		GeppettoModel s = GeppettoModelReader.readGeppettoModel(xml);
		
		Assert.assertTrue(s != null);
		Assert.assertTrue(sim.getEntities().size() == 1);
		Assert.assertTrue(sim.getEntities().get(0).getId().equals("Entity1"));
		Assert.assertTrue(s.getEntities().get(0).getAspects().size() == 1);
		Assert.assertTrue(s.getEntities().get(0).getAspects().get(0).getModel().getModelInterpreterId().equals("sphModelInterpreter"));
		Assert.assertTrue(s.getEntities().get(0).getAspects().get(0).getModel().getModelURL().equals("someurl"));
		Assert.assertTrue(s.getEntities().get(0).getAspects().get(0).getId().equals("sph"));
	}
	
	// SIM TODO
	//@Test
	public void testReadHierarchicalSimulation() throws MalformedURLException, GeppettoInitializationException {
		GeppettoModel sim = GeppettoModelReader.readGeppettoModel(new File("./src/test/resources/hierarchicalSimulationSample1.xml").toURI().toURL());
		
		Assert.assertTrue(sim != null);
		Assert.assertTrue(sim.getEntities().size() == 1);
		Assert.assertTrue(sim.getEntities().get(0).getId().equals("network"));
		Assert.assertTrue(sim.getEntities().get(0).getAspects().size() == 2);
		// SIM TODO
//		Assert.assertTrue(sim.getEntities().get(0).getAspects().get(0).getSimulator().getSimulatorId().equals("jLemsSimulator"));
//		Assert.assertTrue(sim.getEntities().get(0).getAspects().get(1).getSimulator().getSimulatorId().equals("sphSimulator"));
		Assert.assertTrue(sim.getEntities().get(0).getEntities().size()==2);
		
		

	}
	
	@Test
	public void testWritingSimulation() throws MalformedURLException, GeppettoInitializationException, JAXBException {
		GeppettoModel sim=new GeppettoModel();
		Entity parent=new Entity();
		parent.setId("parent");
		Entity c1=new Entity();
		c1.setId("c1");
		Entity c2=new Entity();
		c2.setId("c2");
		sim.getEntities().add(parent);
		parent.getEntities().add(c1);
		parent.getEntities().add(c2);
		Marshaller m=JAXBContext.newInstance(GeppettoModel.class).createMarshaller();
		m.marshal(sim, new File("./target/simtmp.xml"));
	}
}
