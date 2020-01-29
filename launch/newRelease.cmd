call mvn "versions:set" "versions:commit" "-DnewVersion=%1%" "-DoldVersion=*" "-DgroupId=*" "-DartifactId=*" 
cd owlcms-publicresults
git commit -a -m "%1%
git pull
git push
cd ..
git commit -a -m "%1%"
git pull
git push
echo Done. %1% %2% %3% %4% %5%
echo -X POST "http://owlcms:owlcms@localhost:8080/job/owlcms/job/develop/buildWithParameters?VERSION=%1%&REPO_OWNER=%2%&O_REPO_NAME=%3%&P_REPO_NAME=%4%&PRERELEASE=%5%"
