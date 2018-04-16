#!/bin/bash

read -p "please input your andrew id: " andrewId
export andrewId=${andrewId}
# read -p "please input your ssh password: " password
# export andrewPass=${password}

ssh ${andrewId}"@unix7.andrew.cmu.edu" /bin/bash << eeooff
cd /dev/shm

# create folder
echo "create foler <14736team> for this test"
mkdir 14736team
cd 14736team

# install cassandra
echo "wget and install cassandra"
wget http://apache.claz.org/cassandra/3.11.2/apache-cassandra-3.11.2-bin.tar.gz
tar xzf apache-cassandra-3.11.2-bin.tar.gz > /dev/null
mv apache-cassandra-3.11.2 cassandra
wget https://s3.amazonaws.com/14736team/cassandra-dir.yaml
mv cassandra-dir.yaml cassandra.yaml
mv cassandra.yaml cassandra/conf/cassandra.yaml
echo "run cassandra"
cd cassandra
bin/cassandra -f

eeooff
echo done!