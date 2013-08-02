/*******************************************************************************
 * The MIT License (MIT)
 *
 * Copyright (c) 2011, 2013 OpenWorm.
 * http://openworm.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     	OpenWorm - http://openworm.org/people.html
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
 * USE OR OTHER DEALINGS IN THE SOFTWARE.
 *******************************************************************************/

package org.geppetto.simulation;

import java.io.IOException;
import java.net.URL;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.simulation.model.Simulation;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SimulationConfigReader {

	public static Simulation readConfig(URL url) throws GeppettoInitializationException {
		
		
		Simulation sim = null;
		try {
			Unmarshaller unmarshaller = JAXBContext.newInstance(Simulation.class).createUnmarshaller();
			sim = (Simulation) unmarshaller.unmarshal(url);
		} catch (JAXBException e1) {
			throw new GeppettoInitializationException("Unable to unmarshall simulation with ur : " + url.toString());
		}

		return sim;
	}
	
	/**
	 * Takes a JSON object with simulation information and creates a Simulation out of it.
	 * 
	 * @param simulationConfig
	 * @return
	 * @throws GeppettoInitializationException
	 */
	public static Simulation readSimulationConfig(String simulationConfig) throws GeppettoInitializationException {

		ObjectMapper mapper = new ObjectMapper();
		Simulation sim = null;
		try {
			sim = mapper.readValue(simulationConfig, Simulation.class);
		} catch (JsonParseException e) {
			throw new GeppettoInitializationException("Unable to unmarshall simulation");
		} catch (JsonMappingException e) {
			throw new GeppettoInitializationException("Unable to unmarshall simulation, invalid information");
		} catch (IOException e) {
			throw new GeppettoInitializationException("Unable to unmarshall simulation");
		}
		
		return sim;
	}
	
	/**
	 * Takes a URL for simulation file and creates a json object out of it with the 
	 * simulation information.
	 * 
	 * @param url
	 * @return
	 */
	public static String writeSimulationConfig(URL url){

		ObjectMapper mapper = new ObjectMapper();

		String json = null;

		Simulation simulation;
		try {
			simulation = readConfig(url);
			json = mapper.writeValueAsString(simulation);
		} catch (GeppettoInitializationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return json;
	}
}
