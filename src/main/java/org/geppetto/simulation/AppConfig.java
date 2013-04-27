package org.geppetto.simulation;

public class AppConfig {
	
	private int _maxBufferSize;
	private int _updateCycle;
	
	public int getMaxBufferSize() {
		return _maxBufferSize;
	}
	
	public void setMaxBufferSize(int elemCount) {
		this._maxBufferSize = elemCount;
	}

	public int getUpdateCycle()
	{
		return _updateCycle;
	}

	public void setUpdateCycle(int updateCycle)
	{
		this._updateCycle = updateCycle;
	}
}
