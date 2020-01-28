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
wsl curl -X POST "http://owlcms:owlcms@localhost:8080/job/build%%20owlcms/job/develop/buildWithParameter/" --data-urlencode json='{"parameter": [{"name":"VERSION", "value":"%1%"}]}'

rem curl -X POST http://owlcms:owlcms@localhost:8080/job/build%%20owlcms/job/develop/build
