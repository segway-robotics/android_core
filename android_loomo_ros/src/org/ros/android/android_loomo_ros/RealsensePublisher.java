package org.ros.android.android_loomo_ros;

import android.graphics.Bitmap;
import android.nfc.Tag;
import android.util.Log;

import com.segway.robot.sdk.vision.Vision;
import com.segway.robot.sdk.vision.calibration.Intrinsic;
import com.segway.robot.sdk.vision.frame.Frame;
import com.segway.robot.sdk.vision.stream.StreamInfo;
import com.segway.robot.sdk.vision.stream.StreamType;

import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.ros.internal.message.MessageBuffers;
import org.ros.message.Duration;
import org.ros.message.Time;
import org.ros.node.topic.Publisher;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import sensor_msgs.CameraInfo;
import sensor_msgs.CompressedImage;
import sensor_msgs.Image;
import tf2_msgs.TFMessage;

/**
 * Created by mfe on 7/17/18.
 */

public class RealsensePublisher {
    public static final String TAG = "RealsensePublisher";

    private Vision mVision;
    private LoomoRosBridgeNode mBridgeNode;

    public static final String RsDepthOpticalFrame = "rs_depth_optical_frame";
    public static final String RsColorOpticalFrame = "rs_color_optical_frame";

    private Intrinsic mRsColorIntrinsic, mRsDepthIntrinsic;
    private int mRsColorWidth = 640;
    private int mRsColorHeight = 480;
    private int mRsDepthWidth = 320;
    private int mRsDepthHeight = 240;

    private ChannelBufferOutputStream mRsColorOutStream, mRsDepthOutStream;
    private Queue<Long> mDepthStamps;
    private Bitmap mRsColorBitmap;

    boolean mIsPubRsColor;
    boolean mIsPubRsDepth;

    public RealsensePublisher(Vision mVision, LoomoRosBridgeNode mBridgeNode) {
        this.mVision = mVision;
        this.mBridgeNode = mBridgeNode;

        mDepthStamps = new ConcurrentLinkedDeque<>();
        mRsColorOutStream = new ChannelBufferOutputStream(MessageBuffers.dynamicBuffer());
        mRsDepthOutStream = new ChannelBufferOutputStream(MessageBuffers.dynamicBuffer());
    }

    public synchronized void start_all() {
        Log.d(TAG, "start_all() called");
        StreamInfo[] infos = mVision.getActivatedStreamInfo();
        for(StreamInfo info : infos) {
            switch (info.getStreamType()) {
                case StreamType.COLOR:
                    updateCameraInfo(2, mVision.getColorDepthCalibrationData().colorIntrinsic,
                            info.getWidth(), info.getHeight());
                    mVision.startListenFrame(StreamType.COLOR, mRsColorListener);
                    break;
                case StreamType.DEPTH:
                    updateCameraInfo(3, mVision.getColorDepthCalibrationData().depthIntrinsic,
                            info.getWidth(), info.getHeight());
                    mVision.startListenFrame(StreamType.DEPTH, mRsDepthListener);
                    break;
            }
        }
        Log.w(TAG, "start_all() done.");
    }

    public synchronized void stop_all() {
        Log.d(TAG, "stop_all() called");
        StreamInfo[] streamInfos = mVision.getActivatedStreamInfo();
        for (StreamInfo info : streamInfos) {
            switch (info.getStreamType()) {
                case StreamType.COLOR:
                    // Stop color listener
                    mVision.stopListenFrame(StreamType.COLOR);
                    break;
                case StreamType.DEPTH:
                    // Stop depth listener
                    mVision.stopListenFrame(StreamType.DEPTH);
                    break;
            }
        }
    }

    public synchronized void start_color(){
        Log.d(TAG, "start_color() called");
        updateCameraInfo(2, mVision.getColorDepthCalibrationData().colorIntrinsic,
                mRsColorWidth, mRsColorHeight);
        mVision.startListenFrame(StreamType.COLOR, mRsColorListener);
    }

    public synchronized void start_depth(){
        Log.d(TAG, "start_depth() called");
        updateCameraInfo(3, mVision.getColorDepthCalibrationData().depthIntrinsic,
                mRsDepthWidth, mRsDepthHeight);
        mVision.startListenFrame(StreamType.DEPTH, mRsDepthListener);
    }

    public synchronized void stop_color() {
        Log.d(TAG, "stop_color() called");
        mVision.stopListenFrame(StreamType.COLOR);
    }

    public synchronized void stop_depth() {
        Log.d(TAG, "stop_depth() called");
        mVision.stopListenFrame(StreamType.DEPTH);
    }

    Vision.FrameListener mRsColorListener = new Vision.FrameListener() {
        private double lastFrameStamp = 0.d; // in millisecond
        @Override
        public void onNewFrame(int streamType, Frame frame) {
            Log.d(TAG, "mRsColorListener onNewFrame...");
            if (!mIsPubRsColor) {
                Log.d(TAG, "mRsColorListener: !mIsPubRsColor");
                return;
            }
            if (streamType != StreamType.COLOR) {
                Log.e(TAG, "onNewFrame@mRsColorListener: stream type not COLOR! THIS IS A BUG");
                return;
            }
            if (mRsColorBitmap == null || mRsColorBitmap.getWidth() != mRsColorWidth
                    || mRsColorBitmap.getHeight() != mRsColorHeight) {
                mRsColorBitmap = Bitmap.createBitmap(mRsColorWidth, mRsColorHeight, Bitmap.Config.ARGB_8888);
            }

            mRsColorBitmap.copyPixelsFromBuffer(frame.getByteBuffer()); // copy once

            CompressedImage image = mBridgeNode.mRsColorCompressedPubr.newMessage();
            image.setFormat("jpeg");
//            image.getHeader().setStamp(currentTime);
            image.getHeader().setFrameId(RsColorOpticalFrame);

            mRsColorBitmap.compress(Bitmap.CompressFormat.JPEG, 100, mRsColorOutStream);
            image.setData(mRsColorOutStream.buffer().copy());              // copy twice

            mRsColorOutStream.buffer().clear();

            mBridgeNode.mRsColorCompressedPubr.publish(image);
            publishCameraInfo(2, image.getHeader());
        }
    };

    Vision.FrameListener mRsDepthListener = new Vision.FrameListener() {

        private double lastFrameStamp = 0.d; // in millisecond
        @Override
        public void onNewFrame(int streamType, Frame frame) {
            Log.d(TAG, "mRsDepthListener onNewFrame...");
            if (!mIsPubRsDepth)
                return;
            if (streamType != StreamType.DEPTH) {
                Log.e(TAG, "onNewFrame@mRsDepthListener: stream type not DEPTH! THIS IS A BUG");
                return;
            }
            mDepthStamps.add(frame.getInfo().getPlatformTimeStamp());

            Image image = mBridgeNode.mRsDepthPubr.newMessage();
            image.setWidth(mRsDepthWidth);
            image.setHeight(mRsDepthHeight);
            image.setStep(mRsDepthWidth * 2);
            image.setEncoding("mono16");

//            image.getHeader().setStamp(currentTime);
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

            mBridgeNode.mRsDepthPubr.publish(image);
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
        Log.d(TAG, "publish camerainfo...");
        Publisher<CameraInfo> pubr;
        CameraInfo info;
        Intrinsic intrinsic;
        int width, height;
        // type: 1 for pcam, 2 for RsColor, 3 for RsDepth
        if (type == 1) {
            // Currently does not have camera info of platform camera
            Log.d(TAG, "publishCameraInfo type==1 -> not implemented.");
            return;
        } else if (type == 2) {
            Log.d(TAG, "publishCameraInfo type==2.");
            pubr = mBridgeNode.mRsColorInfoPubr;
            intrinsic = mRsColorIntrinsic;
            width = mRsColorWidth;
            height = mRsColorHeight;
        }
        else {
            pubr = mBridgeNode.mRsDepthInfoPubr;
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
        Log.d(TAG, "published camerainfo...");
    }
}
