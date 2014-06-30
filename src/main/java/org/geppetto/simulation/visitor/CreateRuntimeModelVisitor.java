package org.geppetto.simulation.visitor;

import org.geppetto.core.common.GeppettoErrorCodes;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.model.simulation.Aspect;
import org.geppetto.core.model.simulation.Model;
import org.geppetto.core.model.state.AspectNode;
import org.geppetto.core.simulation.ISimulationCallbackListener;
import org.geppetto.simulation.SessionContext;

import com.massfords.humantask.BaseVisitor;
import com.massfords.humantask.Traverser;
import com.massfords.humantask.TraversingVisitor;
import com.massfords.humantask.Visitor;

public class CreateRuntimeModelVisitor extends TraversingVisitor{

	private SessionContext _sessionContext;
	private ISimulationCallbackListener _simulationCallback;

	public CreateRuntimeModelVisitor(SessionContext sessionContext, ISimulationCallbackListener simulationCallback)
	{
		super(new DepthFirstTraverserEntitiesFirst(), new BaseVisitor());
		_sessionContext = sessionContext;
		_simulationCallback=simulationCallback;
	}
	
	public CreateRuntimeModelVisitor(Traverser aTraverser, Visitor aVisitor) {
		super(aTraverser, aVisitor);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.massfords.humantask.TraversingVisitor#visit(org.geppetto.simulation.model.Aspect)
	 */
	@Override
	public void visit(Aspect aspect)
	{
		Model model = aspect.getModel();
		AspectNode clientAspect = new AspectNode();
		clientAspect.setId(aspect.getId());
		clientAspect.setInstancePath(aspect.getInstancePath());

		if(model != null)
		{
			try
			{
				IModelInterpreter modelInterpreter = _sessionContext.getModelInterpreter(model);
				modelInterpreter.populateRuntimeTree(clientAspect);
			}
			catch(GeppettoInitializationException e)
			{
				_simulationCallback.error(GeppettoErrorCodes.SIMULATION, this.getClass().getName(), null,e);
			}
		}
	}

}
