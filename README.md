#Generic simulation bundle for the OpenWorm simulation engine / http://openworm.org

This is a generic simulation loader that will retrieve a given simulation from configuration [ https://www.dropbox.com/s/iyr085zcegyis0n/sph-sim-config.xml?dl=1 ], discover required OSGi services dynamically based on cofigurationparameters and will initiate/control the simulation in a generic way.