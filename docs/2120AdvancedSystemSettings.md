

## Advanced System Settings

These settings are used when running large competitions, or when running in the cloud.

### Access Control

This section controls who can access the system.

![10](nimg/2120AdvancedSystemSettings/10.png)

The settings are as follows.  In actual practice only the first (Password for Officials) is in common use.

- **Password for Officials**  When doing streaming, it is possible that you want to prevent the people running the video from accessing the technical official's consoles. A password will be required for the various technical official screens
- **Authorized IP addresses for Officials** For the same reason, you may want to instead (or in addition to password) use whitelisting.  Technical officials will be required to come from this network.  If running in the cloud but from a gym, this would be the public IP address shown by https://ip4.me/.
- **IP Addresses Authorized to Access Displays** and **Password for Displays**.  You may want to authorize a video production company to see your displays, likely without setting a password.  The screen password setting is there for symmetry and is unlikely to be used.
- **Backdoor Access IP**  This grants access without requesting a password, to all the technical official screens and displays.  This also is required to [launch a simulated competition](Simulation).

### Public Results Scoreboard

This section controls how owlcms reaches the [Public Scoreboard](PublicScoreboard) application in order to send it updates. The installation of `publicresults` is documented for [fly.io](Fly) and for [Heroku](Heroku).

![20](nimg/2120AdvancedSystemSettings/20.png)

- **publicresults Cloud Application URL**  This is the location of the publicresults application. If running on Heroku, it will be something like https://myclub-results.herokuapp.com if you used `myclub-results` as the name.  When deploying on fly.io this box is empty because a shared secret is used -- a shared secret defines an environment variable which takes precedence.
- **Secret Update Key** the publicresults application is installed, it is configured with a secret key.  owlcms needs to send this secret key to publicresults for its update to be considered valid.  When deploying on fly.io this box is empty because a shared secret is used -- a shared secret defines an environment variable which takes precedence.

### Overriding Styles and Templates

The installation directory on a laptop contains a folder called `local`.  Underneath that folder are found all the files and resources used by the program.  You may edit these files if you need to change translation wording, scoreboard colors, the Excel templates, etc.   

If you want to run the program in the cloud, you will need to perform the changes on a local laptop, and zip the local directory.  See the page on [Uploading Customizations](2125UploadingCustomizations) for details.

