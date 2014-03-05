/*******************************************************************************
 * The MIT License (MIT)
 * 
 * Copyright (c) 2011, 2013 OpenWorm.
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

import org.geppetto.core.model.simulation.Aspect;
import org.geppetto.core.model.simulation.Entity;
import org.geppetto.core.model.simulation.Model;
import org.geppetto.core.model.simulation.Simulator;

import com.massfords.humantask.BaseVisitor;
import com.massfords.humantask.TraversingVisitor;

/**
 * This visitor decorates the simulation tree with the instance path
 * 
 * @author matteocantarelli
 * 
 */
public class InstancePathDecoratorVisitor extends TraversingVisitor
{
	
	/**
	 * 
	 */
	public InstancePathDecoratorVisitor()
	{
		super(new DepthFirstTraverserEntitiesFirst(), new BaseVisitor());
	}

	private static final String DOT = ".";
	private static final String COLON = ":";
	private String _currentPath = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.massfords.humantask.TraversingVisitor#visit(org.geppetto.simulation.model.Aspect)
	 */
	@Override
	public void visit(Aspect aspect)
	{
		String beforePath=_currentPath;
		_currentPath+=COLON+aspect.getId();
		aspect.setInstancePath(_currentPath);
		super.visit(aspect);
		_currentPath=beforePath;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.massfords.humantask.TraversingVisitor#visit(org.geppetto.simulation.model.Entity)
	 */
	@Override
	public void visit(Entity entity)
	{
		String beforePath=_currentPath;
		if(_currentPath != null)
		{
			_currentPath += DOT + entity.getId();
		}
		else
		{
			_currentPath = entity.getId();
		}
		entity.setInstancePath(_currentPath);
		super.visit(entity);
		_currentPath=beforePath;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.massfords.humantask.TraversingVisitor#visit(org.geppetto.simulation.model.Model)
	 */
	@Override
	public void visit(Model model)
	{
		model.setInstancePath(_currentPath);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.massfords.humantask.TraversingVisitor#visit(org.geppetto.simulation.model.Simulator)
	 */
	@Override
	public void visit(Simulator simulator)
	{
		simulator.setInstancePath(_currentPath);
	}

}
