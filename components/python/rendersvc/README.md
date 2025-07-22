# SciServer rendering service web application
Server-side rendering for content on SciServer (e.g. notebooks)

## testing

Use tox:

```
tox
```

## Build

```
docker build -f docker/Dockerfile .
```

If you want to include local builds of any dependencies, the distribution files can be loaded into build/localdeps and
will be installed prior to the module.

## Deployment and Ingress

The entrypoint will pass the root-path option to uvicorn if SCISERVER_PREFIX is set. However, note that uvicorn/fastapi
assume that the reverse proxy strip the prefix before sending, so this must be arranged, e.g. for ingress-nginx:

```
path: /prefix/(.*)
annotations:
  nginx.ingress.kubernetes.io/rewrite-target: /$1
```
