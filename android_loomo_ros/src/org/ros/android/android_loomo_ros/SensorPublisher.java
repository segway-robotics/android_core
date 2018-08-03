package org.ros.android.android_loomo_ros;

import android.os.Handler;
import android.util.Log;

import com.segway.robot.sdk.perception.sensor.InfraredData;
import com.segway.robot.sdk.perception.sensor.Sensor;
import com.segway.robot.sdk.perception.sensor.UltrasonicData;

import java.util.Queue;

import std_msgs.Float32;

/**
 * Created by mfe on 8/3/18.
 */

public class SensorPublisher {
    public static final String TAG = "SensorPublisher";

    public boolean mIsPubSensor;
    private Sensor mSensor;
    private LoomoRosBridgeNode mBridgeNode;
    private Thread mSensorPublishThread;

    public SensorPublisher(Sensor mSensor, LoomoRosBridgeNode mBridgeNode) {
        this.mSensor = mSensor;
        this.mBridgeNode = mBridgeNode;
    }

    public void start_sensor() {
        Log.d(TAG, "start_sensor()");
        if (mSensorPublishThread == null) {
            mSensorPublishThread = new SensorPublisherThread();
        }
        Handler handler=new Handler();
        Runnable r=new Runnable() {
            public void run() {
                //what ever you do here will be done after 5 seconds delay.
                Log.d(TAG, "Waited for ROS publisher to connect. Going to start publishing sensor data now.");
                mSensorPublishThread.start();
            }
        };
        handler.postDelayed(r, 5000);

    }

    public void stop_sensor() {
        Log.d(TAG, "stop_sensor()");
        try {
            mSensorPublishThread.join();
        } catch (InterruptedException e) {
            Log.w(TAG, "onUnbind: mSensorPublishThread.join() ", e);
        }
    }

    private class SensorPublisherThread extends Thread {
        @Override
        public void run() {
            Log.d(TAG, "run: SensorPublisherThread");
            super.run();

            while (null != mSensor) {
                UltrasonicData ultrasonicData = mSensor.getUltrasonicDistance();
                InfraredData infraredData = mSensor.getInfraredDistance();

                Float32 ultrasonicMessage = mBridgeNode.mUltrasonicPubr.newMessage();
                ultrasonicMessage.setData(ultrasonicData.getDistance());
                mBridgeNode.mUltrasonicPubr.publish(ultrasonicMessage);

                Float32 infraredMessage = mBridgeNode.mInfraredPubr.newMessage();
                infraredMessage.setData(infraredData.getLeftDistance());
                mBridgeNode.mInfraredPubr.publish(infraredMessage);
            }

//            while (null != mSensor) {
//                    TFMessage tfMessage = mBridgeNode.mTfPubr.newMessage();
//                    for (Pair<Integer, Integer> index : frameIndices) {
//                        String target = frameNames.get(index.second);
//                        String source = frameNames.get(index.first);
//                        AlgoTfData tfData = mSensor.getTfData(source, target, stamp, 500);
//                        if (stamp != tfData.timeStamp) {
//                            Log.d(TAG, String.format("run: getTfData failed for frames[%d]: %s -> %s",
//                                    stamp, source, target));
//                            continue;
//                        }
//                        TransformStamped transformStamped = algoTf2TfStamped(tfData, stamp);
//                        tfMessage.getTransforms().add(transformStamped);
//                    }
//                    if (tfMessage.getTransforms().size() > 0)
//                        mBridgeNode.mTfPubr.publish(tfMessage);
//                }
//            }
        }
    }
}
