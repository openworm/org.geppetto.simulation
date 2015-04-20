/*******************************************************************************
 * The MIT License (MIT)
 * 
 * Copyright (c) 2011 - 2015 OpenWorm.
 * http://openworm.org
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     	OpenWorm - http://openworm.org/people.html
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights 
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell 
 * copies of the Software, and to permit persons to whom the Software is 
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR 
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE 
 * USE OR OTHER DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package org.geppetto.simulation.visitor;

import java.util.ArrayList;
import java.util.List;

import org.geppetto.core.model.simulation.Aspect;
import org.geppetto.core.model.simulation.Model;
import org.geppetto.core.model.simulation.Simulator;
import org.geppetto.core.model.simulation.visitor.BaseVisitor;
import org.geppetto.core.model.simulation.visitor.TraversingVisitor;

/**
 * This visitor discovers any model which belongs to the scope
 * of the entity which we are visiting. This visitor should be applied
 * on the parent entity of the simulator we are interested in discovering
 * the scope for.
 * 
 * Example:
 * 
 * E1
 *   - A1(mechanical)
 *       -S1
 *   - E2
 *       - A2(mechanical)
 *           -S2
 *       - E3
 *           - A3(mechanical)
 *               -S3
 *               -M3
 *           - E4
 *               - A4(mechanical)
 *               - M4
 *               
 * If this visitor was applied to E1 the list returned should be empty.
 * If this visitor was applied to E2 the list returned should be empty.
 * If this visitor was applied to E3 the list returned would contain M3 and M4
 * This visitor should never be applied to E4 as its aspect doesn't contain a simulator.
 * 
 * @author matteocantarelli
 * 
 */
public class GetModelsForSimulatorVisitor extends TraversingVisitor
{

	private Simulator _simulator = null;
	private String _simulatorAspectId = null;
	private List<Model> _models = new ArrayList<Model>();

	/**
	 * @param simulatorModel
	 * @param sessionContext
	 */
	public GetModelsForSimulatorVisitor(Simulator simulatorModel)
	{
		super(new DepthFirstTraverserEntitiesFirst(), new BaseVisitor());
		_simulator = simulatorModel;
		_simulatorAspectId = _simulator.getParentAspect().getId();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.massfords.humantask.TraversingVisitor#visit(org.geppetto.simulation.model.Model)
	 */
	@Override
	public void visit(Model model)
	{
		//we are interested in this model if it has no simulator associated with it or if the simulator associated with it
		//is the one we are targeting with this visit
		if(model.getParentAspect().getSimulator() == null || model.getParentAspect().getSimulator().equals(_simulator))
		{
			//we are interested in this model if it has no simulator associated with it but its aspect id
			//is the same that as the aspect id which contains the simulator we are targeting with this visit
			if(model.getParentAspect().getId().equalsIgnoreCase(_simulatorAspectId))
			{
				_models.add(model);
			}
		}
	}

	@Override
	public void visit(Aspect aspect)
	{
		// NOTE: the if statements in this method are not combined to make it easier to understand the logic
		
		// Let's check the simulator is set (not null) otherwise we don't need to fetch models
		// as the aspect that doens't need to be simulated.
		if(aspect.getSimulator()!=null)
		{
			// Let's check it's not the same simulator as the parent entity the visitor was applied to.
			if(!aspect.getSimulator().equals(_simulator))
			{
				// If we find another aspect in a child entity that has a simulator for the same aspect
				// we return without fetching models that fall in the inner simulator scope.
				if(aspect.getId().equals(_simulatorAspectId))
				{
					return;
				}
			}
			super.visit(aspect);
		}
	}

	/**
	 * @return
	 */
	public List<Model> getModels()
	{
		return _models;
	}

}
