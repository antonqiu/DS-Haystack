#!/bin/bash

read -p "please input your andrew id: " andrewId
export andrewId=${andrewId}

for i in 5
do 
	ssh ${andrewId}@unix${i}.andrew.cmu.edu /bin/bash << 'eeooff'
	cd /dev/shm

	# create folder
	echo "create foler <14736asdf> for this test"
	rm -rf 14736asdf > /dev/null
	mkdir 14736asdf
	cd 14736asdf

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
	wget "https://s3.amazonaws.com/14736team/caheServer.tar.gz"
	tar xzf caheServer.tar.gz > /dev/null
	cd cacheSever 
	nohup ./deploy.sh &

eeooff
	echo done!

done


