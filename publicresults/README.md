

# Public Scoreboard Publisher
This module is a slave Vaadin application that receives HTTP POST events from a competition site and displays the scoreboards for the fields of play.

In this way the competition system has no inbound connection from the outside.

The [owlcms-publisher](https://github.com/jflamy/owlcms-publisher) repository pulls this module so it can be cloned and pushed as a Heroku cloud project using a Heroku button.