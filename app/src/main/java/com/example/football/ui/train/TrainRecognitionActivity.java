package com.example.football.ui.train;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.example.football.R;
import com.example.football.data.AppRepository;
import com.example.football.data.RepositoryProvider;
import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrainRecognitionActivity extends AppCompatActivity {

    // 核心控件声明
    private PreviewView previewView;
    private RadioGroup rgMode;
    private Button btnStartRecognize, btnStopRecognize, btnSaveResult;
    private LinearLayout llRecognizing, llRecognized;
    private TextView tvCurrentAction, tvConfidence, tvScore, tvTotalCount, tvAvgScore, tvSuggestion;

    // CameraX 相关
    private ExecutorService cameraExecutor;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    // 识别状态控制
    private boolean isRecognizing = false;
    private String currentMode = "射门"; // 默认选中射门模式
    private int actionCount = 0; // 动作计数
    private int totalScore = 0; // 总评分
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable recognizeRunnable; // 模拟识别的定时任务
    private AppRepository repository;


    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    initCameraPreview();
                } else {
                    Toast.makeText(this, "请先允许相机权限", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_train_recognition);

        repository = RepositoryProvider.get(this);

        // 1. 初始化所有控件
        initViews();
        // 2. 检查权限后再初始化相机预览
        startCameraFlow();
        // 3. 设置所有点击事件和监听
        setViewListeners();
    }

    private void startCameraFlow() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            initCameraPreview();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    /**
     * 初始化所有控件（绑定ID）
     */
    private void initViews() {
        // 相机预览
        previewView = findViewById(R.id.previewView);
        // 模式选择
        rgMode = findViewById(R.id.rg_mode);
        // 按钮
        btnStartRecognize = findViewById(R.id.btn_start_recognize);
        btnStopRecognize = findViewById(R.id.btn_stop_recognize);
        btnSaveResult = findViewById(R.id.btn_save_result);
        // 识别状态布局
        llRecognizing = findViewById(R.id.ll_recognizing);
        llRecognized = findViewById(R.id.ll_recognized);
        // 识别结果文本
        tvCurrentAction = findViewById(R.id.tv_current_action);
        tvConfidence = findViewById(R.id.tv_confidence);
        tvScore = findViewById(R.id.tv_score);
        tvTotalCount = findViewById(R.id.tv_total_count);
        tvAvgScore = findViewById(R.id.tv_avg_score);
        tvSuggestion = findViewById(R.id.tv_suggestion);

        // 初始化相机线程池
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    /**
     * 初始化相机预览（打开后置摄像头）
     */
    private void initCameraPreview() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        // 相机初始化回调
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                // 配置预览功能
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                // 选择后置摄像头
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                // 解绑原有相机，绑定新的预览
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview);
            } catch (Exception e) {
                // 相机初始化失败提示
                Toast.makeText(this, "相机初始化失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void saveTrainRecord() {
        int avgScore = actionCount > 0 ? totalScore / actionCount : 0;
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
        String record = time + " | " + currentMode + " | 次数:" + actionCount + " | 均分:" + avgScore;

        String account = repository.getCurrentAccount();
        repository.prependTrainRecord(account, record);
        repository.recordTrainingResult(account, currentMode, avgScore, actionCount);
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
                    if (isRecognizing) {
                        actionCount++;
                        // 随机生成60-100分的评分（后续替换为真实识别结果）
                        int randomScore = (int) (60 + Math.random() * 40);
                        totalScore += randomScore;
                        // 随机生成0.8-1.0的置信度
                        float randomConfidence = (float) (0.8 + Math.random() * 0.2);

                        // 更新UI（必须在主线程）
                        runOnUiThread(() -> {
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
            saveTrainRecord();
            Toast.makeText(this, "训练结果已保存到本地", Toast.LENGTH_SHORT).show();
            // 返回上一页
            finish();
        });
    }

    /**
     * 页面销毁时释放资源（防止内存泄漏）
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 关闭相机线程池
        cameraExecutor.shutdown();
        // 停止识别定时任务
        handler.removeCallbacks(recognizeRunnable);
    }
}