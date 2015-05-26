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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;
import ncsa.hdf.object.h5.H5File;
import ncsa.hdf.utils.SetNatives;

import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.common.HDF5Reader;
import org.geppetto.core.model.IModel;
import org.geppetto.core.model.RecordingModel;
import org.geppetto.core.model.runtime.ACompositeNode;
import org.geppetto.core.model.runtime.AspectNode;
import org.geppetto.core.model.runtime.CompositeNode;
import org.geppetto.core.model.runtime.EntityNode;
import org.geppetto.core.model.runtime.RuntimeTreeRoot;
import org.geppetto.core.model.runtime.VariableNode;
import org.geppetto.core.simulation.ISimulatorCallbackListener;
import org.geppetto.simulation.recording.RecordingsSimulator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author matteocantarelli
 *
 */
public class RecordingsSimulatorTest
{
	private RuntimeTreeRoot runtime;
	private EntityNode entity;
	private AspectNode aspectNode;

	@Before
	public void setup(){
		try {
			SetNatives.getInstance().setHDF5Native(System.getProperty("user.dir"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void test() throws GeppettoExecutionException, GeppettoInitializationException, IOException
	{
		
		runtime = new RuntimeTreeRoot("runtime");
		entity = new EntityNode("Entity");
		aspectNode = new AspectNode("Aspect");
		System.out.println("new aspect");
		
		URL url = this.getClass().getResource("/recording_small.h5");
		H5File file=HDF5Reader.readHDF5File(url);
		RecordingModel recording=new RecordingModel(file);
		recording.setInstancePath("entity.model");
		RecordingsSimulator simulator=new RecordingsSimulator();

		ISimulatorCallbackListener listener=new ISimulatorCallbackListener()
		{
			int current=0;
			final double[] expected={4.596967281768264E-8, 2.2727011770243068E-7, 3, 4, 5, 6};
			final double[] expectedTime={0, 0.4, 0.5, 0.51, 0.52, 0.6, 0.7};
			
			@Override
			public void stepped(AspectNode aspect) throws GeppettoExecutionException
			{
				ACompositeNode wtree = (ACompositeNode) aspectNode.getChildren().get(0);
				VariableNode time = (VariableNode) wtree.getChildren().get(0);
				CompositeNode a =  (CompositeNode) wtree.getChildren().get(1);
				CompositeNode b = (CompositeNode) a.getChildren().get(0);
				VariableNode c = (VariableNode) b.getChildren().get(0);
				VariableNode d = (VariableNode) b.getChildren().get(1);

				
				double value = Double.valueOf(time.getTimeSeries().get(0).getValue().getStringValue());
				double value2 = Double.valueOf(c.getTimeSeries().get(0).getValue().getStringValue());
				double value3 = Double.valueOf(d.getTimeSeries().get(0).getValue().getStringValue());

				System.out.println(value + " : " + value2 + " : " + value3);
				Assert.assertEquals(expectedTime[current], value,0);
				Assert.assertEquals(expected[current], value2,0);
			}

			@Override
			public void endOfSteps(String message, java.io.File recordingsFile) {
				// TODO Auto-generated method stub
				
			}
		};
		List<IModel> models=new ArrayList<IModel>();
		models.add(recording);
		simulator.initialize(models, listener);
//		VariableList vlist=simulator.getWatchableVariables();
//		Assert.assertNotNull(vlist);
//		Assert.assertFalse(vlist.getVariables().isEmpty());
//		List<String> variablesToWatch=new ArrayList<String>();
//		variablesToWatch.add("Entity.Aspect.SimulationTree.time");
//		variablesToWatch.add("Entity.Aspect.SimulationTree.P.neuron0.ge");
//		variablesToWatch.add("Entity.Aspect.SimulationTree.P.neuron0.gi");
//		simulator.addWatchVariables(variablesToWatch);
//		runtime.addChild(entity);
//		entity.addChild(aspectNode);
//		simulator.simulate(null,aspectNode);
		try {
			file.close();
		} catch (HDF5Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
