set LOCAL_TOKEN=11b65aee741aa15d84be95b8d4fac9e27a
call mvn "versions:set" "versions:commit" "-DnewVersion=%1" "-DoldVersion=*" "-DgroupId=*" "-DartifactId=*" 
cd publicresults-heroku
git commit -a -m "%1"
git pull
git push
cd ..
cd owlcms4-heroku
git commit -a -m "%1"
git pull
git push
cd ..
git commit -a -m "%1"
git pull
git push
git tag "build_%1"
echo Launching Build VERSION=%1 REPO_OWNER=%2 O_REPO_NAME=%3 P_REPO_NAME=%4 PRERELEASE=%5 BRANCH=%6
curl --user owlcms:%LOCAL_TOKEN% -X POST "http://owlcms:owlcms@localhost:8080/job/owlcms/job/%6/buildWithParameters?VERSION=%1&REPO_OWNER=%2&O_REPO_NAME=%3&P_REPO_NAME=%4&PRERELEASE=%5&BRANCH=%6&H_REPO_NAME=%7"
