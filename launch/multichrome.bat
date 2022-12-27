set SITE=http://localhost:8080
set PLATFORM=A

set CHROME=C:\Program Files\Google\Chrome\Application\chrome.exe
set EDGE=C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe

rem scaling factor controls is applied to zoom, windows size and window position
set commonoptions= --force-device-scale-factor=0.85 --new-window --chrome-frame

rem in order to have independent chrome instances, we need to create individual user directories.
rem we use the RANDOM variable to create new ones on every run
rem to avoid accumulation, we remove files from previous runs
rd /s /q "%tmp%\owlcms-tests"

start "" "%CHROME%" --app="%SITE%/lifting/announcer?fop=%PLATFORM%" --window-size=920,565 --window-position=-5,0 %commonoptions% --user-data-dir=%tmp%\owlcms-tests\%RANDOM%
start "" "%CHROME%" --app="%SITE%/lifting/jury?fop=%PLATFORM%" --window-size=920,650 --window-position=-5,560 %commonoptions% --user-data-dir=%tmp%\owlcms-tests\%RANDOM%

start "" "%CHROME%" --app="%SITE%/lifting/timekeeper?fop=%PLATFORM%" --window-size=640,398 --window-position=1200,358 %commonoptions% --user-data-dir=%tmp%\owlcms-tests\%RANDOM% --force-device-scale-factor=0.64

start "" "%CHROME%" --app="%SITE%/displays/attemptBoard?fop=%PLATFORM%" --window-size=500,300 --window-position=1375,270 %commonoptions% --user-data-dir=%tmp%\owlcms-tests\%RANDOM% 
start "" "%CHROME%" --app="%SITE%/displays/athleteFacingDecision?fop=%PLATFORM%" --window-size=400,300 --window-position=1865,270 %commonoptions% --user-data-dir=%tmp%\owlcms-tests\%RANDOM%

start "" "%CHROME%" --app="%SITE%/ref?fop=%PLATFORM%&num=1" --window-size=400,275 --window-position=905,1 %commonoptions% --user-data-dir=%tmp%\owlcms-tests\%RANDOM%  
start "" "%CHROME%" --app="%SITE%/ref?fop=%PLATFORM%&num=2" --window-size=400,275 --window-position=1295,1 %commonoptions% --user-data-dir=%tmp%\owlcms-tests\%RANDOM%  
start "" "%CHROME%" --app="%SITE%/ref?fop=%PLATFORM%&num=3" --window-size=400,275 --window-position=1685,1 %commonoptions% --user-data-dir=%tmp%\owlcms-tests\%RANDOM%

start "" "%CHROME%" --app="%SITE%/displays/resultsLeadersRanks?fop=%PLATFORM%&dark=false&em=0.90" --window-size=1280,720 --window-position=1025,650 %commonoptions% --user-data-dir=%tmp%\owlcms-tests\%RANDOM% --force-device-scale-factor=0.75


