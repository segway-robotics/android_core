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

    private Sensor mSensor;

    private ConnectedNode mConnectedNode;
    private MessageFactory mMessageFactory;
//    private Publisher<Image> mPcamPubr;
//    private Publisher<CameraInfo> mPcamInfoPubr;
    public Publisher<Image> mRsColorPubr;
    public Publisher<CompressedImage> mRsColorCompressedPubr;
    public Publisher<Image> mRsDepthPubr;
    public Publisher<CameraInfo> mRsColorInfoPubr;
    public Publisher<CameraInfo> mRsDepthInfoPubr;
    private Publisher<TFMessage> mTfPubr;

    private Thread mSensorPublishThread;

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

        Log.d(TAG, "onStart() creating SensorPublisherThread.");
        mSensorPublishThread = new SensorPublisherThread();
    }

    @Override
    public void onShutdown(Node node) {
        super.onShutdown(node);
        Sensor.getInstance().unbindService();
    }

    @Override
    public void onShutdownComplete(Node node) {
        super.onShutdownComplete(node);
        try {
            mSensorPublishThread.join();
        } catch (InterruptedException e) {
            Log.w(TAG, "onShutdownComplete: mSensorPublishThread.join() ", e);
        }
    }

    @Override
    public void onError(Node node, Throwable throwable) {
        super.onError(node, throwable);
        Sensor.getInstance().unbindService();
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("loomo_ros_bridge_node");
    }


    // TODO: Move TFs out of this class, into their own class.

//    ServiceBinder.BindStateListener mSensorBindListener = new ServiceBinder.BindStateListener() {
//        @Override
//        public void onBind() {
//            Log.i(TAG, "onBindSensor: ");
//            mSensor = Sensor.getInstance();
//            mSensorPublishThread.start();
//        }
//
//        @Override
//        public void onUnbind(String reason) {
//            Log.i(TAG, "onUnbindSensor: " + reason);
//            mSensor = null;
//            try {
//                mSensorPublishThread.join();
//            } catch (InterruptedException e) {
//                Log.w(TAG, "onUnbind: mSensorPublishThread.join() ", e);
//            }
//        }
//    };

//    private TransformStamped algoTf2TfStamped(AlgoTfData tfData, long stamp) {
//        Vector3 vector3 = mMessageFactory.newFromType(Vector3._TYPE);
//        vector3.setX(tfData.t.x);
//        vector3.setY(tfData.t.y);
//        vector3.setZ(tfData.t.z);
//        Quaternion quaternion = mMessageFactory.newFromType(Quaternion._TYPE);
//        quaternion.setX(tfData.q.x);
//        quaternion.setY(tfData.q.y);
//        quaternion.setZ(tfData.q.z);
//        quaternion.setW(tfData.q.w);
//        Transform transform = mMessageFactory.newFromType(Transform._TYPE);
//        transform.setTranslation(vector3);
//        transform.setRotation(quaternion);
//        TransformStamped transformStamped = mMessageFactory.newFromType(TransformStamped._TYPE);
//        transformStamped.setTransform(transform);
//        transformStamped.setChildFrameId(tfData.tgtFrameID);
//        transformStamped.getHeader().setFrameId(tfData.srcFrameID);
//        transformStamped.getHeader().setStamp(Time.fromMillis(Utils.platformStampInMillis(stamp)));
//        return transformStamped;
//    }

    private class SensorPublisherThread extends Thread {
        @Override
        public void run() {
            Log.d(TAG, "run: SensorPublisherThread");
        }
//            super.run();
//            final List<String> frameNames = Arrays.asList(Sensor.WORLD_ODOM_ORIGIN, Sensor.BASE_POSE_FRAME,
//                    Sensor.BASE_ODOM_FRAME, Sensor.NECK_POSE_FRAME, Sensor.HEAD_POSE_Y_FRAME,
//                    Sensor.RS_COLOR_FRAME, Sensor.RS_DEPTH_FRAME, Sensor.HEAD_POSE_P_R_FRAME,
//                    Sensor.PLATFORM_CAM_FRAME);
//            final List<Pair<Integer, Integer>> frameIndices = Arrays.asList(new Pair<>(0, 1),
//                    new Pair<>(1, 2), new Pair<>(2, 3), new Pair<>(3, 4), new Pair<>(4, 5),
//                    new Pair<>(4, 6), new Pair<>(4, 7), new Pair<>(7, 8));
//
//            while (null != mSensor) {
//                Long stamp = mDepthStamps.poll();
//                if (null != stamp) {
//                    TFMessage tfMessage = mTfPubr.newMessage();
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
//                    mTfPubr.publish(tfMessage);
////                    if (tfMessage.getTransforms().size() > 0)
////                        mTfPubr.publish(tfMessage);
//                }
//            }
//        }
    }
}
