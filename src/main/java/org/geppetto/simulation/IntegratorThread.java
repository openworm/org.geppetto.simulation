package org.geppetto.simulation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Map;

import py4j.GatewayServer;

public class IntegratorThread extends Thread
{
	private static Process _p;
	private static GatewayServer _server;
	private static IIntegrator _integrator;
	private static int _steps = 0;

	private static Map<String, Double> _states = new LinkedHashMap<String, Double>();
	private boolean _run = false;
	private String _path;

	public int integratorReady(IIntegrator integrator) throws InterruptedException
	{
		_integrator = integrator;

		for(String state : _states.keySet())
		{
			integrator.addState(state, _states.get(state).doubleValue());
		}

		return 1;
	}

	public IntegratorThread()
	{
		super();
		_path = getClass().getResource("/integration/sample.py").getPath();
		

	}

	@Override
	public void run()
	{
		String s = null;

		_states.put("v1", 0.1d);
		_states.put("v2", 0.2d);
		_states.put("v3", 0.3d);
		_states.put("v4", 0.4d);
		_server = new GatewayServer(this);
		_server.start();

		try
		{
			
			_p = Runtime.getRuntime().exec("python " + _path);

			BufferedReader stdInput = new BufferedReader(new InputStreamReader(_p.getInputStream()));

			BufferedReader stdError = new BufferedReader(new InputStreamReader(_p.getErrorStream()));

			// read the output from the command
			System.out.println("Here is the standard output of the command:\n");
			while((s = stdInput.readLine()) != null)
			{
				System.out.println(s);
			}

			// read any errors from the attempted command
			System.out.println("Here is the standard error of the command (if any):\n");
			while((s = stdError.readLine()) != null)
			{
				System.out.println(s);
			}
		}
		catch(IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while(true)
		{
			if(_run)
			{
				_run = false;
				_integrator.runIntegration();
				_steps++;
			}
		}
	}

	public void resultsReady()
	{
		System.out.println("Results ready!");
		for(String state : _states.keySet())
		{
			_states.put(state, _integrator.getState(state));
			System.out.println("State" + state + "[" + _steps + "]=" + _states.get(state));
		}
	}

	public void dispose()
	{
		_integrator.stopScript();
	}

	public void runIntegration()
	{
		_run = true;
	}

}