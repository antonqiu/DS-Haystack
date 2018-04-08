# Cassandra as Object Store

## Configuration

Configuration currently resides in the following files:
- conf/cassandra.yaml
  - All ports except JMX (Storage port: 7999; native_transport_port: 9043)
  - data_file_directories: /dev/shm/cassandra/..
  - commitlog_directory: /dev/shm/cassandra/..
  - saved_caches_directory: /dev/shm/cassandra/..
- conf/cassandra-env.sh
  - JMX port: 7200
  
## Setup
TBA
