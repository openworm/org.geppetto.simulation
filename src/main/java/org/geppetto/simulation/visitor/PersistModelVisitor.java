
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
import org.geppetto.model.GeppettoLibrary;
import org.geppetto.model.util.GeppettoSwitch;
import org.geppetto.model.util.GeppettoVisitingException;
import org.geppetto.simulation.manager.RuntimeProject;

/**
 * @author matteocantarelli
 *
 */
public class PersistModelVisitor extends GeppettoSwitch<Object>
{

	private IGeppettoProject project;

	private Map<String, String> replaceMap = new HashMap<String, String>();

	private Path localGeppettoModelFile;

	private RuntimeProject runtimeProject;

	/**
	 * @param localGeppettoModelFile
	 * @param runtimeProject
	 * @param project
	 */
	public PersistModelVisitor(Path localGeppettoModelFile, RuntimeProject runtimeProject, IGeppettoProject project)
	{
		this.runtimeProject = runtimeProject;
		this.project = project;
		this.localGeppettoModelFile = localGeppettoModelFile;
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
					String newPath = "projects/" + Long.toString(project.getId()) + url.getPath();
					// we process the path by building a new URL which will take care of relative paths if they exist
					String processedPath = new URL(new URL("http://127.0.0.1/"), newPath).getPath();
					if(processedPath.charAt(0) == '/')
					{
						processedPath = processedPath.substring(1);
					}
					replaceMap.put(url.toString(), processedPath);
				}
				for(URL url : dependentModels)
				{
					Path localFile = Paths.get(URLReader.createLocalCopy(Scope.CONNECTION, project.getId(), url,true).toURI());
					// let's replace every occurrence of the original URLs inside the file with their copy
					replaceURLs(localFile, replaceMap);
					// noew let's save the file in S3
					S3Manager.getInstance().saveFileToS3(localFile.toFile(), replaceMap.get(url.toString()));
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
			content = content.replaceAll(Pattern.quote(old), S3Manager.getInstance().getURL(replaceMap.get(old)).toString());
		}
		Files.write(localFile, content.getBytes(charset));
	}

	public void processLocalGeppettoFile() throws IOException
	{
		replaceURLs(localGeppettoModelFile, replaceMap);
	}
}
