NQSIM, a sandbox to find a performant redesign of MATSim's QSIM
===============================================================

The idea of this project is to have an isolated sandbox to test designs of transport network simulations based on event systems. The planning and replanning part is completely randomized and the network is built like a chessboard-grid with double linking between nodes (forward and backward), a structure I'm calling "Chinese City" since historical imperial cities in China were built that way. Today you may be more familiar with this structure from American cities. The network is constructed from a JSON file (`chinese_capital_187x187.json`) for 187x187 nodes with links of 1km length and random speed limits chosen between 30, 40, 50, 60, 80 and 100 km/h. This is chosen in order to be close to a "Swiss scale" network since this work is being done for a public transport provider in Switzerland attempting to achieve performant transport simulation of the entire Swiss transport network at 100% sampling rate. The agents are initialized with a plan containing random consecutive links, the plan length being 200 + [0-49] links. This is chosen such that all agents are still on the network after 3600 seconds of simulation time. Currently a simulation always ends after 3600 seconds.

The repository contains a python- and a Java version. Only the Java version is being developed further, python was only used as an early prototype and as a network generator the network `json` files are generated in python.

Setup
-----
* Requires an OpenMPI built with Java extensions.
* `MPI_JAR_PATH` environment variable needs to be set to the path of `mpi.jar` given by OpenMPI.
* Requires Java 8 installed and available on `PATH`.
* Requires maven.
* Optional: OpenMPI compatible hosts file with `HOSTS` variable set to its path if you want to run on multi-node.

Building
--------
    cd [repo-dir]/java
    mvn install
    
Running
-------
    cd [repo-dir]/java
    ./Run.sh [number-of-processes] [number-of-agents]
    
Supported number of processes: The current version is tested to work with 1, 2, 4, 8, 16 and 25 processes. There are still some bugs in the decomposition that show up at higher number of processes.
    
Performance
-----------
    With 3 million agents on 25 processes running on 4x Intel(R) Core(TM) i5-8250U CPU @ 1.60GHz I'm currently getting ~590 simulated seconds per second.
    
