from py4j.java_gateway import JavaGateway
from abc import ABCMeta, abstractmethod

class Integrator(object):
    __metaclass__ = ABCMeta
    
    states = {}
    
    def __init__(self):
        self.gateway=JavaGateway(start_callback_server=True)
        self.gateway.entry_point.integratorReady(self)
        return
            
    def addState(self, state, value):
        print "addState invoked"
        self.states[state]=value
        return
        
    def getState(self, state):
        print "getState invoked"
        return self.states[state]
    
    def getStates(self):
        return self.states

    def stopScript(self):
        print "stopScript invoked"
        self.gateway.shutdown()
        return
    
    @abstractmethod
    def runIntegration(self):
        print "runIntegration invoked"
        self.gateway.entry_point.resultsReady()
        return
        
    class Java:
        implements = ['org.geppetto.simulation.test.Integrator']


