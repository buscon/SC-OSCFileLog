Fork of the Supercollider library https://github.com/sensestage/SC-OSCFileLog

small changes in comparison to the forked library:
- the open function does not automatically start to write the osc message when it is called
- the writing of the OSC messages is done by the new method writeOSC. it accepts a list of possible OSC messages to be recorded
- the initialization accepts as an optional argument a port number where the osc can be recorded

