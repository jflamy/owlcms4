#!/bin/bash -x
cd "${project.basedir}"
git tag -a ${project.version} -m "${project.version}" -f
cp target/owlcms_setup/owlcms_setup.exe ~/Dropbox/owlcms/owlcms_setup_${project.version}.exe
cp target/owlcms.zip ~/Dropbox/owlcms/owlcms_${project.version}.zip
