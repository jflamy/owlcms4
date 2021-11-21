git pull --recurse-submodules
git add .
git commit -m "sync submodules [skip ci]" publicresults-heroku
git commit -m "sync submodules [skip ci]" owlcms-heroku
(cd publicresults-heroku; git push)
(cd owlcms-heroku; git push)
echo Done. synced develop submodules.
pause

