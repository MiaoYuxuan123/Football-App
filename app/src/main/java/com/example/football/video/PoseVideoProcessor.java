package com.example.football.video;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class PoseVideoProcessor {

    public interface Callback {
        void onProgress(int progress);

        void onCompleted(@NonNull String outputPath);

        void onFailed(@NonNull Exception error);
    }

    private static final String TAG = "PoseVideoProcessor";
    private static final long TIMEOUT_US = 10_000;
    private static final long EOS_QUEUE_TIMEOUT_US = 200_000;

    private final PoseFrameDrawer frameDrawer = new PoseFrameDrawer();

    public void process(
            @NonNull Context context,
            @NonNull String inputPath,
            @NonNull String outputPath,
            @NonNull Callback callback
    ) {
        try {
            processInternal(context, inputPath, outputPath, callback);
            callback.onCompleted(outputPath);
        } catch (Exception e) {
            callback.onFailed(e);
        }
    }

    private void processInternal(
            @NonNull Context context,
            @NonNull String inputPath,
            @NonNull String outputPath,
            @NonNull Callback callback
    ) throws Exception {
        File inputFile = new File(inputPath);
        if (!inputFile.exists()) {
            throw new IllegalArgumentException("Input video does not exist: " + inputPath);
        }

        VideoMeta meta = readVideoMeta(inputPath);

        PoseLandmarker poseLandmarker = createVideoPoseLandmarker(context);
        MediaExtractor videoExtractor = new MediaExtractor();
        MediaExtractor audioExtractor = new MediaExtractor();
        MediaCodec decoder = null;
        MediaCodec encoder = null;
        MediaMuxer muxer = null;
        ImageReader imageReader = null;

        try {
            videoExtractor.setDataSource(inputPath);
            int videoTrackIndex = selectTrack(videoExtractor, "video/");
            if (videoTrackIndex < 0) {
                throw new IllegalStateException("No video track in input file");
            }
            videoExtractor.selectTrack(videoTrackIndex);

            MediaFormat inputVideoFormat = videoExtractor.getTrackFormat(videoTrackIndex);
            int width = inputVideoFormat.getInteger(MediaFormat.KEY_WIDTH);
            int height = inputVideoFormat.getInteger(MediaFormat.KEY_HEIGHT);
            FrameBuffers frameBuffers = new FrameBuffers(width, height);

            imageReader = ImageReader.newInstance(width, height, android.graphics.ImageFormat.YUV_420_888, 3);

            String inVideoMime = inputVideoFormat.getString(MediaFormat.KEY_MIME);
            if (inVideoMime == null) {
                throw new IllegalStateException("Input video mime is null");
            }
            decoder = MediaCodec.createDecoderByType(inVideoMime);
            decoder.configure(inputVideoFormat, imageReader.getSurface(), null, 0);
            decoder.start();

            MediaFormat outVideoFormat = MediaFormat.createVideoFormat("video/avc", width, height);
            outVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE, estimateBitrate(width, height));
            outVideoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, meta.frameRate);
            outVideoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            outVideoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);

            encoder = MediaCodec.createEncoderByType("video/avc");
            try {
                encoder.configure(outVideoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            } catch (Exception semiPlanarErr) {
                Log.w(TAG, "COLOR_FormatYUV420SemiPlanar not accepted, fallback to flexible", semiPlanarErr);
                outVideoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
                encoder.configure(outVideoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            }
            encoder.start();

            File outputFile = new File(outputPath);
            if (outputFile.exists() && !outputFile.delete()) {
                throw new IllegalStateException("Cannot overwrite output file: " + outputPath);
            }

            muxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            if (meta.rotationDegrees != 0) {
                muxer.setOrientationHint(meta.rotationDegrees);
            }

            audioExtractor.setDataSource(inputPath);
            int sourceAudioTrack = selectTrack(audioExtractor, "audio/");
            MediaFormat audioFormat = null;
            if (sourceAudioTrack >= 0) {
                audioFormat = audioExtractor.getTrackFormat(sourceAudioTrack);
            }

            MediaCodec.BufferInfo decoderInfo = new MediaCodec.BufferInfo();
            MediaCodec.BufferInfo encoderInfo = new MediaCodec.BufferInfo();

            int muxerVideoTrack = -1;
            int muxerAudioTrack = -1;
            boolean muxerStarted = false;
            boolean decoderInputDone = false;
            boolean decoderOutputDone = false;
            boolean encoderOutputDone = false;

            long lastPtsUs = 0;
            while (!encoderOutputDone) {
                if (!decoderInputDone) {
                    int inputIndex = decoder.dequeueInputBuffer(TIMEOUT_US);
                    if (inputIndex >= 0) {
                        ByteBuffer inputBuffer = decoder.getInputBuffer(inputIndex);
                        if (inputBuffer == null) {
                            throw new IllegalStateException("Decoder input buffer is null");
                        }
                        int sampleSize = videoExtractor.readSampleData(inputBuffer, 0);
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            decoderInputDone = true;
                        } else {
                            long sampleTimeUs = videoExtractor.getSampleTime();
                            decoder.queueInputBuffer(inputIndex, 0, sampleSize, sampleTimeUs, 0);
                            videoExtractor.advance();
                        }
                    }
                }

                if (!decoderOutputDone) {
                    int outIndex = decoder.dequeueOutputBuffer(decoderInfo, TIMEOUT_US);
                    if (outIndex >= 0) {
                        boolean isEos = (decoderInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                        boolean shouldRender = !isEos;
                        decoder.releaseOutputBuffer(outIndex, shouldRender);

                        if (shouldRender) {
                            Image image = acquireImage(imageReader);
                            if (image != null) {
                                Bitmap frameBitmap = yuv420ToBitmap(image, frameBuffers);
                                image.close();

                                long timestampMs = Math.max(0, decoderInfo.presentationTimeUs / 1000);
                                PoseLandmarkerResult poseResult = poseLandmarker.detectForVideo(
                                        new BitmapImageBuilder(frameBitmap).build(),
                                        timestampMs
                                );
                                frameDrawer.draw(frameBitmap, poseResult, false);

                                byte[] yuv = bitmapToNV12(frameBitmap, frameBuffers);
                                queueEncoderFrame(encoder, yuv, decoderInfo.presentationTimeUs, false);
                                lastPtsUs = decoderInfo.presentationTimeUs;

                                int progress = (int) Math.min(100,
                                        meta.durationUs <= 0 ? 0 : (decoderInfo.presentationTimeUs * 100L / meta.durationUs));
                                callback.onProgress(progress);
                            }
                        }

                        drainEncoder(encoder, encoderInfo, muxer, audioFormat,
                                callback, meta, false,
                                new MuxStateHolder(muxerVideoTrack, muxerAudioTrack, muxerStarted));

                        MuxStateHolder state = MuxStateHolder.LAST_STATE;
                        muxerVideoTrack = state.videoTrack;
                        muxerAudioTrack = state.audioTrack;
                        muxerStarted = state.started;

                        if (isEos) {
                            decoderOutputDone = true;
                            queueEncoderEos(encoder, lastPtsUs + 1_000_000L / Math.max(1, meta.frameRate));
                        }
                    }
                }

                drainEncoder(encoder, encoderInfo, muxer, audioFormat,
                        callback, meta, decoderOutputDone,
                        new MuxStateHolder(muxerVideoTrack, muxerAudioTrack, muxerStarted));

                MuxStateHolder state = MuxStateHolder.LAST_STATE;
                muxerVideoTrack = state.videoTrack;
                muxerAudioTrack = state.audioTrack;
                muxerStarted = state.started;

                if (state.encoderDone) {
                    encoderOutputDone = true;
                }
            }

            if (muxerStarted && sourceAudioTrack >= 0) {
                copyAudioTrack(audioExtractor, sourceAudioTrack, muxer, muxerAudioTrack);
            }

            callback.onProgress(100);
        } finally {
            if (poseLandmarker != null) {
                poseLandmarker.close();
            }
            safeStopRelease(decoder);
            safeStopRelease(encoder);
            if (imageReader != null) {
                imageReader.close();
            }
            videoExtractor.release();
            audioExtractor.release();
            if (muxer != null) {
                try {
                    muxer.stop();
                } catch (Exception ignored) {
                }
                muxer.release();
            }
        }
    }

    private PoseLandmarker createVideoPoseLandmarker(@NonNull Context context) {
        BaseOptions baseOptions = BaseOptions.builder()
                .setModelAssetPath("pose_landmarker_lite.task")
                .build();

        PoseLandmarker.PoseLandmarkerOptions options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.VIDEO)
                .setNumPoses(1)
                .setMinPoseDetectionConfidence(0.5f)
                .setMinPosePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .build();

        return PoseLandmarker.createFromOptions(context, options);
    }

    private static int selectTrack(@NonNull MediaExtractor extractor, @NonNull String mimePrefix) {
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith(mimePrefix)) {
                return i;
            }
        }
        return -1;
    }

    private static int estimateBitrate(int width, int height) {
        int base = width * height * 5;
        return Math.max(base, 2_000_000);
    }

    @Nullable
    private Image acquireImage(@NonNull ImageReader imageReader) {
        long deadline = SystemClock.elapsedRealtime() + 300;
        while (SystemClock.elapsedRealtime() < deadline) {
            Image image = imageReader.acquireLatestImage();
            if (image != null) {
                return image;
            }
        }
        return null;
    }

    private void queueEncoderFrame(
            @NonNull MediaCodec encoder,
            @NonNull byte[] frameData,
            long ptsUs,
            boolean eos
    ) {
        int inputIndex = encoder.dequeueInputBuffer(TIMEOUT_US);
        if (inputIndex < 0) {
            return;
        }
        ByteBuffer inputBuffer = encoder.getInputBuffer(inputIndex);
        if (inputBuffer == null) {
            return;
        }

        inputBuffer.clear();
        int flags = eos ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0;
        int size = 0;
        if (!eos) {
            size = Math.min(inputBuffer.capacity(), frameData.length);
            inputBuffer.put(frameData, 0, size);
        }
        encoder.queueInputBuffer(inputIndex, 0, size, Math.max(0, ptsUs), flags);
    }

    private void queueEncoderEos(@NonNull MediaCodec encoder, long ptsUs) {
        long end = SystemClock.elapsedRealtimeNanos() / 1000 + EOS_QUEUE_TIMEOUT_US;
        while ((SystemClock.elapsedRealtimeNanos() / 1000) < end) {
            int inputIndex = encoder.dequeueInputBuffer(TIMEOUT_US);
            if (inputIndex < 0) {
                continue;
            }
            ByteBuffer inputBuffer = encoder.getInputBuffer(inputIndex);
            if (inputBuffer != null) {
                inputBuffer.clear();
            }
            encoder.queueInputBuffer(inputIndex, 0, 0, Math.max(0, ptsUs), MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            return;
        }
        throw new IllegalStateException("Unable to queue encoder EOS in time");
    }

    private void drainEncoder(
            @NonNull MediaCodec encoder,
            @NonNull MediaCodec.BufferInfo encoderInfo,
            @NonNull MediaMuxer muxer,
            @Nullable MediaFormat audioFormat,
            @NonNull Callback callback,
            @NonNull VideoMeta meta,
            boolean decoderOutputDone,
            @NonNull MuxStateHolder state
    ) {
        boolean encoderDone = false;
        while (true) {
            int outIndex = encoder.dequeueOutputBuffer(encoderInfo, decoderOutputDone ? TIMEOUT_US : 0);
            if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                break;
            }

            if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                state.videoTrack = muxer.addTrack(encoder.getOutputFormat());
                if (audioFormat != null) {
                    state.audioTrack = muxer.addTrack(audioFormat);
                }
                muxer.start();
                state.started = true;
                continue;
            }

            if (outIndex < 0) {
                continue;
            }

            ByteBuffer encodedData = encoder.getOutputBuffer(outIndex);
            if (encodedData == null) {
                encoder.releaseOutputBuffer(outIndex, false);
                continue;
            }

            if ((encoderInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                encoderInfo.size = 0;
            }

            if (encoderInfo.size > 0 && state.started) {
                encodedData.position(encoderInfo.offset);
                encodedData.limit(encoderInfo.offset + encoderInfo.size);
                muxer.writeSampleData(state.videoTrack, encodedData, encoderInfo);

                int progress = (int) Math.min(100,
                        meta.durationUs <= 0 ? 0 : (encoderInfo.presentationTimeUs * 100L / meta.durationUs));
                callback.onProgress(progress);
            }

            boolean eos = (encoderInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
            encoder.releaseOutputBuffer(outIndex, false);
            if (eos) {
                encoderDone = true;
                break;
            }
        }
        state.encoderDone = encoderDone;
        MuxStateHolder.LAST_STATE = state;
    }

    private void copyAudioTrack(
            @NonNull MediaExtractor audioExtractor,
            int sourceAudioTrack,
            @NonNull MediaMuxer muxer,
            int muxerAudioTrack
    ) {
        if (muxerAudioTrack < 0) {
            return;
        }

        audioExtractor.selectTrack(sourceAudioTrack);
        ByteBuffer audioBuffer = ByteBuffer.allocate(256 * 1024);
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        while (true) {
            int sampleSize = audioExtractor.readSampleData(audioBuffer, 0);
            if (sampleSize < 0) {
                break;
            }
            info.offset = 0;
            info.size = sampleSize;
            info.presentationTimeUs = audioExtractor.getSampleTime();
            int sampleFlags = audioExtractor.getSampleFlags();
            int codecFlags = 0;
            if ((sampleFlags & MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                codecFlags |= MediaCodec.BUFFER_FLAG_KEY_FRAME;
            }
            if ((sampleFlags & MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME) != 0) {
                codecFlags |= MediaCodec.BUFFER_FLAG_PARTIAL_FRAME;
            }
            info.flags = codecFlags;
            muxer.writeSampleData(muxerAudioTrack, audioBuffer, info);
            audioExtractor.advance();
        }
    }

    private void safeStopRelease(@Nullable MediaCodec codec) {
        if (codec == null) {
            return;
        }
        try {
            codec.stop();
        } catch (Exception ignored) {
        }
        codec.release();
    }

    @NonNull
    private Bitmap yuv420ToBitmap(@NonNull Image image, @NonNull FrameBuffers frameBuffers) {
        int width = image.getWidth();
        int height = image.getHeight();

        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int yRowStride = planes[0].getRowStride();
        int yPixelStride = planes[0].getPixelStride();
        int uRowStride = planes[1].getRowStride();
        int uPixelStride = planes[1].getPixelStride();
        int vRowStride = planes[2].getRowStride();
        int vPixelStride = planes[2].getPixelStride();

        int[] out = frameBuffers.argbPixels;

        for (int y = 0; y < height; y++) {
            int yBase = y * yRowStride;
            int uvBase = (y / 2) * uRowStride;
            int vvBase = (y / 2) * vRowStride;
            for (int x = 0; x < width; x++) {
                int yValue = yBuffer.get(yBase + x * yPixelStride) & 0xFF;
                int uValue = uBuffer.get(uvBase + (x / 2) * uPixelStride) & 0xFF;
                int vValue = vBuffer.get(vvBase + (x / 2) * vPixelStride) & 0xFF;

                int c = yValue - 16;
                int d = uValue - 128;
                int e = vValue - 128;

                int r = clamp((298 * c + 409 * e + 128) >> 8);
                int g = clamp((298 * c - 100 * d - 208 * e + 128) >> 8);
                int b = clamp((298 * c + 516 * d + 128) >> 8);
                out[y * width + x] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        }

        frameBuffers.frameBitmap.setPixels(out, 0, width, 0, 0, width, height);
        return frameBuffers.frameBitmap;
    }

    @NonNull
    private byte[] bitmapToNV12(@NonNull Bitmap bitmap, @NonNull FrameBuffers frameBuffers) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int frameSize = width * height;
        byte[] out = frameBuffers.nv12Data;

        int[] pixels = frameBuffers.reusedPixels;
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        int yIndex = 0;
        int uvIndex = frameSize;

        for (int j = 0; j < height; j++) {
            int rowBase = j * width;
            for (int i = 0; i < width; i++) {
                int c = pixels[rowBase + i];
                int r = (c >> 16) & 0xFF;
                int g = (c >> 8) & 0xFF;
                int b = c & 0xFF;

                int y = ((66 * r + 129 * g + 25 * b + 128) >> 8) + 16;
                int u = ((-38 * r - 74 * g + 112 * b + 128) >> 8) + 128;
                int v = ((112 * r - 94 * g - 18 * b + 128) >> 8) + 128;

                out[yIndex++] = (byte) clamp(y);

                if ((j & 1) == 0 && (i & 1) == 0) {
                    out[uvIndex++] = (byte) clamp(u);
                    out[uvIndex++] = (byte) clamp(v);
                }
            }
        }

        return out;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    @NonNull
    private VideoMeta readVideoMeta(@NonNull String inputPath) throws IOException {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        String durationMsStr;
        String rotationStr;
        try {
            retriever.setDataSource(inputPath);
            durationMsStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        } finally {
            retriever.release();
        }

        VideoMeta meta = new VideoMeta();
        meta.durationUs = parseLong(durationMsStr) * 1000L;
        meta.rotationDegrees = (int) parseLong(rotationStr);
        meta.frameRate = 30;
        return meta;
    }

    private long parseLong(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            Log.w(TAG, "parseLong failed for value=" + value, e);
            return 0L;
        }
    }

    private static class VideoMeta {
        long durationUs;
        int rotationDegrees;
        int frameRate;
    }

    private static class MuxStateHolder {
        static MuxStateHolder LAST_STATE = new MuxStateHolder(-1, -1, false);

        int videoTrack;
        int audioTrack;
        boolean started;
        boolean encoderDone;

        MuxStateHolder(int videoTrack, int audioTrack, boolean started) {
            this.videoTrack = videoTrack;
            this.audioTrack = audioTrack;
            this.started = started;
            this.encoderDone = false;
        }
    }

    private static class FrameBuffers {
        final Bitmap frameBitmap;
        final int[] argbPixels;
        final int[] reusedPixels;
        final byte[] nv12Data;

        FrameBuffers(int width, int height) {
            int frameSize = width * height;
            this.frameBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            this.argbPixels = new int[frameSize];
            this.reusedPixels = new int[frameSize];
            this.nv12Data = new byte[frameSize + frameSize / 2];
        }
    }
}

