FROM eclipse-temurin:17.0.16_8-jdk
COPY build/libs/compm.jar /compm.jar
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/compm.jar"]
