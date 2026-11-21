# Dockerfile for tinyscript

# Ubuntu 24.04 LTS variant: since 2026-11-16 the plain 25-jre tag is based on
# Ubuntu 26.04, whose image ships pebble with a vulnerable Go runtime
# (CVE-2026-97054); the noble image has no pebble.
FROM eclipse-temurin:25-jre-noble

# Run as an unprivileged user. The container can also run with a read-only
# root filesystem (docker run --read-only); Tomcat only needs a writable /tmp.
RUN useradd --system --uid 10001 --no-create-home --shell /usr/sbin/nologin tinyscript

VOLUME /tmp

# Add fat jar
COPY target/tinyscript-webdemo.jar /app.jar

USER 10001
EXPOSE 8080

# Run the app using the /dev/urandom-hack to prevent long start-up times
# see: http://www.thezonemanager.com/2015/07/whats-so-special-about-devurandom.html
# (CVE-2026-114938, "Java4Shell", is fixed in the base image's JRE 25.0.5.1, so
# the workaround -XX:-UseBASE64Intrinsics is no longer needed.)
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
