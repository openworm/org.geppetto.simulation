package org.geppetto.simulation.test;
public interface IIntegrator {

        public void addState(String stateName, double value);

        public double getState(String state);
        
        public void runIntegration();

		public void stopScript();

		

        

}