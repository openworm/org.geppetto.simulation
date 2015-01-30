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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.model.simulation.Simulation;
import org.xml.sax.SAXException;


public class SimulationConfigReader
{

	public static Simulation readConfig(URL url) throws GeppettoInitializationException
	{

		Simulation sim = null;
		try
		{
			Unmarshaller unmarshaller = JAXBContext.newInstance(Simulation.class).createUnmarshaller();
			sim = (Simulation) unmarshaller.unmarshal(url);
		}
		catch(JAXBException e1)
		{
			throw new GeppettoInitializationException("Unable to unmarshall simulation with url : " + url.toString(), e1);
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
	public static Simulation readSimulationConfig(String simulationConfig) throws GeppettoInitializationException
	{

		Simulation sim = null;

		StringReader reader = new StringReader(simulationConfig);
		try
		{
			Unmarshaller unmarshaller = JAXBContext.newInstance(Simulation.class).createUnmarshaller();
			//unmarshaller.setSchema(parseSchema(new URL("https://raw.githubusercontent.com/openworm/org.geppetto.core/master/src/main/resources/schema/simulation/simulationSchema.xsd")));
			sim = (Simulation) unmarshaller.unmarshal(reader);
		}
		catch(JAXBException e)
		{
			throw new GeppettoInitializationException(e);
		}

		return sim;
	}
	
	
	public static Schema parseSchema(URL schema) {
		Schema parsedSchema = null;
		SchemaFactory sf = SchemaFactory
				.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		try {
			parsedSchema = sf.newSchema(schema);
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			//System.out.println("Problems parsing schema " + schema.getName());
			e.printStackTrace();
		}
		return parsedSchema;
	}

	/**
	 * Takes a URL for simulation file and creates a json object out of it with the simulation information.
	 * 
	 * @param url
	 * @return
	 * @throws GeppettoInitializationException
	 */
	public static String writeSimulationConfig(URL url) throws GeppettoInitializationException
	{

		String line = null;
		StringBuilder sb = new StringBuilder();

		try
		{

			BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));

			while((line = br.readLine()) != null)
			{
				sb.append(line.trim());
			}
		}
		catch(IOException e)
		{
			throw new GeppettoInitializationException("Error while attempting to read simulation's configuration");
		}

		return sb.toString();
	}
}
