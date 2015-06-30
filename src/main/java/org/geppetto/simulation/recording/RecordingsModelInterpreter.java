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
package org.geppetto.simulation.recording;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ncsa.hdf.object.h5.H5File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geppetto.core.beans.PathConfiguration;
import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.common.HDF5Reader;
import org.geppetto.core.data.model.IAspectConfiguration;
import org.geppetto.core.features.IFeature;
import org.geppetto.core.model.IModel;
import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.model.ModelInterpreterException;
import org.geppetto.core.model.ModelWrapper;
import org.geppetto.core.model.RecordingModel;
import org.geppetto.core.model.runtime.AspectNode;
import org.geppetto.core.services.GeppettoFeature;
import org.geppetto.core.services.ModelFormat;
import org.geppetto.core.services.registry.ServicesRegistry;
import org.springframework.stereotype.Service;

/**
 * @author matteocantarelli
 * 
 */
@Service
public class RecordingsModelInterpreter implements IModelInterpreter
{

	private static Log _logger = LogFactory.getLog(RecordingsModelInterpreter.class);

	private static final String ID = "RECORDING_";

	private List<URL> dependentModels=new ArrayList<URL>();

	public RecordingsModelInterpreter()
	{
		_logger.info("New recordings model service created");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.model.IModelInterpreter#readModel(java.net.URL, java.util.List)
	 */
	@Override
	public IModel readModel(URL url, List<URL> recordings, String instancePath) throws ModelInterpreterException
	{
		dependentModels.clear();
		// the model URL is ignored in a recordings model interpreter
		ModelWrapper recordingsModel = new ModelWrapper(null);
		try
		{
			if(recordings != null)
			{
				int i = 1;
				for(URL recording : recordings)
				{
					dependentModels.add(recording);
					H5File file = HDF5Reader.readHDF5File(recording);
					RecordingModel recordingModel = new RecordingModel(file);
					recordingModel.setInstancePath(instancePath);
					recordingsModel.wrapModel(ID + i++, recordingModel);
				}
			}
		}
		catch(GeppettoExecutionException e)
		{
			throw new ModelInterpreterException(e);
		}
		return recordingsModel;
	}

	@Override
	public boolean populateModelTree(AspectNode aspectNode)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean populateRuntimeTree(AspectNode aspectNode)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName()
	{
		return "Recording Model Interpreter";
	}

	@Override
	public void registerGeppettoService()
	{
		List<ModelFormat> modelFormats = new ArrayList<ModelFormat>(Arrays.asList(ServicesRegistry.registerModelFormat("GEPPETTO_RECORDING_SIMULATOR")));
		ServicesRegistry.registerModelInterpreterService(this, modelFormats);

	}

	@Override
	public boolean isSupported(GeppettoFeature feature)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public IFeature getFeature(GeppettoFeature feature)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addFeature(IFeature feature)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public File downloadModel(AspectNode aspectNode, ModelFormat format, IAspectConfiguration aspectConfiguration) throws ModelInterpreterException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ModelFormat> getSupportedOutputs(AspectNode aspectNode) throws ModelInterpreterException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<URL> getDependentModels()
	{
		return dependentModels;
	}

	@Override
	public PathConfiguration getPathConfiguration()
	{
		// TODO Auto-generated method stub
		return null;
	}

}
