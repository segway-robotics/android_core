# Android Loomo ROS Core #
#### Michael Everett & Jonathan P. How : Aerospace Controls Laboratory, MIT ####

This is early research code (Android app) that enables Loomo-to-Ubuntu PC connection through USB, with message passing in ROS.

The important steps for connecting to the Loomo are (steps 0-2 are one-time only, steps 1-4 described more here: https://gist.github.com/mfe7/c8b21b38a1574f6c319095415ae9a9e8)

0) Set up an NTP server on your Ubuntu PC in order to sync the Ubuntu PC clock to the Loomo Android clock (very important!!). Sync to an NTP server from the internet, then provide a local NTP server for anyone in the 192.168.42.xxx network (Loomo-to-PC network created when USB is connected in next steps). I can share a `ntp.conf` file if needed.
1) Switch Loomo into Developer Mode (Settings > System > Loomo Developer > Developer Mode)
    - (I think this requires an internet connection, so join a wifi network now.)
2) You might need to switch Android into Developer Mode (tap some setting 7 times...look online for instructions).

Note: For the current version of the ROS node, it's very important that the Loomo *not* be connected to wifi,
because it'll try to use that interface (wlan0) for ROS msgs.

3) Connect USB cable from Loomo's USB-C port to PC's USB-A port.
4) On Loomo, turn on USB Tethering (Settings > Wireless & Networks > More... > Tethering & portable hotspot > USB tethering)
On Ubuntu, you should see a popup from NetworkManager saying there's a new Wired Connection. You can rename it to Loomo if you want.

At this point, the two devices are on a common network, and you should be able to ping one another.

5) Start ROS master on Ubuntu PC (i.e. `$ roscore`) (use `ROS_MASTER_URI=http://192.168.42.34:11311/`)
6) Start the Loomo ROS app on the Loomo's touch screen interface
It should automatically start with the TF Publisher switch enabled, and after 5 seconds, the 2 Realsense/camera switches should go green automatically
7) On Ubuntu PC, try `$ rostopic echo /LO01/realsense/depth`
If that works, /tf should also have a lot of Loomo transforms and some of the other realsense camera topics should work.

8) Try to send Twist msgs (linear speed, angular rate) to the Loomo on `/LO01/cmd_vel`, e.g. by using RQT or rostopic pub command line tool.
The robot base should spin/drive forward/backward if you send these commands at a reasonable frequency (e.g. 10Hz).

If all of that worked, you're able to send/receive data from your Loomo using ROS! And there is little latency by using the USB connection, as compared to communicating through WiFi.

There are a couple hard-coded parameters but you should feel free to change:
- `ROS_MASTER_URI`: set to `192.168.42.134` - it's possible yours will be different
- `tf_prefix`: set to `LO01` (short for Loomo 01, in case there are multiple loomos communicating with the same ROS master)

For instructions to get the Android code compiled/installed on the Loomo, I used this:
https://github.com/segway-robotics/vision_msg_proc/blob/master/README.md
