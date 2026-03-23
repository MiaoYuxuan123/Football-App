package com.example.football.ui.result;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.football.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ResultDetailActivity extends AppCompatActivity {

    public static final String EXTRA_VIDEO_PATH = "extra_video_path";
    public static final String EXTRA_FEEDBACK_JSON = "extra_feedback_json";

    private static final Map<String, String> KEY_NAME_MAP = new HashMap<>();

    static {
        KEY_NAME_MAP.put("title", "标题");
        KEY_NAME_MAP.put("action_summary", "动作总结");
        KEY_NAME_MAP.put("overall_assessment", "整体评估");
        KEY_NAME_MAP.put("overall_score", "综合评分");
        KEY_NAME_MAP.put("score_breakdown", "分项评分");
        KEY_NAME_MAP.put("strengths", "动作亮点");
        KEY_NAME_MAP.put("improvements", "待提升项");
        KEY_NAME_MAP.put("issue", "问题");
        KEY_NAME_MAP.put("evidence", "依据");
        KEY_NAME_MAP.put("suggestion", "建议");
        KEY_NAME_MAP.put("training_drills", "训练建议");
        KEY_NAME_MAP.put("cautions", "注意事项");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_detail);

        TextView backView = findViewById(R.id.tv_back);
        VideoView videoView = findViewById(R.id.video_result);
        TextView feedbackView = findViewById(R.id.tv_feedback_content);

        backView.setOnClickListener(v -> finish());

        String videoPath = getIntent().getStringExtra(EXTRA_VIDEO_PATH);
        String feedbackJson = getIntent().getStringExtra(EXTRA_FEEDBACK_JSON);

        bindVideo(videoView, videoPath);
        bindFeedback(feedbackView, feedbackJson);
    }

    private void bindVideo(VideoView videoView, String videoPath) {
        if (TextUtils.isEmpty(videoPath)) {
            return;
        }
        File videoFile = new File(videoPath);
        if (!videoFile.exists()) {
            return;
        }
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);
        videoView.setVideoURI(Uri.fromFile(videoFile));
        videoView.seekTo(10);
    }

    private void bindFeedback(TextView feedbackView, String feedbackJson) {
        if (TextUtils.isEmpty(feedbackJson)) {
            feedbackView.setText("暂无分析反馈");
            return;
        }

        try {
            JSONObject jsonObject = new JSONObject(feedbackJson);
            jsonObject.remove("provider_model");
            jsonObject.remove("used_fallback");
            feedbackView.setText(formatObject(jsonObject, "", 0));
        } catch (Exception e) {
            feedbackView.setText("反馈解析失败，请稍后重试。\n\n原始内容：\n" + feedbackJson);
        }
    }

    private String formatObject(JSONObject object, String keyName, int depth) {
        StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(keyName)) {
            sb.append(indent(depth)).append(labelOf(keyName)).append("\n");
        }

        List<String> keys = new ArrayList<>();
        Iterator<String> iterator = object.keys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            if ("provider_model".equals(key) || "used_fallback".equals(key)) {
                continue;
            }
            keys.add(key);
        }
        Collections.sort(keys);

        for (String key : keys) {
            Object value = object.opt(key);
            if (value instanceof JSONObject) {
                sb.append(indent(depth)).append("【").append(labelOf(key)).append("】\n");
                sb.append(formatObject((JSONObject) value, "", depth + 1));
            } else if (value instanceof JSONArray) {
                sb.append(indent(depth)).append("【").append(labelOf(key)).append("】\n");
                sb.append(formatArray((JSONArray) value, depth + 1));
            } else {
                sb.append(indent(depth))
                        .append(labelOf(key))
                        .append("：")
                        .append(String.valueOf(value))
                        .append("\n");
            }
        }
        return sb.toString().trim();
    }

    private String formatArray(JSONArray array, int depth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length(); i++) {
            Object item = array.opt(i);
            if (item instanceof JSONObject) {
                sb.append(indent(depth)).append(i + 1).append(".\n");
                sb.append(formatObject((JSONObject) item, "", depth + 1)).append("\n");
            } else if (item instanceof JSONArray) {
                sb.append(indent(depth)).append(i + 1).append(".\n");
                sb.append(formatArray((JSONArray) item, depth + 1)).append("\n");
            } else {
                sb.append(indent(depth)).append("- ").append(String.valueOf(item)).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private String indent(int depth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            sb.append("  ");
        }
        return sb.toString();
    }

    private String labelOf(String key) {
        if (KEY_NAME_MAP.containsKey(key)) {
            return KEY_NAME_MAP.get(key);
        }
        return key.replace("_", " ");
    }
}