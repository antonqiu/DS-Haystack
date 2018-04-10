#!/bin/bash
# after start the server
curl -F 'img=@testImages/test1.png' http://127.0.0.1:8080/upload

curl -F 'img=@testImages/test2.jpg' http://127.0.0.1:8080/upload