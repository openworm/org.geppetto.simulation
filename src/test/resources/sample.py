from py4j.java_gateway import JavaGateway
import numpy as np

class Integrator(object):
    states = {}
    
    def __init__(self, gateway):
        self.gateway=gateway
        return
            
    def addState(self, state, value):
        print "addState invoked"
        self.states[state]=value
        return
        
    def getState(self, state):
        print "getState invoked"
        return self.states[state]
        
    def runIntegration(self):
        print "runIntegration invoked"
        for key in self.states:
            self.states[key]=self.states[key]+0.1
            print key ,":", self.states[key]
        self.gateway.entry_point.resultsReady()
        return

    def stopScript(self):
        print "stopScript invoked"
        self.gateway.shutdown()
        return
        
    class Java:
        implements = ['org.geppetto.simulation.test.Integrator']


if __name__ == '__main__':
    gateway = JavaGateway(start_callback_server=True)
    integrator = Integrator(gateway)
    gateway.entry_point.integratorReady(integrator)
