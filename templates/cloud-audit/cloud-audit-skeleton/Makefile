VERSION ?= integration-SNAPSHOT

docker/login:
	$$(aws ecr get-login --no-include-email --region us-east-1)

docker/build: docker/login
	./gradlew clean build installDist --refresh-dependencies --info -PcustomVersion=${VERSION}
	cp -f dockerfiles/aer-server/Dockerfile aer-server/build/install
	cd aer-server/build/install ; docker build -t sailpoint/aer:$(VERSION) .

docker/push: docker/build
	docker tag sailpoint/aer:$(VERSION) 406205545357.dkr.ecr.us-east-1.amazonaws.com/sailpoint/aer:$(VERSION)
	docker push 406205545357.dkr.ecr.us-east-1.amazonaws.com/sailpoint/aer:$(VERSION)

.PHONY: docker/build docker/push docker/login
