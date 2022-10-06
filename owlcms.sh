cd owlcms
mvn -s ../.gitpod/settings.xml dependency:build-classpath -Dmdep.outputFile=cp.txt
cd ..
java -cp "owlcms/target/classes:owlcms/target:shared/target/classes:`cat owlcms/cp.txt`" app.owlcms.Main
