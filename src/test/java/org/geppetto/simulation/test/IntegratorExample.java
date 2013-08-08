package org.geppetto.simulation.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Map;

import py4j.GatewayServer;

public class IntegratorExample
{
	private static Process _p;
	private static GatewayServer _server; 
	private static Integrator _integrator;
	private static int _steps = 0;
	private static int N=2;
	
	private static Map<String, Double> _states = new LinkedHashMap<String, Double>();

	public int integratorReady(Integrator integrator) throws InterruptedException
	{
		_integrator = integrator;

		for(String state : _states.keySet())
		{
			integrator.addState(state, _states.get(state).doubleValue());
		}
		
		while(_steps<N)
		{
			_steps++;
			_integrator.runIntegration();
			Thread.sleep(1000);
		}

		_integrator.stopScript();
		return 1;
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

	public static void main(String[] args)
	{
		String s = null;

		_states.put("v1", 0.1d);
		_states.put("v2", 0.2d);
		_states.put("v3", 0.3d);
		_states.put("v4", 0.4d);
		_server = new GatewayServer(new IntegratorExample());
		_server.start();

		
		try
		{
			_p = Runtime.getRuntime().exec("python ./src/test/resources/sample.py");

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

	}

}