#!/bin/bash
# ============ preamble ================== #
set -o errexit #exit when command fails
set -o pipefail #pass along errors within a pipe

prev_dir=$(pwd)
script_dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
cd "${script_dir}"

mpi_jar_path=$(dirname "$(which mpirun)")/../lib/mpi.jar
timestamp=$(date +"%Y-%m-%d-%H_%M")
jackson_core_path="${HOME}/.m2/repository/com/fasterxml/jackson/core"
jackson_dataformat_path="${HOME}/.m2/repository/com/fasterxml/jackson/dataformat"
jackson_module_path="${HOME}/.m2/repository/com/fasterxml/jackson/module"

classpath="\
${script_dir}/target/test-classes:${script_dir}/target/classes:\
${jackson_core_path}/jackson-databind/2.9.5/jackson-databind-2.9.5.jar:\
${jackson_dataformat_path}/jackson-dataformat-xml/2.9.5/jackson-dataformat-xml-2.9.5.jar:\
${jackson_core_path}/jackson-annotations/2.9.5/jackson-annotations-2.9.5.jar:\
${jackson_core_path}/jackson-core/2.9.5/jackson-core-2.9.5.jar:\
${jackson_module_path}/jackson-module-jaxb-annotations/2.9.5/jackson-module-jaxb-annotations-2.9.5.jar:\
${HOME}/.m2/repository/org/codehaus/woodstox/stax2-api/3.1.4/stax2-api-3.1.4.jar:\
${HOME}/.m2/repository/org/codehaus/woodstox/woodstox-core/5.0.3/woodstox-core-5.0.3.jar:\
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
"

echo "============== classpath: ================="
echo $classpath

# "C:\Program Files\Java\jdk-10.0.2\bin\java.exe" -Dvisualvm.id=135543407797434 -Xmx10G -enableassertions "-javaagent:C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2018.2\lib\idea_rt.jar=60775:C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2018.2\bin" -Dfile.encoding=UTF-8 -classpath C:\Users\muellermichel\systems\NQSIM\java\target\test-classes;C:\Users\muellermichel\systems\NQSIM\java\target\classes;C:\Users\muellermichel\systems\NQSIM\openmpi.jar;C:\Users\muellermichel\.m2\repository\com\fasterxml\jackson\core\jackson-databind\2.9.5\jackson-databind-2.9.5.jar;C:\Users\muellermichel\.m2\repository\com\fasterxml\jackson\dataformat\jackson-dataformat-xml\2.9.5\jackson-dataformat-xml-2.9.5.jar;C:\Users\muellermichel\.m2\repository\com\fasterxml\jackson\module\jackson-module-jaxb-annotations\2.9.5\jackson-module-jaxb-annotations-2.9.5.jar;C:\Users\muellermichel\.m2\repository\org\codehaus\woodstox\stax2-api\3.1.4\stax2-api-3.1.4.jar;C:\Users\muellermichel\.m2\repository\com\fasterxml\woodstox\woodstox-core\5.0.3\woodstox-core-5.0.3.jar;C:\Users\muellermichel\.m2\repository\com\fasterxml\jackson\core\jackson-annotations\2.9.5\jackson-annotations-2.9.5.jar;C:\Users\muellermichel\.m2\repository\com\fasterxml\jackson\core\jackson-core\2.9.5\jackson-core-2.9.5.jar;C:\Users\muellermichel\.m2\repository\org\junit\jupiter\junit-jupiter-api\5.3.2\junit-jupiter-api-5.3.2.jar;C:\Users\muellermichel\.m2\repository\org\apiguardian\apiguardian-api\1.0.0\apiguardian-api-1.0.0.jar;C:\Users\muellermichel\.m2\repository\org\opentest4j\opentest4j\1.1.1\opentest4j-1.1.1.jar;C:\Users\muellermichel\.m2\repository\org\junit\platform\junit-platform-commons\1.3.2\junit-platform-commons-1.3.2.jar;C:\Users\muellermichel\.m2\repository\fastutil\fastutil\5.0.9\fastutil-5.0.9.jar;C:\Users\muellermichel\.m2\repository\io\protostuff\protostuff-core\1.5.9\protostuff-core-1.5.9.jar;C:\Users\muellermichel\.m2\repository\io\protostuff\protostuff-api\1.5.9\protostuff-api-1.5.9.jar;C:\Users\muellermichel\.m2\repository\io\protostuff\protostuff-runtime\1.5.9\protostuff-runtime-1.5.9.jar;C:\Users\muellermichel\.m2\repository\io\protostuff\protostuff-collectionschema\1.5.9\protostuff-collectionschema-1.5.9.jar;C:\Users\muellermichel\.m2\repository\commons-io\commons-io\1.3.2\commons-io-1.3.2.jar ch.ethz.systems.matsimtooling.MainTest
java \
	-Xmx10G \
	-enableassertions \
	-classpath "${classpath}" \
	ch.ethz.systems.matsimtooling.MainTest \
| tee tooling_verification_output_${timestamp}_np${np}.txt \
&& :
rv=$?
cd "${prev_dir}"
exit "${rv}"