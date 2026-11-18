# Dockerfile for tinyscript

FROM eclipse-temurin:21-jre

VOLUME /tmp

# Add fat jar
ADD target/tinyscript-webdemo.jar app.jar
RUN bash -c 'touch /app.jar'

# Run the app using the /dev/urandom-hack to prevent long start-up times
# see: http://www.thezonemanager.com/2015/07/whats-so-special-about-devurandom.html
# -XX:-UseBASE64Intrinsics mitigates CVE-2026-114938 ("Java4Shell"): heap overflow
# in the AVX-512 VBMI Base64 decoder intrinsic (x86-64). Remove once the base
# image ships a fixed JRE (21.0.13.1 or later).
ENTRYPOINT ["java","-XX:-UseBASE64Intrinsics","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]