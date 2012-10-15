package org.openworm.simulationengine.simulation;

import java.net.URL;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.openworm.simulationengine.simulation.model.Simulation;

public class SimulationConfigReader {

	public static Simulation readConfig(URL url) {
		
		
		Simulation sim = null;
		try {
			Unmarshaller unmarshaller = JAXBContext.newInstance(Simulation.class).createUnmarshaller();
			sim = (Simulation) unmarshaller.unmarshal(url);
		} catch (JAXBException e1) {
			e1.printStackTrace();
		}

		return sim;
	}
}
