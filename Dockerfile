# syntax=docker/dockerfile:1
FROM eclipse-temurin:25-jre

ENV OWLCMS_CONTROLPANEL=3.0.5

WORKDIR /maven

COPY owlcms-docker/target/docker-context/owlcms.jar /maven/owlcms.jar
COPY owlcms-docker/target/docker-context/classes/logback.xml /maven/classes/logback-test.xml

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-cp", "/maven/classes:/maven/owlcms.jar", "app.owlcms.Main"]