#!/bin/bash

read -p "please input your andrew id: " andrewId
export andrewId=${andrewId}

for ((count=4; count < 5; count++)){
	do 
		ssh ${andrewId}"@unix"${count}"lemonshark.ics.cs.cmu.edu" /bin/bash << eeooff
		cd /dev/shm

		# create folder
		echo "create foler <14736team> for this test"
		mkdir 14736team_$count

		# cd 14736team_
		eeooff
		echo done!
}


# install redis
# echo "install redis on the target machine"
# mkdir redis
# cd redis
# wget http://download.redis.io/releases/redis-4.0.9.tar.gz > /dev/null
# tar xzf redis-4.0.9.tar.gz > /dev/null
# cd redis-4.0.9
# make > /dev/null
# echo "finish installing redis!"

# # run redis on port 6380
# echo "run redis on the port 6380 background"
# src/redis-server --port 6380 --daemonize yes

# # wget the web server package and uncompress
# echo "downlaod the file package and uncompress"
# cd ../../
# wget "https://s3.amazonaws.com/14736team/webserver.tar.gz"
# tar xzf webserver.tar.gz > /dev/null

# # run server
# echo "run the server"
# cd webServer

