package org.openworm.simulationengine.simulation;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.catalina.websocket.WsOutbound;
import org.openworm.simulationengine.core.model.IModel;
import org.openworm.simulationengine.core.model.IModelInterpreter;
import org.openworm.simulationengine.core.simulation.ITimeConfiguration;
import org.openworm.simulationengine.core.simulator.ISimulator;

public class SessionContext {

	public ConcurrentHashMap<String, List<IModel>> _modelsByAspect = new ConcurrentHashMap<String, List<IModel>>();
	
	public ConcurrentHashMap<String, IModelInterpreter> _modelInterpretersByAspect = new ConcurrentHashMap<String, IModelInterpreter>();
	
	public ConcurrentHashMap<String, ISimulator> _simulatorsByAspect = new ConcurrentHashMap<String, ISimulator>();
	
	public ConcurrentHashMap<String, String> _modelURLByAspect = new ConcurrentHashMap<String, String>();
	
	public int _processedAspects = 0;

	public boolean _runningCycle=false;

	public boolean _runSimulation=false;
}
