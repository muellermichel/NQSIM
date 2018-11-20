#!/bin/bash

nqsim_home="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
nqsim_work="$nqsim_home/data"

classpath="$classpath:$nqsim_home/target/nqsim-0.1-SNAPSHOT.jar"
classpath="$classpath:/usr/local/lib/mpi.jar"

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

# Parameters
world="$nqsim_work/world"
edgesz=256
agents=10000000
plansz=32
realms=32
timestep=60
nsteps=32

function generation {
    generator=ch.ethz.systems.nqsim2.SquareWorldGenerator
    generator_opts="$world $realms $agents $plansz $edgesz"
    time java $jvm_opts -classpath $classpath $generator $generator_opts | tee $nqsim_work/generator.log
}

function simulation {
    simulator=ch.ethz.systems.nqsim2.WorldSimulator
    simulator_opts="$world $timestep $nsteps"
    time mpirun -np $realms ${hosts_option} \
        java $jvm_opts -classpath $classpath $simulator $simulator_opts | tee $nqsim_work/simulator.log
    sort -k4 -n $world-realm-*.log | grep "Routed" | tee $nqsim_work/performance.log
    sort -k4 -n $world-realm-*.log | grep "\->" | tee $nqsim_work/result.log
}

function serial {
    realms=1
    cp -r $nqsim_work $nqsim_work-mpi
    rm $nqsim_work/*
    generation
    simulation
    cp -r $nqsim_work $nqsim_work-serial
}

mvn clean
mvn install
rm $nqsim_work/*
rm -r $nqsim_work-mpi
rm -r $nqsim_work-serial

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

while true; do
    read -p "Run serial? " should_serial
    case $should_serial in
          [Yy]* ) serial; break;;
          [Nn]* ) break ;;
              * ) echo "[Yy]es or [Nn]o?";;
    esac
done
