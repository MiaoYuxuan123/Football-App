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
import com.example.football.utils.SPUtils;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
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
    private String currentMode = "射门"; // 默认选中射门模式
    private int actionCount = 0; // 动作计数
    private int totalScore = 0; // 总评分
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable recognizeRunnable; // 模拟识别的定时任务

    private static final String TAG = "TrainFragment";
    private static final String SP_KEY_TRAIN_RECORDS = "train_records";
    private static final String SAVE_TEXT_DEFAULT = "保存训练结果";
    private static final String SAVE_TEXT_WAIT = "视频保存中...";
    private static final String START_TEXT_DEFAULT = "开始识别";
    private static final String START_TEXT_RETRY = "重新录制";

    private String pendingVideoPath = "";
    private boolean videoFinalizeDone = false;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    initCameraPreview();
                } else if (isAdded()) {
                    Toast.makeText(requireContext(), "请先允许相机权限", Toast.LENGTH_SHORT).show();
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
        updateRecordStatus("视频状态：待开始录制", false, SAVE_TEXT_DEFAULT);
        updateStartButtonStyle(false);
    }

    private void updateRecordStatus(String text, boolean canSave, String saveButtonText) {
        tvRecordingState.setText(text);
        btnSaveResult.setEnabled(canSave);
        btnSaveResult.setText(saveButtonText);
    }

    private void updateStartButtonStyle(boolean isRetry) {
        btnStartRecognize.setText(isRetry ? START_TEXT_RETRY : START_TEXT_DEFAULT);
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
                    Toast.makeText(requireContext(), "相机初始化失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void startVideoRecording() {
        if (videoCapture == null || !isAdded()) {
            Toast.makeText(requireContext(), "录像未就绪，请稍后重试", Toast.LENGTH_SHORT).show();
            return;
        }

        File baseDir = requireContext().getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES);
        if (baseDir == null) {
            Toast.makeText(requireContext(), "无法访问视频存储目录", Toast.LENGTH_SHORT).show();
            return;
        }

        File videoDir = new File(baseDir, "train_videos");
        if (!videoDir.exists() && !videoDir.mkdirs()) {
            Toast.makeText(requireContext(), "无法创建视频目录", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = "train_" + System.currentTimeMillis() + ".mp4";
        File videoFile = new File(videoDir, fileName);
        pendingVideoPath = videoFile.getAbsolutePath();
        lastVideoPath = "";
        videoFinalizeDone = false;
        btnSaveResult.setEnabled(false);
        updateRecordStatus("视频状态：录制中", false, SAVE_TEXT_WAIT);
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
                            updateRecordStatus("视频状态：已保存，可回放", true, SAVE_TEXT_DEFAULT);
                            updateStartButtonStyle(false);
                            Toast.makeText(requireContext(), "视频已保存，可点击保存训练结果", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Video saved: " + lastVideoPath + ", size=" + videoFile.length());
                        } else {
                            Log.e(TAG, "Video finalize error: " + finalizeEvent.getError());
                            lastVideoPath = "";
                            videoFinalizeDone = false;
                            updateRecordStatus("视频状态：保存失败，请重录", false, SAVE_TEXT_DEFAULT);
                            btnStartRecognize.setVisibility(View.VISIBLE);
                            updateStartButtonStyle(true);
                            Toast.makeText(requireContext(), "视频保存失败，请点击重新录制", Toast.LENGTH_SHORT).show();
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
    }

    /**
     * 设置所有控件的监听事件
     */
    private void setViewListeners() {
        // 1. 模式选择监听（射门/运球/传球切换）
        rgMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_shoot) {
                currentMode = "射门";
            } else if (checkedId == R.id.rb_dribble) {
                currentMode = "运球";
            } else if (checkedId == R.id.rb_pass) {
                currentMode = "传球";
            }
            // 如果正在识别，实时更新当前动作
            if (isRecognizing) {
                tvCurrentAction.setText("当前动作：" + currentMode);
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
                            tvCurrentAction.setText("当前动作：" + currentMode);
                            tvConfidence.setText("置信度：" + String.format("%.2f", randomConfidence));
                            tvScore.setText("评分：" + randomScore + " 分");
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
            updateRecordStatus("视频状态：正在保存，请稍候", false, SAVE_TEXT_WAIT);
            // 停止定时任务
            handler.removeCallbacks(recognizeRunnable);
            // UI状态切换：隐藏识别中布局，显示识别完成布局
            llRecognizing.setVisibility(View.GONE);
            llRecognized.setVisibility(View.VISIBLE);

            // 计算平均评分
            int avgScore = actionCount > 0 ? totalScore / actionCount : 0;
            // 更新识别完成结果
            tvTotalCount.setText(currentMode + "次数：" + actionCount);
            tvAvgScore.setText("平均评分：" + avgScore);
            // 根据模式显示不同的改进建议
            switch (currentMode) {
                case "射门":
                    tvSuggestion.setText("- 支撑脚不稳\n- 射门角度偏低");
                    break;
                case "运球":
                    tvSuggestion.setText("- 运球频率过快\n- 重心偏高");
                    break;
                case "传球":
                    tvSuggestion.setText("- 传球力度不足\n- 传球方向偏差");
                    break;
            }
        });

        // 4. 保存结果按钮点击
        btnSaveResult.setOnClickListener(v -> {
            if (isAdded()) {
                if (!videoFinalizeDone) {
                    Toast.makeText(requireContext(), "请先结束识别并等待视频保存完成", Toast.LENGTH_SHORT).show();
                    return;
                }
                saveTrainRecord();
                Toast.makeText(requireContext(), "训练结果已保存到本地", Toast.LENGTH_SHORT).show();
                // 重置UI状态
                btnStartRecognize.setVisibility(View.VISIBLE);
                llRecognized.setVisibility(View.GONE);
                updateRecordStatus("视频状态：待开始录制", false, SAVE_TEXT_DEFAULT);
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

