
package org.geppetto.simulation.visitor;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.geppetto.core.features.IDefaultViewCustomiserFeature;
import org.geppetto.core.model.GeppettoModelAccess;
import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.model.ModelInterpreterException;
import org.geppetto.core.services.GeppettoFeature;
import org.geppetto.core.utilities.URLReader;
import org.geppetto.model.GeppettoLibrary;
import org.geppetto.model.GeppettoPackage;
import org.geppetto.model.types.ImportType;
import org.geppetto.model.types.Type;
import org.geppetto.model.types.util.TypesSwitch;
import org.geppetto.model.util.GeppettoVisitingException;
import org.geppetto.model.variables.VariablesPackage;

import com.google.gson.JsonObject;

/**
 * This visitor traverses the Geppetto Model and creates the variables and types delegating to the model interpreter creation of the domain specific types
 * 
 * @author matteocantarelli
 * 
 */
public class ImportTypesVisitor extends TypesSwitch<Object>
{

	private Map<GeppettoLibrary, IModelInterpreter> modelInterpreters;
	private List<JsonObject> viewCustomisations = new ArrayList<JsonObject>();
	private GeppettoModelAccess geppettoModelAccess;
	private boolean gatherDefaultView = false;
	private String baseURL;
	private boolean forceResolve = true;

	@Override
	public Object caseImportType(ImportType type)
	{
		if(type.isAutoresolve() || this.forceResolve)
		{
			try
			{

				Type importedType = null;
				if(type.eContainingFeature().getFeatureID() == GeppettoPackage.GEPPETTO_LIBRARY__TYPES)
				{
					// this import type is inside a library
					GeppettoLibrary library = (GeppettoLibrary) type.eContainer();
					IModelInterpreter modelInterpreter = modelInterpreters.get(library);
					URL url = null;
					if(type.getUrl() != null)
					{
						url = URLReader.getURL(type.getUrl(), baseURL);
					}
					importedType = modelInterpreter.importType(url, type.getId(), library, geppettoModelAccess);

					if(this.gatherDefaultView && modelInterpreter.isSupported(GeppettoFeature.DEFAULT_VIEW_CUSTOMISER_FEATURE))
					{
						viewCustomisations
								.add(((IDefaultViewCustomiserFeature) modelInterpreter.getFeature(GeppettoFeature.DEFAULT_VIEW_CUSTOMISER_FEATURE)).getDefaultViewCustomisation(importedType));
					}

					geppettoModelAccess.swapType(type, importedType, library);

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
		}
		return super.caseImportType(type);
	}

	/**
	 * @param modelInterpreters
	 * @param commonLibraryAccess
	 * @param libraryManager
	 */
	public ImportTypesVisitor(Map<GeppettoLibrary, IModelInterpreter> modelInterpreters, GeppettoModelAccess commonLibraryAccess, boolean gatherDefaultView, String urlBase)
	{
		super();
		this.modelInterpreters = modelInterpreters;
		this.geppettoModelAccess = commonLibraryAccess;
		this.gatherDefaultView = gatherDefaultView;
		this.baseURL = urlBase;
	}

	/**
	 * @param modelInterpreters
	 * @param commonLibraryAccess
	 * @param libraryManager
	 */
	public ImportTypesVisitor(Map<GeppettoLibrary, IModelInterpreter> modelInterpreters, GeppettoModelAccess commonLibraryAccess, boolean gatherDefaultView, String urlBase, Boolean resolve)
	{
		this(modelInterpreters, commonLibraryAccess, gatherDefaultView, urlBase);
		this.setForceResolve(resolve);
	}

	/**
	 * @return
	 */
	public boolean isForceResolve()
	{
		return forceResolve;
	}

	/**
	 * @param forceResolve
	 */
	public void setForceResolve(boolean forceResolve)
	{
		this.forceResolve = forceResolve;
	}
	
	/**
	 * @return
	 * 
	 */
	public List<JsonObject> getDefaultViewCustomisations()
	{
		return viewCustomisations;
	}

}
