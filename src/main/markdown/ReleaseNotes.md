



⚠️⚠️⚠️ 
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md)** 
- **User Documentation for the Control Panel is located at [this location](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md)**

<br>

**Change Log**

62.0.0-rc01: nogrid style background back to black after accidental change to blue

62.0.0-beta03: Ensured that F or W is consistently used for female categories according to the default language of the database

62.0.0-beta02: Fixed Import of Coaches and Technical Officials Excel files to correctly interpret translated headers

62.0.0-beta02: Forcing the default language to a language with a country dialect (fr_CA, pt_BR) did not work as expected.

**New in Release 62.0**

62.0.0-beta03: The default language for the database is always used for translating the gender in displayed and exported category codes. In this way, all sessions and all printouts will have either W or F depending on the translation chosen for that language (this does not apply when the code is explicitly gendered)

62.0.0: Experimental capability to produce credentials for Athletes, Coaches and TOs, with pictures/logos/flags.

- There is now a Credentials button in the Pre-Competition documents
- Templates have moved to `templates/credentials`  All the credentials templates will go there.
- See this [folder](https://github.com/jflamy/owlcms4/tree/dev62/owlcms/scripts) in the development repository for [CREDENTIALS_README](https://github.com/jflamy/owlcms4/blob/dev62/owlcms/scripts/CREDENTIALS_README.md) and Python support scripts (see [CROP_README](https://github.com/jflamy/owlcms4/blob/dev62/owlcms/scripts/CROP_README.md))

62.0.0: The download mechanism for all the documents on the Documents page has been completely redone to match the current UI toolkit recommended programming practices.  The Weigh-in page also uses the exact same mechanism.

62.0.0: Ability to enter information about coaches, with name filtering.

62.0.0: Technical Officials now have a role.  There is now a name and role filter on the TO page.

- Overall roles such as Competition Secretary can now be selected
- Officials in the Technical Officials list can be marked as Active; any official assigned to a Session is implicitly Active and does not need to be marked as such.

62.0.0: The eligibility/non-eligibility status of an athlete is now enumerated. The reason for disqualification or non-competition can be selected.

62.0.0: Support of MQTT devices in cloud configurations using websockets (this is *not* needed for local network setups when owlcms runs on a laptop)

- The endpoint used in the URL is /mqtt
  - if the frontend has secure TLS (port 443), the protocol should be wss:
  - for local testing,  you can use normal http ports starting with 8 (80, 8080, etc.)
- Any other port than 443 and those starting with 8 will be assumed to be normal MQTT
- owlcms-firmata version 2.5.0 correctly selects the protocol depending on the port you indicate (indicate 443 for cloud, 1883 for typical local use)

62.0.0: jx:image directive fixed for JXLS3 templates. Current limitations:

- The cell containing the image should not be the first in the row.

- The image should have the same proportions as the cell where it will be shown.  If doing accreditation forms, and using 5/7 image ratios, make the cell and images both have this 5/7 image ratio.


For other recent changes, see [the release repository](https://github.com/owlcms/owlcms4/releases) 
