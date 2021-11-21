git pull --recurse-submodules

cd owlcms-heroku
git checkout develop
git merge
git commit -m "sync submodules [skip ci]" .
git push

cd ..\publicresults-heroku
git checkout develop
git merge
git commit -m "sync submodules [skip ci]" .
git push

cd ..
git commit -m "sync submodules [skip ci]" .
git push
echo Done. synced develop submodules.


