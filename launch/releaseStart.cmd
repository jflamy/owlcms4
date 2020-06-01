rem merge the develop branch to master prior to stable release build
cd publicresults-heroku
git checkout master
git pull
git merge origin/develop --no-ff
git commit -a -m "start"
git push origin master
cd ..
cd owlcms-heroku
git checkout master
git pull
git merge origin/develop --no-ff
git commit -a -m "start"
git push origin master
cd ..
git checkout master
git pull
git merge origin/develop --no-ff
git commit -a -m "start"
git push origin master
echo Done.  pulled develop into master.
