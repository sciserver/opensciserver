FROM eclipse-temurin:17.0.16_8-jdk
EXPOSE 8080
ENV SERVER_PORT=8080
ENV LOG4J_CONFIGURATION_FILE=/etc/log4j2-console.xml
COPY src/main/resources/log4j2-console.xml /etc/
# Staging area for uploaded CSVs. The app refuses to start if file.upload.basePath
# does not exist, so create the default here; the Helm chart mounts an emptyDir
# over whatever path it is configured with.
RUN mkdir -p /var/tmp/sciquery-uploads
COPY build/libs/sciquery.jar /sciserver-sciquery.jar
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/sciserver-sciquery.jar"]
