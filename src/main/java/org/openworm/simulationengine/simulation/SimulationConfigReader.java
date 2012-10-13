package org.openworm.simulationengine.simulation;

import java.net.URL;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.openworm.simulationengine.simulation.model.Simulation;

public class SimulationConfigReader {

	public static Simulation readConfig(URL url) 
	{
		JAXBContext context;
		
		Simulation sim = null;
		try 
		{
			context = JAXBContext.newInstance(Simulation.class);
			Unmarshaller um = context.createUnmarshaller();
			sim = (Simulation) um.unmarshal(url);
		} 
		catch (JAXBException e1) 
		{
			e1.printStackTrace();
		}
		
		return sim;
	}
}
