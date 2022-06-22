#!/usr/bin/env bash

echo "Logging in to ECR"
unset AWS_ACCESS_KEY_ID
sudo $(aws ecr get-login --region us-east-1 --no-include-email)
