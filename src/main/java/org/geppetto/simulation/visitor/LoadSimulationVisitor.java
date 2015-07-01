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
package org.geppetto.simulation.visitor;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.model.IModel;
import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.model.ModelInterpreterException;
import org.geppetto.core.model.simulation.Model;
import org.geppetto.core.model.state.visitors.GeppettoModelVisitor;
import org.geppetto.core.utilities.URLReader;

/**
 * This visitor loads a simulation
 * 
 * @author matteocantarelli
 * 
 */
public class LoadSimulationVisitor extends GeppettoModelVisitor
{

	private Map<String, IModelInterpreter> _modelInterpreters;
	private Map<String, IModel> _model;
	private final String SERVER_ROOT_TOKEN = "%SERVER_ROOT%";
	private static Log _logger = LogFactory.getLog(LoadSimulationVisitor.class);

	public LoadSimulationVisitor(Map<String, IModelInterpreter> modelInterpreters, Map<String, IModel> model)
	{
		super();
		_modelInterpreters = modelInterpreters;
		_model = model;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.massfords.humantask.TraversingVisitor#visit(org.geppetto.simulation.model.Model)
	 */
	@Override
	public void visit(Model pModel)
	{
		super.visit(pModel);
		try
		{
			IModelInterpreter modelInterpreter = _modelInterpreters.get(pModel.getInstancePath());
			IModel model = _model.get(pModel.getInstancePath());
			if(model == null)
			{
				List<URL> recordings = new ArrayList<URL>();
				if(pModel.getRecordingURL() != null)
				{
					// add all the recordings found
					for(String recording : pModel.getRecordingURL())
					{
						URL url = null;

						if(recording.contains(SERVER_ROOT_TOKEN))
						{
							recording = recording.replace(SERVER_ROOT_TOKEN, "");
							url = this.getLocalURL(recording);
						}
						else
						{
							url = this.getClass().getResource(recording);
						}

						recordings.add(url);
					}
				}

				long start = System.currentTimeMillis();

				URL modelUrl = null;
				String modelUrlStr = pModel.getModelURL();
				if(modelUrlStr != null)
				{
					if(modelUrlStr.contains(SERVER_ROOT_TOKEN))
					{
						modelUrlStr = modelUrlStr.replace(SERVER_ROOT_TOKEN, "");
						modelUrl = this.getLocalURL(modelUrlStr);
					}
					else
					{

						modelUrl = URLReader.getURL(pModel.getModelURL());
					}
					model = modelInterpreter.readModel(modelUrl, recordings, pModel.getParentAspect().getInstancePath());
					model.setInstancePath(pModel.getInstancePath());
					_model.put(pModel.getInstancePath(), model);

					long end = System.currentTimeMillis();
					_logger.info("Finished reading model, took " + (end - start) + " ms ");

				}
				else
				{
					// the model is already loaded, we are coming here after a stop simulation which doesn't delete the model, do nothing.
				}
			}
		}
		catch(ModelInterpreterException | IOException e)
		{
			exception = new GeppettoExecutionException(e);
		}

	}

	private URL getLocalURL(String localPath) throws MalformedURLException
	{
		File catalinaBase = new File(System.getProperty("catalina.home")).getAbsoluteFile();
		return new File(catalinaBase, localPath).toURI().toURL();
	}

}
