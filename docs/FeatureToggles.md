Feature Switches (also known as Feature Toggles) are parameters that can be turned on or off to change the application behavior.  They are typically used when a feature is added, before it becomes clear how to control through the regular interface. To access the list, use to the Languages and System Settings button from the "Preparation" page and scroll down.

![040_FeatureToggles](img/SystemSettings/040_FeatureToggles.png)



Feature switches are not case sensitive.  Enter them separated by a comma if you need more than one.

| Feature Switch     | Description                                                  | Normal Way to Activate                                       |
| ------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| localTemplatesOnly | If present, the default templates distributed inside the owlcms binary will not be shown.  Only the templates found in the local folder will be used.  If a .zip file is used to package the local folder and upload it to the program, then only these templates will be shown.<br />This is normally used to create a zip with only the files used in a given federation, potentially renamed in the local language. | This feature can only be activated using the feature switch. |
| oldScoreboards     | Used to use the old scoreboard layouts, for people that have not switched to the new parameterized scoreboards introduced in version 4.31 | This feature can only be activated using the feature switch  |
| sinclairMeets      | Treat the competition as if there is only a single weight category for men and one for women.  Use Sinclair to compute rankings and leaders | Use the checkbox on the Competition parameter page.          |