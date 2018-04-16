#!/bin/bash

# read -p "please input your andrew id: " andrewId
# export andrewId=${andrewId}
# read -p "please input your ssh password: " password
# export andrewPass=${password}

fab test
# ssh ${andrewId}"@lemonshark.ics.cs.cmu.edu" /bin/bash << eeooff
# cd /dev/shm

# # create folder
# echo "create foler <14736team> for this test"
# mkdir 14736team
# cd 14736team

# # install redis
# echo "install redis on the target machine"
# mkdir redis
# cd redis
# wget http://download.redis.io/releases/redis-4.0.9.tar.gz > /dev/null
# tar xzf redis-4.0.9.tar.gz > /dev/null
# cd redis-4.0.9
# make > /dev/null
# echo "finish installing redis!"


# eeooff
# echo done!
