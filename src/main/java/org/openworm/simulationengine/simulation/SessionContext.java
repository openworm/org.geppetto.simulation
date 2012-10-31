package org.openworm.simulationengine.simulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.openworm.simulationengine.core.model.IModel;
import org.openworm.simulationengine.core.model.IModelInterpreter;
import org.openworm.simulationengine.core.simulator.ISimulator;

public class SessionContext
{
	public ConcurrentHashMap<String, HashMap<String, List<IModel>>> modelsByAspect = new ConcurrentHashMap<String, HashMap<String, List<IModel>>>();

	public ConcurrentHashMap<String, IModelInterpreter> modelInterpretersByAspect = new ConcurrentHashMap<String, IModelInterpreter>();

	public ConcurrentHashMap<String, ISimulator> simulatorsByAspect = new ConcurrentHashMap<String, ISimulator>();

	public ConcurrentHashMap<String, String> modelURLByAspect = new ConcurrentHashMap<String, String>();

	public ConcurrentHashMap<String, Integer> processedElementsByAspect = new ConcurrentHashMap<String, Integer>();

	public ConcurrentHashMap<String, Integer> elementCountByAspect = new ConcurrentHashMap<String, Integer>();

	public List<String> aspectIDs = new ArrayList<String>();

	public boolean runningCycle = false;

	public boolean runSimulation = false;
}
