#!/bin/bash -x
cd "${project.basedir}"
git push --all
git checkout develop
git merge master
git tag -a ${project.version} -m "${project.version}"
git push ${project.version} --force
git push --all
git branch -D release/${project.version}
