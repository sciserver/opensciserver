# opensciserver

SciServer is a collaborative environment for server-side analysis with extremely
large datasets. The flagship installation is hosted at JHU at
https://apps.sciserver.org/ - this repository contains the software that powers
that system (and others), so you can install and start working with your
datasets!

For more information about the background of SciServer, see “SciServer: a
Science Platform for Astronomy and Beyond” (Taghizadeh-Popp et al., 2020,
arXiv:2001.08619). If you use SciServer in your work, please cite that paper.

🚧❗ While SciServer is a mature software system, we are in the process of
setting up the open repository for it here. Please feel free to create issues
and/or pull requests, but note that our capacity and guidance on resolving those
will be limited until we complete activities related to setting this repository
up! ❗🚧

## Installation

The SciServer system comprises of several distinct components that communicate
with each-other over HTTP, we support installation on
[Kubernetes](https://kubernetes.io/) via [Helm](https://helm.sh/).

Prerequisites:
* A kubernetes cluster with an ingress controller
* A domain name that points to the ingress controller, and SSL certificates
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
helm -n {namespace} \
  upgrade --install \
  --set prefix={name} \
  --set baseDomain={domain-name} \
  --set logging.api.image.tag=x \
  --set backup.enable=false \
  --set logging.api.replicaCount=0 \
  --set proxy.cidrWhiteList=0.0.0.0/0 \
  --set dev.nopvc=true \
  -f helm/sciserver/password-manifest.yaml \
  {name} helm/build/sciserver-main.tar.gz \
```

replace {namespace}, {name} and {domain-name} Some options above (such as the
logging api image tag) are there due to incompleteness of this repo, or needs
fixing. Once this is installed, the dashboard will be available at
https://{domain-name}/{name}!

🚧❗ More detailed installation instructions coming soon! ❗🚧

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

