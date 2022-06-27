#!/usr/bin/env bash

echo "Push Docker to ECR"
cd build/docker/
sudo docker build --tag sailpoint/ets:$BUILD_NUMBER .

sudo docker tag sailpoint/ets:$BUILD_NUMBER 406205545357.dkr.ecr.us-east-1.amazonaws.com/sailpoint/ets:$BUILD_NUMBER
sudo docker push 406205545357.dkr.ecr.us-east-1.amazonaws.com/sailpoint/ets:$BUILD_NUMBER
