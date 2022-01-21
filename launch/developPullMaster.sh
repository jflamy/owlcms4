#!/bin/bash

#*******************************************************************************
# Copyright (c) 2009-2022 Jean-François Lamy
#
# Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
# License text at https://opensource.org/licenses/NPOSL-3.0
#*******************************************************************************
# merge the develop branch to master prior to stable release build
cd publicresults-heroku
git checkout develop
git pull
git merge origin/master --no-ff
git commit -a -m "merge master [skip ci]"
git push origin develop
cd ..
cd owlcms-heroku
git checkout develop
git pull
git merge origin/master --no-ff
git commit -a -m "merge master [skip ci]"
git push origin develop
cd ..
git checkout develop
git pull
git merge origin/master --no-ff
git commit -a -m "merge master [skip ci]"
git push origin develop
echo Done.  pulled master into develop.
