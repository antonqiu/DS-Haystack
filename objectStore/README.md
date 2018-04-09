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
  
## Deployment
1. Copy 'cassandra' to home directory. ~/private/cassandra, for example.

2. Create data directroy in local storage if neccessary.
```
mkdir /dev/shm/cassandra
```
3. Source the bash configuration file or restart the session.
```
. ~/.bash_profile
```
4. Update ~/.bash_profile.
```
export CASSANDRA_HOME=~/private/cassandra
export PATH=$PATH:$CASSANDRA_HOME/bin
```
5. Run Cassandra in foreground.
```
cassandra -f
```
