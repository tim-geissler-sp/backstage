VERSION ?= integration-SNAPSHOT

docker/login:
	$$(aws ecr get-login --no-include-email --region us-east-1)

docker/build: docker/login
	./gradlew clean build --refresh-dependencies --info -PcustomVersion=${VERSION}
	docker build -t sailpoint/ets:$(VERSION) .

docker/push: docker/build
	docker tag sailpoint/ets:$(VERSION) 406205545357.dkr.ecr.us-east-1.amazonaws.com/sailpoint/ets:$(VERSION)
	docker push 406205545357.dkr.ecr.us-east-1.amazonaws.com/sailpoint/ets:$(VERSION)

.PHONY: docker/build docker/push docker/login
