package org.geppetto.simulation.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geppetto.core.common.GeppettoCommonUtils;
import org.geppetto.simulation.manager.ViewProcessor;
import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonObject;

public class ViewProcessorTest
{

	@Test
	public void test() throws IOException
	{
		
		String expected = GeppettoCommonUtils.readString(ViewProcessorTest.class.getResourceAsStream("/expectedJSON.json"));
		
		JsonObject object1 = new JsonObject();
		object1.addProperty("name", "nonna");
		
		JsonObject object2 = new JsonObject();
		object1.addProperty("age", 88);
		
		List<JsonObject> viewCustomisations = new ArrayList<JsonObject>();
		viewCustomisations.add(object1);
		viewCustomisations.add(object2);
		String actual = ViewProcessor.getView(viewCustomisations);
		
		Assert.assertEquals(expected, actual);

	}

}
