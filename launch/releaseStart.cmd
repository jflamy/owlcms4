cd publicresults-heroku
git checkout master
git pull
git merge origin/develop
git commit -a -m "start"
git push origin master
cd ..
cd owlcms-heroku
git checkout master
git pull
git merge origin develop
git commit -a -m "start"
git push origin master
cd ..
git checkout master
git pull
git merge origin/develop
git commit -a -m "start"
git push origin master
