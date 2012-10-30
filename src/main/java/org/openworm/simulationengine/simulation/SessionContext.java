package org.openworm.simulationengine.simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.openworm.simulationengine.core.model.IModel;
import org.openworm.simulationengine.core.model.IModelInterpreter;
import org.openworm.simulationengine.core.simulator.ISimulator;

public class SessionContext {

	public ConcurrentHashMap<String, List<IModel>> modelsByAspect = new ConcurrentHashMap<String, List<IModel>>();
	
	public ConcurrentHashMap<String, IModelInterpreter> modelInterpretersByAspect = new ConcurrentHashMap<String, IModelInterpreter>();
	
	public ConcurrentHashMap<String, ISimulator> simulatorsByAspect = new ConcurrentHashMap<String, ISimulator>();
	
	public ConcurrentHashMap<String, String> modelURLByAspect = new ConcurrentHashMap<String, String>();
	
	public List<String> aspectIDs = new ArrayList<String>();
	
	public int processedAspects = 0;

	public boolean runningCycle=false;

	public boolean runSimulation=false;
}
