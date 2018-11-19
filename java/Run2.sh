#!/bin/bash

nqsim_home="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
nqsim_work="$nqsim_home/data"

classpath="$classpath:$nqsim_home/target/test-classes"
classpath="$classpath:$nqsim_home/target/classes"
classpath="$classpath:/usr/local/lib/mpi.jar"

jvm_opts="$jvm_opts -server"
jvm_opts="$jvm_opts -XX:-TieredCompilation"
jvm_opts="$jvm_opts -XX:+AggressiveOpts"
jvm_opts="$jvm_opts -XX:-UseBiasedLocking"

jvm_opts="$jvm_opts -Xmx20g"
jvm_opts="$jvm_opts -Xms20g"

#jvm_opts="$jvm_opts -XX:+AlwaysPreTouch"
jvm_opts="$jvm_opts -enableassertions"
jvm_opts="$jvm_opts -Xloggc:nqsim.jvm"
jvm_opts="$jvm_opts -XX:+PrintGCDetails"

# Parameters
world="$nqsim_work/world"
edgesz=2
agents=1
plansz=10
realms=2
#realms=1
timestep=60
nsteps=5

generator=ch.ethz.systems.nqsim2.SquareWorldGenerator
simulator=ch.ethz.systems.nqsim2.WorldSimulator

generator_opts="$world $realms $agents $plansz $edgesz"
simulator_opts="$world $timestep $nsteps"

function generation {
    time java $jvm_opts -classpath $classpath $generator $generator_opts | tee generator.log
}

function simulation {
    time mpirun -np $realms ${hosts_option} \
        java $jvm_opts -classpath $classpath $simulator $simulator_opts | tee simulator.log
    sort -k4 -n $world-realm-*.log | grep "Routed"
    sort -k4 -n $world-realm-*.log | grep "\->"
}

mvn clean
mvn install
rm $nqsim_work/*

while true; do
    read -p "Generate world? " should_gen
    case $should_gen in
          [Yy]* ) generation; break;;
          [Nn]* ) break ;;
              * ) echo "[Yy]es or [Nn]o?";;
    esac
done

while true; do
    read -p "Simulate world? " should_sim
    case $should_sim in
          [Yy]* ) simulation; break;;
          [Nn]* ) break ;;
              * ) echo "[Yy]es or [Nn]o?";;
    esac
done

#java $jvm_opts -classpath $classpath ch.ethz.systems.nqsim2.SquareWorldGenerator /tmp/world | tee nqsim2.txt
#java $jvm_opts -classpath $classpath ch.ethz.systems.nqsim2.WorldSimulator /tmp/world | tee nqsim2.txt
