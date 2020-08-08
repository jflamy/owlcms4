Normally, `publicresults` is installed in the cloud.  If you have no Internet access and would still like the coaches to have access to the scoreboards, you can use a setup like the following

- You need two networks.  The competition network used by the technical officials and for the field of play must be kept separate from the network used by the coaches and the public.  We must prevent disgruntled or mischievous persons from interfering with the competition network. 
- The competition router protects the competition network.  
  - If you use a typical domestic or gaming router, by default it will call itself something like 192.168.1.1 and allocate addresses that start with 192.168.1.x.
  - It is connected to the coaches network by running a wire from the WAN/Internet port to the other router 
  - The competition router will prevent anything from the outside from coming in.  Coaches cannot reach the owlcms machine, period.  Only connections initiated from the blue competition network to the orange competition network are possible.

![Slide1](img/PublicResults/LocalPublicResults/Slide1.SVG)

- The coaches router provides WiFi access to the coaches and/or the public at the competition
  - The second router can be a second domestic or small business router. In our example, we have used an actual example of a router using a different type of private address (10.0.0.x)
  - This second router should have excellent WiFi capabilities, as it will be servicing many WiFi connections.
  - You will provide the WPA key to the coaches so they can connect to the WiFi router
  - The `publicresults` machine will connect to the coaches router using an ethernet cable (ideally); 
    - In order to discover the address for the publicresults machine, you will need to run a local program such as "ipconfig" (see [this link](https://redisoft.uk/ipconfig-gui/) for a simple interface)
  - You will need to tell the coaches the URL to use to reach the publicresults application.

### Making `publicresults` accessible from `owlcms`

We will use the IP address of publicresults to connect from owlcms to publicresults.  This works because the coaches network is visible from the competition network via the WAN connection, but the opposite is forbidden.

For our example, we will assume that publicresults is reachable at 10.0.0.234

### Configuration and test

1. We need to configure publicresults so that it has a shared secret with owlcms
   Go to the installation location for publicresults and edit the `publicresults.l4j.ini` file.
   Make sure it has a line that defines the secret

   ```
   -Dupdatekey=SomeVeryLongStringYouWillConfigureAlsoInOwlcms
   ```

3. We can test the connection by going to the owlcms laptop and trying the the destination address with a browser
   ![05_checkConnectivity](img/PublicResults/LocalPublicResults/05_checkConnectivity.png)
   
4. You can now test actual updates from the owlcms application. Configure owlcms on the `Prepare Competition`/`Technical Configuration` 
   
   - set the destination url for publicresults to the tested IP address (http://10.0.0.234:8080 in our example)
   - set the shared secret update key to what you used in the l4j.ini file (SomeVeryLongStringYouWillConfigureAlsoInOwlcms in our example)
   
4. ![06_actualTest](img/PublicResults/LocalPublicResults/06_actualTest.png)

### Protecting `owlcms`

It is very important that the WPA security key for the competition router be set to something that cannot easily be guessed, and that it be kept private.  Only the people that set up the competition network should know it.