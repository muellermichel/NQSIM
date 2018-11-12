#!/bin/bash

classpath="$classpath:target/test-classes"
classpath="$classpath:target/classes"

jvm_opts="$jvm_opts -server"
jvm_opts="$jvm_opts -XX:-TieredCompilation"
jvm_opts="$jvm_opts -XX:+AggressiveOpts"
jvm_opts="$jvm_opts -XX:-UseBiasedLocking"

jvm_opts="$jvm_opts -Xmx10g"
jvm_opts="$jvm_opts -Xms10g"
#jvm_opts="$jvm_opts -XX:MaxNewSize=128m"
#jvm_opts="$jvm_opts -XX:NewSize=128m"

#jvm_opts="$jvm_opts -XX:+AlwaysPreTouch"
#jvm_opts="$jvm_opts -enableassertions"
jvm_opts="$jvm_opts -Xloggc:nqsim2.jvm"
jvm_opts="$jvm_opts -XX:+PrintGCDetails"

java $jvm_opts -classpath $classpath ch.ethz.systems.nqsim2.WorldSimulator | tee nqsim2.txt

cat nqsim2.jvm | grep secs | awk '{ sum += $11; } END {print "GC Time = " sum " secs"} '
