# Offline Pose Video Pipeline

This package adds offline MP4 post-processing after camera recording.

## Flow

1. `TrainFragment` receives the raw recorded MP4 path on `VideoRecordEvent.Finalize`.
2. `PoseVideoProcessor` runs in a background executor.
3. Frames are decoded by `MediaExtractor + MediaCodec`.
4. Each frame is inferred by `PoseLandmarker` in `RunningMode.VIDEO` (`detectForVideo`).
5. `PoseFrameDrawer` overlays keypoints and skeleton on each frame.
6. Frames are encoded to H.264 and muxed into a new MP4.
7. Original audio track is copied into the output MP4.
8. `TrainFragment` stores the processed output path and launches `VideoPlayerActivity`.

## Accuracy-safe performance optimizations

- Inference policy is unchanged: no frame skipping and no downscaled inference input.
- Reuse per-frame conversion buffers (`int[]`, `byte[]`) to reduce GC pressure.
- Reuse mutable frame bitmap for YUV->RGB conversion output.
- Reuse drawing `Canvas` in `PoseFrameDrawer` to avoid per-frame allocation.

## Notes

- Model asset: `app/src/main/assets/pose_landmarker_lite.task`
- Timestamp unit in processing loop: microseconds (`us`)
- If post-processing fails, UI keeps the original video path as fallback.

