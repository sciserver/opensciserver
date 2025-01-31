all: build

build: java

java:
	cd components/java && ./gradlew :racm:modelJar && ./gradlew build

clean:
	cd components/java && ./gradlew clean

VTAG=$(shell git describe --tags --always --dirty)
REPO=sciserver
IMAGE_COMPONENTS=fileservice compute racm login-portal
IMAGE_TARGETS=$(addsuffix .image,$(IMAGE_COMPONENTS))
$(IMAGE_TARGETS):
	cd components/java/$(subst .image,,$@) && docker build -t $(REPO)/$(subst .image,,$@):$(VTAG) .

PUSH_TARGETS=$(addsuffix .push,$(IMAGE_COMPONENTS))
%.push: %.image
	docker push $(REPO)/$(subst .push,,$@):$(VTAG)

images: $(IMAGE_TARGETS)

publish-images: $(PUSH_TARGETS)
