git pull
git submodule update --init --recursive --remote --merge
git commit -m "sync submodules [skip ci]" publicresults-heroku
git commit -m "sync submodules [skip ci]" owlcms-heroku
git push
echo Done. synced develop submodules.
pause

