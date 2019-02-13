package org.geppetto.simulation;

/**
 * Configuration class to keep track of simulation server mode
 * 
 * @author  Jesus R. Martinez (jesus@metacell.us)
 *
 */
public class GeppettoManagerConfiguration {
	
	private boolean allowVolatileProjectsSimulation;
	
	public boolean getAllowVolatileProjectsSimulation() {
		return allowVolatileProjectsSimulation;
	}

	public void setAllowVolatileProjectsSimulation(boolean allowVolatileProjectsSimulation) {
		this.allowVolatileProjectsSimulation = allowVolatileProjectsSimulation;
	}
}
