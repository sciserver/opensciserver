.PHONY: helm all build java clean publish-images images docs
all: build

docs:
	cd docs && make html epub

build: java

java:
	cd components/java && ./gradlew :racm:modelJar && ./gradlew build

clean:
	cd components/java && ./gradlew clean
	cd docs && make clean
	cd helm && rm -rf build

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

helm:
	cd helm && mkdir -p build && rm -rf build/sciserver && cp -r sciserver build && cd build/sciserver
	cd helm/build/sciserver && sed -i="" "s%<<<IMAGE_REPO>>>%$(REPO)%" values.yaml && sed -i="" "s%<<<VTAG>>>%$(VTAG)%" values.yaml image-manifest.yaml Chart.yaml
	cd helm/build && COPYFILE_DISABLE=1 tar -czf sciserver-$(VTAG).tar.gz --no-xattrs sciserver