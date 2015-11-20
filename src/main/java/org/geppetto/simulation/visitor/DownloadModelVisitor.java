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
import java.util.List;

import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.data.model.IAspectConfiguration;
import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.model.ModelInterpreterException;
import org.geppetto.core.model.typesystem.AspectNode;
import org.geppetto.core.model.typesystem.visitor.AnalysisVisitor;
import org.geppetto.core.services.ModelFormat;
import org.geppetto.core.services.registry.ServicesRegistry;

/**
 * Visitor used for retrieving model interpreter from aspect node's and sending call to interpreter for downloading the model
 * 
 * @author Adrian Quintana (adrian.perez@ucl.ac.uk)
 *
 */
public class DownloadModelVisitor extends AnalysisVisitor
{


	// The id of aspect we will be populating
	private String _instancePath;
	private ModelFormat _modelFormat;
	private IAspectConfiguration _aspectConfiguration;

	private File _modelFile;

	/**
	 * @param simulationListener
	 * @param instancePath
	 */
	public DownloadModelVisitor(String instancePath, ModelFormat format, IAspectConfiguration aspectConfiguration)
	{
		this._instancePath = instancePath;
		this._modelFormat = format;
		this._aspectConfiguration = aspectConfiguration;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.model.state.visitors.DefaultStateVisitor#inCompositeStateNode(org.geppetto.core.model.state.CompositeStateNode)
	 */
	@Override
	public boolean inAspectNode(AspectNode node)
	{
		if(this._instancePath.equals(node.getInstancePath()))
		{
			IModelInterpreter modelInterpreter = node.getModelInterpreter();
			try
			{
				//If no model format, let's get the model interpreter service format
				if (this._modelFormat == null){
					//FIXME: We are assuming there is only one format
					List<ModelFormat> supportedOutputs = ServicesRegistry.getModelInterpreterServiceFormats(modelInterpreter);
					this._modelFormat = supportedOutputs.get(0);
				}
				
				this._modelFile = modelInterpreter.downloadModel(node, this._modelFormat, this._aspectConfiguration);
			}
			catch(ModelInterpreterException e)
			{
				exception = new GeppettoExecutionException(e);
			}
		}

		return super.inAspectNode(node);
	}

	/**
	 * @return
	 */
	public File getModelFile()
	{
		return this._modelFile;
	}
}
