
package org.geppetto.simulation.visitor;


/**
 * This visitor loads a simulation
 * 
 * @author matteocantarelli
 * 
 */
public class LoadGeppettoModelVisitor //extends GeppettoModelVisitor
{

//	private Map<String, IModelInterpreter> _modelInterpreters;
//	private Map<String, IModel> _model;
//	private static Log _logger = LogFactory.getLog(LoadGeppettoModelVisitor.class);
//
//	public LoadGeppettoModelVisitor(Map<String, IModelInterpreter> modelInterpreters, Map<String, IModel> model)
//	{
//		super();
//		_modelInterpreters = modelInterpreters;
//		_model = model;
//	}
//
//	/*
//	 * (non-Javadoc)
//	 * 
//	 * @see com.massfords.humantask.TraversingVisitor#visit(org.geppetto.simulation.model.Model)
//	 */
//	@Override
//	public void visit(Model pModel)
//	{
//		super.visit(pModel);
//		try
//		{
//			IModelInterpreter modelInterpreter = _modelInterpreters.get(pModel.getInstancePath());
//			IModel model = _model.get(pModel.getInstancePath());
//			if(model == null)
//			{
//				List<URL> recordings = new ArrayList<URL>();
//				if(pModel.getRecordingURL() != null)
//				{
//					// add all the recordings found
//					for(String recording : pModel.getRecordingURL())
//					{
//						URL url = null;
//
//						url = URLReader.getURL(recording);
//
//						recordings.add(url);
//					}
//				}
//
//				long start = System.currentTimeMillis();
//
//				URL modelUrl = null;
//				String modelUrlStr = pModel.getModelURL();
//				if(modelUrlStr != null)
//				{
//					modelUrl = URLReader.getURL(modelUrlStr);
//					
//					model = modelInterpreter.readModel(modelUrl, recordings, pModel.getParentAspect().getInstancePath());
//					model.setInstancePath(pModel.getInstancePath());
//					_model.put(pModel.getInstancePath(), model);
//
//					long end = System.currentTimeMillis();
//					_logger.info("Finished reading model, took " + (end - start) + " ms ");
//				}
//				else
//				{
//					// the model is already loaded, we are coming here after a stop simulation which doesn't delete the model, do nothing.
//				}
//			}
//		}
//		catch(ModelInterpreterException | IOException e)
//		{
//			exception = new GeppettoExecutionException(e);
//		}
//
//	}
}
