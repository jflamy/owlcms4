#*******************************************************************************
# Copyright (c) 2009-2022 Jean-François Lamy
#
# Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
# License text at https://opensource.org/licenses/NPOSL-3.0
#*******************************************************************************
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
git commit -a -m "sync submodules [skip ci]"
git push
echo Done. synced develop submodules.

