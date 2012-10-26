package org.openworm.simulationengine.simulation;

import java.io.IOException;
import java.lang.reflect.Type;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openworm.simulationengine.core.model.IModelInterpreter;
import org.openworm.simulationengine.core.simulator.ISimulator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * Servlet used to test service wiring
 */
public class TestServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	//BundleContext _bc = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			
			// this code works ok - the service is retrieved dynamically by the interface
			/*BundleContext bc = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
			ServiceReference sr = bc.getServiceReference(IModelInterpreter.class.getName());
			IModelInterpreter modelInterpreter = bc.getService(sr);*/
			
			// this is the same as above but uses a more generic routine to encapsulate bolier-plate code
			IModelInterpreter modelInterpreter = this.<IModelInterpreter>getService("sphModelInterpreter", IModelInterpreter.class.getName());
			
			if(modelInterpreter == null){
				response.getWriter().println("modelInterpreter is null");
			}
			else{
				response.getWriter().println("modelInterpreter: " + modelInterpreter.toString());
			}
		} catch (Exception e) {
			response.getWriter().println("error: " + e.getMessage());
		} 
	}
	
	private <T> T getService(String discoveryId, String type){
		BundleContext bc = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
		
		T service = null;
		
		//String filter = String.format("(discoverable-id=%s)", discoveryId);
		ServiceReference sr  =  bc.getServiceReference(type);
		service = (T) bc.getService(sr);
		
		return service;
	}
}
