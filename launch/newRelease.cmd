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
echo %REPO_OWNER%
echo curl -X POST "http://owlcms:owlcms@localhost:8080/job/owlcms/job/develop/buildWithParameters?VERSION=%1%&REPO_OWNER=%%REPO_OWNER%%&O_REPO_NAME=%O_REPO_NAME%&P_REPO_NAME=%P_REPO_NAME%"
