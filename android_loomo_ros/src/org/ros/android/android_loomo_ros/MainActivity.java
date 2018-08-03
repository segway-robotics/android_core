/*
 * Copyright (C) 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.android.android_loomo_ros;

import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.perception.sensor.Sensor;
import com.segway.robot.sdk.vision.Vision;
import com.segway.robot.sdk.locomotion.sbv.Base;

import org.ros.address.InetAddressFactory;
import org.ros.android.RosActivity;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.net.URI;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;


/**
 * @author ethan.rublee@gmail.com (Ethan Rublee)
 * @author damonkohler@google.com (Damon Kohler)
 * @author mfe@mit.edu (Michael Everett)
 */
public class MainActivity extends RosActivity implements CompoundButton.OnCheckedChangeListener {
    public static final String TAG = "MainRosActivity";

    private Vision mVision;
    private Sensor mSensor;
    private Base mBase;

    private Switch mPubRsColorSwitch;
    private Switch mPubRsDepthSwitch;
    private Switch mPubTFSwitch;

    private RealsensePublisher mRealsensePublisher;
    private TFPublisher mTFPublisher;
    private LocomotionSubscriber mLocomotionSubscriber;

    private LoomoRosBridgeNode mBridgeNode;

    private Queue<Long> mDepthStamps;

    // Assumes that ROS master is a different machine, with a hard-coded ROS_MASTER_URI.
    // If you'd like to be able to select the URI in the app on startup, replace
    // super( , , ) with super( , ) to start a different version of RosActivity
    public MainActivity() { super("LoomoROS", "LoomoROS", URI.create("http://192.168.42.134:11311/"));}

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Set up GUI window
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.main);

        // Add some switches to turn on/off sensor publishers
        mPubRsColorSwitch = (Switch) findViewById(R.id.rscolor);
        mPubRsDepthSwitch = (Switch) findViewById(R.id.rsdepth);
        mPubTFSwitch = (Switch) findViewById(R.id.tf);

        // Add some listeners to the states of the switches
        mPubRsColorSwitch.setOnCheckedChangeListener(this);
        mPubRsDepthSwitch.setOnCheckedChangeListener(this);
        mPubTFSwitch.setOnCheckedChangeListener(this);

        // Keep track of timestamps when images published, so corresponding TFs can be published too
        mDepthStamps = new ConcurrentLinkedDeque<>();

        // Start an instance of the LoomoRosBridgeNode
        mBridgeNode = new LoomoRosBridgeNode();

        // get Vision SDK instance
        mVision = Vision.getInstance();
        mVision.bindService(this, mBindVisionListener);

        // get Sensor SDK instance
        mSensor = Sensor.getInstance();
        mSensor.bindService(this, mBindStateListener);

        // get Locomotion SDK instance
        mBase = Base.getInstance();
        mBase.bindService(this, mBindLocomotionListener);

    }


    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart() called");
//        mPubRsColorSwitch.setChecked(false);
//        mPubRsDepthSwitch.setChecked(false);
//        mPubTFSwitch.setChecked(false);
//        mLocomotionSubscriber.start_listening();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        mPubRsColorSwitch.setChecked(false);
//        mPubRsDepthSwitch.setChecked(false);
//        mPubTFSwitch.setChecked(false);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop() called");
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        Log.d(TAG, "init().");
        NodeConfiguration nodeConfiguration =
                NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(),
                        getMasterUri());
        nodeMainExecutor.execute(mBridgeNode, nodeConfiguration);

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // Someone has clicked a button - handle it here
        switch (buttonView.getId()) {
            case R.id.rscolor:
                mRealsensePublisher.mIsPubRsColor = isChecked;
                if (isChecked) {
                    mRealsensePublisher.start_color();
                } else {
                    mRealsensePublisher.stop_color();
                }
                break;
            case R.id.rsdepth:
                mRealsensePublisher.mIsPubRsDepth = isChecked;
                if (isChecked) {
                    mRealsensePublisher.start_depth();
                } else {
                    mRealsensePublisher.stop_depth();
                }
                break;
            case R.id.tf:
                Log.d(TAG, "TF clicked.");
                mTFPublisher.mIsPubTF = isChecked;
                if (isChecked) {
                    mTFPublisher.start_tf();
                } else {
                    mTFPublisher.stop_tf();
                }
                break;
        }
    }

    ServiceBinder.BindStateListener mBindVisionListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            Log.i(TAG, "onBind() mBindVisionListener called");
            if (mRealsensePublisher == null) {
                Log.d(TAG, "bindVision created new RealsensePublisher.");
                mRealsensePublisher = new RealsensePublisher(mVision, mBridgeNode, mDepthStamps);
            }
            Log.d(TAG, "bindVision enabling realsense switches.");
            mPubRsColorSwitch.setEnabled(true);
            mPubRsDepthSwitch.setEnabled(true);
            mPubRsColorSwitch.setChecked(true);
            mPubRsDepthSwitch.setChecked(true);
        }

        @Override
        public void onUnbind(String reason) {
            Log.i(TAG, "onUnbindVision: " + reason);
        }
    };

    ServiceBinder.BindStateListener mBindStateListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            Log.d(TAG, "onBind() mBindStateListener called");
            if (mTFPublisher == null) {
                mTFPublisher = new TFPublisher(mSensor, mBridgeNode, mDepthStamps);
            }
            mPubTFSwitch.setEnabled(true);
            mPubTFSwitch.setChecked(true);
        }

        @Override
        public void onUnbind(String reason) {
            Log.d(TAG, "onUnbind() called with: reason = [" + reason + "]");
        }
    };

    ServiceBinder.BindStateListener mBindLocomotionListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            Log.d(TAG, "mBindLocomotionListener onBind() called");
            if (mLocomotionSubscriber == null) {
                Log.d(TAG, "mBindLocomotionListener creating LocomotionSubscriber instance.");
                mLocomotionSubscriber = new LocomotionSubscriber(mBase, mBridgeNode);
                Log.d(TAG, "mBindLocomotionListener created LocomotionSubscriber instance.");
            }
            mLocomotionSubscriber.start_listening();
        }

        @Override
        public void onUnbind(String reason) {
            Log.d(TAG, "onUnbind() called with: reason = [" + reason + "]");
        }
    };
}
