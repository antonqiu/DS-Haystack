# 14736-project3
-Haystacks project for 14736 distributed system

## Description
###Overview
Data objects: image

###Workflow
1. **Client:** There are three kinds of reuqests for operating the image objects: GET, UPLOAD, DELETE; for simplicity, we did not implement any user interface for this project, the requests are sent directly to the reverse proxy server. 
2. **Proxy Server[unix.andrew machine + Nginx]:** The reverse proxy server sends the request to one front-end web server. 
3. **Web Server[unix.andrew machine + Rapidoid(Java)]:** The web servers are purely http web servers.
	* 	the web server asks the Cassandra server for photo information with corresponding photo ids
	*	the web server constructs photo URLs based on the replies from the Cassandra server
4. the web server returns the modified url to the client via the reverse proxy server
5. the client asks the Cache server for the photos
6. Cache server return the photo from cache or from stores.

## Implementation
###Proxy Server:
We applied Nginx to deal with the requests forwarding and load balancing. Our main work here is installing Nginx on unix.andrew machine. Please check the implementation and configuration details in the "proxyServer" directory.

###Web Server:
We used the Rapidoid framework to implement the http web server. Please check the implementation and configuration details in the "webServer" directory.
