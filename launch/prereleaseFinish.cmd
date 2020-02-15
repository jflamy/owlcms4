git pull
git submodule update --init --recursive --remote
cd publicresults-heroku
git checkout develop
git pull
cd ..
cd owlcms4-heroku
git checkout develop
git pull
cd ..
git checkout develop
git pull

