#!/bin/sh
yarn build
cp ./.next/standalone ./
cp ./.next/standalone/server.js ./
exec node server.js