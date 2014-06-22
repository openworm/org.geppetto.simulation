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
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.common.HDF5Reader;
import org.geppetto.core.data.model.VariableList;
import org.geppetto.core.model.IModel;
import org.geppetto.core.model.RecordingModel;
import org.geppetto.core.model.state.ACompositeStateNode;
import org.geppetto.core.model.state.ASimpleStateNode;
import org.geppetto.core.model.state.StateTreeRoot;
import org.geppetto.core.model.values.DoubleValue;
import org.geppetto.core.simulation.ISimulatorCallbackListener;
import org.geppetto.simulation.recording.RecordingsSimulator;
import org.junit.Assert;
import org.junit.Test;

import ucar.nc2.NetcdfFile;

/**
 * @author matteocantarelli
 *
 */
public class RecordingsSimulatorTest
{

	@Test
	public void test() throws GeppettoExecutionException, GeppettoInitializationException, IOException
	{
		NetcdfFile file=HDF5Reader.readHDF5File(new File("./src/test/resources/example2.h5").toURI().toURL());
		RecordingModel recording=new RecordingModel(file);
		recording.setInstancePath("entity.model");
		RecordingsSimulator simulator=new RecordingsSimulator();
		ISimulatorCallbackListener listener=new ISimulatorCallbackListener()
		{
			int current=0;
			final double[] expected={1, 2, 3, 4, 5, 6};
			final double[] expectedTime={0.1, 0.2, 0.5, 0.51, 0.52, 0.6, 0.7};
			
			@Override
			public void stateTreeUpdated(StateTreeRoot stateTree) throws GeppettoExecutionException
			{
				ACompositeStateNode wtree = (ACompositeStateNode) stateTree.getChildren().get(0);
				ACompositeStateNode entity = (ACompositeStateNode) wtree.getChildren().get(0);
				ACompositeStateNode model =(ACompositeStateNode) entity.getChildren().get(0);
				ACompositeStateNode a =(ACompositeStateNode) model.getChildren().get(1);
				ASimpleStateNode time =(ASimpleStateNode) model.getChildren().get(0);
				ACompositeStateNode b =(ACompositeStateNode) a.getChildren().get(0);
				ACompositeStateNode c =(ACompositeStateNode) b.getChildren().get(0);
				ASimpleStateNode d =(ASimpleStateNode) c.getChildren().get(0);
				Assert.assertEquals(expected[current], ((DoubleValue)d.consumeFirstValue()).getValue(),0);
				Assert.assertEquals(expectedTime[current++], ((DoubleValue)time.consumeFirstValue()).getValue(),0);
			}
		};
		List<IModel> models=new ArrayList<IModel>();
		models.add(recording);
		simulator.initialize(models, listener);
		VariableList vlist=simulator.getWatchableVariables();
		Assert.assertNotNull(vlist);
		Assert.assertFalse(vlist.getVariables().isEmpty());
		List<String> variablesToWatch=new ArrayList<String>();
		variablesToWatch.add("entity.model.a.b.c.d");
		variablesToWatch.add("entity.model.time");
		simulator.addWatchVariables(variablesToWatch);
		simulator.startWatch();
		simulator.simulate(null);
		simulator.simulate(null);
		simulator.simulate(null);
		simulator.simulate(null);
		simulator.simulate(null);
		simulator.simulate(null);
		file.close();
	}

}
