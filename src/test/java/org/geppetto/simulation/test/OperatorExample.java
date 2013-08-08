package org.geppetto.simulation.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import py4j.GatewayServer;

public class OperatorExample
{

	// To prevent integer overflow
	private final static int MAX = 1000;

	public List<Integer> randomBinaryOperator(Operator op)
	{
		Random random = new Random();
		List<Integer> numbers = new ArrayList<Integer>();
		numbers.add(random.nextInt(MAX));
		numbers.add(random.nextInt(MAX));
		numbers.add(op.doOperation(numbers.get(0), numbers.get(1)));
		return numbers;
	}

	public List<Integer> randomTernaryOperator(Operator op)
	{
		Random random = new Random();
		List<Integer> numbers = new ArrayList<Integer>();
		numbers.add(random.nextInt(MAX));
		numbers.add(random.nextInt(MAX));
		numbers.add(random.nextInt(MAX));
		numbers.add(op.doOperation(numbers.get(0), numbers.get(1), numbers.get(2)));
		return numbers;
	}

	public static void main(String[] args)
	{
		GatewayServer server = new GatewayServer(new OperatorExample());
		server.start();
		String s = null;
		try
		{
			Process p = Runtime.getRuntime().exec("python ./src/test/resources/snippet.py");

			BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

			BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

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