# opensciserver

This repo will contain all sciserver components such that they can be built and
vended as a single-versioned unit. This repo is currently incomplete.

## Installation

Prerequisites:
* A kubernetes cluster with an ingress controller
* helm installed
* This repository checked out (until we can vend the built helm charts to an
  appropriate stable web location)

Mainline commits, pull-requests and tagged versions have built artifacts
available on the github container registry with tags matching the version. to
install we need to build the helm chart reference the appropriate repo location
and version of built images:

```sh
# within this repo
make helm REPO=ghcr.io/sciserver/opensciserver VTAG=main
```

The above will build the helm chart where images are located in the official
github container registry for opensciserver and we want those built from the
latest commit to main. This will place the zipped chart under
`helm/build/sciserver-{VTAG}.tar.gz`, which can be directly specified to helm as
the chart source (see below). To reference a pull request, simply replace `VTAG`
as appropriate (`pr-` plus the pull request number):

```sh
make helm REPO=ghcr.io/sciserver/opensciserver VTAG=pr-6
```

Once the charts are built, we can install using helm. For a development
installation with no pre-existing requirements and no persistent data, we can
use options as below:

```sh
helm -n sciserver \
  upgrade --install \
  --set prefix={name} \
  --set baseDomain={domain-name} \
  --set logging.api.image.tag=x --set backup.enable=false \
  --set web.replicaCount=0 --set graphql.replicaCount=0 \
  --set rendersvc.replicaCount=0 --set logging.api.replicaCount=0 \
  --set proxy.cidrWhiteList=0.0.0.0/0 \
  --set dev.nopvc=true \
  -f helm/sciserver/password-manifest.yaml \
  {name} helm/build/sciserver-main.tar.gz \
```

Some options above (such as the logging api image tag and the 0 replica count
specs) are there due to incompleteness of this repo, or needs fixing.

## Building

We coordinate the multi-component builds with a make file at root, which
contains commands for building individual components and the system as a whole.
The github ci action demonstrates what is needed to make a complete build and
upload artifacts, briefly:

```sh
# make the java components
make java

# make all images and their dependencies. The default naming scheme is
# sciserver/component:version where the version is related to the git commit at
# HEAD.
make images
# explicitly set a tag
make images VTAG={tag}

# make documentation
make docs
```

And so on.

