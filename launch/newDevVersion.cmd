call mvn "versions:set" "versions:commit" "-DnewVersion=%1" "-DoldVersion=*" "-DgroupId=*" "-DartifactId=*" 
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
call mvn "versions:set" "versions:commit" "-DnewVersion=4.7" "-DoldVersion=*" "-DgroupId=*" "-DartifactId=*" 
cd ..
git commit -a -m "%1"
git pull
git push
echo Done.
