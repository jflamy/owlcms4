



⚠️⚠️⚠️ 
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md)** 
- **User Documentation for the Control Panel is located at [this location](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md)**

<br>

**New in Release 62.0**

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
