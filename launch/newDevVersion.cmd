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
