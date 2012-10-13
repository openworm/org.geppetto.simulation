package org.openworm.simulationengine.simulation;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WebSocketServlet;
import org.openworm.simulationengine.core.model.IModelInterpreter;
import org.openworm.simulationengine.core.simulator.ISimulator;
import org.openworm.simulationengine.simulation.model.Aspect;
import org.openworm.simulationengine.simulation.model.Simulation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

@Configurable
public class SimulationServlet extends WebSocketServlet {
	
	private @Autowired AutowireCapableBeanFactory beanFactory;
	
	List<IModelInterpreter> _modelInterpreters;
	List<ISimulator> _simulators;
	
	// TODO: inject this from a config file local to this bundle
	private URL _configUrl;
	
	private static final long serialVersionUID = 1L;

	//FIXME MC: This is just a skeleton, the servlet an the whole infrastracture needs to be put in place
	@Override
	protected StreamInbound createWebSocketInbound(String subProtocol, HttpServletRequest request) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private void runSimulation()
	{	
		//it's going to be created when we read in the simulation file
		Simulation sim = SimulationConfigReader.readConfig(_configUrl);
		
		for(Aspect aspect : sim.getAspects())
		{
			String id = aspect.getId();
			String modelInterpreterId = aspect.getModelInterpreter();
			String simulatorId = aspect.getSimulator();
			String modelURL = aspect.getModelURL();
			
			//needs to be instantiated dynamically
			IModelInterpreter modelInterpreter = null;
			//beanFactory.(obj);
			
			//needs to be instantiated dynamically
			ISimulator simulator=null;
			try {
				modelInterpreter.readModel(new URL(modelURL));
				
				// send down to the simulator the models that have been read	
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		
	}
	
}
