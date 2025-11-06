FROM mcr.microsoft.com/openjdk/jdk:17-ubuntu
EXPOSE 8080
ENV SERVER_PORT=8080
ENV LOG4J_CONFIGURATION_FILE=/etc/log4j2-console.xml
COPY src/main/resources/log4j2-console.xml /etc/
COPY build/libs/fileservice.jar /sciserver-fileservice.jar
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/sciserver-fileservice.jar"]
