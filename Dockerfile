# ==============================================================================
# PulseRoute Production Multi-Stage Dockerfile
# Stage 1: Compile & Package with Maven & OpenJDK 21
# Stage 2: Run on Apache Tomcat 11 with Jakarta Servlet 6.0
# ==============================================================================

# Stage 1: Build Application WAR
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Copy Maven POM and Source Code
COPY pom.xml .
COPY src ./src

# Compile and package to target/pulseroute.war
RUN mvn clean package -DskipTests

# Stage 2: Runtime Environment
FROM tomcat:11.0-jdk21-temurin

LABEL maintainer="PulseRoute Dev Team"
LABEL description="PulseRoute 2.0 Emergency Healthcare Coordination Platform"

# Remove default Tomcat web applications
RUN rm -rf /usr/local/tomcat/webapps/*

# Deploy compiled artifact as ROOT.war (serves directly at domain root /)
COPY --from=builder /build/target/pulseroute.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080

# Dynamically bind Tomcat to $PORT (assigned by Railway/cloud hosts), defaulting to 8080
CMD ["sh", "-c", "sed -i \"s/port=\\\"8080\\\"/port=\\\"${PORT:-8080}\\\"/g\" /usr/local/tomcat/conf/server.xml && exec catalina.sh run"]
