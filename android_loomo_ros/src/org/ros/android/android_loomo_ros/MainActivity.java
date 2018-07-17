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
import android.widget.Toast;

import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.perception.sensor.Sensor;
import com.segway.robot.sdk.vision.Vision;
import com.segway.robot.sdk.vision.stream.StreamInfo;
import com.segway.robot.sdk.vision.stream.StreamType;

import org.ros.address.InetAddressFactory;
import org.ros.android.RosActivity;
import org.ros.android.view.camera.RosCameraPreviewView;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

/**
 * @author ethan.rublee@gmail.com (Ethan Rublee)
 * @author damonkohler@google.com (Damon Kohler)
 */
public class MainActivity extends RosActivity implements CompoundButton.OnCheckedChangeListener {
    public static final String TAG = "MainRosActivity";

    private Vision mVision;

    private Switch mPubRsColorSwitch;
    private Switch mPubRsDepthSwitch;
    private Switch mPubTfSwitch;

    private RealsensePublisher mRealsensePublisher;

    private LoomoRosBridgeNode mBridgeNode;

    public MainActivity() {
        super("LoomoROS", "LoomoROS");
    }

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
        mPubTfSwitch = (Switch) findViewById(R.id.tf);

        // Add some listeners to the states of the switches
        mPubRsColorSwitch.setOnCheckedChangeListener(this);
        mPubRsDepthSwitch.setOnCheckedChangeListener(this);
        mPubTfSwitch.setOnCheckedChangeListener(this);

        // Start an instance of the LoomoRosBridgeNode
        mBridgeNode = new LoomoRosBridgeNode();

        // get Vision SDK instance
        mVision = Vision.getInstance();
        mVision.bindService(this, mBindVisionListener);
    }


    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart() called");
        mPubRsColorSwitch.setChecked(false);
        mPubRsDepthSwitch.setChecked(false);
        mPubTfSwitch.setChecked(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPubRsColorSwitch.setChecked(false);
        mPubRsDepthSwitch.setChecked(false);
        mPubTfSwitch.setChecked(false);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop() called");
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        NodeConfiguration nodeConfiguration =
                NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(),
                        getMasterUri());
        nodeMainExecutor.execute(mBridgeNode, nodeConfiguration);
    }

//    ServiceBinder.BindStateListener mBindStateListener = new ServiceBinder.BindStateListener() {
//        @Override
//        public void onBind() {
//            Log.d(TAG, "onBind() called");
//            mPubRsColorSwitch.setEnabled(true);
//            mPubRsDepthSwitch.setEnabled(true);
//        }
//
//        @Override
//        public void onUnbind(String reason) {
//            Log.d(TAG, "onUnbind() called with: reason = [" + reason + "]");
//        }
//    };

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
//                if (isChecked) {
//                    if (!Sensor.getInstance().bindService(this, mBridgeNode.mSensorBindListener))
//                        Toast.makeText(this, "Bind sensor service failed!", Toast.LENGTH_SHORT).show();
//                    else
//                        Toast.makeText(this, "Bind sensor service success！", Toast.LENGTH_SHORT).show();
//                } else {
//                    Log.d(TAG, "onCheckedChanged: unbind sensor service");
//                    Sensor.getInstance().unbindService();
//                }
                break;
        }
    }

    ServiceBinder.BindStateListener mBindVisionListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            Log.i(TAG, "onBindVision");
            if (mRealsensePublisher == null) {
                Log.d(TAG, "bindVision need to create new RealsensePublisher.");
                mRealsensePublisher = new RealsensePublisher(mVision, mBridgeNode);
            }
            Log.d(TAG, "bindVision enabling realsense switches.");
            mPubRsColorSwitch.setEnabled(true);
            mPubRsDepthSwitch.setEnabled(true);
        }

        @Override
        public void onUnbind(String reason) {
            Log.i(TAG, "onUnbindVision: " + reason);
        }
    };

//    private synchronized void startRgbdTransfer() {
//        Log.w(TAG, "startRgbdTransfer()");
//        if (null == mVision) {
//            Log.w(TAG, "startRgbdTransfer(): did not bind service!");
//            return;
//        }
//        StreamInfo[] infos = mVision.getActivatedStreamInfo();
//        for(StreamInfo info : infos) {
//            switch (info.getStreamType()) {
//                case StreamType.COLOR:
//                    mBridgeNode.updateCameraInfo(2, mVision.getColorDepthCalibrationData().colorIntrinsic,
//                            info.getWidth(), info.getHeight());
//                    Log.d(TAG, "startListenFrame: called");
//                    mVision.startListenFrame(StreamType.COLOR, mBridgeNode.mRsColorListener);
//                    break;
//                case StreamType.DEPTH:
//                    mBridgeNode.updateCameraInfo(3, mVision.getColorDepthCalibrationData().depthIntrinsic,
//                            info.getWidth(), info.getHeight());
//                    mVision.startListenFrame(StreamType.DEPTH, mBridgeNode.mRsDepthListener);
//                    break;
//            }
//        }
//        Log.w(TAG, "startRgbdTransfer() done.");
//    }

//    private synchronized void stopRgbdTransfer() {
//        if (null == mVision) {
//            Log.w(TAG, "stopRgbdTransfer(): did not bind service!");
//            return;
//        }
//        Log.d(TAG, "stopRgbdTransfer: called");
//        mVision.stopListenFrame(StreamType.COLOR);
//        mVision.stopListenFrame(StreamType.DEPTH);
//    }
}
