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

import org.geppetto.core.data.DataManagerHelper;
import org.geppetto.core.data.model.IExperiment;
import org.geppetto.core.data.model.IInstancePath;
import org.geppetto.core.data.model.ISimulatorConfiguration;
import org.geppetto.core.model.geppettomodel.Aspect;
import org.geppetto.core.model.geppettomodel.visitor.BaseVisitor;
import org.geppetto.core.model.geppettomodel.visitor.TraversingVisitor;
import org.geppetto.core.model.typesystem.visitor.DepthFirstTraverserImportsFirst;

/**
 * @author matteocantarelli
 *
 */
public class PopulateExperimentVisitor extends TraversingVisitor
{

	private IExperiment experiment;

	public PopulateExperimentVisitor(IExperiment experiment)
	{
		super(new DepthFirstTraverserImportsFirst(), new BaseVisitor());
		this.experiment=experiment;
	}

	@Override
	public void visit(Aspect aBean)
	{
		super.visit(aBean);
		IInstancePath instancePath=DataManagerHelper.getDataManager().newInstancePath(aBean.getParentEntity().getInstancePath(),aBean.getId(),"");
		ISimulatorConfiguration simulatorConfiguration=DataManagerHelper.getDataManager().newSimulatorConfiguration("","",0l,0l);
		DataManagerHelper.getDataManager().newAspectConfiguration(experiment,instancePath,simulatorConfiguration);
	}

}
