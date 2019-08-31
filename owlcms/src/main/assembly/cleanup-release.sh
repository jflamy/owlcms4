#!/bin/bash
cd ${project.basedir}
git push --all
git checkout develop
git merge master
git push --all
git branch -D release/${project.version}