#!/bin/bash
# ============ preamble ================== #
set -o errexit #exit when command fails
set -o pipefail #pass along errors within a pipe

hosts_option=""
if [ "$HOSTS" != "" ] && [ -f $HOSTS ]; then
   echo "using ${HOSTS}:"
   cat ${HOSTS}
   hosts_option="--hostfile ${HOSTS}"
fi

prev_dir=$(pwd)
script_dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
cd "${script_dir}"

mpi_jar_path=$(dirname "$(which mpirun)")/../lib/mpi.jar
timestamp=$(date +"%Y-%m-%d-%H_%M")
jackson_core_path="${HOME}/.m2/repository/com/fasterxml/jackson/core"
jackson_dataformat_path="${HOME}/.m2/repository/com/fasterxml/jackson/dataformat"
jackson_module_path="${HOME}/.m2/repository/com/fasterxml/jackson/module"
mpirun \
	-np "${np}" \
	${hosts_option} \
	java \
		-Xmx20G \
		-enableassertions \
		-classpath "\
${script_dir}/target/test-classes:${script_dir}/target/classes:\
${jackson_core_path}/jackson-databind/2.9.5/jackson-databind-2.9.5.jar:\
${jackson_dataformat_path}/jackson-dataformat-xml/2.9.5/jackson-dataformat-xml-2.9.5.jar:\
${jackson_core_path}/jackson-annotations/2.9.5/jackson-annotations-2.9.5.jar:\
${jackson_core_path}/jackson-core/2.9.5/jackson-core-2.9.5.jar:\
${jackson_module_path}/jackson-module-jaxb-annotations/2.9.5/jackson-module-jaxb-annotations-2.9.5.jar\
${HOME}/.m2/repository/org/codehaus/woodstox/stax2-api/3.1.4/stax2-api-3.1.4.jar\
${HOME}/.m2/repository/org/codehaus/woodstox/woodstox-core/5.0.3/woodstox-core-5.0.3.jar\
${HOME}/.m2/repository/org/junit/jupiter/junit-jupiter-api/5.3.1/junit-jupiter-api-5.3.1.jar:\
${HOME}/.m2/repository/org/apiguardian/apiguardian-api/1.0.0/apiguardian-api-1.0.0.jar:\
${HOME}/.m2/repository/org/opentest4j/opentest4j/1.1.1/opentest4j-1.1.1.jar:\
${HOME}/.m2/repository/org/junit/platform/junit-platform-commons/1.3.1/junit-platform-commons-1.3.1.jar:\
${HOME}/.m2/repository/fastutil/fastutil/5.0.9/fastutil-5.0.9.jar:\
${HOME}/.m2/repository/io/protostuff/protostuff-core/1.5.9/protostuff-core-1.5.9.jar:\
${HOME}/.m2/repository/io/protostuff/protostuff-api/1.5.9/protostuff-api-1.5.9.jar:\
${HOME}/.m2/repository/io/protostuff/protostuff-runtime/1.5.9/protostuff-runtime-1.5.9.jar:\
${HOME}/.m2/repository/io/protostuff/protostuff-collectionschema/1.5.9/protostuff-collectionschema-1.5.9.jar:\
${HOME}/.m2/repository/commons-io/commons-io/1.3.2/commons-io-1.3.2.jar:${mpi_jar_path}\
" \
		ch.ethz.systems.matsimtooling.MainTest \
	| tee tooling_verification_output_${timestamp}_np${np}.txt \
	&& :
rv=$?
cd "${prev_dir}"
exit "${rv}"

 ch.ethz.systems.matsimtooling.MainTest
