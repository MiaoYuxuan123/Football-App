package com.example.football.ui.main.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.football.R;
import com.example.football.database.MilestoneDbHelper;
import com.example.football.database.entity.MilestoneData;
import com.example.football.utils.SPUtils;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.camera.video.FallbackStrategy;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;

public class TrainFragment extends Fragment {

    // 核心控件声明
    private PreviewView previewView;
    private RadioGroup rgMode;
    private Button btnStartRecognize, btnStopRecognize, btnSaveResult;
    private LinearLayout llRecognizing, llRecognized;
    private TextView tvCurrentAction, tvConfidence, tvScore, tvTotalCount, tvAvgScore, tvSuggestion;
    private TextView tvRecordingState;

    // CameraX 相关
    private ExecutorService cameraExecutor;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private VideoCapture<Recorder> videoCapture;
    private Recording activeRecording;
    private String lastVideoPath = "";

    // 识别状态控制
    private boolean isRecognizing = false;
    private String currentMode = "";
    private int actionCount = 0; // 动作计数
    private int totalScore = 0; // 总评分
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable recognizeRunnable; // 模拟识别的定时任务

    private static final String TAG = "TrainFragment";
    private static final String SP_KEY_TRAIN_RECORDS = "train_records";

    private String pendingVideoPath = "";
    private boolean videoFinalizeDone = false;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    initCameraPreview();
                } else if (isAdded()) {
                    Toast.makeText(requireContext(), getString(R.string.train_toast_need_camera_permission), Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 加载训练识别布局
        View view = inflater.inflate(R.layout.fragment_train, container, false);

        // 1. 初始化所有控件
        initViews(view);
        // 2. 检查权限后再初始化相机预览
        startCameraFlow();
        // 3. 设置所有点击事件和监听
        setViewListeners();

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

    /**
     * 初始化所有控件（绑定ID）
     */
    private void initViews(View view) {
        // 相机预览
        previewView = view.findViewById(R.id.previewView);
        // 模式选择
        rgMode = view.findViewById(R.id.rg_mode);
        // 按钮
        btnStartRecognize = view.findViewById(R.id.btn_start_recognize);
        btnStopRecognize = view.findViewById(R.id.btn_stop_recognize);
        btnSaveResult = view.findViewById(R.id.btn_save_result);
        // 识别状态布局
        llRecognizing = view.findViewById(R.id.ll_recognizing);
        llRecognized = view.findViewById(R.id.ll_recognized);
        // 识别结果文本
        tvCurrentAction = view.findViewById(R.id.tv_current_action);
        tvConfidence = view.findViewById(R.id.tv_confidence);
        tvScore = view.findViewById(R.id.tv_score);
        tvTotalCount = view.findViewById(R.id.tv_total_count);
        tvAvgScore = view.findViewById(R.id.tv_avg_score);
        tvSuggestion = view.findViewById(R.id.tv_suggestion);
        tvRecordingState = view.findViewById(R.id.tv_recording_state);

        // 初始化相机线程池
        cameraExecutor = Executors.newSingleThreadExecutor();
        updateRecordStatus(getString(R.string.train_status_idle), false, getString(R.string.train_save_text_default));
        updateStartButtonStyle(false);
    }

    private void updateRecordStatus(String text, boolean canSave, String saveButtonText) {
        tvRecordingState.setText(text);
        btnSaveResult.setEnabled(canSave);
        btnSaveResult.setText(saveButtonText);
    }

    private void updateStartButtonStyle(boolean isRetry) {
        btnStartRecognize.setText(isRetry ? getString(R.string.train_start_text_retry) : getString(R.string.train_start_text_default));
        btnStartRecognize.setBackgroundColor(isRetry ? Color.parseColor("#FF9800") : Color.parseColor("#008000"));
    }

    /**
     * 初始化相机预览（打开后置摄像头）
     */
    private void initCameraPreview() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        // 相机初始化回调
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                Recorder recorder = new Recorder.Builder()
                        .setQualitySelector(QualitySelector.fromOrderedList(
                                Arrays.asList(Quality.FHD, Quality.HD, Quality.SD),
                                FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)))
                        .build();
                videoCapture = VideoCapture.withOutput(recorder);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview, videoCapture);
            } catch (Exception e) {
                // 相机初始化失败提示
                if (isAdded()) {
                    Toast.makeText(requireContext(), getString(R.string.train_toast_camera_init_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
                }
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void startVideoRecording() {
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
        pendingVideoPath = videoFile.getAbsolutePath();
        lastVideoPath = "";
        videoFinalizeDone = false;
        btnSaveResult.setEnabled(false);
        updateRecordStatus(getString(R.string.train_status_recording), false, getString(R.string.train_save_text_wait));
        updateStartButtonStyle(false);

        FileOutputOptions outputOptions = new FileOutputOptions.Builder(videoFile).build();

        Log.d(TAG, "startVideoRecording, path=" + pendingVideoPath);

        activeRecording = videoCapture.getOutput()
                .prepareRecording(requireContext(), outputOptions)
                // 当前只录制无声视频，如果后面要加声音，需要申请 RECORD_AUDIO 权限并 .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(requireContext()), event -> {
                    if (event instanceof VideoRecordEvent.Finalize) {
                        VideoRecordEvent.Finalize finalizeEvent = (VideoRecordEvent.Finalize) event;
                        if (!finalizeEvent.hasError()) {
                            lastVideoPath = pendingVideoPath;
                            videoFinalizeDone = true;
                            updateRecordStatus(getString(R.string.train_status_ready), true, getString(R.string.train_save_text_default));
                            updateStartButtonStyle(false);
                            Toast.makeText(requireContext(), getString(R.string.train_toast_video_saved), Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Video saved: " + lastVideoPath + ", size=" + videoFile.length());
                        } else {
                            Log.e(TAG, "Video finalize error: " + finalizeEvent.getError());
                            lastVideoPath = "";
                            videoFinalizeDone = false;
                            updateRecordStatus(getString(R.string.train_status_save_failed), false, getString(R.string.train_save_text_default));
                            btnStartRecognize.setVisibility(View.VISIBLE);
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

        // Keep milestone page in sync when a training result is persisted.
        updateMilestoneProgress(avgScore);
    }

    private void updateMilestoneProgress(int avgScore) {
        String account = SPUtils.getString(requireContext(), "account", "default");
        MilestoneDbHelper db = MilestoneDbHelper.getInstance(requireContext());
        MilestoneData milestone = db.getOrCreate(account);

        int xpGain = Math.max(5, avgScore / 3);
        milestone.experience += xpGain;

        while (milestone.experience >= milestone.experienceToNext) {
            milestone.experience -= milestone.experienceToNext;
            milestone.level += 1;
            milestone.experienceToNext += 50;
            milestone.xpPerTraining = Math.min(80, milestone.xpPerTraining + 2);
            milestone.technicalTitle = resolveTitleByLevel(milestone.level);
        }

        List<Float> trend = parseFloatList(milestone.technicalScoresJson);
        if (trend == null) {
            trend = new ArrayList<>();
        }
        trend.add((float) avgScore);
        if (trend.size() > 12) {
            trend = new ArrayList<>(trend.subList(trend.size() - 12, trend.size()));
        }
        milestone.technicalScoresJson = new Gson().toJson(trend);

        float base = avg(trend);
        float[] radar = new float[]{
                clamp(base + modeOffset(getString(R.string.train_mode_shoot)), 50f, 99f),
                clamp(base + modeOffset(getString(R.string.train_mode_pass)), 50f, 99f),
                clamp(base + modeOffset(getString(R.string.train_mode_dribble)), 50f, 99f),
                clamp(base - 6f, 45f, 95f),
                clamp(base - 2f, 45f, 98f),
                clamp(base, 45f, 99f)
        };
        milestone.radarScoresJson = new Gson().toJson(radarToList(radar));

        db.update(milestone);
    }

    private String resolveTitleByLevel(int level) {
        if (level >= 10) return getString(R.string.milestone_title_elite);
        if (level >= 7) return getString(R.string.milestone_title_advanced);
        if (level >= 4) return getString(R.string.milestone_title_intermediate);
        return getString(R.string.milestone_title_beginner);
    }

    private float modeOffset(String mode) {
        if (mode.equals(currentMode)) {
            return 4f;
        }
        return 0f;
    }

    private List<Float> parseFloatList(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            Type type = new TypeToken<List<Float>>() { }.getType();
            List<Float> list = new Gson().fromJson(json, type);
            return list == null ? null : new ArrayList<>(list);
        } catch (Exception ignored) {
            return null;
        }
    }

    private float avg(List<Float> values) {
        if (values == null || values.isEmpty()) {
            return 75f;
        }
        float sum = 0f;
        for (Float v : values) {
            sum += (v == null ? 0f : v);
        }
        return sum / values.size();
    }

    private List<Float> radarToList(float[] values) {
        List<Float> list = new ArrayList<>();
        for (float value : values) {
            list.add(value);
        }
        return list;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 设置所有控件的监听事件
     */
    private void setViewListeners() {
        // 1. 模式选择监听（射门/运球/传球切换）
        rgMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_shoot) {
                currentMode = getString(R.string.train_mode_shoot);
            } else if (checkedId == R.id.rb_dribble) {
                currentMode = getString(R.string.train_mode_dribble);
            } else if (checkedId == R.id.rb_pass) {
                currentMode = getString(R.string.train_mode_pass);
            }
            // 如果正在识别，实时更新当前动作
            if (isRecognizing) {
                tvCurrentAction.setText(getString(R.string.train_current_action_format, currentMode));
            }
        });

        // 2. 开始识别按钮点击
        btnStartRecognize.setOnClickListener(v -> {
            isRecognizing = true;
            pendingVideoPath = "";
            lastVideoPath = "";
            videoFinalizeDone = false;
            startVideoRecording();
            // UI状态切换：隐藏开始按钮，显示识别中布局
            btnStartRecognize.setVisibility(View.GONE);
            llRecognizing.setVisibility(View.VISIBLE);
            llRecognized.setVisibility(View.GONE);
            // 重置计数
            actionCount = 0;
            totalScore = 0;

            // 模拟实时识别（每秒刷新一次评分/置信度）
            recognizeRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isRecognizing && isAdded()) {
                        actionCount++;
                        // 随机生成60-100分的评分（后续替换为真实识别结果）
                        int randomScore = (int) (60 + Math.random() * 40);
                        totalScore += randomScore;
                        // 随机生成0.8-1.0的置信度
                        float randomConfidence = (float) (0.8 + Math.random() * 0.2);

                        // 更新UI（必须在主线程）
                        requireActivity().runOnUiThread(() -> {
                            tvCurrentAction.setText(getString(R.string.train_current_action_format, currentMode));
                            String confidenceText = String.format(Locale.getDefault(), "%.2f", randomConfidence);
                            tvConfidence.setText(getString(R.string.train_confidence_format, confidenceText));
                            tvScore.setText(getString(R.string.train_score_format, randomScore));
                        });

                        // 每秒执行一次
                        handler.postDelayed(this, 1000);
                    }
                }
            };
            handler.post(recognizeRunnable);
        });

        // 3. 结束识别按钮点击
        btnStopRecognize.setOnClickListener(v -> {
            isRecognizing = false;
            stopVideoRecording();
            updateRecordStatus(getString(R.string.train_status_saving), false, getString(R.string.train_save_text_wait));
            // 停止定时任务
            handler.removeCallbacks(recognizeRunnable);
            // UI状态切换：隐藏识别中布局，显示识别完成布局
            llRecognizing.setVisibility(View.GONE);
            llRecognized.setVisibility(View.VISIBLE);

            // 计算平均评分
            int avgScore = actionCount > 0 ? totalScore / actionCount : 0;
            // 更新识别完成结果
            tvTotalCount.setText(getString(R.string.train_total_count_format, currentMode, actionCount));
            tvAvgScore.setText(getString(R.string.train_avg_score_format, avgScore));
            // 根据模式显示不同的改进建议
            if (currentMode.equals(getString(R.string.train_mode_shoot))) {
                tvSuggestion.setText(getString(R.string.train_suggestion_shoot));
            } else if (currentMode.equals(getString(R.string.train_mode_dribble))) {
                tvSuggestion.setText(getString(R.string.train_suggestion_dribble));
            } else if (currentMode.equals(getString(R.string.train_mode_pass))) {
                tvSuggestion.setText(getString(R.string.train_suggestion_pass));
            }
        });

        // 4. 保存结果按钮点击
        btnSaveResult.setOnClickListener(v -> {
            if (isAdded()) {
                if (!videoFinalizeDone) {
                    Toast.makeText(requireContext(), getString(R.string.train_toast_wait_video_finalize), Toast.LENGTH_SHORT).show();
                    return;
                }
                saveTrainRecord();
                Toast.makeText(requireContext(), getString(R.string.train_toast_record_saved), Toast.LENGTH_SHORT).show();
                // 重置UI状态
                btnStartRecognize.setVisibility(View.VISIBLE);
                llRecognized.setVisibility(View.GONE);
                updateRecordStatus(getString(R.string.train_status_idle), false, getString(R.string.train_save_text_default));
                updateStartButtonStyle(false);
            }
        });
    }

    /**
     * Fragment销毁时释放资源（防止内存泄漏）
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopVideoRecording();
        // 关闭相机线程池
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        // 停止识别定时任务
        handler.removeCallbacks(recognizeRunnable);
    }
}
