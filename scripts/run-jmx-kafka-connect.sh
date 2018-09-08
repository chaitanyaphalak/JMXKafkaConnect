#!/bin/bash
# set -x
# This script assumes java is already installed on the machine you are running it
# To use this script properties_location and jar_directory have to be set to appropriate paths
# properties_location is the full path location of the config.properties file
# jar_directory is the full path location of the directory in which the JMXKafkaConnect.jar is present
# jar_name is the name of the jar file which is by default JMXKafkaConnect.jar

properties_location=/Users/cphalak/workspace/JMXKafkaConnect/config.properties
jar_directory=/Users/cphalak/workspace/JMXKafkaConnect/out/artifacts/JMXKafkaConnect_jar
jar_name=JMXKafkaConnect.jar
properties_file_name=config.properties

print_msg() {
    datetime=`date "+%Y-%m-%d %H:%M:%S"`
    echo "$datetime: $1"
}

start_jmx_kafka_connect() {
    print_msg "Starting JMXKafkaConnect"
    cd ${jar_directory}
    rm ${properties_file_name}
    cp -rp ${properties_location} ${jar_directory}/
    datetime=`date "+%Y-%m-%d-%H:%M:%S"`
    PID=`ps -ef | grep ${jar_name} | grep -v "grep" | cut -d " " -f 4`
    if [[ -z "$PID" ]]; then
        nohup java -jar ${jar_name} > ${jar_directory}/JMXKafkaConnect-${datetime}.log 2>&1 &
    else
        print_msg "JMXKafkaConnect is already running!!! PID is $PID"
    fi
    print_msg "Finished Starting JMXKafkaConnect"
}

stop_jmx_kafka_connect() {
    print_msg "Stopping JMXKafkaConnect"
    PID=`ps -ef | grep ${jar_name} | grep -v "grep" | cut -d " " -f 4`
    if [[ -z "$PID" ]]; then
        print_msg "JMXKafkaConnect is not running!!!"
    else
        print_msg "Killing $PID"
        kill -9 $PID
    fi
    print_msg "Finished Stopping JMXKafkaConnect"
}

usage() {
cat << EOF
Usage: $0 options

    OPTIONS:
    --help    # Show this message
    --start   # Start JMXKafkaConnect
    --stop    # Stop JMXKafkaConnect
EOF
exit 1
}

for arg in "$@"; do
    shift
    case "$arg" in
        "--help")
            set -- "$@" "-h"
            ;;
        "--start")
            set -- "$@" "-s"
            ;;
        "--stop")
            set -- "$@" "-p"
            ;;
        *)
            set -- "$@" "$arg"
    esac
done

cmd=

while getopts "hsropdlnc:i:" OPTION
do
    case $OPTION in
        h)
            usage
            ;;
        s)
            cmd="start"
            ;;
        p)
            cmd="stop"
            ;;
        *)
            usage
            ;;
    esac
done

if [[ "$cmd" == "start" ]]; then
    start_jmx_kafka_connect
elif [[ "$cmd" == "stop" ]]; then
    stop_jmx_kafka_connect
else
    usage
fi
