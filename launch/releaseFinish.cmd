git pull
git submodule update --init --recursive --remote
cd publicresults-heroku
git checkout develop
git pull
git merge origin/master
git commit -a -m "%1"
git push
cd ..
cd owlcms4-heroku
git checkout develop
git pull
git merge origin/master
git commit -a -m "%1"
git push
cd ..
git checkout develop
git pull
git merge origin/master
git commit -a -m "%1"
git push
