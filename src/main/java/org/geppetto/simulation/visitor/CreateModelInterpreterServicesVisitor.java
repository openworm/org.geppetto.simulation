
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
 * A new model interpreter is created for a given library only if it doesn't already exists in the modelInterpreters map
 * 
 * @author matteocantarelli
 * 
 */
public class CreateModelInterpreterServicesVisitor extends TypesSwitch<Object>
{

	private Map<GeppettoLibrary, IModelInterpreter> modelInterpreters;
	private Scope scope;
	private long projectId;

	public CreateModelInterpreterServicesVisitor(Map<GeppettoLibrary, IModelInterpreter> modelInterpreters, long projectId, Scope scope)
	{
		super();
		this.scope = scope;
		this.projectId = projectId;
		this.modelInterpreters = modelInterpreters;
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
				if(!modelInterpreters.containsKey(library))
				{
					AModelInterpreter modelInterpreter = (AModelInterpreter) ServiceCreator.getNewServiceInstance(type.getModelInterpreterId());
					modelInterpreter.setProjectId(projectId);
					modelInterpreter.setScope(scope);
					modelInterpreters.put(library, modelInterpreter);
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
