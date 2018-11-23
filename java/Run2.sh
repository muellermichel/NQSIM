#!/bin/bash

nqsim_home="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"

classpath="$classpath:$nqsim_home/target/nqsim-0.1-SNAPSHOT.jar"
classpath="$classpath:/usr/local/lib/mpi.jar"

# Parameters
edgesz=187
agents=3000000
plansz=128
timestep=1
nsteps=128

function prepare_jvm_opts {
    jvm_opts="$jvm_opts -server"
    jvm_opts="$jvm_opts -XX:-TieredCompilation"
    jvm_opts="$jvm_opts -XX:+AggressiveOpts"
    jvm_opts="$jvm_opts -XX:-UseBiasedLocking"

    jvm_opts="$jvm_opts -Xmx20g"
    jvm_opts="$jvm_opts -Xms20g"

    #jvm_opts="$jvm_opts -XX:+AlwaysPreTouch"
    #jvm_opts="$jvm_opts -enableassertions"
    jvm_opts="$jvm_opts -Xloggc:$nqsim_work/nqsim.jvm"
    jvm_opts="$jvm_opts -XX:+PrintGCDetails"
}

function generation {
    world="$nqsim_work/world"
    generator=ch.ethz.systems.nqsim2.SquareWorldGenerator
    generator_opts="$world $realms $agents $plansz $edgesz"
    time java $jvm_opts -classpath $classpath $generator $generator_opts | tee $nqsim_work/generator.log
}

function simulation {
    world="$nqsim_work/world"
    simulator=ch.ethz.systems.nqsim2.WorldSimulator
    simulator_opts="$world $realms $timestep $nsteps"
#    time mpirun -np $realms ${hosts_option} \
        java $jvm_opts -classpath $classpath $simulator $simulator_opts | tee $nqsim_work/simulator.log
    sort -k4 -n $world-realm-*.log | grep "Processed" | tee $nqsim_work/performance.log
    sort -k4 -n $world-realm-*.log | grep "\->" | tee $nqsim_work/result.log
}

mvn clean
mvn install

for i in 1
#for i in 1 2 4 8 16 32 48
do
    echo "Running with $i realms..."
    realms=$i
    nqsim_work=$nqsim_home/data/$realms
    rm -rf $nqsim_work &> /dev/null
    mkdir $nqsim_work &> /dev/null
    prepare_jvm_opts
    generation
    simulation
    echo "Running with $i realms...done!"
done
