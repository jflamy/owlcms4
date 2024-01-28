# syntax=docker/dockerfile:1
#Stage 1
# initialize build and set base image for first stage
FROM maven:3.8.8-eclipse-temurin-17 as stage1
# speed up Maven JVM a bit
ENV MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
ENV DEBIAN_FRONTEND=noninteractive
RUN apt update && apt install -y gettext-base && rm -rf /var/lib/apt/lists/*
# set working directory
WORKDIR /app
# copy just pom.xml
COPY pom.xml .
# go-offline using the pom.xml
RUN mvn dependency:go-offline package -P production
# copy your other files
COPY ./src ./src
COPY ./frontend ./frontend

# compile the source code and package it in a jar file
RUN mvn clean package -P production -Dmaven.test.skip=true

FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

COPY --from=stage1 /app/target/fly-manager.jar /app

EXPOSE 8080
ENTRYPOINT ["/opt/java/openjdk/bin/java", "-jar", "owlcms.jar"]