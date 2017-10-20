package org.ros.android.android_tutorial_camera;

import android.graphics.Bitmap;
import android.util.Log;

import com.segway.robot.sdk.vision.Vision;
import com.segway.robot.sdk.vision.calibration.Intrinsic;
import com.segway.robot.sdk.vision.frame.Frame;
import com.segway.robot.sdk.vision.stream.StreamType;

import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.ros.internal.message.MessageBuffers;
import org.ros.message.Duration;
import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.PublisherListener;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import sensor_msgs.CameraInfo;
import sensor_msgs.CompressedImage;
import sensor_msgs.Image;

/**
 * Created by kai on 17-7-16.
 */

public class LoomoRosBridgeNode extends AbstractNodeMain {
    private static final String TAG = "LoomoRosBridgeNode";

    private Intrinsic mRsColorIntrinsic, mRsDepthIntrinsic;
    private int mRsColorWidth = 640;
    private int mRsColorHeight = 480;
    private int mRsDepthWidth = 320;
    private int mRsDepthHeight = 240;

    private ConnectedNode mConnectedNode;
    private Publisher<Image> mPcamPubr;
    private Publisher<CameraInfo> mPcamInfoPubr;
    private Publisher<Image> mRsColorPubr;
    private Publisher<CompressedImage> mRsColorCompressedPubr;
    private Publisher<Image> mRsDepthPubr;
    private Publisher<CameraInfo> mRsColorInfoPubr;
    private Publisher<CameraInfo> mRsDepthInfoPubr;

    private Bitmap mRsColorBitmap;
    private ChannelBufferOutputStream mRsColorOutStream, mRsDepthOutStream;

    public static final String RsDepthOpticalFrame = "rs_depth_optical_frame";
    public static final String RsColorOpticalFrame = "rs_color_optical_frame";

    public LoomoRosBridgeNode() {
        super();
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        super.onStart(connectedNode);

        mRsColorOutStream = new ChannelBufferOutputStream(MessageBuffers.dynamicBuffer());
        mRsDepthOutStream = new ChannelBufferOutputStream(MessageBuffers.dynamicBuffer());

        mConnectedNode = connectedNode;
        mPcamPubr = connectedNode.newPublisher("loomo/pcam/rgb", Image._TYPE);
        mPcamInfoPubr = connectedNode.newPublisher("loomo/pcam/camera_info", CameraInfo._TYPE);
        mRsColorPubr = connectedNode.newPublisher("loomo/realsense/rgb", Image._TYPE);
        mRsColorCompressedPubr = connectedNode.newPublisher("loomo/realsense/rgb/compressed", CompressedImage._TYPE);
        mRsColorInfoPubr = connectedNode.newPublisher("loomo/realsense/rgb/camera_info", CameraInfo._TYPE);
        mRsDepthPubr = connectedNode.newPublisher("loomo/realsense/depth", Image._TYPE);
        mRsDepthInfoPubr = connectedNode.newPublisher("loomo/realsense/depth/camera_info", CameraInfo._TYPE);

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

    Vision.FrameListener mRsColorListener = new Vision.FrameListener() {
        @Override
        public void onNewFrame(int streamType, Frame frame) {
            synchronized (Vision.FrameListener.class) {
                if (streamType != StreamType.COLOR) {
                    Log.e(TAG, "onNewFrame@mRsColorListener: stream type not COLOR! THIS IS A BUG");
                    return;
                }
                if (mRsColorBitmap == null || mRsColorBitmap.getWidth() != mRsColorWidth
                        || mRsColorBitmap.getHeight() != mRsColorHeight) {
                    mRsColorBitmap = Bitmap.createBitmap(mRsColorWidth, mRsColorHeight, Bitmap.Config.ARGB_8888);
                }
                String frameId = RsColorOpticalFrame;
                Time currentTime = mConnectedNode.getCurrentTime();
                mRsColorBitmap.copyPixelsFromBuffer(frame.getByteBuffer()); // copy once

                CompressedImage image = mRsColorCompressedPubr.newMessage();
                image.setFormat("jpeg");
                image.getHeader().setStamp(currentTime);
                image.getHeader().setFrameId(frameId);

                mRsColorBitmap.compress(Bitmap.CompressFormat.JPEG, 100, mRsColorOutStream);
                image.setData(mRsColorOutStream.buffer().copy());              // copy twice

                Duration diff = mConnectedNode.getCurrentTime().subtract(currentTime);
                Log.w(TAG, String.format("publishRsColor: capacity,readableBytes[%d,%d]KB, time[%g]",
                        mRsColorOutStream.buffer().capacity() / 1024, mRsColorOutStream.buffer().readableBytes() / 1024,
                        (double) diff.totalNsecs() / 1.0E9D));
                mRsColorOutStream.buffer().clear();

                mRsColorCompressedPubr.publish(image);
                publishCameraInfo(2, image.getHeader());
            }
        }
    };

    Vision.FrameListener mRsDepthListener = new Vision.FrameListener() {
        @Override
        public void onNewFrame(int streamType, Frame frame) {
            synchronized (Vision.FrameListener.class) {
                if (streamType != StreamType.DEPTH) {
                    Log.e(TAG, "onNewFrame@mRsDepthListener: stream type not DEPTH! THIS IS A BUG");
                    return;
                }
                Time currentTime = mConnectedNode.getCurrentTime();
                String frameId = RsDepthOpticalFrame;

                Image image = mRsDepthPubr.newMessage();
                image.setWidth(mRsDepthWidth);
                image.setHeight(mRsDepthHeight);
                image.setStep(mRsDepthWidth * 2);
                image.setEncoding("mono16");

                image.getHeader().setStamp(currentTime);
                image.getHeader().setFrameId(frameId);

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
                Log.w(TAG, String.format("publishRsDepth: capacity,readableBytes[%d,%d]KB, time[%g]",
                        image.getData().capacity() / 1024, image.getData().readableBytes() / 1024,
                        (double) diff.totalNsecs() / 1.0E9D));

                mRsDepthPubr.publish(image);
                publishCameraInfo(3, image.getHeader());
            }
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

    private void publishCameraInfo(int type, std_msgs.Header header) {
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
}
