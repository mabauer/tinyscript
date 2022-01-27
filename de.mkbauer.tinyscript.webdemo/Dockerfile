# Dockerfile for tinyscript

# Spring boot needs Java 8
# FROM java:8
FROM openjdk:11-jre-slim

VOLUME /tmp

# Add fat jar
ADD target/tinyscript-webdemo-0.10.0-SNAPSHOT.jar app.jar
RUN bash -c 'touch /app.jar'

# Run the app using the /dev/urandom-hack to prevent long start-up times
# see: http://www.thezonemanager.com/2015/07/whats-so-special-about-devurandom.html
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]