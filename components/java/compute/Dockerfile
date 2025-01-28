FROM openjdk:17-ea-jdk-alpine AS build-env

RUN apk add git

WORKDIR /app

ARG NEXUS_URL
ARG NEXUS_USERNAME
ARG NEXUS_PASSWORD

COPY . /app
RUN ./gradlew -I init.gradle build

FROM tomcat:8.5-jre17

ENV CONTEXT_PATH_NAME compute
COPY --from=build-env /app/dist/compute.war /app/compute.war

CMD ["sh", "-c", "cp /app/compute.war /usr/local/tomcat/webapps/${CONTEXT_PATH_NAME}.war && exec catalina.sh run"]
