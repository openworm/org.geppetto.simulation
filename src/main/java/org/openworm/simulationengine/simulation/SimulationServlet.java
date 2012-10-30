package org.openworm.simulationengine.simulation;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.websocket.MessageInbound;
import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WebSocketServlet;
import org.apache.catalina.websocket.WsOutbound;
import org.openworm.simulationengine.core.visualisation.model.Scene;
import org.openworm.simulationengine.simulation.model.Simulation;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

@Configurable
@SuppressWarnings("serial")
public class SimulationServlet extends WebSocketServlet {
		
	// TODO: inject this from a config file local to this bundle
	private URL _configUrl;
	private static final long UPDATE_CYCLE = 100;
	private final Timer _simTimer = new Timer(SimulationServlet.class.getSimpleName() + " Timer");
	private final AtomicInteger _connectionIds = new AtomicInteger(0);
	private final SessionContext _sessionContext = new SessionContext();
	private final ConcurrentHashMap<Integer, SimDataInbound> _connections = new ConcurrentHashMap<Integer, SimDataInbound>();

	@Override
	protected StreamInbound createWebSocketInbound(String subProtocol, HttpServletRequest request) {
		return new SimDataInbound(_connectionIds.incrementAndGet());
	}

	@Override
	public void init() throws ServletException {
		super.init();
	    SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
		_simTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					update();
				} catch (RuntimeException e) {
					// log.error("Caught to prevent timer from shutting down", e);
				}
			}
		}, UPDATE_CYCLE, UPDATE_CYCLE);
		
		// TODO: this is here temporarily - should be set from the client or at least inject from config
		try {
			_configUrl = new File("./src/main/resources/config/sph-sim-config.xml").toURI().toURL();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void update() {
		StringBuilder sb = new StringBuilder();
		
		// TODO: build json data to push to the clients (data is stored in _sessionContext)
		/*Scene scene = modelInterpreter.getSceneFromModel(model); //model is ÊList<IModel>
		ObjectMapper mapper = new ObjectMapper();

		mapper.writeValue(sb, scene);*/
		
		sendUpdate(sb.toString());
	}
	
	private void sendUpdate(String message) {
		for (SimDataInbound connection : getConnections()) {
			try {
				CharBuffer buffer = CharBuffer.wrap(message);
				connection.getWsOutbound().writeTextMessage(buffer);
			} catch (IOException ignore) {
				// Ignore
			}
		}
	}

	private Collection<SimDataInbound> getConnections() {
		return Collections.unmodifiableCollection(_connections.values());
	}
	
	private final class SimDataInbound extends MessageInbound {

		private final int id;

		private SimDataInbound(int id) {
			super();
			this.id = id;
		}

		@Override
		protected void onOpen(WsOutbound outbound) {
			_connections.put(Integer.valueOf(id), this);
		}

		@Override
		protected void onClose(int status) {
			_connections.remove(Integer.valueOf(id));
		}

		@Override
		protected void onBinaryMessage(ByteBuffer message) throws IOException {
			throw new UnsupportedOperationException("Binary message not supported.");
		}

		@Override
		protected void onTextMessage(CharBuffer message) throws IOException {
			String msg = message.toString();
			if (msg.equals("start")) {
				// grab config simulation object
				Simulation sim = SimulationConfigReader.readConfig(_configUrl);
				// start simulation thread
				new SimulationThread(_sessionContext, sim).start();
			} else if (msg.equals("stop")) {
				_sessionContext._runSimulation = false;
				_sessionContext._runningCycle = false;
			} else {
				// NOTE: doesn't necessarily ned to do smt here - could be just start/stop
			}
		}
	}

}
