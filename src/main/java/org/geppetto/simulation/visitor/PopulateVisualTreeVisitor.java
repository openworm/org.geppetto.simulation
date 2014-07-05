package org.geppetto.simulation.visitor;

import org.geppetto.core.model.runtime.AspectNode;
import org.geppetto.core.model.runtime.StateVariableNode;
import org.geppetto.core.model.state.visitors.DefaultStateVisitor;
import org.geppetto.core.simulation.ISimulationCallbackListener;
import org.geppetto.simulation.SessionContext;

public class PopulateVisualTreeVisitor extends DefaultStateVisitor{

	private SessionContext _sessionContext;
	private ISimulationCallbackListener _simulationCallback;
	
	public PopulateVisualTreeVisitor(SessionContext sessionContext, ISimulationCallbackListener simulationCallback)
	{
		_sessionContext = sessionContext;
		_simulationCallback=simulationCallback;
	}

	/* (non-Javadoc)
	 * @see org.geppetto.core.model.state.visitors.DefaultStateVisitor#inCompositeStateNode(org.geppetto.core.model.state.CompositeStateNode)
	 */
	@Override
	public boolean inAspectNode(AspectNode node)
	{
		return super.inAspectNode(node);
	}

	/* (non-Javadoc)
	 * @see org.geppetto.core.model.state.visitors.DefaultStateVisitor#outCompositeStateNode(org.geppetto.core.model.state.CompositeStateNode)
	 */
	@Override
	public boolean outAspectNode(AspectNode node)
	{
		return super.outAspectNode(node);
	}

	/* (non-Javadoc)
	 * @see org.geppetto.core.model.state.visitors.DefaultStateVisitor#visitSimpleStateNode(org.geppetto.core.model.state.SimpleStateNode)
	 */
	@Override
	public boolean visitStateVariableNode(StateVariableNode node)
	{
		return super.visitStateVariableNode(node);
	}
}
