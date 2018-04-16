#!/bin/bash

read -p "please input your andrew id: " andrewId
export andrewId=${andrewId}
read -p "please input your ssh password: " password
export andrewPass=${password}

ssh ${andrewId}"@lemonshark.ics.cs.cmu.edu" /bin/bash << eeooff
cd /dev/shm

# create folder
echo "create foler <14736team> for this test"
mkdir 14736team
cd 14736team

# install redis
echo "install redis on the target machine"
mkdir redis
cd redis
wget http://download.redis.io/releases/redis-4.0.9.tar.gz > /dev/null
tar xzf redis-4.0.9.tar.gz > /dev/null
cd redis-4.0.9
make > /dev/null
echo "finish installing redis!"

# run redis on port 6380
echo "run redis on the port 6380 background"
src/redis-server --port 6380 --daemonize yes

# wget the web server package and uncompress
echo "downlaod the file package and uncompress"
cd ../../
wget "https://s3.amazonaws.com/14736team/14736-project.tar.gz"
tar xzf 14736-project.tar.gz > /dev/null

# install maven
echo "install maven on target machine"
mkdir maven
cd maven
wget http://www.trieuvan.com/apache/maven/maven-3/3.5.3/binaries/apache-maven-3.5.3-bin.tar.gz
tar xzvf apache-maven-3.5.3-bin.tar.gz > /dev/null

cd webServer



eeooff
echo done!
