FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-11
ENV JAVA_OPTS="-Dlogback.configurationFile=logback-remote.xml"
ENV APPLICATION_PROFILE="remote"
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]