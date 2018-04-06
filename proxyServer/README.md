# 14736-project3 Proxy Server:

We applied Nginx to deal with the requests forwarding and load balancing. Our main work here is installing Nginx on unix.andrew machine. Please refer check the implementation and configuration details in the "proxyServer" directory.

## Steps:
1. SSH to the unix.andrew machine The list of all the available unix machines can be seen from here [List of unix.andrew machine](http://www.andrew.cmu.edu/user//tdecker/Untitled%20Folder/CMU_Linux_Clusters_Usage/Clusters/gates.html), we can choose from the number 4 :
	* E.g: Choose "unix4.andrew.cmu.edu", command: `ssh <andrewId>@unix4.andrew.cmu.edu`
	* Input the password.
	* Please cd to the private folder for security.

2. **[Download and Install]** Since we can not use sudo under the user mode, we used wget to install all the tools. [Reference](https://docs.nginx.com/nginx/admin-guide/installing-nginx/installing-nginx-open-source/)
	* Download the source files for the stable version from nginx.org:
		`wget http://nginx.org/download/nginx-1.12.1.tar.gz`
		`tar zxf nginx-1.12.1.tar.gz`<br>
		`cd nginx-1.12.1`
	* Install: set the prefix as user's directory [Reference](https://www.jianshu.com/p/d5114a2a2052), for example: 
		`./configure --prefix=/afs/andrew.cmu.edu/usr7/<andrewId>/private/nginx`
		`make`<br>
		`make install`
3. **[Configuration]** Modify the configuration file. Since we can not run in the root mode, we have to change the listening port larger than 1024 instead of 80.	Also, we have to make many other configurations. The configuration file is in this directory. [Reference](https://stackoverflow.com/questions/42329261/running-nginx-as-non-root-user)
4. **[Test and Visit]** Now we can run the nginx with the command `/afs/andrew.cmu.edu/usr7/wurongw/private/nginx/sbin/nginx`, we can check our public IP by `ifconfig` command and the ip is shown in the inet part, then we can check with ip:port to see the default page. 
5. **[Change the path]** Change the path. [Reference](https://unix.stackexchange.com/questions/26047/how-to-correctly-add-a-path-to-path)
	* Modify the bashrc: `vi ~/.bashrc`
	* Set the content as (just an example):
	`export PATH=$PATH:/afs/andrew.cmu.edu/usr7/wurongw/private/nginx/sbin`
	* Apply the modification: `source ~/.bashrc`
	* Now we can simply use "nginx" command instead of "/aws/...../nginx"
6. Some useful commands of Nginx:
	* Start: `nginx`
	* Reload: `nginx –s reload`
	* Stop: `nginx -s stop`
	* Check the configuration file: `nginx -t`

TODO:
proxy forward after web server is done
	