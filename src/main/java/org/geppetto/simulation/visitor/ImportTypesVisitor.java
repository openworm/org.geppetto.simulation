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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.geppetto.core.model.GeppettoModelAccess;
import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.model.ModelInterpreterException;
import org.geppetto.core.utilities.URLReader;
import org.geppetto.model.GeppettoLibrary;
import org.geppetto.model.GeppettoPackage;
import org.geppetto.model.types.ImportType;
import org.geppetto.model.types.Type;
import org.geppetto.model.types.util.TypesSwitch;
import org.geppetto.model.util.GeppettoVisitingException;
import org.geppetto.model.variables.Variable;
import org.geppetto.model.variables.VariablesPackage;

/**
 * This visitor traverses the Geppetto Model and creates the variables and types delegating to the model interpreter creation of the domain specific types
 * 
 * @author matteocantarelli
 * 
 */
public class ImportTypesVisitor extends TypesSwitch<Object>
{

	private Map<GeppettoLibrary, IModelInterpreter> modelInterpreters;
	private GeppettoModelAccess commonLibraryAccess;
	private List<ImportType> processed=new ArrayList<ImportType>();

	/**
	 * This method is used to remove the types that were replaced by real ones.
	 * It needs to be done after the iteration or we mess the iterator.
	 */
	public void removeProcessedImportType()
	{
		for(ImportType type:processed)
		{
			((GeppettoLibrary)type.eContainer()).getTypes().remove(type);
		}
		processed.clear();
	}
	
	@Override
	public Object caseImportType(ImportType type)
	{
		try
		{

			Type importedType = null;
			if(type.eContainingFeature().getFeatureID() == GeppettoPackage.GEPPETTO_LIBRARY__TYPES)
			{
				// this import type is inside a library
				GeppettoLibrary library = (GeppettoLibrary) type.eContainer();
				IModelInterpreter modelInterpreter = modelInterpreters.get(library);
				importedType = modelInterpreter.importType(URLReader.getURL(type.getUrl()), type.getId(), library, commonLibraryAccess);
				//TODO User GeppettoModelAccess and Commands to perform this swapping
				List<Variable> referencedVars=new ArrayList<Variable>(type.getReferencedVariables());
				for(Variable v:referencedVars)
				{
					v.getTypes().remove(type);
					v.getTypes().add(importedType);
				}
				processed.add(type);
				library.getTypes().add(importedType);
			}
			else if(type.eContainingFeature().getFeatureID() == VariablesPackage.VARIABLE__ANONYMOUS_TYPES)
			{
				// this import is inside a variable as anonymous type
				// importedType = modelInterpreter.importType(URLReader.getURL(type.getUrl()), type.getName(), library);
				// ((Variable) type.eContainer()).getAnonymousTypes().remove(type);
				// ((Variable) type.eContainer()).getAnonymousTypes().add(importedType);
				return new GeppettoVisitingException("Anonymous types at the root level initially not supported");
			}

		}
		catch(IOException e)
		{
			return new GeppettoVisitingException(e);
		}
		catch(ModelInterpreterException e)
		{
			return new GeppettoVisitingException(e);
		}
		return super.caseImportType(type);
	}

	/**
	 * @param modelInterpreters
	 * @param commonLibraryAccess 
	 * @param libraryManager
	 */
	public ImportTypesVisitor(Map<GeppettoLibrary, IModelInterpreter> modelInterpreters, GeppettoModelAccess commonLibraryAccess)
	{
		super();
		this.modelInterpreters = modelInterpreters;
		this.commonLibraryAccess=commonLibraryAccess;

	}

}
