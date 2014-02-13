/*******************************************************************************
 * The MIT License (MIT)
 *
 * Copyright (c) 2011, 2013 OpenWorm.
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

import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.simulation.SimulationConfigReader;
import org.geppetto.simulation.model.Simulation;
import org.junit.Assert;
import org.junit.Test;

public class TestSimulationConfigReader {

	@Test
	public void testReadConfig() throws MalformedURLException, GeppettoInitializationException {
		Simulation sim = SimulationConfigReader.readConfig(new File("./src/test/resources/sim-config.xml").toURI().toURL());
		
		Assert.assertTrue(sim != null);
		Assert.assertTrue(sim.getEntities().size() == 1);
		Assert.assertTrue(sim.getEntities().get(0).getId().equals("Entity1"));
		Assert.assertTrue(sim.getEntities().get(0).getAspects().size() == 1);
		Assert.assertTrue(sim.getEntities().get(0).getAspects().get(0).getModel().getModelInterpreterId().equals("sphModelInterpreter"));
		Assert.assertTrue(sim.getEntities().get(0).getAspects().get(0).getModel().getModelURL().equals("someurl"));
		Assert.assertTrue(sim.getEntities().get(0).getAspects().get(0).getSimulator().getSimulatorId().equals("sphSimulator"));
		Assert.assertTrue(sim.getEntities().get(0).getAspects().get(0).getId().equals("sph"));
		
		String xml = SimulationConfigReader.writeSimulationConfig(new File("./src/test/resources/sim-config.xml").toURI().toURL());
				
		Assert.assertNotNull(xml);
								
		Simulation s = SimulationConfigReader.readSimulationConfig(xml);
		
		Assert.assertTrue(s != null);
		Assert.assertTrue(sim.getEntities().size() == 1);
		Assert.assertTrue(sim.getEntities().get(0).getId().equals("Entity1"));
		Assert.assertTrue(s.getEntities().get(0).getAspects().size() == 1);
		Assert.assertTrue(s.getEntities().get(0).getAspects().get(0).getModel().getModelInterpreterId().equals("sphModelInterpreter"));
		Assert.assertTrue(s.getEntities().get(0).getAspects().get(0).getModel().getModelURL().equals("someurl"));
		Assert.assertTrue(s.getEntities().get(0).getAspects().get(0).getSimulator().getSimulatorId().equals("sphSimulator"));
		Assert.assertTrue(s.getEntities().get(0).getAspects().get(0).getId().equals("sph"));
	}
}
