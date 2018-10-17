package org.ros.android.android_loomo_ros;

import android.os.AsyncTask;
import android.util.Log;

import com.segway.robot.sdk.locomotion.sbv.Base;

import org.ros.android.RosActivity;
import org.ros.message.MessageListener;

import java.util.concurrent.TimeUnit;

import geometry_msgs.Twist;

import android.os.Handler;

/**
 * Created by mfe on 7/24/18.
 */

public class LocomotionSubscriber {
    public static final String TAG = "LocomotionSubscriber";

    private Base mBase;
    private LoomoRosBridgeNode mBridgeNode;

    public LocomotionSubscriber(Base mBase, LoomoRosBridgeNode mBridgeNode){
        this.mBase = mBase;
        this.mBridgeNode = mBridgeNode;

        // Configure Base to accept raw linear/angular velocity commands
        this.mBase.setControlMode(Base.CONTROL_MODE_RAW);

    }

    public void start_listening(){
        // wait til ROS subscriber is set up, then start listening TODO: make this better
        Handler handler=new Handler();
        Runnable r=new Runnable() {
            public void run() {
                //what ever you do here will be done after 5 seconds delay.
                Log.d(TAG, "Waited for ROS subscriber to connect. Going to hook up to cmd_vel now.");
                mBridgeNode.mCmdVelSubr.addMessageListener(cmdVelListener);
            }
        };
        handler.postDelayed(r, 5000);
    };

    MessageListener<Twist> cmdVelListener = new MessageListener<Twist>() {
        @Override
        public void onNewMessage(Twist message) {
            mBase.setLinearVelocity((float)message.getLinear().getX());
            mBase.setAngularVelocity((float)message.getAngular().getZ());
        }
    };

}
