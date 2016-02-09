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

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.geppetto.simulation.manager.ExperimentRunManager;
import org.geppetto.simulation.manager.GeppettoManager;
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
public class NoAccessGeppettoManagerTest
{

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	private static ArrayList<UserPrivileges> privileges;
	private static IExperiment existingExperiment;
	private static GeppettoManager manager = new GeppettoManager(Scope.CONNECTION);
	private static IGeppettoProject geppettoProject;
	private static RuntimeProject runtimeProject;

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
		privileges = new ArrayList<UserPrivileges>();
		IUserGroup userGroup = DataManagerHelper.getDataManager().newUserGroup("guest", privileges, value, value * 2);
		manager.setUser(DataManagerHelper.getDataManager().newUser("nonna", "passauord", true, userGroup));
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
	public void test02LoadProjectNegative() throws IOException, GeppettoInitializationException, GeppettoExecutionException, GeppettoAccessException
	{
		InputStreamReader inputStreamReader = new InputStreamReader(NoAccessGeppettoManagerTest.class.getResourceAsStream("/test/geppettoManagerTest.json"));
		geppettoProject = DataManagerHelper.getDataManager().getProjectFromJson(TestUtilities.getGson(), inputStreamReader);
		exception.expect(GeppettoAccessException.class);
		manager.loadProject("1", geppettoProject);

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

		InputStreamReader inputStreamReader = new InputStreamReader(NoAccessGeppettoManagerTest.class.getResourceAsStream("/test/geppettoManagerTest.json"));
		geppettoProject = DataManagerHelper.getDataManager().getProjectFromJson(TestUtilities.getGson(), inputStreamReader);
		privileges.add(UserPrivileges.READ_PROJECT);
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
		existingExperiment = geppettoProject.getExperiments().get(0);
		ExperimentState experimentState = manager.loadExperiment("1", existingExperiment);
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
		exception.expect(GeppettoAccessException.class);
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
		exception.expect(GeppettoAccessException.class);
		manager.setWatchedVariables(watchedVariables, runtimeProject.getActiveExperiment(), geppettoProject, false);
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
		exception.expect(GeppettoAccessException.class);
		manager.newExperiment("2", geppettoProject);

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
		exception.expect(GeppettoAccessException.class);
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
		exception.expect(GeppettoAccessException.class);
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
		exception.expect(GeppettoAccessException.class);
		manager.setWatchedVariables(watchedVariables, runtimeProject.getActiveExperiment(), geppettoProject, true);

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
		Assert.assertEquals(1, existingExperiment.getAspectConfigurations().size());
		IAspectConfiguration ac = existingExperiment.getAspectConfigurations().get(0);
		Assert.assertEquals("testVar(testType)", ac.getAspect().getInstancePath());
		Assert.assertNotNull(ac.getSimulatorConfiguration());
		exception.expect(GeppettoAccessException.class);
		manager.runExperiment("1", existingExperiment);
		Assert.assertEquals(3, existingExperiment.getAspectConfigurations().get(0).getWatchedVariables().size());
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
		privileges.remove(0);
		exception.expect(GeppettoAccessException.class);
		manager.playExperiment("1", existingExperiment, null);
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
		exception.expect(GeppettoAccessException.class);
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
		exception.expect(GeppettoAccessException.class);
		manager.uploadModelToDropBox("testVar(testType)", existingExperiment, geppettoProject, ServicesRegistry.getModelFormat("TEST_FORMAT"));
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
		exception.expect(GeppettoAccessException.class);
		manager.uploadResultsToDropBox("testVar(testType)", existingExperiment, geppettoProject, ResultsFormat.GEPPETTO_RECORDING);
		manager.uploadResultsToDropBox("testVar(testType)", existingExperiment, geppettoProject, ResultsFormat.RAW);
	}

	/**
	 * Test method for {@link org.geppetto.simulation.manager.GeppettoManager#unlinkDropBoxAccount(java.lang.String)}.
	 * 
	 * @throws Exception
	 */
	public void testUnlinkDropBoxAccount() throws Exception
	{
		exception.expect(GeppettoAccessException.class);
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
		exception.expect(GeppettoAccessException.class);
		File model = manager.downloadModel("testVar(testType)", ServicesRegistry.getModelFormat("TEST_FORMAT"), existingExperiment, geppettoProject);
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
		exception.expect(GeppettoAccessException.class);
		URL geppettoRecording = manager.downloadResults("testVar(testType)", ResultsFormat.GEPPETTO_RECORDING, existingExperiment, geppettoProject);
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
		exception.expect(GeppettoAccessException.class);
		List<ModelFormat> formats = manager.getSupportedOuputs("testVar(testType)", existingExperiment, geppettoProject);
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
		exception.expect(GeppettoAccessException.class);
		manager.deleteExperiment("1", existingExperiment);
		Assert.assertEquals(1, geppettoProject.getExperiments().size());
	}

	/**
	 * Test method for {@link org.geppetto.simulation.manager.GeppettoManager#closeProject(java.lang.String, org.geppetto.core.data.model.IGeppettoProject)}.
	 * 
	 * @throws GeppettoExecutionException
	 */
	@Test
	public void test29CloseProject() throws GeppettoExecutionException
	{
		manager.closeProject("1", geppettoProject);
		Assert.assertNull(runtimeProject.getActiveExperiment());
		exception.expect(NullPointerException.class);
		Assert.assertNull(runtimeProject.getRuntimeExperiment(existingExperiment).getExperimentState());
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
		exception.expect(GeppettoAccessException.class);
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
