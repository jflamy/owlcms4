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

cd publicresults-heroku
git checkout $LOCAL
git fetch
git merge origin/$REMOTE --no-ff -m "merge $REMOTE [skip ci]"
git push origin $LOCAL
cd ..
cd owlcms-heroku
git checkout $LOCAL
git fetch
git merge origin/$REMOTE --no-ff -m "merge $REMOTE [skip ci]"
git push origin $LOCAL
cd ..
git checkout $LOCAL
git fetch
git merge origin/$REMOTE --no-ff -m "merge $REMOTE [skip ci]"
git push origin $LOCAL
echo Done.  pulled $REMOTE into $LOCAL.