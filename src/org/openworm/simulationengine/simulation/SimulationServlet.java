package org.openworm.simulationengine.simulation;

import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WebSocketServlet;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;

@Configurable(autowire=Autowire.BY_TYPE)
public class SimulationServlet extends WebSocketServlet {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//FIXME MC: This is just a skeleton, the servlet an the whole infrastracture needs to be put in place
	@Override
	protected StreamInbound createWebSocketInbound(String subProtocol,
			HttpServletRequest request) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
