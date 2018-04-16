#!/bin/bash

echo Please enter your andrewid:
read andrewId

# log in every unix* machines and kill all `jobs -l`
for i in 4 5 6 7 8
do
	host="unix$i.andrew.cmu.edu"
	ssh ${andrewId}@$host /bin/bash << 'EOF'
	echo "cleaning up $host";
	pid=$(ps aux | grep changtoq | grep [j]ava | awk '{print $2}')
	if [ -n "$pid" ]
	then
		kill $pid
		echo "killed: pid $pid"
	fi
	echo "..."
EOF
done
