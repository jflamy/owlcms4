call mvn "versions:set" "-DnewVersion=%1" "-DoldVersion=*" "-DgroupId=*" "-DartifactId=*" 
cd publicresults-heroku
git commit -a -m "%1"
git pull
git push
cd ..
cd owlcms-heroku
git commit -a -m "%1"
git pull
git push
cd ..
cd installtools
call mvn "versions:set" "-DnewVersion=4.7" "-DoldVersion=*" "-DgroupId=*" "-DartifactId=*" 
cd ..
git commit -a -m "%1"
git pull
git push
call mvn "versions:commit"
echo Done.
