#!/bin/bash

read -p "please input your andrew id: " andrewId
export andrewId=${andrewId}
# read -p "please input your ssh password: " password
# export andrewPass=${password}

ssh ${andrewId}"@unix4.andrew.cmu.edu" /bin/bash << 'eeooff'
cd /dev/shm

# create folder
echo "create foler <14736asdf> for this test"
rm -rf 14736asdf > /dev/null
mkdir 14736asdf
cd 14736asdf

# install cassandra
echo "wget and install cassandra"
wget http://apache.claz.org/cassandra/3.11.2/apache-cassandra-3.11.2-bin.tar.gz
tar xzf apache-cassandra-3.11.2-bin.tar.gz > /dev/null
mv apache-cassandra-3.11.2 cassandra
wget https://s3.amazonaws.com/14736team/cassandra-dir.yaml
wget https://s3.amazonaws.com/14736team/cassandra-env.sh
mv cassandra-dir.yaml cassandra.yaml
mv cassandra.yaml cassandra/conf/cassandra.yaml
mv cassandra-env.sh cassandra/conf/cassandra-env.sh
echo "run cassandra"
cd cassandra
export CASSANDRA_HOME=`pwd`
nohup ./bin/cassandra &

eeooff
echo done!
