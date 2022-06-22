VERSION ?= dev
DIR := $(shell pwd -L)

all: test

run:
	go run ./cmd/sp-scheduler

lint:
	docker pull golangci/golangci-lint:latest
	docker run \
	--mount src="$(DIR)",\
	target="$(DIR)",\
	type="bind" \
	-w "$(DIR)" \
	golangci/golangci-lint \
	golangci-lint run --config .golangci.yaml ./... -v

clean:
	go clean ./cmd/sp-scheduler
	rm -f sp-scheduler

test: mocks
	go test -count=1 ./...

integration:
	docker-compose -f docker-compose.test.yml up --build --abort-on-container-exit
	docker-compose -f docker-compose.test.yml down --volumes

integration_local:
	-docker-compose -f docker-compose.test.yml up --build --abort-on-container-exit
	docker-compose -f docker-compose.test.yml down --volumes

postgres:
	docker-compose -f docker-compose.test.yml run -p "5432:5432" postgres

docker/login:
	$$(aws ecr get-login --no-include-email --region us-east-1)
	#aws ecr get-login-password | docker login --username AWS --password-stdin 406205545357.dkr.ecr.us-east-1.amazonaws.com

docker/build: docker/login
	docker build -t sailpoint/sp-scheduler:$(VERSION) --build-arg BUILDER_IMAGE=406205545357.dkr.ecr.us-east-1.amazonaws.com/mirror/golang:1.16-alpine3.13 \
	--build-arg RUNTIME_IMAGE=406205545357.dkr.ecr.us-east-1.amazonaws.com/mirror/alpine:3.13 .

docker/push: docker/build
	docker tag sailpoint/sp-scheduler:$(VERSION) 406205545357.dkr.ecr.us-east-1.amazonaws.com/sailpoint/sp-scheduler:$(VERSION)
	docker push 406205545357.dkr.ecr.us-east-1.amazonaws.com/sailpoint/sp-scheduler:$(VERSION)

mocks:
	# Ref: https://github.com/golang/mock
	mockgen -source=internal/sp/scheduler/scheduler.go -destination=internal/sp/scheduler/mocks/mock_action_repo.go -package=mocks
	# --build_flags required as workaround for mockgen reflect mode on vendored dependencies
	mockgen --build_flags=--mod=mod -destination internal/sp/scheduler/mocks/mock_atlas_publisher.go -package=mocks github.com/sailpoint/atlas-go/atlas/event Publisher

licenses:
	go install github.com/sailpoint/cloud-tools/sp-license@sp-license/v3
	sp-license > license-report.csv

.PHONY: clean test run docker/build docker/push docker/login mocks
