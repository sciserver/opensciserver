#!/bin/bash -x

REPO=$1
VTAG=$2

mkdir -p build
rm -rf build/sciserver
cp -r sciserver build
cd build/sciserver

sed -i="" "s%<<<IMAGE_REPO>>>%${REPO}%" values.yaml
sed -i="" "s%<<<VTAG>>>%${VTAG}%" values.yaml image-manifest.yaml Chart.yaml

if [[ $VTAG =~ v.* ]]; then
    HELM_CHART_VERSION=${VTAG:1}
else
    HELM_CHART_VERSION=0.0.0-${VTAG}
fi

sed -i="" "s%<<<HELM_CHART_VERSION>>>%${HELM_CHART_VERSION}%" Chart.yaml

rm *=

cd ..
COPYFILE_DISABLE=1 tar -czf sciserver-${VTAG}.tgz --no-xattrs sciserver
