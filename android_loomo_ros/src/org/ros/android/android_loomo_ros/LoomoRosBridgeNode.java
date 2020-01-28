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

    private final NodeParams params = Utils.loadParams();

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

    public String node_name = params.getNodeName();  // loomo_ros_bridge_node
    public String tf_prefix = params.getTfPrefix();  // LO01
    public boolean should_pub_ultrasonic = params.getShouldPubUltrasonic();  // false
    public boolean should_pub_infrared = params.getShouldPubInfrared();  // false
    public boolean should_pub_base_pitch = params.getShouldPubBasePitch();  // true
    public boolean use_tf_prefix = params.getUseTfPrefix();  // true

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
        mFisheyeCamPubr = connectedNode.newPublisher(tf_prefix+params.getmFisheyeCamPubrTopic(), Image._TYPE);  // "/fisheye/rgb"
        mFisheyeCompressedPubr = connectedNode.newPublisher(tf_prefix+params.getmFisheyeCompressedPubrTopic(), CompressedImage._TYPE);  // "/fisheye/rgb/compressed"
        mFisheyeCamInfoPubr = connectedNode.newPublisher(tf_prefix+params.getmFisheyeCamInfoPubrTopic(), CameraInfo._TYPE);  // "/fisheye/camera_info"
        mRsColorPubr = connectedNode.newPublisher(tf_prefix+params.getmRsColorPubrTopic(), Image._TYPE);  // "/realsense_loomo/rgb"
        mRsColorCompressedPubr = connectedNode.newPublisher(tf_prefix+params.getmRsColorCompressedPubrTopic(), CompressedImage._TYPE);  // "/realsense_loomo/rgb/compressed"
        mRsColorInfoPubr = connectedNode.newPublisher(tf_prefix+params.getmRsColorInfoPubrTopic(), CameraInfo._TYPE);  // "/realsense_loomo/rgb/camera_info"
        mRsDepthPubr = connectedNode.newPublisher(tf_prefix+params.getmRsDepthPubrTopic(), Image._TYPE);  // "/realsense_loomo/depth"
        mRsDepthInfoPubr = connectedNode.newPublisher(tf_prefix+params.getmRsDepthInfoPubrTopic(), CameraInfo._TYPE);  // "/realsense_loomo/depth/camera_info"
        mTfPubr = connectedNode.newPublisher(params.getmTfPubrTopic(), TFMessage._TYPE);  // "/tf"
        mInfraredPubr = connectedNode.newPublisher(tf_prefix+params.getmInfraredPubrTopic(), Float32._TYPE);  // "/infrared"
        mUltrasonicPubr = connectedNode.newPublisher(tf_prefix+params.getmUltrasonicPubrTopic(), Float32._TYPE);  // "/ultrasonic"
        mBasePitchPubr = connectedNode.newPublisher(tf_prefix+params.getmBasePitchPubrTopic(), Float32._TYPE);  // "/base_pitch"

        // Subscribe to commanded twist msgs (e.g. from joystick or autonomous driving software)
        mCmdVelSubr = mConnectedNode.newSubscriber(tf_prefix+params.getmCmdVelSubrTopic(), Twist._TYPE);  // "/cmd_vel"

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
