package org.geppetto.simulation.visitor;

import java.io.File;
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
import java.util.regex.Matcher;
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
	private Map<URL, Path> localFileMap = new HashMap<URL, Path>();
	private RuntimeProject runtimeProject;
	private String URLIdentifier = "amazon.s3";
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
					File urlFile = new File(url.getFile());
					if(urlFile.exists()){
						//create local copy of model file, to edit and used for zipping
						URL localCopy = URLReader.createLocalCopy(Scope.CONNECTION, runtimeProject.getGeppettoProject().getId(), url,false);
						Path localFile = Paths.get(localCopy.toURI());
						
						//Paths stored in dependent models object don't match the ones inside the XML/NML files.
						//A regexp is needed to look through the file and get it instead
						Charset charset = StandardCharsets.UTF_8;
						String content = new String(Files.readAllBytes(localFile), charset);
						String regExp = "\\<include\\s*(href|file|url)\\s*=\\s*\\\"(.*)\\\"\\s*(\\/>|><\\/include>)";
						Pattern pattern = Pattern.compile(regExp, Pattern.CASE_INSENSITIVE);
						String smallerDocumentString = cleanLEMSNeuroMLDocument(content);
						Matcher matcher = pattern.matcher(smallerDocumentString);
						
						//loop through matches inside the contents of file
						while (matcher.find()) {
						    String fullPath = matcher.group(2);
						    String newPath = this.getRelativePath(fullPath);
						    
						    // let's create a map for the new file paths
						    if(fullPath.contains(URLIdentifier)||!fullPath.startsWith("http")){
						    	replaceMap.put(fullPath, newPath);
						    }
						}
						//keeps track of location of file, to modified it later on when replacing the URLs
						localFileMap.put(url, localFile);
					}
				}
				for(URL url : dependentModels)
				{
					File urlFile = new File(url.getFile());
					if(urlFile.exists()){
						// let's replace every occurrence of the original URLs inside the file with their copy
						replaceURLs(this.localFileMap.get(url), replaceMap);
						zipper.addToZip(url);
					}
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
	
	private String cleanLEMSNeuroMLDocument(String lemsString)
	{
		String smallerLemsString = lemsString.replaceAll("(?s)<!--.*?-->", ""); // remove comments
		smallerLemsString = smallerLemsString.replaceAll("(?m)^[ \t]*\r?\n", "").trim();// remove empty lines
		return smallerLemsString;
	}
}

