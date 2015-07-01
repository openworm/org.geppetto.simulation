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

import java.util.Map;

import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.model.simulation.Model;
import org.geppetto.core.model.state.visitors.GeppettoModelVisitor;
import org.geppetto.core.services.ServiceCreator;

/**
 * This visitor discovers and instantiates the services for each model interpreter and simulator. A thread is used to instantiate the services so that a new instance of the services is created at each
 * time (the services use a ThreadScope).
 * 
 * @author matteocantarelli
 * 
 */
public class CreateModelInterpreterServicesVisitor extends GeppettoModelVisitor
{

	private Map<String, IModelInterpreter> models;


	public CreateModelInterpreterServicesVisitor(Map<String, IModelInterpreter> models)
	{
		super();
		this.models = models;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.massfords.humantask.TraversingVisitor#visit(org.geppetto.simulation.model.Model)
	 */
	@Override
	public void visit(Model model)
	{
		super.visit(model);
		try
		{
			models.put(model.getInstancePath(), (IModelInterpreter) ServiceCreator.getNewServiceInstance(model.getModelInterpreterId()));
		}
		catch(GeppettoInitializationException e)
		{
			exception=new GeppettoExecutionException(e);
		}
	}
	

}
