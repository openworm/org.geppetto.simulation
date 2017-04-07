package org.geppetto.simulation.visitor;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.data.model.IGeppettoProject;
import org.geppetto.core.manager.Scope;
import org.geppetto.core.manager.SharedLibraryManager;
import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.s3.S3Manager;
import org.geppetto.core.utilities.URLReader;
import org.geppetto.core.utilities.Zipper;
import org.geppetto.model.GeppettoLibrary;
import org.geppetto.model.util.GeppettoSwitch;
import org.geppetto.model.util.GeppettoVisitingException;
import org.geppetto.simulation.manager.RuntimeProject;


public class GeppettoModelVisitor extends GeppettoSwitch<Object>{
	private IGeppettoProject project;

	private Map<String, String> replaceMap = new HashMap<String, String>();

	private RuntimeProject runtimeProject;

	private Zipper zipper;

	/**
	 * @param localGeppettoTypeFile 
	 * @param localGeppettoModelFile
	 * @param runtimeProject
	 * @param project
	 * @param zipper 
	 */
	public GeppettoModelVisitor(RuntimeProject runtimeProject, Zipper zipper)
	{
		this.runtimeProject = runtimeProject;
		this.zipper = zipper;
	}
	
	@Override
	public Object caseGeppettoLibrary(GeppettoLibrary library)
	{

		try
		{
			if(!library.getId().equals(SharedLibraryManager.getSharedCommonLibrary().getId()))
			{
				IModelInterpreter modelInterpreter = runtimeProject.getModelInterpreter(library);
				List<URL> dependentModels = modelInterpreter.getDependentModels();		
				for(URL url : dependentModels)
				{
					// let's create a map for the new file paths
					String fullPath = url.getPath();
					String newPath = this.getRelativePath(fullPath);
					
					replaceMap.put(fullPath, newPath);
					
					zipper.addToZip(url);
				}
				for(URL url : dependentModels)
				{
					Path localFile = 
							Paths.get(URLReader.createLocalCopy(Scope.CONNECTION, runtimeProject.getGeppettoProject().getId(), url,false).toURI());
					// let's replace every occurrence of the original URLs inside the file with their copy
					replaceURLs(localFile, replaceMap);
				}
			}
		}
		catch(URISyntaxException | IOException | GeppettoInitializationException e)
		{
			return new GeppettoVisitingException(e);
		} 

		return super.caseGeppettoLibrary(library);
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
}

