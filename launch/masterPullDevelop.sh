#*******************************************************************************
# Copyright (c) 2009-2022 Jean-François Lamy
#
# Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
# License text at https://opensource.org/licenses/NPOSL-3.0
#*******************************************************************************
# merge the develop branch to master prior to stable release build
cd publicresults-heroku
git checkout master
git fetch
git merge origin/develop --no-ff
git commit -a -m "start [skip ci]"
git push origin master
cd ..
cd owlcms-heroku
git checkout master
git fetch
git merge origin/develop --no-ff
git commit -a -m "start [skip ci]"
git push origin master
cd ..
git checkout master
git fetch
git merge origin/develop --no-ff
git commit -a -m "start [skip ci]"
git push origin master
echo Done.  pulled develop into master.
