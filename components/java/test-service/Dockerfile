FROM eclipse-temurin:17.0.16_8-jre
COPY build/libs/test-service.jar /app.jar
USER 10001:10001
ENTRYPOINT ["java", "-jar", "/app.jar"]
