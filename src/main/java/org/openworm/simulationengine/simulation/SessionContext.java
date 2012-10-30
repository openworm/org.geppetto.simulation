package org.openworm.simulationengine.simulation;

import java.util.List;
import java.util.Map;

import org.apache.catalina.websocket.WsOutbound;
import org.openworm.simulationengine.core.model.IModel;
import org.openworm.simulationengine.core.simulation.ITimeConfiguration;

public class SessionContext {
	public ITimeConfiguration _timeConfiguration=null;

	public Map<String, List<IModel>> _models;
	
	public int _processedElements = 0;

	public boolean _runningCycle=false;

	public boolean _runSimulation=false;
}
