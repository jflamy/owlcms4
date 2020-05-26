rem call mvn "versions:set" "versions:commit" "-DnewVersion=%1" "-DoldVersion=4.**" "-DgroupId=*" "-DartifactId=*"
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
cd ..
git commit -a -m "%1"
git pull
git push
echo Done.
