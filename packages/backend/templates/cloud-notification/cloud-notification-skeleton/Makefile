VERSION ?= integration-SNAPSHOT

docker/login:
	$$(aws ecr get-login --no-include-email --region us-east-1)
	#aws ecr get-login-password | docker login --username AWS --password-stdin 406205545357.dkr.ecr.us-east-1.amazonaws.com

docker/build: docker/login
	./gradlew clean build installDist --refresh-dependencies --info -PcustomVersion=${VERSION}
	docker build -t sailpoint/hermes:$(VERSION) .

docker/push: docker/build
	docker tag sailpoint/hermes:$(VERSION) 406205545357.dkr.ecr.us-east-1.amazonaws.com/sailpoint/hermes:$(VERSION)
	docker push 406205545357.dkr.ecr.us-east-1.amazonaws.com/sailpoint/hermes:$(VERSION)

.PHONY: docker/build docker/push docker/login
