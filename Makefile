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
DOCKER_BUILD_OPTS=--platform linux/amd64
$(IMAGE_TARGETS):
	cd components/java/$(subst .image,,$@) && docker build $(DOCKER_BUILD_OPTS) -t $(REPO)/$(subst .image,,$@):$(VTAG) .
keystone.image:
	cd components/keystone-image && docker build $(DOCKER_BUILD_OPTS) -t $(REPO)/keystone:$(VTAG) .
dashboard-build.image:
	cd components/ui/dashboard && docker build $(DOCKER_BUILD_OPTS) -t $(REPO)/dashboard-build:$(VTAG) -f Dockerfile-configure .
dashboard.image:
	cd components/ui/dashboard && docker build $(DOCKER_BUILD_OPTS) -t $(REPO)/dashboard:$(VTAG) -f Dockerfile-run .

PUSH_TARGETS=$(addsuffix .push,$(IMAGE_COMPONENTS)) keystone.push dashboard.push dashboard-build.push
%.push: %.image
	docker push $(REPO)/$(subst .push,,$@):$(VTAG)

images: $(IMAGE_TARGETS)

publish-images: $(PUSH_TARGETS)

helm:
	cd helm && mkdir -p build && rm -rf build/sciserver && cp -r sciserver build && cd build/sciserver
	cd helm/build/sciserver && sed -i="" "s%<<<IMAGE_REPO>>>%$(REPO)%" values.yaml && sed -i="" "s%<<<VTAG>>>%$(VTAG)%" values.yaml image-manifest.yaml Chart.yaml
	cd helm/build && COPYFILE_DISABLE=1 tar -czf sciserver-$(VTAG).tar.gz --no-xattrs sciserver