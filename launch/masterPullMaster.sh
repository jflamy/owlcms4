#!/bin/bash 

#*******************************************************************************
# Copyright (c) 2009-2022 Jean-François Lamy
#
# Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
# License text at https://opensource.org/licenses/NPOSL-3.0
#*******************************************************************************

# merge the remote master branch back into local develop
LOCAL=master
REMOTE=master

git checkout $LOCAL
git pull

cd publicresults-heroku
git checkout $LOCAL
git fetch
git merge origin/$REMOTE --no-ff -m "merge origin/$REMOTE [skip ci]"
git push origin $LOCAL
cd ..
git add publicresults-heroku

cd owlcms-heroku
git checkout $LOCAL
git fetch
git merge origin/$REMOTE --no-ff -m "merge origin/$REMOTE [skip ci]"
git push origin $LOCAL
cd ..
git add owlcms-heroku

git commit -m "sync submodules"
git merge origin/$REMOTE --no-ff -m "merge origin/$REMOTE [skip ci]"
git push origin $LOCAL
echo Done.  pulled $REMOTE into $LOCAL.
read -p "Hit enter to exit" VAR