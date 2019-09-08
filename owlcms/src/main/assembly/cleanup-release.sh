#!/bin/bash -x
cd "${project.basedir}"
git push --all
# tag master
git tag -a ${project.version} -m "${project.version}"
git push origin ${project.version} --force
# bring release changes to develop
git checkout develop
git merge master
git push --all
# delete local branch
git branch -D release/${project.version}
