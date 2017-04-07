package org.geppetto.simulation.visitor;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.geppetto.core.manager.Scope;
import org.geppetto.core.utilities.Zipper;
import org.geppetto.model.GeppettoPackage;
import org.geppetto.model.types.ImportType;
import org.geppetto.model.types.util.TypesSwitch;
import org.geppetto.model.util.GeppettoModelTraversal;
import org.geppetto.model.util.GeppettoVisitingException;
import org.geppetto.simulation.manager.RuntimeProject;

public class GeppettoModelTypesVisitor extends TypesSwitch<Object>
{
	private Map<String, String> replaceMap = new HashMap<String, String>();
	private Path localGeppettoModelFile;
	private Scope scope;
	private RuntimeProject runtimeProject;
	private Zipper zipper;
	private String URLIdentifier = "amazon.s3";

	@Override
	public Object caseImportType(ImportType type)
	{
		try
		{

			if(type.eContainingFeature().getFeatureID() == GeppettoPackage.GEPPETTO_LIBRARY__TYPES)
			{
				String fullPath = type.getUrl();
				String newPath = this.getRelativePath(fullPath);

				//only replace URLs for local and amazon instance paths, not other hhttp
				if(fullPath.contains(URLIdentifier)||!fullPath.startsWith("http")){
					replaceMap.put(fullPath, newPath);

					// let's replace every occurrence of the original URLs inside the file with their copy
					replaceURLs(localGeppettoModelFile, replaceMap);				

					//user visitor to traverse through Geppetto Model
					GeppettoModelVisitor dependentModelsVisitor = new GeppettoModelVisitor(this.runtimeProject, zipper);
					GeppettoModelTraversal.apply(this.runtimeProject.getGeppettoModel(), dependentModelsVisitor);
				}
			}
		}
		catch(Exception e)
		{
			return new GeppettoVisitingException(e);
		}
		return super.caseImportType(type);
	}

	/**
	 * Makes a fullpath become a relativve path
	 * @param fullPath
	 * @return
	 */
	public String getRelativePath(String fullPath){
		String[] fullPathSplit = fullPath.split("/");
		String relativePath = "/"+fullPathSplit[fullPathSplit.length-1];
		
		return relativePath;
	}
	
	/**
	 * This method replaces in the localFile all the occurrences of the old URLs with the new ones
	 * 
	 * @param localFile
	 * @param replaceMap
	 * @throws IOException
	 */
	private void replaceURLs(Path localFile, Map<String, String> replaceMap) throws IOException
	{
		Charset charset = StandardCharsets.UTF_8;

		String content = new String(Files.readAllBytes(localFile), charset);
		for(String old : replaceMap.keySet())
		{
			content = content.replaceAll(Pattern.quote(old), replaceMap.get(old));
		}
		Files.write(localFile, content.getBytes(charset));
	}

	public void processLocalGeppettoFile() throws IOException
	{
		replaceURLs(localGeppettoModelFile, replaceMap);
	}
	
	/**
	 * @param projectId
	 * @param scope
	 * @param libraryManager
	 */
	public GeppettoModelTypesVisitor(Path localGeppettoModelFile,RuntimeProject runtimeProject,Zipper zipper, Scope scope)
	{
		super();
		this.runtimeProject = runtimeProject;
		this.zipper = zipper;
		this.localGeppettoModelFile = localGeppettoModelFile;
		this.scope=scope;
	}

}