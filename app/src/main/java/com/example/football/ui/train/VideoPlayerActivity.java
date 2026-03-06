package com.example.football.ui.train;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.football.R;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ui.PlayerView;

import java.io.File;

public class VideoPlayerActivity extends AppCompatActivity {

    public static final String EXTRA_VIDEO_PATH = "extra_video_path";
    private static final String TAG = "VideoPlayerActivity";

    private PlayerView playerView;
    private TextView tvVideoPath;
    private ExoPlayer exoPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        playerView = findViewById(R.id.player_view);
        tvVideoPath = findViewById(R.id.tv_video_path);

        String videoPath = getIntent().getStringExtra(EXTRA_VIDEO_PATH);
        if (TextUtils.isEmpty(videoPath)) {
            Toast.makeText(this, "未找到视频路径", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        File file = new File(videoPath);
        if (!file.exists()) {
            Toast.makeText(this, "视频文件不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvVideoPath.setText("视频路径: " + videoPath);

        // 初始化 ExoPlayer 并绑定到 PlayerView
        exoPlayer = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(exoPlayer);

        MediaItem mediaItem = MediaItem.fromUri(file.toURI().toString());
        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
        exoPlayer.play();

        Log.d(TAG, "Start play video: " + videoPath);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (exoPlayer != null) {
            exoPlayer.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }
}
