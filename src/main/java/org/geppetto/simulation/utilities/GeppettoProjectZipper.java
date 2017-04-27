package org.geppetto.simulation.utilities;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geppetto.core.data.model.IExperiment;
import org.geppetto.core.data.model.IGeppettoProject;
import org.geppetto.core.data.model.IView;
import org.geppetto.core.utilities.LocalViewSerializer;
import org.geppetto.core.utilities.URLReader;
import org.geppetto.core.utilities.Zipper;
import org.geppetto.simulation.manager.GeppettoManager;

import com.amazonaws.util.json.JSONException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.rits.cloning.Cloner;

/**
 * Helper class for zipping a geppetto project. Clones an existing geppetto project, 
 * changes paths to be relative and writes it to a temporary folder
 * @author jrmartin
 *
 */
public class GeppettoProjectZipper {
	
	private static Log logger = LogFactory.getLog(GeppettoManager.class);
	/**
	 * Clones a Geppetto Project Object and writes it in JSON format to a temporary file
	 * @param geppettoProject
	 * @param dir
	 * @param zipper 
	 * @return
	 * @throws JSONException
	 * @throws IOException
	 */
	public File writeIGeppettoProjectToJson(IGeppettoProject geppettoProject, File dir, Zipper zipper) throws JSONException, IOException{
		//clone geppetto project
		Cloner cloner = new Cloner();
		IGeppettoProject clonegeppettoProject = cloner.deepClone(geppettoProject);

		//convert geppetto project object to json
		Gson gson = new Gson();
		String message = gson.toJson(clonegeppettoProject);
		JsonObject jObject  =new JsonParser().parse(message).getAsJsonObject();
	      
		//replace URL for geppettomodel with relative path
		JsonObject geppettoModel = jObject.getAsJsonObject("geppettoModel");
		JsonPrimitive fullPath = geppettoModel.getAsJsonPrimitive("url");
		jObject.getAsJsonObject("geppettoModel").addProperty("url",this.getRelativePath(fullPath.getAsString()));

		//keeps track of id, to be used to name location of .json 
		String id = jObject.getAsJsonPrimitive("id").getAsString();
		
		if(jObject.has("view")){
			JsonObject view =  jObject.getAsJsonObject("view");
			if(view.has("viewStates")){
				GsonBuilder gsonBuilder = new GsonBuilder();
				gsonBuilder.registerTypeHierarchyAdapter(IView.class, new LocalViewSerializer());
				IView iview = null;
				for(IExperiment e : clonegeppettoProject.getExperiments()){
					String idE =view.get("id").toString();
					if(Integer.valueOf(idE)==e.getId()){
						iview= e.getView();
					}
				}
				if(iview != null){
					String json= gsonBuilder.create().toJson(iview);
					JsonParser parser = new JsonParser();
					JsonObject jsonObject  = parser.parse(json).getAsJsonObject();
					jObject.add("view", jsonObject);
				}
			}
		}
		
		//keep track of scripts and results paths
		List<String>  scripts = new ArrayList<String>();
		List<String>  simulationResultsPaths = new ArrayList<String>();		
		//loop through experiments to change relative paths
		JsonArray experiments = (JsonArray) jObject.get("experiments");
		for(int i =0; i<experiments.size();i++){
			JsonObject experiment = (JsonObject) experiments.get(i);
			if(experiment.has("script")){
				//full path to existing project script
				String scriptPath =  experiment.getAsJsonPrimitive("script").getAsString();
				if(!scripts.contains(scriptPath)){
					scripts.add(scriptPath);
					//add script to zip if it hasn't already
					zipper.addToZip(URLReader.getURL(scriptPath));
				}
				experiment.addProperty("script", this.getRelativePath(scriptPath));
			}
			
			if(experiment.has("lastModified")){
				//full path to existing project lastModified
				String lastModified =  experiment.getAsJsonPrimitive("lastModified").getAsString();
				Date timestamp = new Date(lastModified);
				experiment.addProperty("lastModified", timestamp.getTime()/1000);
			}
			
			if(experiment.has("creationDate")){
				//full path to existing project lastModified
				String lastModified =  experiment.getAsJsonPrimitive("creationDate").getAsString();
				Date timestamp = new Date(lastModified);
				experiment.addProperty("creationDate", timestamp.getTime()/1000);
			}
			
			if(experiment.has("view")){
				//full path to existing project lastModified
				JsonObject view =  experiment.getAsJsonObject("view");
				if(view.has("viewStates")){
					GsonBuilder gsonBuilder = new GsonBuilder();
					gsonBuilder.registerTypeHierarchyAdapter(IView.class, new LocalViewSerializer());
					IView iview = null;
					for(IExperiment e : clonegeppettoProject.getExperiments()){
						String idE =experiment.get("id").toString();
						if(Integer.valueOf(idE)==e.getId()){
							iview= e.getView();
						}
					}
					if(iview != null){
						String json= gsonBuilder.create().toJson(iview);
						JsonParser parser = new JsonParser();
						JsonObject jsonObject  = parser.parse(json).getAsJsonObject();
						experiment.add("view", jsonObject);
					}
				}
			}

			//loop through simulation results to change relative path
			if(experiment.has("simulationResults")){
				JsonArray simulationResults = (JsonArray) experiment.get("simulationResults");
				for(int j =0; j<simulationResults.size();j++){
					JsonObject simulationResult = (JsonObject) simulationResults.get(j);
					if(simulationResult.has("result")){
						if(simulationResult.getAsJsonObject("result").has("url")){
							//full path to simulations results file
							String resultsPath = 
									simulationResult.getAsJsonObject("result").getAsJsonPrimitive("url").getAsString();
							if(!simulationResultsPaths.contains(resultsPath)){
								simulationResultsPaths.add(resultsPath);
								URL resultsLocation = URLReader.getURL(resultsPath);
								//only add simulations results to zip if it hasn'e been already added
								if(resultsLocation!=null){
									zipper.addToZip(resultsLocation);
								}
							}
							simulationResult.getAsJsonObject("result").addProperty("url", this.getRelativePath(resultsPath));
						}
					}
				}
			}
		}
		//TODO: Remove before merging and when done testing
		logger.info(jObject);
		
		//write JSON to file in temporary project folder
		File jsonFile = new File(dir, id+".json");
		if(!jsonFile.exists()){
			jsonFile.createNewFile();
		}
		
		FileWriter writer = new FileWriter(jsonFile.getAbsolutePath()); 
		writer.write(jObject.toString());
		writer.close();

		return jsonFile;
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
