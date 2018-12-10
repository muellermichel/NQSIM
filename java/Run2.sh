#!/bin/bash

matsim_dir=/home/rbruno/git/matsim
nqsim_home="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"

classpath="$classpath:$nqsim_home/target/nqsim-0.1-SNAPSHOT.jar"
classpath="$classpath:/usr/local/lib/mpi.jar"

# Berlin Generator dependencies...
classpath="$classpath:\
${matsim_dir}/matsim/target/matsim-0.11.0-SNAPSHOT.jar:\
${HOME}/.m2/repository/log4j/log4j/1.2.15/log4j-1.2.15.jar:\
${HOME}/.m2/repository/org/geotools/gt-main/14.0/gt-main-14.0.jar:\
${HOME}/.m2/repository/org/geotools/gt-api/14.0/gt-api-14.0.jar:\
${HOME}/.m2/repository/com/vividsolutions/jts/1.13/jts-1.13.jar:\
${HOME}/.m2/repository/org/jdom/jdom/1.1.3/jdom-1.1.3.jar:\
${HOME}/.m2/repository/javax/media/jai_core/1.1.3/jai_core-1.1.3.jar:\
${HOME}/.m2/repository/org/geotools/gt-referencing/14.0/gt-referencing-14.0.jar:\
${HOME}/.m2/repository/com/googlecode/efficient-java-matrix-library/core/0.26/core-0.26.jar:\
${HOME}/.m2/repository/commons-pool/commons-pool/1.5.4/commons-pool-1.5.4.jar:\
${HOME}/.m2/repository/org/geotools/gt-metadata/14.0/gt-metadata-14.0.jar:\
${HOME}/.m2/repository/org/geotools/gt-opengis/14.0/gt-opengis-14.0.jar:\
${HOME}/.m2/repository/net/java/dev/jsr-275/jsr-275/1.0-beta-2/jsr-275-1.0-beta-2.jar:\
${HOME}/.m2/repository/jgridshift/jgridshift/1.0/jgridshift-1.0.jar:\
${HOME}/.m2/repository/net/sf/geographiclib/GeographicLib-Java/1.44/GeographicLib-Java-1.44.jar:\
${HOME}/.m2/repository/org/geotools/gt-shapefile/14.0/gt-shapefile-14.0.jar:\
${HOME}/.m2/repository/org/geotools/gt-data/14.0/gt-data-14.0.jar:\
${HOME}/.m2/repository/org/geotools/gt-epsg-hsql/14.0/gt-epsg-hsql-14.0.jar:\
${HOME}/.m2/repository/org/hsqldb/hsqldb/2.3.0/hsqldb-2.3.0.jar:\
${HOME}/.m2/repository/org/jfree/jfreechart/1.0.19/jfreechart-1.0.19.jar:\
${HOME}/.m2/repository/org/jfree/jcommon/1.0.23/jcommon-1.0.23.jar:\
${HOME}/.m2/repository/com/google/inject/guice/4.1.0/guice-4.1.0.jar:\
${HOME}/.m2/repository/javax/inject/javax.inject/1/javax.inject-1.jar:\
${HOME}/.m2/repository/aopalliance/aopalliance/1.0/aopalliance-1.0.jar:\
${HOME}/.m2/repository/com/google/guava/guava/19.0/guava-19.0.jar:\
${HOME}/.m2/repository/com/google/inject/extensions/guice-multibindings/4.1.0/guice-multibindings-4.1.0.jar:\
${HOME}/.m2/repository/net/sf/trove4j/trove4j/3.0.3/trove4j-3.0.3.jar:\
${HOME}/.m2/repository/org/jvnet/ogc/kml-v_2_2_0/2.2.0/kml-v_2_2_0-2.2.0.jar:\
${HOME}/.m2/repository/org/hisrc/w3c/atom-v_1_0/1.1.0/atom-v_1_0-1.1.0.jar:\
${HOME}/.m2/repository/com/sun/xml/bind/jaxb-core/2.3.0.1/jaxb-core-2.3.0.1.jar:\
${HOME}/.m2/repository/com/sun/xml/bind/jaxb-core/2.3.0.1/jaxb-core-2.3.0.1-sources.jar:\
${HOME}/.m2/repository/com/sun/xml/bind/jaxb-impl/2.3.0.1/jaxb-impl-2.3.0.1-sources.jar:\
${HOME}/.m2/repository/com/sun/xml/bind/jaxb-impl/2.3.0.1/jaxb-impl-2.3.0.1.jar:\
${HOME}/.m2/repository/net/jpountz/lz4/lz4/1.3.0/lz4-1.3.0.jar:\
${HOME}/.m2/repository/com/github/SchweizerischeBundesbahnen/matsim-sbb-extensions/0.10.0/matsim-sbb-extensions-0.10.0.jar:\
${HOME}/.m2/repository/org/matsim/contrib/common/0.11.0-SNAPSHOT/common-0.11.0-20180817.090538-156.jar:\
${HOME}/.m2/repository/org/apache/commons/commons-math/2.2/commons-math-2.2.jar:\
${HOME}/.m2/repository/commons-lang/commons-lang/2.3/commons-lang-2.3.jar:\
${HOME}/.m2/repository/org/matsim/contrib/analysis/0.11.0-SNAPSHOT/analysis-0.11.0-20180817.090538-156.jar:\
${HOME}/.m2/repository/org/matsim/contrib/roadpricing/0.11.0-SNAPSHOT/roadpricing-0.11.0-20180817.090538-156.jar:\
${HOME}/.m2/repository/org/osgeo/proj4j/0.1.0/proj4j-0.1.0.jar:\
${HOME}/.m2/repository/com/googlecode/json-simple/json-simple/1.1.1/json-simple-1.1.1.jar"

# Parameters
edgesz=187
agents=3000000
plansz=200
timestep=1
nsteps=200

function prepare_jvm_opts {
    jvm_opts="$jvm_opts -server"
    jvm_opts="$jvm_opts -XX:-TieredCompilation"
    jvm_opts="$jvm_opts -XX:+AggressiveOpts"
    jvm_opts="$jvm_opts -XX:-UseBiasedLocking"

    jvm_opts="$jvm_opts -Xmx25g"
    jvm_opts="$jvm_opts -Xms25g"

    #jvm_opts="$jvm_opts -enableassertions"
    jvm_opts="$jvm_opts -Xloggc:$nqsim_work/nqsim.jvm"
    jvm_opts="$jvm_opts -XX:+PrintGCDetails"
}

function generation {
    world="$nqsim_work/world"
    generator=ch.ethz.systems.nqsim2.SquareWorldGenerator
    generator_opts="$world $realms $agents $plansz $edgesz"
    time java $jvm_opts -classpath $classpath $generator $generator_opts &> $nqsim_work/generator.log
}

function load_scenario {
    generator=ch.ethz.systems.nqsim2.ScenarioRunner
#    config="/home/rbruno/git/matsim-berlin/scenarios/berlin-v5.1-1pct/input/berlin-v5.1-1pct-1it.config.xml"
    config="/home/rbruno/git/matsim-berlin/scenarios/berlin-v5.1-10pct-1agent/input/berlin-v5.1-10pct-1agent.config.xml"
    world="$nqsim_work/world"
    generator_opts="$world $config"
    time java $jvm_opts -classpath $classpath $generator $generator_opts &> $nqsim_work/generator.log

}

function simulation {
    world="$nqsim_work/world"
    simulator=ch.ethz.systems.nqsim2.WorldSimulator
    simulator_opts="$world $timestep $nsteps"
#    perf stat -e cycles,instructions,cache-references,cache-misses,bus-cycles -a \
    time mpirun -np $realms ${hosts_option} \
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
    load_scenario
#    generation
#    simulation
    echo "Running with $i realms...done!"
done
