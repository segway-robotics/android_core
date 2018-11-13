package org.ros.android.android_loomo_ros;

import android.util.Log;

import com.segway.robot.sdk.perception.sensor.Sensor;

import org.ros.message.MessageFactory;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;
import org.ros.message.MessageListener;
import org.ros.time.NtpTimeProvider;

import sensor_msgs.CameraInfo;
import sensor_msgs.CompressedImage;
import sensor_msgs.Image;
import std_msgs.Float32;
import tf2_msgs.TFMessage;
import geometry_msgs.Twist;

/**
 * Created by kai on 17-7-16.
 * Modified by mfe on 10-9-18.
 */

public class LoomoRosBridgeNode extends AbstractNodeMain {
    private static final String TAG = "LoomoRosBridgeNode";

    public ConnectedNode mConnectedNode;
    public MessageFactory mMessageFactory;
    public Publisher<Image> mFisheyeCamPubr;
    public Publisher<CompressedImage> mFisheyeCompressedPubr;
    public Publisher<CameraInfo> mFisheyeCamInfoPubr;
    public Publisher<Image> mRsColorPubr;
    public Publisher<CompressedImage> mRsColorCompressedPubr;
    public Publisher<Image> mRsDepthPubr;
    public Publisher<CameraInfo> mRsColorInfoPubr;
    public Publisher<CameraInfo> mRsDepthInfoPubr;
    public Publisher<TFMessage> mTfPubr;
    public Publisher<Float32> mInfraredPubr;
    public Publisher<Float32> mUltrasonicPubr;
    public Publisher<Float32> mBasePitchPubr;

    public Subscriber<Twist> mCmdVelSubr;

    public NtpTimeProvider mNtpProvider;

    public String node_name = "loomo_ros_bridge_node";
    public String tf_prefix = "LO01";
    public boolean should_pub_ultrasonic = false;
    public boolean should_pub_infrared = false;
    public boolean should_pub_base_pitch = true;
    public boolean use_tf_prefix = true;

    public LoomoRosBridgeNode() {
        super();
//        this.mNtpProvider = ntpTimeProvider;
        Log.d(TAG, "Created instance of LoomoRosBridgeNode().");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        Log.d(TAG, "onStart()");
        super.onStart(connectedNode);

        Log.d(TAG, "onStart() creating publishers.");
        mConnectedNode = connectedNode;
        mMessageFactory = connectedNode.getTopicMessageFactory();

        if (use_tf_prefix == false){
            tf_prefix = "";
        }

        // Create publishers for many Loomo topics
        mFisheyeCamPubr = connectedNode.newPublisher(tf_prefix+"/fisheye/rgb", Image._TYPE);
        mFisheyeCompressedPubr = connectedNode.newPublisher(tf_prefix+"/fisheye/rgb/compressed", CompressedImage._TYPE);
        mFisheyeCamInfoPubr = connectedNode.newPublisher(tf_prefix+"/fisheye/camera_info", CameraInfo._TYPE);
        mRsColorPubr = connectedNode.newPublisher(tf_prefix+"/realsense_loomo/rgb", Image._TYPE);
        mRsColorCompressedPubr = connectedNode.newPublisher(tf_prefix+"/realsense_loomo/rgb/compressed", CompressedImage._TYPE);
        mRsColorInfoPubr = connectedNode.newPublisher(tf_prefix+"/realsense_loomo/rgb/camera_info", CameraInfo._TYPE);
        mRsDepthPubr = connectedNode.newPublisher(tf_prefix+"/realsense_loomo/depth", Image._TYPE);
        mRsDepthInfoPubr = connectedNode.newPublisher(tf_prefix+"/realsense_loomo/depth/camera_info", CameraInfo._TYPE);
        mTfPubr = connectedNode.newPublisher("/tf", TFMessage._TYPE);
        mInfraredPubr = connectedNode.newPublisher(tf_prefix+"/infrared", Float32._TYPE);
        mUltrasonicPubr = connectedNode.newPublisher(tf_prefix+"/ultrasonic", Float32._TYPE);
        mBasePitchPubr = connectedNode.newPublisher(tf_prefix+"/base_pitch", Float32._TYPE);

        // Subscribe to commanded twist msgs (e.g. from joystick or autonomous driving software)
        mCmdVelSubr = mConnectedNode.newSubscriber(tf_prefix+"/cmd_vel", Twist._TYPE);

    }

    @Override
    public void onShutdown(Node node) {
        super.onShutdown(node);
    }

    @Override
    public void onShutdownComplete(Node node) {
        super.onShutdownComplete(node);
    }

    @Override
    public void onError(Node node, Throwable throwable) {
        super.onError(node, throwable);
    }

    @Override
    public GraphName getDefaultNodeName() {
        if (use_tf_prefix) {
            node_name = tf_prefix + "/" + node_name;
        }
        return GraphName.of(node_name);
    }

}
