FROM ghcr.io/navikt/baseimages/temurin:21
ENV JAVA_OPTS="-Dlogback.configurationFile=logback-remote.xml"
ENV APPLICATION_PROFILE="remote"
COPY build/libs/*.jar app.jar
