package org.openworm.simulationengine.simulation;

import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WebSocketServlet;
import org.openworm.simulationengine.core.model.IModel;
import org.openworm.simulationengine.core.model.IModelInterpreter;
import org.openworm.simulationengine.core.simulation.TimeConfiguration;
import org.openworm.simulationengine.core.simulator.ISimulator;
import org.openworm.simulationengine.simulation.model.Aspect;
import org.openworm.simulationengine.simulation.model.Simulation;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

@Configurable
public class SimulationServlet extends WebSocketServlet {
	
	BundleContext _bc = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
	
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
		try {
			//it's going to be created when we read in the simulation file
			Simulation sim = SimulationConfigReader.readConfig(_configUrl);
		
			for(Aspect aspect : sim.getAspects())
			{
				String id = aspect.getId();
				String modelInterpreterId = aspect.getModelInterpreter();
				String simulatorId = aspect.getSimulator();
				String modelURL = aspect.getModelURL();
				
				IModelInterpreter modelInterpreter = this.<IModelInterpreter>getService(modelInterpreterId, IModelInterpreter.class.getName());
				ISimulator simulator = this.<ISimulator>getService(simulatorId, ISimulator.class.getName());
				
				List<IModel> models = modelInterpreter.readModel(new URL(modelURL));
					
				// send down to the simulator the models that have been read
				// simulator.simulate()
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * A generic routine to encapsulate boiler-plate code for dynamic service discovery
	 */
	private <T> T getService(String discoveryId, String type) throws InvalidSyntaxException{
		T service = null;
		
		String filter = String.format("(discoverableID=%s)", discoveryId);
		ServiceReference[] sr  =  _bc.getServiceReferences(type, filter);
		if(sr != null && sr.length > 0)
		{
			service = (T) _bc.getService(sr[0]);
		}
		
		return service;
	}
	
}
