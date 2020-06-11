# merge the develop branch to master prior to stable release build
cd publicresults-heroku
git checkout develop
git pull
git merge origin/master --no-ff
git commit -a -m "merge master"
git push origin develop
cd ..
cd owlcms-heroku
git checkout develop
git pull
git merge origin/master --no-ff
git commit -a -m "merge master"
git push origin develop
cd ..
git checkout develop
git pull
git merge origin/master --no-ff
git commit -a -m "merge master"
git push origin develop
echo Done.  pulled master into develop.
