# JMXKafkaConnect
A tool to fetch JMX metrics from kafka connect nodes

Usage:\
java -jar out/artifacts/JMXKafkaConnect_jar/JMXKafkaConnect.jar\
This assumes config.properties file is in the same folder as the jar file

Config:\
#JMXKafkaConnect Properties\
sleep=3\
servers=3\
server0_host=X.X.X.X\
server0_port=X\
server1_host=Y.Y.Y.Y\
server1_port=Y\
server2_host=Z.Z.Z.Z\
server2_port=Z

Config File Parameters:\
sleep	- The duration at which the metrics will be collected regularly in seconds\
servers	- The number of kafka connect nodes from which the metrics are to be collected\
server0_host	- This specifies the IP address/hostname of the first server to get the metrics from\
server0_port	- This specified the port of the first server to get the metrics from\
server1_host	- This specifies the IP address/hostname of the second server to get the metrics from\
server1_port	- This specified the port of the second server to get the metrics from\
And so on...

Metrics:\
The tool includes the following ones:  
1. "kafka.consumer:type=consumer-coordinator-metrics,client-id=*"
2. "kafka.consumer:type=consumer-fetch-manager-metrics,client-id=*"
3. "kafka.connect:type=connector-task-metrics,connector=kafka-connect-splunk,task=*"
4. "kafka.connect:type=task-error-metrics,connector=kafka-connect-splunk,task=*"

The metrics reference is at https://kafka.apache.org/documentation/#monitoring

Splunk Dashboard:\
There is a dashboard for kafka connect errors(primarily) and some other metrics at https://github.com/chaitanyaphalak/JMXKafkaConnect/blob/master/dashboard/splunk_dashboard.xml

Scripts:\
run-jmx-kafka-connect.sh can be used to start and stop JMXKafkaConnect
