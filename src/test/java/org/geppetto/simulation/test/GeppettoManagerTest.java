/*******************************************************************************
 * The MIT License (MIT)
 * 
 * Copyright (c) 2011 - 2015 OpenWorm.
 * http://openworm.org
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     	OpenWorm - http://openworm.org/people.html
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights 
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell 
 * copies of the Software, and to permit persons to whom the Software is 
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR 
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE 
 * USE OR OTHER DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package org.geppetto.simulation.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geppetto.core.common.GeppettoAccessException;
import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.data.DataManagerHelper;
import org.geppetto.core.data.DefaultGeppettoDataManager;
import org.geppetto.core.data.model.ExperimentStatus;
import org.geppetto.core.data.model.IAspectConfiguration;
import org.geppetto.core.data.model.IExperiment;
import org.geppetto.core.data.model.IGeppettoProject;
import org.geppetto.core.data.model.IUserGroup;
import org.geppetto.core.data.model.ResultsFormat;
import org.geppetto.core.data.model.UserPrivileges;
import org.geppetto.core.manager.Scope;
import org.geppetto.core.services.registry.ApplicationListenerBean;
import org.geppetto.core.services.registry.ServicesRegistry;
import org.geppetto.model.ExperimentState;
import org.geppetto.model.GeppettoLibrary;
import org.geppetto.model.GeppettoModel;
import org.geppetto.model.ModelFormat;
import org.geppetto.model.VariableValue;
import org.geppetto.model.types.Type;
import org.geppetto.model.values.Quantity;
import org.geppetto.model.values.TimeSeries;
import org.geppetto.simulation.manager.ExperimentRunManager;
import org.geppetto.simulation.manager.GeppettoManager;
import org.geppetto.simulation.manager.RuntimeExperiment;
import org.geppetto.simulation.manager.RuntimeProject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * 
 * This is an integration test which checks the workkflows of the GeppettoManager. Provides coverage also for RuntimeProject and RuntimeExperiment.
 * 
 * @author matteocantarelli
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GeppettoManagerTest
{

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	private static GeppettoManager manager = new GeppettoManager(Scope.CONNECTION);
	private static IGeppettoProject geppettoProject;
	private static RuntimeProject runtimeProject;
	private static IExperiment addedExperiment;

	/**
	 * @throws java.lang.Exception
	 */
	@SuppressWarnings("deprecation")
	@BeforeClass
	public static void setUp() throws Exception
	{
		GenericWebApplicationContext context = new GenericWebApplicationContext();
		BeanDefinition modelInterpreterBeanDefinition = new RootBeanDefinition(TestModelInterpreterService.class);
		BeanDefinition simulatorBeanDefinition = new RootBeanDefinition(TestSimulatorService.class);
		context.registerBeanDefinition("testModelInterpreter", modelInterpreterBeanDefinition);
		context.registerBeanDefinition("scopedTarget.testModelInterpreter", modelInterpreterBeanDefinition);
		context.registerBeanDefinition("testSimulator", simulatorBeanDefinition);
		context.registerBeanDefinition("scopedTarget.testSimulator", simulatorBeanDefinition);
		ContextRefreshedEvent event = new ContextRefreshedEvent(context);
		ApplicationListenerBean listener = new ApplicationListenerBean();
		context.refresh();
		listener.onApplicationEvent(event);
		ApplicationContext retrievedContext = ApplicationListenerBean.getApplicationContext("testModelInterpreter");
		Assert.assertNotNull(retrievedContext.getBean("scopedTarget.testModelInterpreter"));
		Assert.assertTrue(retrievedContext.getBean("scopedTarget.testModelInterpreter") instanceof TestModelInterpreterService);
		Assert.assertNotNull(retrievedContext.getBean("scopedTarget.testSimulator"));
		Assert.assertTrue(retrievedContext.getBean("scopedTarget.testSimulator") instanceof TestSimulatorService);
		DataManagerHelper.setDataManager(new DefaultGeppettoDataManager());
		Assert.assertNotNull(ExperimentRunManager.getInstance());
	}

	/**
	 * Test method for {@link org.geppetto.simulation.manager.GeppettoManager#setUser(org.geppetto.core.data.model.IUser)}.
	 * 
	 * @throws GeppettoExecutionException
	 */
	@Test
	public void test01SetUser() throws GeppettoExecutionException
	{
		long value = 1000l * 1000 * 1000;
		List<UserPrivileges> privileges = new ArrayList<UserPrivileges>();
		privileges.add(UserPrivileges.READ_PROJECT);
		privileges.add(UserPrivileges.WRITE_PROJECT);
		privileges.add(UserPrivileges.DOWNLOAD);
		privileges.add(UserPrivileges.DROPBOX_INTEGRATION);
		privileges.add(UserPrivileges.RUN_EXPERIMENT);
		IUserGroup userGroup = DataManagerHelper.getDataManager().newUserGroup("unaccountableAristocrats", privileges, value, value * 2);
		manager.setUser(DataManagerHelper.getDataManager().newUser("nonna", "passauord", true, userGroup));
	}

	/**
	 * Test method for {@link org.geppetto.simulation.manager.GeppettoManager#getUser()}.
	 */
	@Test
	public void test02GetUser()
	{
		Assert.assertEquals("nonna", manager.getUser().getName());
		Assert.assertEquals("passauord", manager.getUser().getPassword());
	}

	/**
	 * Test method for {@link org.geppetto.simulation.manager.GeppettoManager#loadProject(java.lang.String, org.geppetto.core.data.model.IGeppettoProject)}.
	 * 
	 * @throws IOException
	 * @throws GeppettoExecutionException
	 * @throws GeppettoInitializationException
	 * @throws GeppettoAccessException
	 */
	@Test
	public void test03LoadProject() throws IOException, GeppettoInitializationException, GeppettoExecutionException, GeppettoAccessException
	{
		InputStreamReader inputStreamReader = new InputStreamReader(GeppettoManagerTest.class.getResourceAsStream("/test/geppettoManagerTest.json"));
		geppettoProject = DataManagerHelper.getDataManager().getProjectFromJson(TestUtilities.getGson(), inputStreamReader);
		manager.loadProject("1", geppettoProject);

	}

	/**
	 * Test method for {@link org.geppetto.simulation.manager.GeppettoManager#getRuntimeProject(org.geppetto.core.data.model.IGeppettoProject)}.
	 * 
	 * @throws GeppettoExecutionException
	 */
	@Test
	public void test04RuntimeProject() throws GeppettoExecutionException
	{
		runtimeProject = manager.getRuntimeProject(geppettoProject);
		Assert.assertNotNull(runtimeProject);

		// Testing ImportType visitor is working properly
		GeppettoModel geppettoModel = runtimeProject.getGeppettoModel();
		Assert.assertEquals(1, geppettoModel.getVariables().get(0).getTypes().size());
		Type type = geppettoModel.getVariables().get(0).getTypes().get(0);
		Assert.assertEquals("testType", type.getId());
		Assert.assertEquals("testType", type.getName());

		// Testing libraries are there
		Assert.assertEquals(2, geppettoModel.getLibraries().size());
		GeppettoLibrary common = geppettoModel.getLibraries().get(1);
		Assert.assertEquals("common", common.getId());
		Assert.assertEquals("Geppetto Common Library", common.getName());
		GeppettoLibrary testLibrary = geppettoModel.getLibraries().get(0);
		Assert.assertEquals("testLibrary", testLibrary.getId());
		Assert.assertEquals("testLibrary", testLibrary.getName());

	}

	/**
	 * Test method for {@link org.geppetto.simulation.manager.GeppettoManager#loadExperiment(java.lang.String, org.geppetto.core.data.model.IExperiment)}.
	 * 
	 * @throws GeppettoExecutionException
	 * @throws GeppettoAccessException
	 */
	@Test
	public void test05LoadExperiment() throws GeppettoExecutionException, GeppettoAccessException
	{
		ExperimentState experimentState = manager.loadExperiment("1", geppettoProject.getExperiments().get(0));
		Assert.assertNotNull(experimentState);
		List<VariableValue> parameters = experimentState.getSetParameters();
		List<VariableValue> recordedVariables = experimentState.getRecordedVariables();
		Assert.assertEquals(2, parameters.size());
		Assert.assertEquals(3, recordedVariables.size());
		VariableValue b = recordedVariables.get(1);
		Assert.assertEquals("testVar(testType).b(StateVariable)", b.getPointer().getInstancePath());
		VariableValue p1 = parameters.get(0);
		Assert.assertEquals("testVar(testType).p1(Parameter)", p1.getPointer().getInstancePath());
		Quantity q = (Quantity) p1.getValue();
		Assert.assertEquals(0.2, q.getValue(), 0);

	}

	/**
	 * Test method for {@link org.geppetto.simulation.manager.GeppettoManager#checkExperimentsStatus(java.lang.String, org.geppetto.core.data.model.IGeppettoProject)}.
	 */
	@Test
	public void test06CheckExperimentsStatus()
	{
		List<? extends IExperiment> status = manager.checkExperimentsStatus("1", geppettoProject);
		Assert.assertEquals(1, status.size());
		Assert.assertEquals(ExperimentStatus.COMPLETED, status.get(0).getStatus());
	}

	/**
	 * Test method for
	 * {@link org.geppetto.simulation.manager.GeppettoManager#setModelParameters(java.util.Map, org.geppetto.core.data.model.IExperiment, org.geppetto.core.data.model.IGeppettoProject)}.
	 * 
	 * @throws GeppettoExecutionException
	 * @throws GeppettoAccessException
	 */
	@Test
	public void test07SetModelParametersNegativeNoDesign() throws GeppettoExecutionException, GeppettoAccessException
	{
		Map<String, String> parametersMap = new HashMap<String, String>();
		parametersMap.put("testVar(testType).p2(Parameter)", "0.234");
		exception.expect(GeppettoExecutionException.class);
		manager.setModelParameters(parametersMap, runtimeProject.getActiveExperiment(), geppettoProject);

	}

	/**
	 * Test method for
	 * {@link org.geppetto.simulation.manager.GeppettoManager#setWatchedVariables(java.util.List, org.geppetto.core.data.model.IExperiment, org.geppetto.core.data.model.IGeppettoProject)}.
	 * 
	 * @throws GeppettoExecutionException
	 * @throws GeppettoAccessException
	 */
	@Test
	public void test08SetWatchedVariablesNegativeNoDesign() throws GeppettoExecutionException, GeppettoAccessException
	{
		List<String> watchedVariables = new ArrayList<String>();
		// the following line stops recording a
		watchedVariables.add("testVar(testType).a(StateVariable)");
		exception.expect(GeppettoExecutionException.class);
		manager.setWatchedVariables(watchedVariables, runtimeProject.getActiveExperiment(), geppettoProject, true);
	}

	/**
	 * Test method for {@link org.geppetto.simulation.manager.GeppettoManager#newExperiment(java.lang.String, org.geppetto.core.data.model.IGeppettoProject)}.
	 * 
	 * @throws GeppettoExecutionException
	 * @throws GeppettoAccessException
	 */
	@Test
	public void test09NewExperiment() throws GeppettoExecutionException, GeppettoAccessException
	{
		Assert.assertEquals(1, geppettoProject.getExperiments().size());
		addedExperiment = manager.newExperiment("2", geppettoProject);
		Assert.assertEquals(2, geppettoProject.getExperiments().size());
		Assert.assertEquals(1, addedExperiment.getAspectConfigurations().size());
		IAspectConfiguration ac = addedExperiment.getAspectConfigurations().get(0);
		ac.getSimulatorConfiguration().setSimulatorId("testSimulator");

	}

	/**
	 * Test method for {@link org.geppetto.simulation.manager.GeppettoManager#loadExperiment(java.lang.String, org.geppetto.core.data.model.IExperiment)}.
	 * 
	 * @throws GeppettoExecutionException
	 * @throws GeppettoAccessException
	 */
	@Test
	public void test10LoadNewExperiment() throws GeppettoExecutionException, GeppettoAccessException
	{
		ExperimentState experimentState = manager.loadExperiment("1", geppettoProject.getExperiments().get(1));
		Assert.assertNotNull(experimentState);
		Assert.assertEquals(1, experimentState.getExperimentId());
		List<VariableValue> parameters = experimentState.getSetParameters();
		List<VariableValue> recordedVariables = experimentState.getRecordedVariables();
		Assert.assertEquals(0, parameters.size());
		Assert.assertEquals(1, recordedVariables.size()); // just time
	}

	/**
	 * Test method for
	 * {@link org.geppetto.simulation.manager.GeppettoManager#setModelParameters(java.util.Map, org.geppetto.core.data.model.IExperiment, org.geppetto.core.data.model.IGeppettoProject)}.
	 * 
	 * @throws GeppettoExecutionException
	 * @throws GeppettoAccessException
	 */
	@Test
	public void test11SetModelParameters() throws GeppettoExecutionException, GeppettoAccessException
	{
		Map<String, String> parametersMap = new HashMap<String, String>();
		parametersMap.put("testVar(testType).p2(Parameter)", "0.234");
		ExperimentState experimentState = manager.setModelParameters(parametersMap, runtimeProject.getActiveExperiment(), geppettoProject);
		List<VariableValue> parameters = experimentState.getSetParameters();
		Assert.assertEquals(1, parameters.size());
		VariableValue p2 = parameters.get(0);
		Assert.assertEquals("testVar(testType).p2(Parameter)", p2.getPointer().getInstancePath());
		Quantity q = (Quantity) p2.getValue();
		Assert.assertEquals(0.234, q.getValue(), 0);
	}

	/**
	 * Test method for
	 * {@link org.geppetto.simulation.manager.GeppettoManager#setModelParameters(java.util.Map, org.geppetto.core.data.model.IExperiment, org.geppetto.core.data.model.IGeppettoProject)}.
	 * 
	 * @throws GeppettoExecutionException
	 * @throws GeppettoAccessException
	 */
	@Test
	public void test12SetModelParametersNegativeWrongParameter() throws GeppettoExecutionException, GeppettoAccessException
	{
		Map<String, String> parametersMap = new HashMap<String, String>();
		parametersMap.put("testVar(testType).p3(Parameter)", "0.234");
		// p3 does not exist
		exception.expect(GeppettoExecutionException.class);
		manager.setModelParameters(parametersMap, runtimeProject.getActiveExperiment(), geppettoProject);
	}

	/**
	 * Test method for
	 * {@link org.geppetto.simulation.manager.GeppettoManager#setWatchedVariables(java.util.List, org.geppetto.core.data.model.IExperiment, org.geppetto.core.data.model.IGeppettoProject)}.
	 * 
	 * @throws GeppettoExecutionException
	 * @throws GeppettoAccessException
	 */
	@Test
	public void test13SetWatchedVariables() throws GeppettoExecutionException, GeppettoAccessException
	{
		List<String> watchedVariables = new ArrayList<String>();
		watchedVariables.add("testVar(testType).a(StateVariable)");
		watchedVariables.add("testVar(testType).c(StateVariable)");
		ExperimentState experimentState = manager.setWatchedVariables(watchedVariables, runtimeProject.getActiveExperiment(), geppettoProject, true);
		List<VariableValue> recordedVariables = experimentState.getRecordedVariables();
		Assert.assertEquals(3, recordedVariables.size()); // a+c+time
		Assert.assertEquals(2, addedExperiment.getAspectConfigurations().get(0).getWatchedVariables().size()); // no time inside aspectConfiguration
		VariableValue c = recordedVariables.get(2);
		Assert.assertEquals("testVar(testType).c(StateVariable)", c.getPointer().getInstancePath());

		// the following line stops recording a
		List<String> watchedVariables2 = new ArrayList<String>();
		watchedVariables2.add("testVar(testType).a(StateVariable)");
		ExperimentState experimentState2 = manager.setWatchedVariables(watchedVariables2, runtimeProject.getActiveExperiment(), geppettoProject, false);
		List<VariableValue> recordedVariables2 = experimentState2.getRecordedVariables();
		Assert.assertEquals(2, recordedVariables2.size());
		Assert.assertEquals(1, addedExperiment.getAspectConfigurations().get(0).getWatchedVariables().size());
		c = recordedVariables2.get(1);
		Assert.assertEquals("testVar(testType).c(StateVariable)", c.getPointer().getInstancePath());

		// Let's add a again and b too
		List<String> watchedVariables3 = new ArrayList<String>();
		watchedVariables3.add("testVar(testType).a(StateVariable)");
		watchedVariables3.add("testVar(testType).b(StateVariable)");
		ExperimentState experimentState3 = manager.setWatchedVariables(watchedVariables3, runtimeProject.getActiveExperiment(), geppettoProject, true);
		List<VariableValue> recordedVariables3 = experimentState3.getRecordedVariables();
		Assert.assertEquals(4, recordedVariables3.size());
		Assert.assertEquals(3, addedExperiment.getAspectConfigurations().get(0).getWatchedVariables().size());
		c = recordedVariables3.get(1);
		VariableValue a = recordedVariables3.get(2);
		VariableValue b = recordedVariables3.get(3);
		Assert.assertEquals("testVar(testType).c(StateVariable)", c.getPointer().getInstancePath());
		Assert.assertEquals("testVar(testType).a(StateVariable)", a.getPointer().getInstancePath());
		Assert.assertEquals("testVar(testType).b(StateVariable)", b.getPointer().getInstancePath());

	}

	/**
	 * Test method for
	 * {@link org.geppetto.simulation.manager.GeppettoManager#setWatchedVariables(java.util.List, org.geppetto.core.data.model.IExperiment, org.geppetto.core.data.model.IGeppettoProject)}.
	 * 
	 * @throws GeppettoExecutionException
	 * @throws GeppettoAccessException
	 */
	@Test
	public void test14SetWatchedVariablesNegativeWrongVariable() throws GeppettoExecutionException, GeppettoAccessException
	{
		List<String> watchedVariables = new ArrayList<String>();
		watchedVariables.add("testVar(testType).d(Parameter)");
		// d does not exist
		exception.expect(GeppettoExecutionException.class);
		manager.setWatchedVariables(watchedVariables, runtimeProject.getActiveExperiment(), geppettoProject, true);
	}

	/**
	 * Test method for {@link org.geppetto.simulation.manager.GeppettoManager#checkExperimentsStatus(java.lang.String, org.geppetto.core.data.model.IGeppettoProject)}.
	 */
	@Test
	public void test15CheckExperimentsStatusAgain()
	{
		List<? extends IExperiment> status = manager.checkExperimentsStatus("1", geppettoProject);
		Assert.assertEquals(2, status.size());
		Assert.assertEquals(ExperimentStatus.COMPLETED, status.get(0).getStatus());
		Assert.assertEquals(ExperimentStatus.DESIGN, status.get(1).getStatus());
	}

	/**
	 * Test method for {@link org.geppetto.simulation.manager.GeppettoManager#runExperiment(java.lang.String, org.geppetto.core.data.model.IExperiment)}.
	 * 
	 * @throws GeppettoExecutionException
	 * @throws InterruptedException
	 * @throws GeppettoAccessException
	 */
	@Test
	public void test16RunExperiment() throws GeppettoExecutionException, InterruptedException, GeppettoAccessException
	{
		Assert.assertEquals(1, addedExperiment.getAspectConfigurations().size());
		IAspectConfiguration ac = addedExperiment.getAspectConfigurations().get(0);
		Assert.assertEquals("testVar(testType)", ac.getInstance());
		Assert.assertNotNull(ac.getSimulatorConfiguration());
		manager.runExperiment("1", addedExperiment);
		Assert.assertEquals(3, addedExperiment.getAspectConfigurations().get(0).getWatchedVariables().size());
	}

	/**
	 * Test method for {@link org.geppetto.simulation.manager.GeppettoManager#checkExperimentsStatus(java.lang.String, org.geppetto.core.data.model.IGeppettoProject)}.
	 */
	@Test
	public void test17CheckExperimentsStatusAgain2()
	{
		List<? extends IExperiment> status = manager.checkExperimentsStatus("1", geppettoProject);
		Assert.assertEquals(2, status.size());
		Assert.assertEquals(ExperimentStatus.COMPLETED, status.get(0).getStatus());
		Assert.assertEquals(ExperimentStatus.QUEUED, status.get(1).getStatus());
	}

	/**
	 * Test method for {@link org.geppetto.simulation.manager.GeppettoManager#cancelExperimentRun(java.lang.String, org.geppetto.core.data.model.IExperiment)}.
	 * 
	 * @throws GeppettoExecutionException
	 */
	@Test
	public void test18CancelExperimentRun() throws GeppettoExecutionException
	{
		manager.cancelExperimentRun("1", addedExperiment);
	}

	/**
	 * Test method for {@link org.geppetto.simulation.manager.GeppettoManager#checkExperimentsStatus(java.lang.String, org.geppetto.core.data.model.IGeppettoProject)}.
	 */
	@Test
	public void test19CheckExperimentsStatusAgain3()
	{
		List<? extends IExperiment> status = manager.checkExperimentsStatus("1", geppettoProject);
		Assert.assertEquals(2, status.size());
		Assert.assertEquals(ExperimentStatus.COMPLETED, status.get(0).getStatus());
		Assert.assertEquals(ExperimentStatus.DESIGN, status.get(1).getStatus());
	}

	/**
	 * Test method for {@link org.geppetto.simulation.manager.GeppettoManager#runExperiment(java.lang.String, org.geppetto.core.data.model.IExperiment)}.
	 * 
	 * @throws GeppettoExecutionException
	 * @throws InterruptedException
	 * @throws GeppettoAccessException
	 */
	@Test
	public void test20RunExperimentAgain() throws GeppettoExecutionException, InterruptedException, GeppettoAccessException
	{
		Assert.assertNotNull(ExperimentRunManager.getInstance());
		Assert.assertEquals(1, addedExperiment.getAspectConfigurations().size());
		IAspectConfiguration ac = addedExperiment.getAspectConfigurations().get(0);
		Assert.assertEquals("testVar(testType)", ac.getInstance());
		Assert.assertNotNull(ac.getSimulatorConfiguration());
		manager.runExperiment("1", addedExperiment);
		Assert.assertEquals(3, addedExperiment.getAspectConfigurations().get(0).getWatchedVariables().size());
		Thread.sleep(3000);
	}

	/**
	 * Test method for {@link org.geppetto.simulation.manager.GeppettoManager#checkExperimentsStatus(java.lang.String, org.geppetto.core.data.model.IGeppettoProject)}.
	 */
	@Test
	public void test21CheckExperimentsStatusAgain4()
	{
		List<? extends IExperiment> status = manager.checkExperimentsStatus("1", geppettoProject);
		Assert.assertEquals(2, status.size());
		Assert.assertEquals(ExperimentStatus.COMPLETED, status.get(0).getStatus());
		Assert.assertEquals(ExperimentStatus.COMPLETED, status.get(1).getStatus());
	}

	/**
	 * Test method for {@link org.geppetto.simulation.manager.GeppettoManager#playExperiment(java.lang.String, org.geppetto.core.data.model.IExperiment)}.
	 * 
	 * @throws GeppettoExecutionException
	 * @throws IOException
	 * @throws NumberFormatException
	 * @throws GeppettoAccessException
	 */
	@Test
	public void test22PlayExperiment() throws GeppettoExecutionException, NumberFormatException, IOException, GeppettoAccessException
	{
		ExperimentState experimentState = manager.getExperimentState("1", addedExperiment, null);
		List<VariableValue> recorded = experimentState.getRecordedVariables();
		Assert.assertEquals(4, recorded.size());
		VariableValue time = recorded.get(0);
		VariableValue c = recorded.get(1);
		VariableValue a = recorded.get(2);
		VariableValue b = recorded.get(3);
		Assert.assertEquals("time(StateVariable)", time.getPointer().getInstancePath());
		Assert.assertEquals("testVar(testType).c(StateVariable)", c.getPointer().getInstancePath());
		Assert.assertEquals("testVar(testType).a(StateVariable)", a.getPointer().getInstancePath());
		Assert.assertEquals("testVar(testType).b(StateVariable)", b.getPointer().getInstancePath());
		Assert.assertNotNull(time.getValue());
		Assert.assertNotNull(c.getValue());
		Assert.assertNotNull(a.getValue());
		Assert.assertNotNull(b.getValue());

		VariableValue p2 = experimentState.getSetParameters().get(0);
		Assert.assertEquals("testVar(testType).p2(Parameter)", p2.getPointer().getInstancePath());
		Assert.assertEquals(0.234d, ((Quantity) p2.getValue()).getValue(), 0d);

		checkValues(b, 3);
		checkValues(time, 0);
		checkValues(a, 2);
		checkValues(c, 1);

		List<String> variables = new ArrayList<String>();
		variables.add("testVar.a");
		experimentState = manager.getExperimentState("1", addedExperiment, variables);
		recorded = experimentState.getRecordedVariables();
		time = recorded.get(0);
		a = recorded.get(1);
		Assert.assertEquals(2, recorded.size());
		Assert.assertEquals("time(StateVariable)", time.getPointer().getInstancePath());
		Assert.assertEquals("testVar(testType).a(StateVariable)", a.getPointer().getInstancePath());
		p2 = experimentState.getSetParameters().get(0);
		Assert.assertEquals("testVar(testType).p2(Parameter)", p2.getPointer().getInstancePath());
		Assert.assertEquals(0.234d, ((Quantity) p2.getValue()).getValue(), 0d);

	}

	/**
	 * @param variable
	 * @param columnIndexInTheDATFile
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	private void checkValues(VariableValue variable, int columnIndexInTheDATFile) throws NumberFormatException, IOException
	{
		// read DAT into a buffered reader
		BufferedReader input = new BufferedReader(new FileReader("./src/test/resources/test/testResults.dat"));

		ArrayList<Double> storedValues = new ArrayList<Double>();

		// read rest of DAT file and extract values
		while(input.read() != -1)
		{
			String line = input.readLine();
			String[] columns = line.split("\\s+");
			storedValues.add(Double.valueOf(columns[columnIndexInTheDATFile]));

		}

		Assert.assertArrayEquals(storedValues.toArray(), ((TimeSeries) variable.getValue()).getValue().toArray());

		input.close();
	}

	/**
	 * Test method for {@link org.geppetto.simulation.manager.GeppettoManager#linkDropBoxAccount(java.lang.String)}.
	 * 
	 * @throws Exception
	 */
	public void testLinkDropBoxAccount() throws Exception
	{
		// Dropbox tests are commented out as they require the API token to work which should not go in the code
		// Until someone finds a strategy...
		manager.linkDropBoxAccount("");
	}

	/**
	 * Test method for
	 * {@link org.geppetto.simulation.manager.GeppettoManager#uploadModelToDropBox(java.lang.String, org.geppetto.core.data.model.IExperiment, org.geppetto.core.data.model.IGeppettoProject, org.geppetto.model.ModelFormat)}
	 * .
	 * 
	 * @throws Exception
	 */
	public void testUploadModelToDropBox() throws Exception
	{
		manager.uploadModelToDropBox("testVar(testType)", addedExperiment, geppettoProject, ServicesRegistry.getModelFormat("TEST_FORMAT"));
	}

	/**
	 * Test method for
	 * {@link org.geppetto.simulation.manager.GeppettoManager#uploadResultsToDropBox(java.lang.String, org.geppetto.core.data.model.IExperiment, org.geppetto.core.data.model.IGeppettoProject, org.geppetto.core.data.model.ResultsFormat)}
	 * .
	 * 
	 * @throws GeppettoExecutionException
	 * @throws GeppettoAccessException
	 */
	public void testUploadResultsToDropBox() throws GeppettoExecutionException, GeppettoAccessException
	{
		manager.uploadResultsToDropBox("testVar(testType)", addedExperiment, geppettoProject, ResultsFormat.GEPPETTO_RECORDING);
		manager.uploadResultsToDropBox("testVar(testType)", addedExperiment, geppettoProject, ResultsFormat.RAW);
	}

	/**
	 * Test method for {@link org.geppetto.simulation.manager.GeppettoManager#unlinkDropBoxAccount(java.lang.String)}.
	 * 
	 * @throws Exception
	 */
	public void testUnlinkDropBoxAccount() throws Exception
	{
		manager.unlinkDropBoxAccount("");
	}

	/**
	 * Test method for
	 * {@link org.geppetto.simulation.manager.GeppettoManager#downloadModel(java.lang.String, org.geppetto.model.ModelFormat, org.geppetto.core.data.model.IExperiment, org.geppetto.core.data.model.IGeppettoProject)}
	 * .
	 * 
	 * @throws GeppettoExecutionException
	 * @throws GeppettoAccessException
	 */
	@Test
	public void test23DownloadModel() throws GeppettoExecutionException, GeppettoAccessException
	{
		File model = manager.downloadModel("testVar(testType)", ServicesRegistry.getModelFormat("TEST_FORMAT"), addedExperiment, geppettoProject);
		Assert.assertNotNull(model);
		Assert.assertEquals("ModelFile", model.getName());
	}

	/**
	 * Test method for
	 * {@link org.geppetto.simulation.manager.GeppettoManager#downloadResults(java.lang.String, org.geppetto.core.data.model.ResultsFormat, org.geppetto.core.data.model.IExperiment, org.geppetto.core.data.model.IGeppettoProject)}
	 * .
	 * 
	 * @throws GeppettoExecutionException
	 * @throws IOException
	 * @throws GeppettoAccessException
	 */
	@Test
	public void test24DownloadResults() throws GeppettoExecutionException, IOException, GeppettoAccessException
	{
		URL geppettoRecording = manager.downloadResults("testVar(testType)", ResultsFormat.GEPPETTO_RECORDING, addedExperiment, geppettoProject);
		Assert.assertTrue(geppettoRecording.getPath().endsWith("./src/test/resources/test/testResults.h5"));
		geppettoRecording.openConnection().connect();
		URL rawRecording = manager.downloadResults("testVar(testType)", ResultsFormat.RAW, addedExperiment, geppettoProject);
		rawRecording.openConnection().connect();
		// unix and windows output paths differ, checks both
		boolean rawRecPath = rawRecording.getPath().endsWith("testVar(testType)/rawRecording.zip") || rawRecording.getPath().endsWith("testVar(testType)\\rawRecording.zip");

		Assert.assertTrue(rawRecPath);
	}

	/**
	 * Test method for
	 * {@link org.geppetto.simulation.manager.GeppettoManager#getSupportedOuputs(java.lang.String, org.geppetto.core.data.model.IExperiment, org.geppetto.core.data.model.IGeppettoProject)}.
	 * 
	 * @throws GeppettoExecutionException
	 * @throws GeppettoAccessException
	 */
	@Test
	public void test25GetSupportedOuputs() throws GeppettoExecutionException, GeppettoAccessException
	{
		List<ModelFormat> formats = manager.getSupportedOuputs("testVar(testType)", addedExperiment, geppettoProject);
		Assert.assertEquals(1, formats.size());
		Assert.assertEquals(ServicesRegistry.getModelFormat("TEST_FORMAT"), formats.get(0));
	}

	/**
	 * Test method for {@link org.geppetto.simulation.manager.GeppettoManager#getScope()}.
	 */
	@Test
	public void test26GetScope()
	{
		Assert.assertEquals(Scope.CONNECTION, manager.getScope());
	}

	/**
	 * Test method for {@link org.geppetto.simulation.manager.GeppettoManager#deleteExperiment(java.lang.String, org.geppetto.core.data.model.IExperiment)}.
	 * 
	 * @throws GeppettoExecutionException
	 * @throws GeppettoAccessException
	 */
	@Test
	public void test27DeleteExperiment() throws GeppettoExecutionException, GeppettoAccessException
	{
		manager.deleteExperiment("1", addedExperiment);
		Assert.assertEquals(1, geppettoProject.getExperiments().size());
	}

	/**
	 * Test method for {@link org.geppetto.simulation.manager.GeppettoManager#checkExperimentsStatus(java.lang.String, org.geppetto.core.data.model.IGeppettoProject)}.
	 */
	@Test
	public void test28CheckExperimentsStatusAgain5()
	{
		List<? extends IExperiment> status = manager.checkExperimentsStatus("1", geppettoProject);
		Assert.assertEquals(1, status.size());
		Assert.assertEquals(ExperimentStatus.COMPLETED, status.get(0).getStatus());
	}

	/**
	 * Test method for {@link org.geppetto.simulation.manager.GeppettoManager#closeProject(java.lang.String, org.geppetto.core.data.model.IGeppettoProject)}.
	 * 
	 * @throws GeppettoExecutionException
	 */
	@Test
	public void test29CloseProject() throws GeppettoExecutionException
	{
		RuntimeExperiment re=runtimeProject.getRuntimeExperiment(addedExperiment);
		manager.closeProject("1", geppettoProject);
		Assert.assertNull(runtimeProject.getActiveExperiment());
		Assert.assertNull(re.getExperimentState());
	}

	/**
	 * Test method for {@link org.geppetto.simulation.manager.GeppettoManager#deleteProject(java.lang.String, org.geppetto.core.data.model.IGeppettoProject)}.
	 * 
	 * @throws GeppettoExecutionException
	 * @throws GeppettoAccessException
	 */
	@Test
	public void test30DeleteProject() throws GeppettoExecutionException, GeppettoAccessException
	{
		manager.deleteProject("1", geppettoProject);
	}

	@AfterClass
	public static void doYourOneTimeTeardown()
	{
		File geppettoResult = new File("./src/test/resources/test/testResults.h5");
		geppettoResult.delete();
		File rawResult = new File("./src/test/resources/test/rawRecording.zip");
		rawResult.delete();
	}

}
