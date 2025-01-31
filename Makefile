all: build

build: java

java:
	cd components/java && ./gradlew :racm:modelJar && ./gradlew build

clean:
	cd components/java && ./gradlew clean

VTAG=$(shell git describe --tags --always --dirty)
REPO=sciserver
images: build
	cd components/java/fileservice && docker build -t $(REPO)/fileservice:$(VTAG) .

publish-images: images
	docker push $(REPO)/fileservice:$(VTAG)