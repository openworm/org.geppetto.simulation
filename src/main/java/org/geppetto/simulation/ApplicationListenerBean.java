package org.geppetto.simulation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

public class ApplicationListenerBean implements ApplicationListener<ContextRefreshedEvent>
{
	private static Log _logger = LogFactory.getLog(ApplicationListenerBean.class);

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event)
	{
		new ExperimentRunManager();
		_logger.info("Experiment run manager started");
	}
}