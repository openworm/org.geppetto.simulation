from geppetto import Integrator

class MyIntegrator(Integrator):

    def runIntegration(self):
        #Add here your implementation        
        for key in self.states:
            self.states[key]=self.states[key]+0.1
            print key ,":", self.states[key]
        #End
        return super(MyIntegrator, self).runIntegration()


MyIntegrator()