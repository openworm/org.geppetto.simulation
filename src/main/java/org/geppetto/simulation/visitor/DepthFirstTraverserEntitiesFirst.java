/**
 * 
 */
package org.geppetto.simulation.visitor;

import org.geppetto.simulation.model.Aspect;
import org.geppetto.simulation.model.Entity;

import com.massfords.humantask.DepthFirstTraverserImpl;
import com.massfords.humantask.Visitor;

/**
 * @author matteocantarelli
 *
 */
public class DepthFirstTraverserEntitiesFirst extends DepthFirstTraverserImpl
{

	@Override
	public void traverse(Entity aBean, Visitor aVisitor)
	{
        for (Entity bean: aBean.getEntities()) {
            bean.accept(aVisitor);
        }
        for (Aspect bean: aBean.getAspects()) {
            bean.accept(aVisitor);
        }
        if (aBean.getPosition()!= null) {
            aBean.getPosition().accept(aVisitor);
        }
	}



}
