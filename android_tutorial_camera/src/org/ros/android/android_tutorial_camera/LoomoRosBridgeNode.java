package org.ros.android.android_tutorial_camera;

import android.graphics.Bitmap;
import android.util.Log;
import android.util.Pair;

import com.segway.robot.algo.tf.AlgoTfData;
import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.perception.sensor.Sensor;
import com.segway.robot.sdk.vision.Vision;
import com.segway.robot.sdk.vision.calibration.Intrinsic;
import com.segway.robot.sdk.vision.frame.Frame;
import com.segway.robot.sdk.vision.stream.StreamType;

import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.ros.internal.message.MessageBuffers;
import org.ros.message.Duration;
import org.ros.message.MessageFactory;
import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Publisher;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import geometry_msgs.Quaternion;
import geometry_msgs.Transform;
import geometry_msgs.TransformStamped;
import geometry_msgs.Vector3;
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

    public static final String RsDepthOpticalFrame = "rs_depth_optical_frame";
    public static final String RsColorOpticalFrame = "rs_color_optical_frame";

    private Intrinsic mRsColorIntrinsic, mRsDepthIntrinsic;
    private int mRsColorWidth = 640;
    private int mRsColorHeight = 480;
    private int mRsDepthWidth = 320;
    private int mRsDepthHeight = 240;

    private ConnectedNode mConnectedNode;
    private MessageFactory mMessageFactory;
    private Publisher<Image> mPcamPubr;
    private Publisher<CameraInfo> mPcamInfoPubr;
    private Publisher<Image> mRsColorPubr;
    private Publisher<CompressedImage> mRsColorCompressedPubr;
    private Publisher<Image> mRsDepthPubr;
    private Publisher<CameraInfo> mRsColorInfoPubr;
    private Publisher<CameraInfo> mRsDepthInfoPubr;
    private Publisher<TFMessage> mTfPubr;
    boolean mIsPubRsColor;
    boolean mIsPubRsDepth;

    private Bitmap mRsColorBitmap;
    private ChannelBufferOutputStream mRsColorOutStream, mRsDepthOutStream;
    private Queue<Long> mDepthStamps;

    private Thread mSensorPublishThread;

    public LoomoRosBridgeNode() {
        super();
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        super.onStart(connectedNode);

        mDepthStamps = new ConcurrentLinkedDeque<>();
        mRsColorOutStream = new ChannelBufferOutputStream(MessageBuffers.dynamicBuffer());
        mRsDepthOutStream = new ChannelBufferOutputStream(MessageBuffers.dynamicBuffer());

        mConnectedNode = connectedNode;
        mMessageFactory = connectedNode.getTopicMessageFactory();
        mPcamPubr = connectedNode.newPublisher("loomo/pcam/rgb", Image._TYPE);
        mPcamInfoPubr = connectedNode.newPublisher("loomo/pcam/camera_info", CameraInfo._TYPE);
        mRsColorPubr = connectedNode.newPublisher("loomo/realsense/rgb", Image._TYPE);
        mRsColorCompressedPubr = connectedNode.newPublisher("loomo/realsense/rgb/compressed", CompressedImage._TYPE);
        mRsColorInfoPubr = connectedNode.newPublisher("loomo/realsense/rgb/camera_info", CameraInfo._TYPE);
        mRsDepthPubr = connectedNode.newPublisher("loomo/realsense/depth", Image._TYPE);
        mRsDepthInfoPubr = connectedNode.newPublisher("loomo/realsense/depth/camera_info", CameraInfo._TYPE);
        mTfPubr = connectedNode.newPublisher("/tf", TFMessage._TYPE);

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

    ServiceBinder.BindStateListener mSensorBindListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            Log.i(TAG, "onBindSensor: ");
            mSensor = Sensor.getInstance();
            mSensorPublishThread.start();
        }

        @Override
        public void onUnbind(String reason) {
            Log.i(TAG, "onUnbindSensor: " + reason);
            mSensor = null;
            try {
                mSensorPublishThread.join();
            } catch (InterruptedException e) {
                Log.w(TAG, "onUnbind: mSensorPublishThread.join() ", e);
            }
        }
    };

    Vision.FrameListener mRsColorListener = new Vision.FrameListener() {
        private double lastFrameStamp = 0.d; // in millisecond
        @Override
        public void onNewFrame(int streamType, Frame frame) {
            if (!mIsPubRsColor)
                return;
            if (streamType != StreamType.COLOR) {
                Log.e(TAG, "onNewFrame@mRsColorListener: stream type not COLOR! THIS IS A BUG");
                return;
            }
            double stampMsecond = Utils.platformStampInMillis(frame.getInfo().getPlatformTimeStamp());
            double lastDiff = stampMsecond - lastFrameStamp;
            if (lastFrameStamp > 0.1 && Math.abs(lastDiff) > 31) {
                Log.d(TAG, "onNewFrame@mRsColorListener: dropped frame diff is: " + lastDiff + "ms");
                lastFrameStamp = stampMsecond;
                return;
            }
            lastFrameStamp = stampMsecond;
            if (mRsColorBitmap == null || mRsColorBitmap.getWidth() != mRsColorWidth
                    || mRsColorBitmap.getHeight() != mRsColorHeight) {
                mRsColorBitmap = Bitmap.createBitmap(mRsColorWidth, mRsColorHeight, Bitmap.Config.ARGB_8888);
            }
//                Time currentTime = mConnectedNode.getCurrentTime();
            Time currentTime = new Time(stampMsecond / 1000);
            mRsColorBitmap.copyPixelsFromBuffer(frame.getByteBuffer()); // copy once

            CompressedImage image = mRsColorCompressedPubr.newMessage();
            image.setFormat("jpeg");
            image.getHeader().setStamp(currentTime);
            image.getHeader().setFrameId(RsColorOpticalFrame);

            mRsColorBitmap.compress(Bitmap.CompressFormat.JPEG, 100, mRsColorOutStream);
            image.setData(mRsColorOutStream.buffer().copy());              // copy twice

            Duration diff = mConnectedNode.getCurrentTime().subtract(currentTime);
//            Log.d(TAG, String.format("publishRsColor: diff[%g]ms, time[%d], ros time[%d]",
//                    (double) diff.totalNsecs() / 1.0E6D, frame.getInfo().getPlatformTimeStamp(),
//                    currentTime.totalNsecs()));
            mRsColorOutStream.buffer().clear();

            mRsColorCompressedPubr.publish(image);
            publishCameraInfo(2, image.getHeader());
        }
    };


    Vision.FrameListener mRsDepthListener = new Vision.FrameListener() {
        private double lastFrameStamp = 0.d; // in millisecond
        @Override
        public void onNewFrame(int streamType, Frame frame) {
            if (!mIsPubRsDepth)
                return;
            if (streamType != StreamType.DEPTH) {
                Log.e(TAG, "onNewFrame@mRsDepthListener: stream type not DEPTH! THIS IS A BUG");
                return;
            }
            mDepthStamps.add(frame.getInfo().getPlatformTimeStamp());
            double stampMsecond = Utils.platformStampInMillis(frame.getInfo().getPlatformTimeStamp());
            double lastDiff = stampMsecond - lastFrameStamp;
            if (lastFrameStamp > 0.1 && Math.abs(lastDiff) > 40) {
                Log.d(TAG, "onNewFrame@mRsDepthListener: dropped frame diff is: " + lastDiff + "ms");
                lastFrameStamp = stampMsecond;
                return;
            }
            lastFrameStamp = stampMsecond;
//                Time currentTime = mConnectedNode.getCurrentTime();
            Time currentTime = new Time(stampMsecond / 1000);

            Image image = mRsDepthPubr.newMessage();
            image.setWidth(mRsDepthWidth);
            image.setHeight(mRsDepthHeight);
            image.setStep(mRsDepthWidth * 2);
            image.setEncoding("mono16");

            image.getHeader().setStamp(currentTime);
            image.getHeader().setFrameId(RsDepthOpticalFrame);

            try {
                WritableByteChannel channel = Channels.newChannel(mRsDepthOutStream);
                channel.write(frame.getByteBuffer());
            } catch (IOException exception) {
                Log.e(TAG, String.format("publishRsDepth: IO Exception[%s]", exception.getMessage()));
                return;
            }
            image.setData(mRsDepthOutStream.buffer().copy());
            mRsDepthOutStream.buffer().clear();

            Duration diff = mConnectedNode.getCurrentTime().subtract(currentTime);
//            Log.d(TAG, String.format("publishRsDepth: diff[%g]ms, time[%d], ros time[%d]",
//                    (double) diff.totalNsecs() / 1.0E6D, frame.getInfo().getPlatformTimeStamp(),
//                    currentTime.totalNsecs()));

            mRsDepthPubr.publish(image);
            publishCameraInfo(3, image.getHeader());
        }
    };

    public void updateCameraInfo(int type, Intrinsic ins, int width, int height) {
        if (type == 1) {
            // platform camera intrinsic not supported yet
            Log.w(TAG, "updateCameraInfo: platform camera intrinsic not supported yet!");
        } else if (type == 2) {
            mRsColorIntrinsic = ins;
            mRsColorWidth = width;
            mRsColorHeight = height;
        } else {
            mRsDepthIntrinsic = ins;
            mRsDepthWidth = width;
            mRsDepthHeight = height;
        }
    }

    private synchronized void publishCameraInfo(int type, std_msgs.Header header) {
        Publisher<CameraInfo> pubr;
        CameraInfo info;
        Intrinsic intrinsic;
        int width, height;
        // type: 1 for pcam, 2 for RsColor, 3 for RsDepth
        if (type == 1) {
            // Currently does not have camera info of platform camera
            return;
        } else if (type == 2) {
            pubr = mRsColorInfoPubr;
            intrinsic = mRsColorIntrinsic;
            width = mRsColorWidth;
            height = mRsColorHeight;
        } else {
            pubr = mRsDepthInfoPubr;
            intrinsic = mRsDepthIntrinsic;
            width = mRsDepthWidth;
            height = mRsDepthHeight;
        }

        info = pubr.newMessage();
        double[] k = new double[9];

//        # Intrinsic camera matrix for the raw (distorted) images.
//        #     [fx  0 cx]
//        # K = [ 0 fy cy]
//        #     [ 0  0  1]
        k[0] = intrinsic.focalLength.x;
        k[4] = intrinsic.focalLength.y;
        k[2] = intrinsic.principal.x;
        k[5] = intrinsic.principal.y;
        k[8] = 1;

        info.setHeader(header);
        info.setWidth(width);
        info.setHeight(height);
        info.setK(k);

        pubr.publish(info);
    }

    private TransformStamped algoTf2TfStamped(AlgoTfData tfData, long stamp) {
        Vector3 vector3 = mMessageFactory.newFromType(Vector3._TYPE);
        vector3.setX(tfData.t.x);
        vector3.setY(tfData.t.y);
        vector3.setZ(tfData.t.z);
        Quaternion quaternion = mMessageFactory.newFromType(Quaternion._TYPE);
        quaternion.setX(tfData.q.x);
        quaternion.setY(tfData.q.y);
        quaternion.setZ(tfData.q.z);
        quaternion.setW(tfData.q.w);
        Transform transform = mMessageFactory.newFromType(Transform._TYPE);
        transform.setTranslation(vector3);
        transform.setRotation(quaternion);
        TransformStamped transformStamped = mMessageFactory.newFromType(TransformStamped._TYPE);
        transformStamped.setTransform(transform);
        transformStamped.setChildFrameId(tfData.tgtFrameID);
        transformStamped.getHeader().setFrameId(tfData.srcFrameID);
        transformStamped.getHeader().setStamp(Time.fromMillis(Utils.platformStampInMillis(stamp)));
        return transformStamped;
    }

    private class SensorPublisherThread extends Thread {
        @Override
        public void run() {
            super.run();
            final List<String> frameNames = Arrays.asList(Sensor.WORLD_ODOM_ORIGIN, Sensor.BASE_POSE_FRAME,
                    Sensor.BASE_ODOM_FRAME, Sensor.NECK_POSE_FRAME, Sensor.HEAD_POSE_Y_FRAME,
                    Sensor.RS_COLOR_FRAME, Sensor.RS_DEPTH_FRAME, Sensor.HEAD_POSE_P_R_FRAME,
                    Sensor.PLATFORM_CAM_FRAME);
            final List<Pair<Integer, Integer>> frameIndices = Arrays.asList(new Pair<>(0, 1),
                    new Pair<>(1, 2), new Pair<>(2, 3), new Pair<>(3, 4), new Pair<>(4, 5),
                    new Pair<>(4, 6), new Pair<>(4, 7), new Pair<>(7, 8));

            while (null != mSensor) {
                Long stamp = mDepthStamps.poll();
                if (null != stamp) {
                    TFMessage tfMessage = mTfPubr.newMessage();
                    for (Pair<Integer, Integer> index : frameIndices) {
                        String target = frameNames.get(index.second);
                        String source = frameNames.get(index.first);
                        AlgoTfData tfData = mSensor.getTfData(source, target, stamp, 500);
                        if (stamp != tfData.timeStamp) {
                            Log.d(TAG, String.format("run: getTfData failed for frames[%d]: %s -> %s",
                                    stamp, source, target));
                            continue;
                        }
                        TransformStamped transformStamped = algoTf2TfStamped(tfData, stamp);
                        tfMessage.getTransforms().add(transformStamped);
                    }
                    if (tfMessage.getTransforms().size() > 0)
                        mTfPubr.publish(tfMessage);
                }
            }
            Log.d(TAG, "run: exit SensorPublisherThread");
        }
    }
}
