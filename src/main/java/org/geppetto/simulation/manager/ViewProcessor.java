/**
 * 
 */
package org.geppetto.simulation.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * @author matteocantarelli
 *
 */
public class ViewProcessor
{
	public static String getView(List<JsonObject> viewCustomisations) throws JsonObjectExtensionConflictException
	{
		JsonParser parser = new JsonParser();
		
		// root element to merge all customisations into one view
		JsonObject root = parser.parse("{\"views\": {}}").getAsJsonObject();

		Map<String, List<JsonObject>> jsonObjectsMap = new HashMap<String, List<JsonObject>>();
		for (JsonObject jsonObj : viewCustomisations) {
			// build map of top level duplicates
			for (Map.Entry<String,JsonElement> entry : jsonObj.entrySet()) {
				// for each entry add to map and if already there increase count
				if(jsonObjectsMap.get(entry.getKey()) != null){
					// already there, add element
					List<JsonObject> objs = jsonObjectsMap.get(entry.getKey());
					objs.add((JsonObject) entry.getValue());
					jsonObjectsMap.put(entry.getKey(), objs);
				} else {
					List<JsonObject> objs = new ArrayList<JsonObject>();
					objs.add((JsonObject) entry.getValue());
					jsonObjectsMap.put(entry.getKey(), objs);
				}
			}
		}
		
		// iterate map and merge all json objects per item (if more than 1)
		JsonObject views = root.getAsJsonObject("views");
		for (String key : jsonObjectsMap.keySet()) {
			if(jsonObjectsMap.get(key).size() > 1){
				// merge objects
				JsonObject mergedObj = new JsonObject();
				JsonObject[] objs = jsonObjectsMap.get(key).toArray(new JsonObject[jsonObjectsMap.get(key).size()]);
				ViewProcessor.extendJsonObject(mergedObj, ConflictStrategy.PREFER_FIRST_OBJ, objs);
				// add merged object to root
				views.add(key, mergedObj);
			} else if(jsonObjectsMap.get(key).size() == 1){
				// only 1 item, simply add to root element with property key
				views.add(key, jsonObjectsMap.get(key).get(0));
			}
		}
		
		return root.toString();
	}

	public static enum ConflictStrategy {

        THROW_EXCEPTION, PREFER_FIRST_OBJ, PREFER_SECOND_OBJ, PREFER_NON_NULL;
    }

    public static class JsonObjectExtensionConflictException extends Exception {

		private static final long serialVersionUID = 1L;

		public JsonObjectExtensionConflictException(String message) {
            super(message);
        }

    }

    public static void extendJsonObject(JsonObject destinationObject, ConflictStrategy conflictResolutionStrategy, JsonObject ... objs) throws JsonObjectExtensionConflictException {
        for (JsonObject obj : objs) {
            extendJsonObject(destinationObject, obj, conflictResolutionStrategy);
        }
    }

    private static void extendJsonObject(JsonObject leftObj, JsonObject rightObj, ConflictStrategy conflictStrategy) throws JsonObjectExtensionConflictException {
        for (Map.Entry<String, JsonElement> rightEntry : rightObj.entrySet()) {
            String rightKey = rightEntry.getKey();
            JsonElement rightVal = rightEntry.getValue();
            if (leftObj.has(rightKey)) {
                //conflict                
                JsonElement leftVal = leftObj.get(rightKey);
                if (leftVal.isJsonArray() && rightVal.isJsonArray()) {
                    JsonArray leftArr = leftVal.getAsJsonArray();
                    JsonArray rightArr = rightVal.getAsJsonArray();
                    //concat the arrays -- there cannot be a conflict in an array, it's just a collection of stuff
                    for (int i = 0; i < rightArr.size(); i++) {
                        leftArr.add(rightArr.get(i));
                    }
                } else if (leftVal.isJsonObject() && rightVal.isJsonObject()) {
                    //recursive merging
                    extendJsonObject(leftVal.getAsJsonObject(), rightVal.getAsJsonObject(), conflictStrategy);
                } else {//not both arrays or objects, normal merge with conflict resolution
                    handleMergeConflict(rightKey, leftObj, leftVal, rightVal, conflictStrategy);
                }
            } else {//no conflict, add to the object
                leftObj.add(rightKey, rightVal);
            }
        }
    }

    private static void handleMergeConflict(String key, JsonObject leftObj, JsonElement leftVal, JsonElement rightVal, ConflictStrategy conflictStrategy) throws JsonObjectExtensionConflictException {
        {
            switch (conflictStrategy) {
                case PREFER_FIRST_OBJ:
                    break;//do nothing, the right val gets thrown out
                case PREFER_SECOND_OBJ:
                    leftObj.add(key, rightVal);//right side auto-wins, replace left val with its val
                    break;
                case PREFER_NON_NULL:
                    //check if right side is not null, and left side is null, in which case we use the right val
                    if (leftVal.isJsonNull() && !rightVal.isJsonNull()) {
                        leftObj.add(key, rightVal);
                    }//else do nothing since either the left value is non-null or the right value is null
                    break;
                case THROW_EXCEPTION:
                    throw new JsonObjectExtensionConflictException("Key " + key + " exists in both objects and the conflict resolution strategy is " + conflictStrategy);
                default:
                    throw new UnsupportedOperationException("The conflict strategy " + conflictStrategy + " is unknown and cannot be processed");
            }
        }
    }
}
