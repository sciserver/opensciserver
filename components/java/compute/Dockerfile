FROM tomcat:8.5-jre17

ENV CONTEXT_PATH_NAME compute
COPY build/dist/compute.war /app/compute.war

CMD ["sh", "-c", "cp /app/compute.war /usr/local/tomcat/webapps/${CONTEXT_PATH_NAME}.war && exec catalina.sh run"]
