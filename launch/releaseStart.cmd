cd publicresults-heroku
git checkout master
git pull
git merge origin/develop
git commit -a -m "%1"
git push
cd ..
cd owlcms4-heroku
git checkout master
git pull
git merge origin/develop
git commit -a -m "%1"
git push
cd ..
git checkout master
git pull
git merge origin/develop
git commit -a -m "%1"
git push
