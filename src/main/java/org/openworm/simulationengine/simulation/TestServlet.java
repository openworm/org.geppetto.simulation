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
	
	BundleContext _bc = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			
			IModelInterpreter modelInterpreter = this.<IModelInterpreter>getService("sphModelInterpreter", IModelInterpreter.class);
			ISimulator simulator = this.<ISimulator>getService("sphSimulator", ISimulator.class);
			
			response.getWriter().println("Looks like it got to the end with no errors!");
		} catch (Exception e) {
			response.getWriter().println("error: " + e.getMessage());
		} 
	}
	
	private <T> T getService(String discoveryId, Type type){
		T service = null;
		
		String filter = String.format("(discoverable-id=%s)", discoveryId);
		ServiceReference sr  =  _bc.getServiceReference(type.getClass().getName());
		service = (T) _bc.getService(sr);
		
		return service;
	}
}
