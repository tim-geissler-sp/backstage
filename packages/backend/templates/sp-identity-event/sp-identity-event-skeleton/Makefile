VERSION ?= integration-SNAPSHOT

docker/login:
	$$(aws ecr get-login --no-include-email --region us-east-1)

docker/build: docker/login
	./gradlew clean build --refresh-dependencies --info -PcustomVersion=${VERSION}
	docker build -t sailpoint/sp-identity-event:$(VERSION) .

docker/push: docker/build
	docker tag sailpoint/sp-identity-event:$(VERSION) 406205545357.dkr.ecr.us-east-1.amazonaws.com/sailpoint/sp-identity-event:$(VERSION)
	docker push 406205545357.dkr.ecr.us-east-1.amazonaws.com/sailpoint/sp-identity-event:$(VERSION)

.PHONY: docker/build docker/push docker/login
