mvn --settings .gitpod/settings.xml -q exec:exec -Dexec.executable=java -Dexec.args="-cp owlcms/target/classes:owlcms/target:shared/target/classes:%classpath app.owlcms.Main"
