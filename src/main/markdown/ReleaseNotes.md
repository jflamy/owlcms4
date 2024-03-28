**Version 48.0 alpha**

> [!CAUTION]
>
> - This is an alpha release, used for validating new features.  *Features may be incomplete or non-functional*.  
> - Alpha releases are **not** normally used in actual competitions.

##### 48.0

- (alpha02) Additional output URL for structured JSON data, to be used for creating video overlays, The same information as publicresults is emitted.  Video production companies can use the source code of publicresults as an example and build a custom web server to receive this information and interface with their video editing software (for example using the APIs exposed by vMix). 
  
- (alpha02) /update endpoint is updated every 15 seconds.  publicresults computes a hashcode before putting the event on its event bus. Individual publicresults user sessions compare the hashcode and ignore duplicate events.  This deals with publicresults being restarted randomly and users joining in at random times.
  
- Clean-up of the JSON information sent to the /update endpoint of publicresults, to support extraction of information for video production
  - in the athlete information, the previous field `goodBadClassName` is now `liftStatus`
  - in the group information, there are now two fields, `groupName` and `groupInfo`.  `groupName` now contains only the group name. `groupInfo` contains what is shown on the second line of the scoreboard (group description + attempts done, etc.) 

- If the Feature Toggle `AthleteCardEntryTotal` is enabled, show the Entry Total in the title of the Athlete Card.
- Jury Sheet labels can now all be translated.

