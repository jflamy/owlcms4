## Manual Installation

If the control panel cannot be installed on your computer, you can still run owlcms as follows

#### Install Java

This is only needed once.

- Go to https://adoptium.net/fr/temurin/releases/
- Select version `17 - LTS` and your computer architecture
- Download the file and install.

#### Install owlcms

- Go to https://github.com/owlcms/owlcms4/releases
- In the "Assets" section below the release notes, download the zip file that starts with `owlcms`
- Create a directory anywhere you want on your computer (even on your Desktop)
- Copy the zip file to that directory and unzip it (on Windows, right-click and "Extract all")

#### Run owlcms

- Start a command line in the directory where you have extracted the files
  - On Windows 10 or 11:
    - Use the File Explorer (the "Folder" icon in your toolbar) to go to the directory where you extracted the files
    - Click in the area at the top where the location is shown and type `cmd`
- Type the following command
  - ```java -jar owlcms.jar```