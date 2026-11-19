FROM node:26.13.1-trixie as builder

WORKDIR /app

COPY . .

# Node >= 25 images no longer ship yarn/corepack, and only /app is copied
# from the builder, so the runtime stage needs its own yarn for `yarn start`.
RUN apt-get update && \
  apt-get install -y --no-install-recommends git python3 && \
  rm -rf /var/lib/apt/lists/* && \
  npm install -g yarn && \
  yarn install \
  --prefer-offline \
  --frozen-lockfile \
  --non-interactive \
  --production=false && \
  yarn cache clean

FROM node:26.13.1-trixie-slim

WORKDIR /app

RUN npm install -g yarn

COPY --from=builder /app  .

ENV HOST 0.0.0.0
EXPOSE 4000

CMD [ "yarn", "start" ]