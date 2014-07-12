package org.geppetto.simulation.visitor;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geppetto.core.common.GeppettoErrorCodes;
import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.model.ModelInterpreterException;
import org.geppetto.core.model.runtime.AspectNode;
import org.geppetto.core.model.runtime.StateVariableNode;
import org.geppetto.core.model.state.visitors.DefaultStateVisitor;
import org.geppetto.core.simulation.ISimulationCallbackListener;
import org.geppetto.simulation.SessionContext;

public class PopulateModelTreeVisitor extends DefaultStateVisitor{

	private static Log _logger = LogFactory.getLog(PopulateModelTreeVisitor.class);

	private ISimulationCallbackListener _simulationCallBack;


	public PopulateModelTreeVisitor(ISimulationCallbackListener simulationListener)
	{
		this._simulationCallBack = simulationListener;
	}

	/* (non-Javadoc)
	 * @see org.geppetto.core.model.state.visitors.DefaultStateVisitor#inCompositeStateNode(org.geppetto.core.model.state.CompositeStateNode)
	 */
	@Override
	public boolean inAspectNode(AspectNode node)
	{
		IModelInterpreter modelInterpreter = node.getModelInterpreter();
		try
		{
			modelInterpreter.populateModelTree(node);
		}
		catch(ModelInterpreterException e)
		{
			_simulationCallBack.error(GeppettoErrorCodes.INITIALIZATION, this.getClass().getName(),null,e);
		}
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
