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

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.utilities.URLReader;
import org.geppetto.model.LibraryManager;
import org.geppetto.model.aspect.Aspect;
import org.geppetto.model.types.ImportType;
import org.geppetto.model.types.Type;
import org.geppetto.model.types.util.TypesSwitch;

/**
 * This visitor traverses the Geppetto Model and creates the variables and types delegating to the model interpreter creation of the domain specific types
 * 
 * @author matteocantarelli
 * 
 */
public class CreateInstanceTreeVisitor extends TypesSwitch<GeppettoExecutionException>
{

	private LibraryManager libraryManager;
	private Map<String, IModelInterpreter> modelInterpreters;


	@Override
	public GeppettoExecutionException caseImportType(ImportType type)
	{
		try
		{
			// use model interpreter from aspect to populate runtime tree
			IModelInterpreter modelInterpreter = modelInterpreters.get(type.getModelInterpreterId());
			// there is not a 1:1 between a model interpreter and an aspect, an aspect defines a domain
			// a model interpreter a particular format that can be handled by Geppetto
			// the same model interpreter, e.g. OBJ, could be used in different aspects.
			// while aspects hold types by domain, libraries hold types by model interpreter
			Collection<Type> types = modelInterpreter.importType(URLReader.getURL(type.getUrl()), type.getName(), libraryManager);
			Aspect aspect = type.getAspect();
			aspect.getTypes().addAll(types);
		}
		catch(IOException e)
		{
			return new GeppettoExecutionException(e);
		}
		return super.caseImportType(type);
	}

	/**
	 * @param modelInterpreters
	 * @param libraryManager
	 */
	public CreateInstanceTreeVisitor(Map<String, IModelInterpreter> modelInterpreters, LibraryManager libraryManager)
	{
		super();
		this.libraryManager = libraryManager;
		this.modelInterpreters = modelInterpreters;

	}

}
