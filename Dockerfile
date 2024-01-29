# syntax=docker/dockerfile:1
#Stage 1
# initialize build and set base image for first stage
FROM maven:3.8.8-eclipse-temurin-17 as stage1
# speed up Maven JVM a bit
ENV MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
ENV DEBIAN_FRONTEND=noninteractive

# set working directory
WORKDIR /app
# copy just pom.xml
COPY pom.xml .
# copy the subdirectories
COPY ./src ./src
COPY owlcms/pom.xml owlcms/
COPY ./owlcms/src ./owlcms/src
COPY ./owlcms/frontend ./owlcms/frontend
COPY shared/pom.xml shared/
COPY ./shared/src ./shared/src
COPY ./publicresults/pom.xml ./publicresults/
COPY ./publicresults/src ./publicresults/src
COPY ./owlcms-docker/pom.xml ./owlcms-docker/
COPY ./owlcms-docker/src ./owlcms-docker/src
COPY ./owlcms-windows/pom.xml ./owlcms-windows/
COPY ./owlcms-windows/src ./owlcms-windows/src
COPY ./publicresults-windows/pom.xml ./publicresults-windows/
COPY ./publicresults-windows/src ./publicresults-windows/src
COPY ./playwright/pom.xml ./playwright/
COPY ./playwright/src ./playwright/src
COPY ./installtools/pom.xml ./installtools/

# go-offline using the pom.xml
RUN mvn dependency:go-offline package -P production -am -pl owlcms -Dmaven.test.skip=true

# compile the source code and package it in a jar file
RUN mvn clean package -P production -am -pl owlcms -Dmaven.test.skip=true

FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

COPY --from=stage1 /app/owlcms/target/owlcms/owlcms.jar /app

EXPOSE 8080
ENTRYPOINT ["/opt/java/openjdk/bin/java", "-jar", "owlcms.jar"]