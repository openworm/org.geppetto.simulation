package org.geppetto.simulation.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geppetto.core.common.GeppettoCommonUtils;
import org.geppetto.simulation.manager.ViewProcessor;
import org.geppetto.simulation.manager.ViewProcessor.JsonObjectExtensionConflictException;
import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ViewProcessorTest
{
	@Test
	public void test() throws IOException, JsonObjectExtensionConflictException
	{
		String expected = GeppettoCommonUtils.readString(ViewProcessorTest.class.getResourceAsStream("/expectedJSON.json"));
		JsonParser parser = new JsonParser();
		
		JsonObject object1 = parser.parse("{\"Plot1\": {"
				+ "\"name\": \"Recorded Variables\"," 
				+ "\"size\": {"
				+ "\"height\": 314.8"
				+ "},"
				+ "\"dataType\": \"object\","
				+ "\"data\": [\"hhcell.hhpop[0].v1\"],"
				+ "}}").getAsJsonObject();
		
		JsonObject object2 = parser.parse("{\"Plot1\": {"
				+ "\"widgetType\": 0,"
				+ "\"size\": {"
				+ "\"width\": 628.8"
				+ "\"data\": [\"hhcell.hhpop[0].v2\"],"
				+ "},"
				+ "}}").getAsJsonObject();
		
		JsonObject object3 = parser.parse("{\"Popup1\": {"
				+ "\"widgetType\": 1,"
				+ "\"name\": \"fix\"," 
				+ "\"position\": {"
				+ "\"left\": 358"
				+ "},"
				+ "\"dataType\": \"string\","
				+ "\"componentSpecific\": {"
				+ "\"customHandlers\": [\"test1\"],"
				+ "},"
				+ "}}").getAsJsonObject();
		
		JsonObject object4 = parser.parse("{\"Popup1\": {"
				+ "\"name\": \"fix\","
				+ "\"position\": {"
				+ "\"top\": 66"
				+ "},"
				+ "\"data\": \"bugs\","
				+ "\"componentSpecific\": {"
				+ "\"customHandlers\": [\"test2\"],"
				+ "},"
				+ "}}").getAsJsonObject();
		
		JsonObject object5 = parser.parse("{\"PopupX\": {"
				+ "\"name\": \"abc\","
				+ "\"widgetType\": 1,"
				+ "\"data\": \"xxx\","
				+ "}}").getAsJsonObject();
		
		List<JsonObject> viewCustomisations = new ArrayList<JsonObject>();
		viewCustomisations.add(object1);
		viewCustomisations.add(object2);
		viewCustomisations.add(object3);
		viewCustomisations.add(object4);
		String actual = ViewProcessor.getView(viewCustomisations);
		
		Assert.assertEquals(expected, actual);
	}
}
