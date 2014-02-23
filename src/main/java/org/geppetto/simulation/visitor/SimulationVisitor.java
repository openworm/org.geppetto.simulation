/**
 * 
 */
package org.geppetto.simulation.visitor;

import org.geppetto.simulation.SessionContext;
import org.geppetto.simulation.model.Aspect;
import org.geppetto.simulation.model.Entity;
import org.geppetto.simulation.model.Model;
import org.geppetto.simulation.model.Simulation;
import org.geppetto.simulation.model.Simulator;

import com.massfords.humantask.BaseVisitor;
import com.massfords.humantask.TraversingVisitor;

/**
 * This is the simulation visitor which traverse the simulation tree and orchestrates
 * the simulation of the different models
 * 
 * @author matteocantarelli
 *
 */
public class SimulationVisitor extends TraversingVisitor
{
	
	private SessionContext _sessionContext;

	public SimulationVisitor(SessionContext sessionContext)
	{
		super(new DepthFirstTraverserEntitiesFirst(), new BaseVisitor());
		_sessionContext=sessionContext;
	}

	@Override
	public void visit(Aspect aBean)
	{
		// TODO Auto-generated method stub
		super.visit(aBean);
	}

	@Override
	public void visit(Entity aBean)
	{
		// TODO Auto-generated method stub
		super.visit(aBean);
	}

	@Override
	public void visit(Model aBean)
	{
		// TODO Auto-generated method stub
		super.visit(aBean);
	}

	@Override
	public void visit(Simulation aBean)
	{
		// TODO Auto-generated method stub
		super.visit(aBean);
	}

	@Override
	public void visit(Simulator aBean)
	{
		// TODO Auto-generated method stub
		super.visit(aBean);
	}

}
