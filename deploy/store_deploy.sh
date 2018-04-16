#!/bin/bash

read -p "please input your andrew id: " andrewId
export andrewId=${andrewId}
# read -p "please input your ssh password: " password
# export andrewPass=${password}

for i in 7 8
do 
	ssh ${andrewId}@unix${i}.andrew.cmu.edu /bin/bash << 'eeooff'
	cd /dev/shm

	# create folder
	echo "create foler <14736team> for this test"
	rm -rf 14736asdf
	mkdir 14736asdf
	cd 14736asdf

	# install cassandra
	echo "wget and install cassandra"
	wget http://apache.claz.org/cassandra/3.11.2/apache-cassandra-3.11.2-bin.tar.gz
	tar xzf apache-cassandra-3.11.2-bin.tar.gz > /dev/null
	mv apache-cassandra-3.11.2 cassandra
	wget https://s3.amazonaws.com/14736team/cassandra-store.yaml
	mv cassandra-store.yaml cassandra.yaml
	mv cassandra.yaml cassandra/conf/cassandra.yaml
	wget https://s3.amazonaws.com/14736team/cassandra-env.sh
	mv cassandra-env.sh cassandra/conf/cassandra-env.sh
	echo "run cassandra"
	cd cassandra
	export CASSANDRA_HOME=`pwd`
	nohup ./bin/cassandra &
eeooff
	echo done!
done