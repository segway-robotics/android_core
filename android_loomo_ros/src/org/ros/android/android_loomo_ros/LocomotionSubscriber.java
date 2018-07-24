package org.ros.android.android_loomo_ros;

import android.util.Log;

import com.segway.robot.sdk.locomotion.sbv.Base;

import org.ros.message.MessageListener;

import geometry_msgs.Twist;

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
        while (mBridgeNode.mCmdVelSubr == null){}
        mBridgeNode.mCmdVelSubr.addMessageListener(cmdVelListener);
    };

    MessageListener<Twist> cmdVelListener = new MessageListener<Twist>() {
        @Override
        public void onNewMessage(Twist message) {
//            Log.d(TAG, Double.toString(message.getAngular().getZ()));
            mBase.setLinearVelocity((float)message.getLinear().getX());
            mBase.setAngularVelocity((float)message.getAngular().getZ());
        }
    };

}
