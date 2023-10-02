Feature Switches (also known as Feature Toggles) are parameters that can be turned on or off to change the application behavior.  They are typically used when a feature is added, before it becomes clear how to control through the regular interface. To access the list, use to the Languages and System Settings button from the "Preparation" page and scroll down.

![040_FeatureToggles](img/SystemSettings/040_FeatureToggles.png)

Feature switches are not case sensitive.  Enter them separated by a comma if you need more than one.

| Feature Switch          | Description                                                  | Normal Way to Activate                                       |
| ----------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| localTemplatesOnly      | If present, the default templates distributed inside the owlcms binary will not be shown.  Only the templates found in the local folder will be used.  If a .zip file is used to package the local folder and upload it to the program, then only these templates will be shown.<br />This is normally used to create a zip with only the files used in a given federation, potentially renamed in the local language. | This feature can be activated on the Languages and Settings page. |
| useCustom2AsSubCategory | If present, the value of the Custom2 field of the athlete is assumed to be A, B, C, D and so on to indicate the sub-category.  If the Custom2 field is empty, the A group is assumed, so only the B... groups need to be filled in.<br />*Will eventually be replaced by an official field.* | Only available as a feature switch.                          |
| shortScoreboardNames    | if present, the normal scoreboards will use the abbreviated first names (like the multiple age group scoreboard does by default) | Only available as a feature switch.                          |