FROM node:16 as builder

WORKDIR /app

COPY . .

RUN yarn install \
  --prefer-offline \
  --frozen-lockfile \
  --non-interactive \
  --production=false

FROM node:16

WORKDIR /app

COPY --from=builder /app  .

ENV HOST 0.0.0.0
EXPOSE 4000

CMD [ "yarn", "start" ]