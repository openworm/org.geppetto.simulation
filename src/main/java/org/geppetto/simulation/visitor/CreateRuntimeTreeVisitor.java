package org.geppetto.simulation.visitor;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.UUID;

import org.geppetto.core.common.GeppettoErrorCodes;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.model.IModel;
import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.model.ModelInterpreterException;
import org.geppetto.core.model.ModelWrapper;
import org.geppetto.core.model.runtime.ANode;
import org.geppetto.core.model.runtime.AspectNode;
import org.geppetto.core.model.runtime.EntityNode;
import org.geppetto.core.model.runtime.RuntimeTreeRoot;
import org.geppetto.core.model.simulation.Aspect;
import org.geppetto.core.model.simulation.Entity;
import org.geppetto.core.model.simulation.Model;
import org.geppetto.core.model.simulation.Simulator;
import org.geppetto.core.simulation.ISimulationCallbackListener;
import org.geppetto.core.simulator.ISimulator;
import org.geppetto.simulation.SessionContext;

import com.massfords.humantask.BaseVisitor;
import com.massfords.humantask.TraversingVisitor;

public class CreateRuntimeTreeVisitor extends TraversingVisitor{

	private SessionContext _sessionContext;
	private ISimulationCallbackListener _simulationCallback;
	
	public CreateRuntimeTreeVisitor(SessionContext sessionContext, ISimulationCallbackListener simulationCallback)
	{
		super(new DepthFirstTraverserEntitiesFirst(), new BaseVisitor());
		this._sessionContext = sessionContext;
		this._simulationCallback=simulationCallback;
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
		Simulator simulator = aspect.getSimulator();
		AspectNode clientAspect = new AspectNode();
		clientAspect.setId(aspect.getId());
		clientAspect.setInstancePath(aspect.getInstancePath());
		clientAspect.setName(aspect.getId());
		
		//attach to parent entity before populating skeleton of aspect node
		addAspectToEntity(clientAspect, aspect.getParentEntity());

		if(model != null)
		{
			try
			{
				IModelInterpreter modelInterpreter = _sessionContext.getModelInterpreter(model);
				modelInterpreter.populateRuntimeTree(clientAspect);
				
				IModel wrapper = modelInterpreter.readModel(new URL(model.getModelURL()), null, model.getParentAspect().getInstancePath());
				
						
				clientAspect.setModel(wrapper);
				clientAspect.setModelInterpreter(modelInterpreter);
			}
			catch(GeppettoInitializationException e)
			{
				_simulationCallback.error(GeppettoErrorCodes.SIMULATION, this.getClass().getName(), null,e);
			}
			catch(MalformedURLException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch(ModelInterpreterException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if(simulator != null)
		{
			try
			{
				ISimulator simulatorService = _sessionContext.getSimulator(simulator);
				
				clientAspect.setSimulator(simulatorService);
			}
			catch(GeppettoInitializationException e)
			{
				_simulationCallback.error(GeppettoErrorCodes.SIMULATION, this.getClass().getName(), null,e);
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.massfords.humantask.TraversingVisitor#visit(org.geppetto.simulation.model.Aspect)
	 */
	@Override
	public void visit(Entity entity)
	{
		EntityNode clientEntity = new EntityNode();
		clientEntity.setName(entity.getId());
		clientEntity.setId(entity.getId());
		clientEntity.setInstancePath(entity.getInstancePath());
		
		getRuntimeModel().addChild(clientEntity);
		
		super.visit(entity);
	}

	/**
	 * Attaches AspectNode to its client parent EntityNode
	 * 
	 * @param aspectNode - Runtime Aspect Node
	 * @param entity - Persistent model Entity
	 */
	private void addAspectToEntity(AspectNode aspectNode, Entity entity){
		List<ANode> children = this.getRuntimeModel().getChildren();
		
		//traverse through runtimetree entities to find the parent of aspectNode
		for(int i =0; i<children.size(); i++){
			EntityNode currentEntity = ((EntityNode)children.get(i));
			if(currentEntity.getId().equals(entity.getId())){
				currentEntity.addChild(aspectNode);
			}
		}
	}
	
	public RuntimeTreeRoot getRuntimeModel(){
		return _sessionContext.get_runtimeTreeRoot();
	}
}
