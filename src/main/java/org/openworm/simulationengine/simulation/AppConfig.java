package org.openworm.simulationengine.simulation;

public class AppConfig {
	
	private int maxBufferSize;
	private String simConfigLocation;
	
	public int getMaxBufferSize() {
		return maxBufferSize;
	}
	
	public void setMaxBufferSize(int elemCount) {
		this.maxBufferSize = elemCount;
	}

	public String getSimConfigLocation()
	{
		return simConfigLocation;
	}

	public void setSimConfigLocation(String simConfigLocation)
	{
		this.simConfigLocation = simConfigLocation;
	}
}
