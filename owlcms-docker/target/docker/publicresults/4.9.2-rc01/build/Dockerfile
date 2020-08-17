FROM adoptopenjdk/openjdk11:latest
COPY maven /maven/
ENTRYPOINT ["java","-Djava.net.preferIPv4Stack=true","-Dcom.sun.management.jmxremote.ssl=false","-Dcom.sun.management.jmxremote.authenticate=false","-cp","maven/classes:/maven/publicresults-4.9.2-rc01.jar","app.owlcms.publicresults.Main"]
