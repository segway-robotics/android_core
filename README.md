# Android Loomo ROS Core
##### Michael Everett & Jonathan P. How - Aerospace Controls Laboratory, MIT - This work is supported by Ford Motor Company

This is early research code (Android app) that enables Loomo-to-Ubuntu PC connection through USB, with message passing in ROS.

If you want to use the Android code as is, you might be able to just `abd install android_loomo_ros-release.apk` after downloading an apk file [here](https://www.dropbox.com/s/wm86eata78v7h9p/android_loomo_ros-release.apk?dl=0). I am curious if this works for anyone, so please let me know if you get a chance to try. You could then skip down to the "After Installing Android App onto Loomo" part of this readme.

### Starting from scratch ###

1. On an Ubuntu 16.04 PC, install [ROS Kinetic](http://wiki.ros.org/kinetic/Installation/Ubuntu) and [Android Studio](http://wiki.ros.org/android/kinetic/Android%20Studio/Download).

2. Install `android_core` package on your Ubuntu 16.04 PC (can clone somewhere other than `~/android_core` if desired):
```
sudo apt-get install ros-kinetic-rosjava-build-tools ros-kinetic-genjava
mkdir -p ~/android_core
wstool init -j4 ~/android_core/src https://raw.github.com/rosjava/rosjava/kinetic/android_core.rosinstall
cd ~/android_core
catkin_make
```

3. Create a catkin workspace for this repo
```
mkdir -p ~/segway_ws/src
git clone https://github.com/mfe7/android_loomo_ros_core.git ~/segway_ws/src
cd ~/segway_ws
catkin_make
```

4. You are now done w/ `catkin_make` (everything else is Android-related). Open up Andorid Studio and open the `android_loomo_ros` project.

5. Connect a USB-C cable between Loomo's head and the Ubuntu PC.

6. Choose `android_loomo_ros` as the build target, and click the green triangle (looks like play button) to build and install the android code onto Loomo.
   - Make sure the `params.yaml` file located in the `android_loomo_ros` package is also on the Loomo at `/sdcard/params.yaml`. This can be done by running: `adb push params.yaml /sdcard/`

7. If that worked, there should be an app on the Loomo's home screen called `Loomo ROS`.

This section was based on [this](https://github.com/segway-robotics/vision_msg_proc/blob/master/README.md) in case I left out important details.


### Common Issues ###
* When running `catkin_make` in `~/android_core`:
    * Make sure the line `export ANDROID_HOME=/opt/android-sdk` is in your `~/.bashrc`
        * If not, run `echo “export ANDROID_HOME=/opt/android-sdk” >> ~/.bashrc`
        * Might have to change `/opt/android-sdk` depending on where you downloaded the SDK to
    * [Error](https://github.com/jitpack/jitpack.io/issues/3687#issuecomment-455885806): "Failed to install the following Android SDK packages as some licences have not been accepted"
        * Solution: `yes | $ANDROID_HOME/tools/bin/sdkmanager "build-tools;28.0.3"`
    * Error: "Task :cv_bridge:verifyReleaseResources FAILED"
        * [Solution](https://github.com/rosjava/android_core/issues/303#issuecomment-488436619): change `package="cv_bridge"` to `package="com.github.rosjava.android_extras.cv_bridge"` in `~/android_core/src/android_extras/cv_bridge/src/main/AndroidManifest.xml`
* When running `catkin_make` in `~/segway_ws`
    * In all of your `build.gradle` files, make sure the compileSdkVersion is compatible with the API version
        * You can check Android Studio and API version in File > Settings > Appearance & Behavior > System Settings > Updates
    * Make sure your Gradle and Gradle Plugin versions are [compatible](https://stackoverflow.com/questions/17727645/how-to-update-gradle-in-android-studio)
        * You can check Gradle and Gradle Plugin versions in File > Project Structure > Project
    * Error: "Gradle SDL method not found: 'google()'
        * Solution: change Gradle version to 4.4 (can be changed in File > Project Structure > Project or in `gradle-wrapper.properties`)
    
        

### After Installing Android App onto Loomo ###



The important steps for connecting to the Loomo are (steps 0-2 are one-time only, steps 1-4 described more here: https://gist.github.com/mfe7/c8b21b38a1574f6c319095415ae9a9e8)

1. Set up an NTP server on your Ubuntu PC in order to sync the Ubuntu PC clock to the Loomo Android clock (very important!!). Sync to an NTP server from the internet, then provide a local NTP server for anyone in the 192.168.42.xxx network (Loomo-to-PC network created when USB is connected in next steps). I can share a `ntp.conf` file if needed.
    - If you edit `/etc/ntp.conf`, run `sudo service ntp restart` to see changes.

2. Switch Loomo into Developer Mode (Settings > System > Loomo Developer > Developer Mode)
    - (I think this requires an internet connection, so join a wifi network now.)

3. You might need to switch Android into Developer Mode (tap some setting 7 times...look [online](https://www.howtogeek.com/129728/how-to-access-the-developer-options-menu-and-enable-usb-debugging-on-android-4.2/) for instructions).

Note: For the current version of the ROS node, it's very important that the Loomo *not* be connected to wifi,
because it'll try to use that interface (wlan0) for ROS msgs.

4. Connect USB cable from Loomo's USB-C port to PC's USB-A port.

5. On Loomo, turn on USB Tethering (Settings > Wireless & Networks > More... > Tethering & portable hotspot > USB tethering)
On Ubuntu, you should see a popup from NetworkManager saying there's a new Wired Connection.
    - If you want to configure a static IP for your PC in the network or want the Loomo to connect to the same "Wired Connection" every time, on Ubuntu PC, search "Network Connections" (Also can be found by clicking Wifi/Network symbol in top right corner > "Edit Connections...")> Add > Ethernet
        - Connection name: Loomo (or whatever you want it to be)
        - Go to "IPv4 Settings" tab
            - Choose "Manual" in the drop down menu
            - Under "Addresses", Add
                - Address: 192.168.42.134 (example)
                - Netmask: 24 (you can leave Gateway blank)
            - Routes... > check "use this connection only for resources on its network" (this allows you to preserve internet connection when the Loomo connects via ethernet)
    - After creating a static IP, add these two lines to your `~/.bashrc` (assuming your PC is the ROS master)
        - `export ROS_MASTER_URI=http://192.168.42.134:11311/
        - `export ROS_IP=192.168.42.134`
    - Run `source ~/.bashrc` to see changes. You can check that you have the right `ROS_MASTER_URI`, etc. with `echo $ROS_MASTER_URI`

At this point, the two devices are on a common network, and you should be able to ping one another.

6. Start ROS master on Ubuntu PC (i.e. `$ roscore`) (use `ROS_MASTER_URI=http://192.168.42.34:11311/`)

7. Start the Loomo ROS app on the Loomo's touch screen interface
    - If you get the error "insufficient permissions for device with ADB" (and when run command `adb devices`, next to device says "no permissions"), run `adb kill-server ; sudo adb start-server`
It should automatically start with the TF Publisher switch enabled, and after 5 seconds, the 2 Realsense/camera switches should go green automatically

8. On Ubuntu PC, try `$ rostopic echo /LO01/realsense/depth`
If that works, /tf should also have a lot of Loomo transforms and some of the other realsense camera topics should work.

9. Try to send Twist msgs (linear speed, angular rate) to the Loomo on `/LO01/cmd_vel`, e.g. by using RQT or rostopic pub command line tool.
The robot base should spin/drive forward/backward if you send these commands at a reasonable frequency (e.g. 10Hz).

If all of that worked, you're able to send/receive data from your Loomo using ROS! And there is little latency by using the USB connection, as compared to communicating through WiFi.

### General Loomo Tips/Troubleshooting ###
* To quit an application, tab the side of the head of the Loomo.
* If Ubuntu PC does not detect Loomo when plugged in via USB, make sure ADB server has started (i.e. run `sudo adb start-server`)
* Get IP address of Loomo: `adb shell netcfg`


There are a couple hard-coded parameters but you should feel free to change:
- `ROS_MASTER_URI`: set to `192.168.42.134` - it's possible yours will be different
- `tf_prefix`: set to `LO01` (short for Loomo 01, in case there are multiple loomos communicating with the same ROS master)
