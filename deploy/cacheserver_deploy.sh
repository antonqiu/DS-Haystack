#!/bin/bash

read -p "please input your andrew id: " andrewId
export andrewId=${andrewId}

for i in 5
do 
	ssh ${andrewId}@unix${i}.andrew.cmu.edu /bin/bash << 'eeooff'
	cd /dev/shm

	# create folder
	echo "create foler <14736anton> for this test"
	rm -rf 14736anton > /dev/null
	mkdir 14736anton
	cd 14736anton

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
	echo "download the file package and uncompress"
	cd ../../
	wget "https://s3.amazonaws.com/14736team/cacheServer.tar.gz"
	tar xzf cacheServer.tar.gz > /dev/null
	cd cacheServer 
	nohup ./deploy.sh > /dev/null &

eeooff
	echo done!

done


