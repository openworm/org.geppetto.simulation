/**
 * 
 */
package org.geppetto.simulation.test;

import org.geppetto.core.model.simulation.Aspect;
import org.geppetto.core.model.simulation.Entity;
import org.geppetto.core.model.simulation.visitor.BaseVisitor;
import org.geppetto.core.model.simulation.visitor.TraversingVisitor;
import org.geppetto.core.model.state.visitors.DepthFirstTraverserEntitiesFirst;
import org.junit.Assert;

/**
 * @author matteocantarelli
 *
 */
public class SimulationTestVisitor extends TraversingVisitor
{

	private int _entityVisit=1;
	private int _aspectVisit=1;
	
	public SimulationTestVisitor()
	{
		super(new DepthFirstTraverserEntitiesFirst(), new BaseVisitor());
	}

	@Override
	public void visit(Aspect aBean)
	{
		super.visit(aBean);
		if(_aspectVisit==1)
		{
			Assert.assertEquals("electrical", aBean.getId());
		}
		else if(_aspectVisit==2)
		{
			Assert.assertEquals("mechanical", aBean.getId());
		}
		else if(_aspectVisit==3)
		{
			Assert.assertEquals("mechanical", aBean.getId());
		}
		else if(_aspectVisit==4)
		{
			Assert.assertEquals("electrical", aBean.getId());
		}
		else if(_aspectVisit==5)
		{
			Assert.assertEquals("electrical", aBean.getId());
		}
		else if(_aspectVisit==6)
		{
			Assert.assertEquals("mechanical", aBean.getId());
		}
		_aspectVisit++;
	}

	@Override
	public void visit(Entity aBean)
	{
		super.visit(aBean);
		if(_entityVisit==1)
		{
			Assert.assertEquals("neuron1", aBean.getId());
		}
		else if(_entityVisit==2)
		{
			Assert.assertEquals("neuron2", aBean.getId());
		}
		else if(_entityVisit==3)
		{
			Assert.assertEquals("network", aBean.getId());
		}
		_entityVisit++;
	}

}
