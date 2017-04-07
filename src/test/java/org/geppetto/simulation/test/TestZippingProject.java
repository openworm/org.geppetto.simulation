package org.geppetto.simulation.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.geppetto.core.beans.PathConfiguration;
import org.geppetto.core.data.DataManagerHelper;
import org.geppetto.core.data.DefaultGeppettoDataManager;
import org.geppetto.core.data.model.IGeppettoProject;
import org.geppetto.core.data.model.IUserGroup;
import org.geppetto.core.data.model.UserPrivileges;
import org.geppetto.core.data.model.local.LocalGeppettoProject;
import org.geppetto.core.manager.Scope;
import org.geppetto.core.services.registry.ApplicationListenerBean;
import org.geppetto.simulation.manager.ExperimentRunManager;
import org.geppetto.simulation.manager.GeppettoManager;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * Junit test for zipping project contens
 * @author jrmartin
 *
 */
public class TestZippingProject {

	private static GeppettoManager manager = new GeppettoManager(Scope.CONNECTION);
	private static IGeppettoProject geppettoProject;
	
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

	@Test
	public void testZippingProject(){

		try
		{
			InputStreamReader inputStreamReader = new InputStreamReader(GeppettoManagerTest.class.getResourceAsStream("/test/hhcell/GEPPETTO.json"));
			geppettoProject = DataManagerHelper.getDataManager().getProjectFromJson(TestUtilities.getGson(), inputStreamReader);
			((LocalGeppettoProject)geppettoProject).setPublic(true);
			manager.loadProject("1", geppettoProject);
			
			Path projectZipped = manager.downloadProject(geppettoProject);
			assertNotNull(projectZipped);
			
			assertTrue(projectZipped.toFile().exists());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testZippingProject1(){

		try
		{
			InputStreamReader inputStreamReader = new InputStreamReader(GeppettoManagerTest.class.getResourceAsStream("/test/project2/geppettoManagerTest2.json"));
			geppettoProject = DataManagerHelper.getDataManager().getProjectFromJson(TestUtilities.getGson(), inputStreamReader);
			((LocalGeppettoProject)geppettoProject).setPublic(true);
			manager.loadProject("1", geppettoProject);

			Path projectZipped = manager.downloadProject(geppettoProject);
			assertNotNull(projectZipped);
			
			assertTrue(projectZipped.toFile().exists());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testZippingProject2(){

		try
		{
			InputStreamReader inputStreamReader = new InputStreamReader(GeppettoManagerTest.class.getResourceAsStream("/test/project3/geppettoManagerTest3.json"));
			geppettoProject = DataManagerHelper.getDataManager().getProjectFromJson(TestUtilities.getGson(), inputStreamReader);
			((LocalGeppettoProject)geppettoProject).setPublic(true);
			manager.loadProject("1", geppettoProject);

			Path projectZipped = manager.downloadProject(geppettoProject);
			
			assertNotNull(projectZipped);
			
			assertTrue(projectZipped.toFile().exists());	
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testZippingProject3(){

		try
		{
			InputStreamReader inputStreamReader = new InputStreamReader(GeppettoManagerTest.class.getResourceAsStream("/test/project4/geppettoManagerTest.json"));
			geppettoProject = DataManagerHelper.getDataManager().getProjectFromJson(TestUtilities.getGson(), inputStreamReader);
			((LocalGeppettoProject)geppettoProject).setPublic(true);
			manager.loadProject("1", geppettoProject);

			Path projectZipped = manager.downloadProject(geppettoProject);
			
			assertNotNull(projectZipped);
			
			assertTrue(projectZipped.toFile().exists());	
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testZippingProject4(){

		try
		{
			InputStreamReader inputStreamReader = new InputStreamReader(GeppettoManagerTest.class.getResourceAsStream("/test/project5/geppettoManagerTest.json"));
			geppettoProject = DataManagerHelper.getDataManager().getProjectFromJson(TestUtilities.getGson(), inputStreamReader);
			((LocalGeppettoProject)geppettoProject).setPublic(true);
			manager.loadProject("1", geppettoProject);

			Path projectZipped = manager.downloadProject(geppettoProject);
			
			assertNotNull(projectZipped);
			
			assertTrue(projectZipped.toFile().exists());	
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
