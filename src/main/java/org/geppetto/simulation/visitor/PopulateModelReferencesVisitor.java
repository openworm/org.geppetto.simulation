
package org.geppetto.simulation.visitor;

import java.util.ArrayList;
import java.util.List;

import org.geppetto.model.types.ImportType;
import org.geppetto.model.types.util.TypesSwitch;

/**
 * @author matteocantarelli
 *
 */
public class PopulateModelReferencesVisitor extends TypesSwitch<Object>
{

	private List<String> modelReferences = new ArrayList<String>();


	@Override
	public Object caseImportType(ImportType importType)
	{
		if(importType.getReferenceURL() != null)
		{
			modelReferences.add(importType.getReferenceURL());
		}
		return super.caseType(importType);
	}



	public List<String> getModelReferences()
	{
		return modelReferences;
	}

}
