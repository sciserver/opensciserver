#!/bin/bash

OPTS_EXTRA=""
if [[ -n "$SCISERVER_PREFIX" ]]; then
    OPTS_EXTRA="--root-path $SCISERVER_PREFIX"
fi

exec uvicorn sciserverapp.rendersvc:app --workers 20 --host 0.0.0.0 --port 8080 $OPTS_EXTRA
