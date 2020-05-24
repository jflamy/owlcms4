git pull
git submodule update --init --recursive --remote
cd publicresults-heroku
git checkout develop
git pull
git merge origin/master
git commit -a -m "develop"
git push origin develop
cd ..
cd owlcms-heroku
git checkout develop
git pull
git merge origin/master
git commit -a -m "develop"
git push origin develop
cd ..
git checkout develop
git pull
git merge origin/master
git commit -a -m "develop"
git push origin develop
