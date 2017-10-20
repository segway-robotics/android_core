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

package org.ros.android.android_tutorial_camera;

import android.hardware.Camera;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import org.ros.address.InetAddressFactory;
import org.ros.android.RosActivity;
import org.ros.android.view.camera.RosCameraPreviewView;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.vision.Vision;
import com.segway.robot.sdk.vision.stream.StreamInfo;
import com.segway.robot.sdk.vision.stream.StreamType;

/**
 * @author ethan.rublee@gmail.com (Ethan Rublee)
 * @author damonkohler@google.com (Damon Kohler)
 */
public class MainActivity extends RosActivity implements CompoundButton.OnCheckedChangeListener {

    private int cameraId;
    private RosCameraPreviewView rosCameraPreviewView;

    boolean mBind;
    private Vision mVision;

    private Switch mPubRgbdSwitch;
    private Switch mPubPcamSwitch;
    private Switch mPubDtsSwitch;
    private Switch mPubSpeechSwitch;
    private Switch mSubMotionSwitch;

    private LoomoRosBridgeNode mBridgeNode;

    public MainActivity() {
        super("CameraTutorial", "CameraTutorial");
    }

    ServiceBinder.BindStateListener mBindStateListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            mBind = true;
        }

        @Override
        public void onUnbind(String reason) {
            mBind = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.main);
        rosCameraPreviewView = (RosCameraPreviewView) findViewById(R.id.ros_camera_preview_view);

        mPubRgbdSwitch = (Switch) findViewById(R.id.rgbd);
        mPubPcamSwitch = (Switch) findViewById(R.id.pcam);
        mPubDtsSwitch = (Switch) findViewById(R.id.dts);
        mPubSpeechSwitch = (Switch) findViewById(R.id.speech);
        mSubMotionSwitch = (Switch) findViewById(R.id.motion);

        mPubRgbdSwitch.setOnCheckedChangeListener(this);
        mPubPcamSwitch.setOnCheckedChangeListener(this);
        mPubDtsSwitch.setOnCheckedChangeListener(this);
        mPubSpeechSwitch.setOnCheckedChangeListener(this);
        mSubMotionSwitch.setOnCheckedChangeListener(this);

        mVision = Vision.getInstance();
        mBridgeNode = new LoomoRosBridgeNode();
        if (!mVision.bindService(this, mBindStateListener))
            Toast.makeText(this, "Bind service failed", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, "Bind service success", Toast.LENGTH_SHORT).show();
    }


    @Override
    protected void onRestart() {
        super.onRestart();
        mPubRgbdSwitch.setChecked(false);
        mPubPcamSwitch.setChecked(false);
        mPubDtsSwitch.setChecked(false);
        mPubSpeechSwitch.setChecked(false);
        mSubMotionSwitch.setChecked(false);

        if (!mBind) {
            if (!mVision.bindService(this, mBindStateListener))
                Toast.makeText(this, "Re-bind service failed", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, "Re-bind service success", Toast.LENGTH_SHORT).show();
        }
    }

//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        if (event.getAction() == MotionEvent.ACTION_UP) {
//            int numberOfCameras = Camera.getNumberOfCameras();
//            final Toast toast;
//            if (numberOfCameras > 1) {
//                cameraId = (cameraId + 1) % numberOfCameras;
//                rosCameraPreviewView.releaseCamera();
//                rosCameraPreviewView.setCamera(getCamera());
//                toast = Toast.makeText(this, "Switching cameras.", Toast.LENGTH_SHORT);
//            } else {
//                toast = Toast.makeText(this, "No alternative cameras to switch to.", Toast.LENGTH_SHORT);
//            }
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    toast.show();
//                }
//            });
//        }
//        return true;
//    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.rgbd:
                if (isChecked) {
                    startImageTransfer();
                } else {
                    stopImageTransfer();
                }
                break;
            case R.id.pcam:
                if (isChecked) {
                    ///
                } else {
                    ///
                }
                break;
            case R.id.dts:
                if (isChecked) {
                    ///
                } else {
                    ///
                }
                break;
            case R.id.speech:
                if (isChecked) {
                    ///
                } else {
                    ///
                }
                break;
            case R.id.motion:
                if (isChecked) {
                    ///
                } else {
                    ///
                }
                break;
        }
    }

    private synchronized void startImageTransfer() {
        StreamInfo[] infos = mVision.getActivatedStreamInfo();
        for(StreamInfo info : infos) {
            switch (info.getStreamType()) {
                case StreamType.COLOR:
                    mBridgeNode.updateCameraInfo(2, mVision.getColorDepthCalibrationData().colorIntrinsic, info.getWidth(), info.getHeight());
                    mVision.startListenFrame(StreamType.COLOR, mBridgeNode.mRsColorListener);
                    break;
                case StreamType.DEPTH:
                    mBridgeNode.updateCameraInfo(3, mVision.getColorDepthCalibrationData().depthIntrinsic, info.getWidth(), info.getHeight());
                    mVision.startListenFrame(StreamType.DEPTH, mBridgeNode.mRsDepthListener);
                    break;
            }
        }

    }

    private synchronized void stopImageTransfer() {
        mVision.stopListenFrame(StreamType.COLOR);
        mVision.stopListenFrame(StreamType.DEPTH);
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        NodeConfiguration nodeConfiguration =
                NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(),
                        getMasterUri());
        nodeMainExecutor.execute(mBridgeNode, nodeConfiguration);

//        cameraId = 0;
//        rosCameraPreviewView.setCamera(getCamera());
//        try {
//            java.net.Socket socket = new java.net.Socket(getMasterUri().getHost(), getMasterUri().getPort());
//            java.net.InetAddress local_network_address = socket.getLocalAddress();
//            socket.close();
//            NodeConfiguration nodeConfiguration =
//                    NodeConfiguration.newPublic(local_network_address.getHostAddress(), getMasterUri());
//            nodeMainExecutor.execute(rosCameraPreviewView, nodeConfiguration);
//        } catch (IOException e) {
//            // Socket problem
//            Log.e("Camera Tutorial", "socket error trying to get networking information from the master uri");
//        }
    }

    private Camera getCamera() {
        Camera cam = Camera.open(cameraId);
        Camera.Parameters camParams = cam.getParameters();
        if (camParams.getSupportedFocusModes().contains(
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            camParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        } else {
//        camParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        cam.setParameters(camParams);
        return cam;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mBind) {
            mVision.unbindService();
            StreamInfo[] infos = mVision.getActivatedStreamInfo();
            for(StreamInfo info : infos) {
                switch (info.getStreamType()) {
                    case StreamType.COLOR:
                        mVision.stopListenFrame(StreamType.COLOR);
                        break;
                    case StreamType.DEPTH:
                        mVision.stopListenFrame(StreamType.DEPTH);
                        break;
                }
            }
        }
    }
}
