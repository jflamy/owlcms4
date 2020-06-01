git pull
git submodule update --init --recursive --remote
cd publicresults-heroku
git checkout develop
git pull
cd ..
cd owlcms-heroku
git checkout develop
git pull
cd ..
git checkout develop
git pull
git add .
git commit -a -m "sync submodules"
git push
echo Done. synced submodules prior to starting release.

