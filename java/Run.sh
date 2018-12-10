#!/bin/bash
# ============ preamble ================== #
set -o errexit #exit when command fails
set -o pipefail #pass along errors within a pipe

np=1
if [ "$1" != "" ]; then
	np="$1"
fi

nagents=5000
if [ "$2" != "" ]; then
	nagents="$2"
fi

verbose="false"
if [ "$3" != "" ]; then
	verbose="$3"
fi

simtime="3600"
if [ "$4" != "" ]; then
	simtime="$4"
fi

min_plan_length="200"
if [ "$5" != "" ]; then
	min_plan_length="$5"
fi

hosts_option=""
if [ "$HOSTS" != "" ] && [ -f $HOSTS ]; then
   echo "using ${HOSTS}:"
   cat ${HOSTS}
   hosts_option="--hostfile ${HOSTS}"
fi

prev_dir=$(pwd)
script_dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
cd "${script_dir}"

timestamp=$(date +"%Y-%m-%d-%H_%M")

mpi_jar_path=/usr/local/lib/mpi.jar

classpath="$classpath:${script_dir}/target/test-classes"
classpath="$classpath:${script_dir}/target/classes"
classpath="$classpath:${HOME}/.m2/repository/com/fasterxml/jackson/core/jackson-databind/2.9.5/jackson-databind-2.9.5.jar"
classpath="$classpath:${HOME}/.m2/repository/com/fasterxml/jackson/core/jackson-annotations/2.9.5/jackson-annotations-2.9.5.jar"
classpath="$classpath:${HOME}/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.9.5/jackson-core-2.9.5.jar"
classpath="$classpath:${HOME}/.m2/repository/org/junit/jupiter/junit-jupiter-api/5.3.1/junit-jupiter-api-5.3.1.jar"
classpath="$classpath:${HOME}/.m2/repository/org/apiguardian/apiguardian-api/1.0.0/apiguardian-api-1.0.0.jar"
classpath="$classpath:${HOME}/.m2/repository/org/opentest4j/opentest4j/1.1.1/opentest4j-1.1.1.jar"
classpath="$classpath:${HOME}/.m2/repository/org/junit/platform/junit-platform-commons/1.3.1/junit-platform-commons-1.3.1.jar"
classpath="$classpath:${HOME}/.m2/repository/fastutil/fastutil/5.0.9/fastutil-5.0.9.jar"
classpath="$classpath:${HOME}/.m2/repository/io/protostuff/protostuff-core/1.5.9/protostuff-core-1.5.9.jar"
classpath="$classpath:${HOME}/.m2/repository/io/protostuff/protostuff-api/1.5.9/protostuff-api-1.5.9.jar"
classpath="$classpath:${HOME}/.m2/repository/io/protostuff/protostuff-runtime/1.5.9/protostuff-runtime-1.5.9.jar"
classpath="$classpath:${HOME}/.m2/repository/io/protostuff/protostuff-collectionschema/1.5.9/protostuff-collectionschema-1.5.9.jar"
classpath="$classpath:${HOME}/.m2/repository/commons-io/commons-io/1.3.2/commons-io-1.3.2.jar"
classpath="$classpath:${mpi_jar_path}"

jvm_opts="$jvm_opts -server"
jvm_opts="$jvm_opts -XX:-TieredCompilation"
jvm_opts="$jvm_opts -XX:+AggressiveOpts"
jvm_opts="$jvm_opts -XX:-UseBiasedLocking"

jvm_opts="$jvm_opts -Xmx10g"
jvm_opts="$jvm_opts -Xms10g"
jvm_opts="$jvm_opts -XX:MaxNewSize=128m"
jvm_opts="$jvm_opts -XX:NewSize=128m"

jvm_opts="$jvm_opts -XX:+AlwaysPreTouch"
#jvm_opts="$jvm_opts -enableassertions"
jvm_opts="$jvm_opts -Xloggc:output_${timestamp}_np${np}.jvm"
jvm_opts="$jvm_opts -XX:+PrintGCDetails"

time mpirun \
	-np "${np}" \
	${hosts_option} \
	java \
		$jvm_opts \
		-classpath $classpath \
		ch.ethz.systems.nqsim.ChineseCityTest ${nagents} ${verbose} ${simtime} ${min_plan_length} \
	| tee output_${timestamp}_np${np}.txt \
	&& :
rv=$?

cat output_${timestamp}_np${np}.jvm | grep secs | awk '{ sum += $11; } END {print "GC Time = " sum " secs"} '

cd "${prev_dir}"
exit "${rv}"
