# ==============================================================================
# PulseRoute Production Dockerfile (Instant Deployment for Railway / Cloud)
# ==============================================================================

FROM tomcat:11.0-jdk21-temurin

LABEL maintainer="PulseRoute Dev Team"
LABEL description="PulseRoute 2.0 Emergency Healthcare Coordination Platform"

# Remove default Tomcat web applications and prepare ROOT folder
RUN rm -rf /usr/local/tomcat/webapps/* && mkdir -p /usr/local/tomcat/webapps/ROOT

# Copy and extract production artifact directly to ROOT directory
COPY pulseroute.war /tmp/pulseroute.war
RUN cd /usr/local/tomcat/webapps/ROOT && jar -xf /tmp/pulseroute.war && rm /tmp/pulseroute.war

EXPOSE 8080

# Dynamically bind Tomcat to $PORT (assigned by Railway/cloud hosts), defaulting to 8080
CMD ["sh", "-c", "sed -i \"s/port=\\\"8080\\\"/port=\\\"${PORT:-8080}\\\"/g\" /usr/local/tomcat/conf/server.xml && exec catalina.sh run"]
