FROM openjdk:17-jdk-alpine
EXPOSE 8080
ENV SERVER_PORT=8080
ENV LOG4J_CONFIGURATION_FILE=/etc/log4j2-console.xml
ARG NEXUS_URL
ARG NEXUS_USERNAME
ARG NEXUS_PASSWORD
COPY . .
RUN cp conf-example/log4j2-console.xml /etc/
RUN ./gradlew -I init.gradle build
RUN cp build/libs/sciserver-fileservice-java.jar /sciserver-fileservice.jar
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/sciserver-fileservice.jar"]
