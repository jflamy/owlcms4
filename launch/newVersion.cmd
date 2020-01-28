echo %1%
call mvn "versions:set" "versions:commit" "-DnewVersion=%1%" "-DoldVersion=*" "-DgroupId=*" "-DartifactId=*" 
cd owlcms-publicresults
git commit -a -m "%1%
git pull
git push
cd ..
git commit -a -m "%1%
git pull
git push
echo Done.
curl -d "VERSION=%1%" -H "Content-Type: application/x-www-form-urlencoded" -X POST http://owlcms:owlcms@localhost:8080/job/build%%20owlcms/job/develop/buildWithParameters
rem curl -X POST http://owlcms:owlcms@localhost:8080/job/build%%20owlcms/job/develop/build
