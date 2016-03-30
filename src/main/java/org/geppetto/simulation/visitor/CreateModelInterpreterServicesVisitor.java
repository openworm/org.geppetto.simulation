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
import org.geppetto.core.manager.Scope;
import org.geppetto.core.model.AModelInterpreter;
import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.services.ServiceCreator;
import org.geppetto.model.GeppettoLibrary;
import org.geppetto.model.GeppettoPackage;
import org.geppetto.model.types.ImportType;
import org.geppetto.model.types.util.TypesSwitch;
import org.geppetto.model.util.GeppettoVisitingException;

/**
 * This visitor discovers and instantiates the services for each model interpreter. A thread is used to instantiate the services so that a new instance of the services is created at each time (the
 * services use a ThreadScope).
 * 
 * @author matteocantarelli
 * 
 */
public class CreateModelInterpreterServicesVisitor extends TypesSwitch<Object>
{

	private Map<GeppettoLibrary, IModelInterpreter> models;
	private Scope scope;
	private long projectId;

	public CreateModelInterpreterServicesVisitor(Map<GeppettoLibrary, IModelInterpreter> models, long projectId, Scope scope)
	{
		super();
		this.scope = scope;
		this.projectId = projectId;
		this.models = models;
	}

	@Override
	public Object caseImportType(ImportType type)
	{
		try
		{
			if(type.eContainingFeature().getFeatureID() == GeppettoPackage.GEPPETTO_LIBRARY__TYPES)
			{
				// this import type is inside a library
				GeppettoLibrary library = (GeppettoLibrary) type.eContainer();
				if(!models.containsKey(library))
				{
					AModelInterpreter modelInterpreter = (AModelInterpreter) ServiceCreator.getNewServiceInstance(type.getModelInterpreterId());
					modelInterpreter.setProjectId(projectId);
					modelInterpreter.setScope(scope);
					models.put(library, modelInterpreter);
				}
			}
			else
			{
				return new GeppettoExecutionException("Anonymous types at the root level initially not supported");
			}
		}
		catch(GeppettoInitializationException e)
		{
			return new GeppettoVisitingException(e);
		}

		return super.caseImportType(type);
	}

}
