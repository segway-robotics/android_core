package org.ros.android.android_loomo_ros;

import android.util.Log;

import com.segway.robot.sdk.perception.sensor.Sensor;

import org.ros.message.MessageFactory;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Publisher;

import sensor_msgs.CameraInfo;
import sensor_msgs.CompressedImage;
import sensor_msgs.Image;
import tf2_msgs.TFMessage;

/**
 * Created by kai on 17-7-16.
 */

public class LoomoRosBridgeNode extends AbstractNodeMain {
    private static final String TAG = "LoomoRosBridgeNode";

    private ConnectedNode mConnectedNode;
    public MessageFactory mMessageFactory;
    //    private Publisher<Image> mPcamPubr;
//    private Publisher<CameraInfo> mPcamInfoPubr;
    public Publisher<Image> mRsColorPubr;
    public Publisher<CompressedImage> mRsColorCompressedPubr;
    public Publisher<Image> mRsDepthPubr;
    public Publisher<CameraInfo> mRsColorInfoPubr;
    public Publisher<CameraInfo> mRsDepthInfoPubr;
    public Publisher<TFMessage> mTfPubr;


    public LoomoRosBridgeNode() {
        super();
        Log.d(TAG, "Created instance of LoomoRosBridgeNode().");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        Log.d(TAG, "onStart()");
        super.onStart(connectedNode);

        // Create publishers for every possible Loomo topic
        Log.d(TAG, "onStart() creating publishers.");
        mConnectedNode = connectedNode;
        mMessageFactory = connectedNode.getTopicMessageFactory();
//        mPcamPubr = connectedNode.newPublisher("loomo/pcam/rgb", Image._TYPE);
//        mPcamInfoPubr = connectedNode.newPublisher("loomo/pcam/camera_info", CameraInfo._TYPE);
        mRsColorPubr = connectedNode.newPublisher("loomo/realsense/rgb", Image._TYPE);
        mRsColorCompressedPubr = connectedNode.newPublisher("loomo/realsense/rgb/compressed", CompressedImage._TYPE);
        mRsColorInfoPubr = connectedNode.newPublisher("loomo/realsense/rgb/camera_info", CameraInfo._TYPE);
        mRsDepthPubr = connectedNode.newPublisher("loomo/realsense/depth", Image._TYPE);
        mRsDepthInfoPubr = connectedNode.newPublisher("loomo/realsense/depth/camera_info", CameraInfo._TYPE);
        mTfPubr = connectedNode.newPublisher("/tf", TFMessage._TYPE);

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
        return GraphName.of("loomo_ros_bridge_node");
    }

}
