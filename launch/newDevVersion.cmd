echo %1%
cd publicresults-heroku
git pull
cd ..
git pull
call mvn "versions:set" "versions:commit" "-DnewVersion=%1%" "-DoldVersion=*" "-DgroupId=*" "-DartifactId=*" 
cd publicresults-heroku
git commit -a -m "%1%
git push
cd ..
git commit -a -m "%1%
git push
echo Done.
