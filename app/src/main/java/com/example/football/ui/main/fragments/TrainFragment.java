package com.example.football.ui.main.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FallbackStrategy;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.football.R;
import com.example.football.database.MilestoneDbHelper;
import com.example.football.coach.CoachFeedbackManager;
import com.example.football.ui.train.VideoPlayerActivity;
import com.example.football.utils.SPUtils;
import com.example.football.video.PoseVideoProcessor;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.nio.ByteBuffer;

public class TrainFragment extends Fragment {

    private static final String ARG_PRESET_MODE = "arg_preset_mode";
    public static final String MODE_KEY_SHOOT = "shoot";
    public static final String MODE_KEY_DRIBBLE = "dribble";
    public static final String MODE_KEY_PASS = "pass";

    // 核心控件声明
    private PreviewView previewView;
    private RadioGroup rgMode;
    private Button btnStartRecognize, btnStopRecognize, btnSaveResult, btnDiscardResult;
    private LinearLayout llRecognizing, llRecognized;
    private TextView tvCurrentAction, tvConfidence, tvScore, tvTotalCount, tvAvgScore, tvSuggestion;
    private TextView tvRecordingState;
    private TextView tvAiCoachFeedback;

    // CameraX 相关
    private ExecutorService cameraExecutor;
    private ExecutorService postProcessExecutor;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private VideoCapture<Recorder> videoCapture;
    private ImageAnalysis imageAnalysis;
    private Recording activeRecording;
    private String lastVideoPath = "";
    private PoseVideoProcessor poseVideoProcessor;
    private volatile boolean isPostProcessing = false;
    private boolean saveRequestedForSession = false;
    private int currentSessionId = 0;

    // MediaPipe 相关
    private PoseLandmarker poseLandmarker;
    private CoachFeedbackManager coachFeedbackManager;

    // 识别状态控制
    private boolean isRecognizing = false;
    private String currentMode = "";
    private int actionCount = 0;
    private int totalScore = 0;
    private long lastRepTimestampMs = 0L;
    private long poseResultCount = 0L;
    private long poseEmptyCount = 0L;
    private long poseErrorCount = 0L;

    private static final String TAG = "TrainFragment";
    private static final String SP_KEY_TRAIN_RECORDS = "train_records";
    private static final long REP_INTERVAL_MS = 900L;

    private String pendingVideoPath = "";
    private boolean videoFinalizeDone = false;
    private String pendingPresetMode;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    initCameraPreview();
                } else if (isAdded()) {
                    Toast.makeText(requireContext(), getString(R.string.train_toast_need_camera_permission), Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            pendingPresetMode = args.getString(ARG_PRESET_MODE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_train, container, false);

        initViews(view);
        initPoseLandmarker();
        startCameraFlow();
        setViewListeners();
        applyPendingPresetMode();

        return view;
    }

    private void startCameraFlow() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            initCameraPreview();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void initViews(View view) {
        previewView = view.findViewById(R.id.previewView);
        // Ensure overlay can be drawn above preview (SurfaceView mode may hide sibling views).
        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        rgMode = view.findViewById(R.id.rg_mode);
        btnStartRecognize = view.findViewById(R.id.btn_start_recognize);
        btnStopRecognize = view.findViewById(R.id.btn_stop_recognize);
        btnSaveResult = view.findViewById(R.id.btn_save_result);
        btnDiscardResult = view.findViewById(R.id.btn_not_save_result);
        llRecognizing = view.findViewById(R.id.ll_recognizing);
        llRecognized = view.findViewById(R.id.ll_recognized);
        tvCurrentAction = view.findViewById(R.id.tv_current_action);
        tvConfidence = view.findViewById(R.id.tv_confidence);
        tvScore = view.findViewById(R.id.tv_score);
        tvTotalCount = view.findViewById(R.id.tv_total_count);
        tvAvgScore = view.findViewById(R.id.tv_avg_score);
        tvSuggestion = view.findViewById(R.id.tv_suggestion);
        tvRecordingState = view.findViewById(R.id.tv_recording_state);
        tvAiCoachFeedback = view.findViewById(R.id.tv_ai_coach_feedback);

        cameraExecutor = Executors.newSingleThreadExecutor();
        postProcessExecutor = Executors.newSingleThreadExecutor();
        poseVideoProcessor = new PoseVideoProcessor();
        currentMode = getString(R.string.train_mode_shoot);
        tvCurrentAction.setText(getString(R.string.train_current_action_format, currentMode));

        coachFeedbackManager = new CoachFeedbackManager(requireContext().getApplicationContext(), feedback -> {
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> tvAiCoachFeedback.setText(feedback));
        });
        coachFeedbackManager.showIdleHint();

        updateRecordStatus(getString(R.string.train_status_idle), false, getString(R.string.train_save_text_default));
        updateStartButtonStyle(false);
    }

    private void initPoseLandmarker() {
        try {
            BaseOptions baseOptions = BaseOptions.builder()
                    .setModelAssetPath("pose_landmarker_lite.task")
                    .build();

            PoseLandmarker.PoseLandmarkerOptions options = PoseLandmarker.PoseLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setRunningMode(RunningMode.LIVE_STREAM)
                    .setNumPoses(1)
                    .setMinPoseDetectionConfidence(0.6f)
                    .setMinPosePresenceConfidence(0.65f)
                    .setMinTrackingConfidence(0.65f)
                    .setResultListener((result, input) -> onPoseResult(result))
                    .setErrorListener(error -> onPoseError(error))
                    .build();

            poseLandmarker = PoseLandmarker.createFromOptions(requireContext(), options);
        } catch (Exception e) {
            Log.e(TAG, "initPoseLandmarker failed", e);
            if (isAdded()) {
                Toast.makeText(requireContext(), "MediaPipe 初始化失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateRecordStatus(String text, boolean canOperate, String saveButtonText) {
        tvRecordingState.setText(text);
        btnSaveResult.setEnabled(canOperate);
        btnSaveResult.setText(saveButtonText);
        if (btnDiscardResult != null) {
            btnDiscardResult.setEnabled(canOperate);
        }
    }

    private void updateStartButtonStyle(boolean isRetry) {
        btnStartRecognize.setText(isRetry ? getString(R.string.train_start_text_retry) : getString(R.string.train_start_text_default));
        btnStartRecognize.setBackgroundResource(isRetry
                ? R.drawable.bg_train_button_retry
                : R.drawable.bg_train_button_primary);
    }

    private void initCameraPreview() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (Exception e) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), getString(R.string.train_toast_camera_init_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
                }
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindCameraUseCases(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        Recorder recorder = new Recorder.Builder()
                .setQualitySelector(QualitySelector.fromOrderedList(
                        Arrays.asList(Quality.FHD, Quality.HD, Quality.SD),
                        FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)))
                .build();
        videoCapture = VideoCapture.withOutput(recorder);

        imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build();
        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImageProxy);

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview, videoCapture, imageAnalysis);
    }

    private void analyzeImageProxy(@NonNull ImageProxy imageProxy) {
        if (poseLandmarker == null) {
            imageProxy.close();
            return;
        }

        try {
            int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
            Bitmap frameBitmap = imageProxyToBitmap(imageProxy);
            MPImage mpImage = new BitmapImageBuilder(frameBitmap).build();
            ImageProcessingOptions imageProcessingOptions = ImageProcessingOptions.builder()
                    .setRotationDegrees(rotationDegrees)
                    .build();
            long timestampMs = SystemClock.elapsedRealtimeNanos() / 1_000_000;
            poseLandmarker.detectAsync(mpImage, imageProcessingOptions, timestampMs);
        } catch (Exception e) {
            Log.e(TAG, "analyzeImageProxy failed", e);
        } finally {
            imageProxy.close();
        }
    }

    private Bitmap imageProxyToBitmap(@NonNull ImageProxy imageProxy) {
        ImageProxy.PlaneProxy[] planes = imageProxy.getPlanes();
        if (planes.length == 0) {
            throw new IllegalStateException("RGBA plane is missing");
        }

        int width = imageProxy.getWidth();
        int height = imageProxy.getHeight();
        ImageProxy.PlaneProxy plane = planes[0];
        ByteBuffer buffer = plane.getBuffer();
        buffer.rewind();

        int rowStride = plane.getRowStride();
        int pixelStride = plane.getPixelStride();
        if (pixelStride != 4) {
            throw new IllegalStateException("Unexpected pixelStride=" + pixelStride);
        }

        int rowPadding = rowStride - pixelStride * width;
        if (rowPadding == 0) {
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);
            return bitmap;
        }

        int paddedWidth = rowStride / pixelStride;
        Bitmap paddedBitmap = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888);
        paddedBitmap.copyPixelsFromBuffer(buffer);
        Bitmap croppedBitmap = Bitmap.createBitmap(paddedBitmap, 0, 0, width, height);
        paddedBitmap.recycle();
        return croppedBitmap;
    }

    private void onPoseResult(PoseLandmarkerResult result) {
        poseResultCount++;
        if (!isAdded()) {
            return;
        }

        List<List<NormalizedLandmark>> allLandmarks = result.landmarks();
        if (allLandmarks == null || allLandmarks.isEmpty()) {
            poseEmptyCount++;
             if (poseResultCount % 30 == 0) {
                 Log.d(TAG, "pose callback alive, empty=" + poseEmptyCount + "/" + poseResultCount);
             }
             return;
        }

        List<NormalizedLandmark> firstPose = allLandmarks.get(0);
        float confidence = calculatePoseConfidence(firstPose);
        int frameScore = calculateFrameScore(confidence);

        if (isRecognizing) {
            boolean repCounted = maybeCountRep(confidence, frameScore);
            if (coachFeedbackManager != null) {
                coachFeedbackManager.onFrameAnalyzed(confidence, frameScore);
                if (repCounted) {
                    coachFeedbackManager.onRepCounted(actionCount);
                }
            }
        }

        requireActivity().runOnUiThread(() -> {
             String confidenceText = String.format(Locale.getDefault(), "%.2f", confidence);
             tvConfidence.setText(getString(R.string.train_confidence_format, confidenceText));
             tvScore.setText(getString(R.string.train_score_format, frameScore));
             if (isRecognizing) {
                tvCurrentAction.setText(getString(R.string.train_current_action_format, currentMode));
            }
        });

        if (poseResultCount % 30 == 0) {
            Log.d(TAG, "pose draw ok, landmarks=" + firstPose.size() + ", conf=" + confidence);
        }
    }

    private void onPoseError(RuntimeException error) {
        poseErrorCount++;
        Log.e(TAG, "PoseLandmarker runtime error count=" + poseErrorCount, error);
    }

    private boolean maybeCountRep(float confidence, int frameScore) {
        long now = SystemClock.elapsedRealtime();
        if (confidence < 0.5f || frameScore < 60) {
            return false;
        }
        if (now - lastRepTimestampMs < REP_INTERVAL_MS) {
            return false;
        }
        lastRepTimestampMs = now;
        actionCount++;
        totalScore += frameScore;
        return true;
    }

    private float calculatePoseConfidence(List<NormalizedLandmark> landmarks) {
        if (landmarks == null || landmarks.isEmpty()) {
            return 0f;
        }
        float sum = 0f;
        int count = 0;
        for (NormalizedLandmark landmark : landmarks) {
            float visibility = landmark.visibility().orElse(0f);
            sum += visibility;
            count++;
        }
        return count == 0 ? 0f : (sum / count);
    }

    private int calculateFrameScore(float confidence) {
        int score = Math.round(confidence * 100f);
        return Math.max(0, Math.min(100, score));
    }

    private void startOfflinePosePostProcess(@NonNull String rawVideoPath, int sessionId) {
        if (isPostProcessing || sessionId != currentSessionId) {
            return;
        }
        final android.content.Context appContext = requireContext().getApplicationContext();
        isPostProcessing = true;
        updateRecordStatus(getString(R.string.train_status_post_processing), false, getString(R.string.train_save_text_wait));

        File rawFile = new File(rawVideoPath);
        File parent = rawFile.getParentFile();
        if (parent == null) {
            isPostProcessing = false;
            videoFinalizeDone = true;
            finishSaveFlow();
            return;
        }
        String outputName = "pose_" + rawFile.getName();
        File outputFile = new File(parent, outputName);

        postProcessExecutor.execute(() -> poseVideoProcessor.process(
                appContext,
                rawVideoPath,
                outputFile.getAbsolutePath(),
                new PoseVideoProcessor.Callback() {
                    @Override
                    public void onProgress(int progress) {
                        if (sessionId != currentSessionId || !isAdded()) {
                            return;
                        }
                        requireActivity().runOnUiThread(() -> tvRecordingState.setText(
                                getString(R.string.train_status_post_processing_progress, progress)
                        ));
                    }

                    @Override
                    public void onCompleted(@NonNull String outputPath) {
                        if (sessionId != currentSessionId) {
                            safeDeleteFile(outputPath);
                            return;
                        }
                        isPostProcessing = false;
                        videoFinalizeDone = true;
                        lastVideoPath = outputPath;
                        if (!isAdded()) {
                            return;
                        }
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), getString(R.string.train_toast_pose_video_ready), Toast.LENGTH_SHORT).show();
                            finishSaveFlow();
                        });
                    }

                    @Override
                    public void onFailed(@NonNull Exception error) {
                        if (sessionId != currentSessionId) {
                            return;
                        }
                        isPostProcessing = false;
                        videoFinalizeDone = true;
                        if (!isAdded()) {
                            return;
                        }
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), getString(R.string.train_toast_pose_video_failed), Toast.LENGTH_SHORT).show();
                            finishSaveFlow();
                        });
                        Log.e(TAG, "offline pose process failed", error);
                    }
                }
        ));
    }

    private void startVideoRecording(int sessionId) {
        if (videoCapture == null || !isAdded()) {
            Toast.makeText(requireContext(), getString(R.string.train_toast_recorder_not_ready), Toast.LENGTH_SHORT).show();
            return;
        }

        File baseDir = requireContext().getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES);
        if (baseDir == null) {
            Toast.makeText(requireContext(), getString(R.string.train_toast_dir_unavailable), Toast.LENGTH_SHORT).show();
            return;
        }

        File videoDir = new File(baseDir, "train_videos");
        if (!videoDir.exists() && !videoDir.mkdirs()) {
            Toast.makeText(requireContext(), getString(R.string.train_toast_dir_create_failed), Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = "train_" + System.currentTimeMillis() + ".mp4";
        File videoFile = new File(videoDir, fileName);
        final String sessionRawPath = videoFile.getAbsolutePath();
        pendingVideoPath = sessionRawPath;
        lastVideoPath = "";
        videoFinalizeDone = false;
        isPostProcessing = false;
        btnSaveResult.setEnabled(false);
        updateRecordStatus(getString(R.string.train_status_recording), false, getString(R.string.train_save_text_wait));
        updateStartButtonStyle(false);

        FileOutputOptions outputOptions = new FileOutputOptions.Builder(videoFile).build();

        Log.d(TAG, "startVideoRecording, path=" + pendingVideoPath);

        activeRecording = videoCapture.getOutput()
                .prepareRecording(requireContext(), outputOptions)
                .start(ContextCompat.getMainExecutor(requireContext()), event -> {
                    if (event instanceof VideoRecordEvent.Finalize) {
                        VideoRecordEvent.Finalize finalizeEvent = (VideoRecordEvent.Finalize) event;
                        if (sessionId != currentSessionId) {
                            safeDeleteFile(sessionRawPath);
                            return;
                        }
                        if (!finalizeEvent.hasError()) {
                            lastVideoPath = sessionRawPath;
                            videoFinalizeDone = true;
                            if (saveRequestedForSession) {
                                startOfflinePosePostProcess(lastVideoPath, sessionId);
                            } else {
                                updateRecordStatus(getString(R.string.train_status_wait_decision), true, getString(R.string.train_save_text_default));
                            }
                            updateStartButtonStyle(false);
                            Toast.makeText(requireContext(), getString(R.string.train_toast_video_saved), Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Video saved: " + lastVideoPath + ", size=" + videoFile.length());
                        } else {
                            Log.e(TAG, "Video finalize error: " + finalizeEvent.getError());
                            lastVideoPath = "";
                            videoFinalizeDone = false;
                            isPostProcessing = false;
                            saveRequestedForSession = false;
                            updateRecordStatus(getString(R.string.train_status_save_failed), false, getString(R.string.train_save_text_default));
                            btnStartRecognize.setVisibility(View.VISIBLE);
                            llRecognized.setVisibility(View.GONE);
                            updateStartButtonStyle(true);
                            Toast.makeText(requireContext(), getString(R.string.train_toast_video_save_failed), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void stopVideoRecording() {
        if (activeRecording != null) {
            activeRecording.stop();
            activeRecording = null;
        }
    }

    private void saveTrainRecord() {
        int avgScore = actionCount > 0 ? totalScore / actionCount : 0;
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
        String videoInfo = (!videoFinalizeDone || lastVideoPath.isEmpty()) ? "视频:无" : "视频:" + lastVideoPath;
        String record = time + " | " + currentMode + " | 次数:" + actionCount + " | 均分:" + avgScore + " | " + videoInfo;

        String oldRecords = SPUtils.getString(requireContext(), SP_KEY_TRAIN_RECORDS, "");
        String newRecords = record + (oldRecords.isEmpty() ? "" : "\n" + oldRecords);
        SPUtils.putString(requireContext(), SP_KEY_TRAIN_RECORDS, newRecords);

        updateMilestoneProgress(record, avgScore);
    }

    private void updateMilestoneProgress(String record, int avgScore) {
        String account = SPUtils.getString(requireContext(), "account", "default");
        String recordKey = getTrainRecordsKey(account);
        String oldRecords = SPUtils.getString(requireContext(), recordKey, "");
        String newRecords = record + (oldRecords.isEmpty() ? "" : "\n" + oldRecords);
        SPUtils.putString(requireContext(), recordKey, newRecords);

        MilestoneDbHelper.getInstance(requireContext())
                .recordTrainingResult(account, currentMode, avgScore, actionCount);
    }

    private String getTrainRecordsKey(String account) {
        return SP_KEY_TRAIN_RECORDS + "_" + account;
    }


    private void setViewListeners() {
        rgMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_shoot) {
                currentMode = getString(R.string.train_mode_shoot);
            } else if (checkedId == R.id.rb_dribble) {
                currentMode = getString(R.string.train_mode_dribble);
            } else if (checkedId == R.id.rb_pass) {
                currentMode = getString(R.string.train_mode_pass);
            }
            if (isRecognizing) {
                tvCurrentAction.setText(getString(R.string.train_current_action_format, currentMode));
            }
            if (coachFeedbackManager != null) {
                coachFeedbackManager.updateMode(currentMode);
            }
        });

        btnStartRecognize.setOnClickListener(v -> {
            if (isPostProcessing) {
                Toast.makeText(requireContext(), getString(R.string.train_toast_wait_post_process), Toast.LENGTH_SHORT).show();
                return;
            }
            currentSessionId++;
            saveRequestedForSession = false;
            isRecognizing = true;
            pendingVideoPath = "";
            lastVideoPath = "";
            videoFinalizeDone = false;
            actionCount = 0;
            totalScore = 0;
            lastRepTimestampMs = 0L;

            if (coachFeedbackManager != null) {
                coachFeedbackManager.startSession(currentMode);
            }
            startVideoRecording(currentSessionId);
            btnStartRecognize.setVisibility(View.GONE);
            llRecognizing.setVisibility(View.VISIBLE);
            llRecognized.setVisibility(View.GONE);
        });

        btnStopRecognize.setOnClickListener(v -> {
            isRecognizing = false;
            stopVideoRecording();
            updateRecordStatus(getString(R.string.train_status_wait_finalize), true, getString(R.string.train_save_text_default));
            llRecognizing.setVisibility(View.GONE);
            llRecognized.setVisibility(View.VISIBLE);

            int avgScore = actionCount > 0 ? totalScore / actionCount : 0;
            tvTotalCount.setText(getString(R.string.train_total_count_format, currentMode, actionCount));
            tvAvgScore.setText(getString(R.string.train_avg_score_format, avgScore));

            if (currentMode.equals(getString(R.string.train_mode_shoot))) {
                tvSuggestion.setText(getString(R.string.train_suggestion_shoot));
            } else if (currentMode.equals(getString(R.string.train_mode_dribble))) {
                tvSuggestion.setText(getString(R.string.train_suggestion_dribble));
            } else if (currentMode.equals(getString(R.string.train_mode_pass))) {
                tvSuggestion.setText(getString(R.string.train_suggestion_pass));
            }
            if (coachFeedbackManager != null) {
                coachFeedbackManager.onChallengeCompleted(actionCount, avgScore);
            }
        });

        btnSaveResult.setOnClickListener(v -> {
            if (!isAdded()) {
                return;
            }
            if (isPostProcessing) {
                Toast.makeText(requireContext(), getString(R.string.train_toast_wait_post_process), Toast.LENGTH_SHORT).show();
                return;
            }
            saveRequestedForSession = true;
            if (!videoFinalizeDone) {
                updateRecordStatus(getString(R.string.train_status_saving), false, getString(R.string.train_save_text_wait));
                return;
            }
            startOfflinePosePostProcess(lastVideoPath, currentSessionId);
        });

        btnDiscardResult.setOnClickListener(v -> discardCurrentSession());
    }

    public void applyPresetMode(@Nullable String presetMode) {
        pendingPresetMode = presetMode;
        applyPendingPresetMode();
    }

    private void applyPendingPresetMode() {
        if (rgMode == null || pendingPresetMode == null || pendingPresetMode.trim().isEmpty()) {
            return;
        }
        int checkedId = View.NO_ID;
        if (MODE_KEY_SHOOT.equals(pendingPresetMode)) {
            checkedId = R.id.rb_shoot;
        } else if (MODE_KEY_DRIBBLE.equals(pendingPresetMode)) {
            checkedId = R.id.rb_dribble;
        } else if (MODE_KEY_PASS.equals(pendingPresetMode)) {
            checkedId = R.id.rb_pass;
        }
        if (checkedId != View.NO_ID) {
            rgMode.check(checkedId);
        }
        pendingPresetMode = null;
    }

    private void finishSaveFlow() {
        if (!isAdded()) {
            return;
        }
        saveTrainRecord();
        Toast.makeText(requireContext(), getString(R.string.train_toast_record_saved), Toast.LENGTH_LONG).show();

        if (!lastVideoPath.isEmpty()) {
            Intent intent = new Intent(requireContext(), VideoPlayerActivity.class);
            intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_PATH, lastVideoPath);
            startActivity(intent);
        }

        resetToInitialState();
    }

    private void discardCurrentSession() {
        int discardedSession = currentSessionId;
        currentSessionId++;
        saveRequestedForSession = false;
        isPostProcessing = false;
        videoFinalizeDone = false;

        String rawPath = pendingVideoPath;
        String processedPath = lastVideoPath;

        stopVideoRecording();
        safeDeleteFile(rawPath);
        if (!processedPath.equals(rawPath)) {
            safeDeleteFile(processedPath);
        }

        if (discardedSession > 0 && isAdded()) {
            Toast.makeText(requireContext(), getString(R.string.train_toast_record_discarded), Toast.LENGTH_SHORT).show();
        }
        resetToInitialState();
    }

    private void resetToInitialState() {
        isRecognizing = false;
        saveRequestedForSession = false;
        pendingVideoPath = "";
        lastVideoPath = "";
        videoFinalizeDone = false;
        actionCount = 0;
        totalScore = 0;
        lastRepTimestampMs = 0L;

        if (getView() == null) {
            return;
        }
        btnStartRecognize.setVisibility(View.VISIBLE);
        llRecognizing.setVisibility(View.GONE);
        llRecognized.setVisibility(View.GONE);
        updateRecordStatus(getString(R.string.train_status_idle), false, getString(R.string.train_save_text_default));
        updateStartButtonStyle(false);
        if (coachFeedbackManager != null) {
            coachFeedbackManager.showIdleHint();
        }
    }

    private void safeDeleteFile(@Nullable String path) {
        if (path == null || path.trim().isEmpty()) {
            return;
        }
        File file = new File(path);
        if (file.exists() && !file.delete()) {
            Log.w(TAG, "Failed to delete file: " + path);
        }
    }

    /**
     * Fragment销毁时释放资源（防止内存泄漏）
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        currentSessionId++;
        stopVideoRecording();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (postProcessExecutor != null) {
            postProcessExecutor.shutdown();
        }
        if (poseLandmarker != null) {
            poseLandmarker.close();
            poseLandmarker = null;
        }
        if (coachFeedbackManager != null) {
            coachFeedbackManager.release();
            coachFeedbackManager = null;
        }
    }

    public static TrainFragment newInstance(String presetMode) {
        TrainFragment fragment = new TrainFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PRESET_MODE, presetMode);
        fragment.setArguments(args);
        return fragment;
    }
}
