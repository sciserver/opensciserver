FROM tomcat:8.5-jre17
COPY build/dist/racm.war /racm.war
ENV CONTEXT_PATH_NAME racm
CMD ["sh", "-c", "cp /racm.war /usr/local/tomcat/webapps/${CONTEXT_PATH_NAME}.war && exec catalina.sh run"]
