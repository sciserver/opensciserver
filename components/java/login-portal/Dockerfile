FROM tomcat:8.5-jre17

ENV CONTEXT_PATH_NAME login-portal
COPY build/dist/login-portal.war /app/login-portal.war
CMD ["sh", "-c", "cp /app/login-portal.war /usr/local/tomcat/webapps/${CONTEXT_PATH_NAME}.war && exec catalina.sh run"]