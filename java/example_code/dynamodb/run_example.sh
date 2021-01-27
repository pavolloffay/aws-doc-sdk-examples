#!/bin/bash
if [[ -z $* ]] ; then
    echo 'Supply the name of one of the example classes as an argument.'
    echo 'If there are arguments to the class, put them in quotes after the class name.'
    exit 1
fi
export CLASSPATH=target/aws-dynamodb-examples-1.0.jar
export className=$1
echo "## Running $className..."
shift
echo "## arguments $@..."

# run ./run_example.sh CreateTable HelloTable
export JAVA_TOOL_OPTIONS="-javaagent:${HOME}/Downloads/hypertrace-agent-all.jar"
export OTEL_EXPORTER=jaeger
export HT_SERVICE_NAME=sqs-test

export AWS_DEFAULT_REGION=us-west-2
export AWS_ACCESS_KEY_ID=
export AWS_SECRET_ACCESS_KEY=

mvn exec:java -Dexec.mainClass="aws.example.dynamodb.$className" -Dexec.args="$@" -Dexec.cleanupDaemonThreads=false
