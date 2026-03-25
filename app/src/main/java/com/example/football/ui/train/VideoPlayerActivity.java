package com.example.football.ui.train;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.football.R;
import com.example.football.utils.VideoUtil;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
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

        adjustPlayerViewForMeta(videoPath, playerView);

        // 初始化 ExoPlayer 并绑定到 PlayerView
        exoPlayer = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(exoPlayer);

        MediaItem mediaItem = MediaItem.fromUri(Uri.fromFile(file));
        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
        exoPlayer.play();

        Log.d(TAG, "Start play video: " + videoPath);
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private void adjustPlayerViewForMeta(@NonNull String videoPath, @NonNull PlayerView playerView) {
        VideoUtil.VideoMeta meta = VideoUtil.extractMeta(videoPath);
        ViewGroup parent = (ViewGroup) playerView.getParent();
        if (meta == null || meta.width <= 0 || meta.height <= 0 || parent == null) {
            if (parent != null) {
                ViewGroup.LayoutParams lp = playerView.getLayoutParams();
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
                playerView.setLayoutParams(lp);
            }
            playerView.setRotation(0f);
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
            return;
        }

        if (parent.getWidth() == 0 || parent.getHeight() == 0) {
            parent.post(() -> adjustPlayerViewForMeta(videoPath, playerView));
            return;
        }

        int rotation = meta.rotation;
        boolean rotated = (rotation == 90 || rotation == 270);
        int naturalW = rotated ? meta.height : meta.width;
        int naturalH = rotated ? meta.width : meta.height;

        int containerW = parent.getWidth();
        int containerH = parent.getHeight();

        float scale = Math.max(containerW / (float) naturalW, containerH / (float) naturalH);
        int targetW = Math.round(naturalW * scale);
        int targetH = Math.round(naturalH * scale);

        ViewGroup.LayoutParams oldLp = playerView.getLayoutParams();
        if (oldLp instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) oldLp;
            flp.width = targetW;
            flp.height = targetH;
            flp.gravity = android.view.Gravity.CENTER;
            playerView.setLayoutParams(flp);
        } else {
            oldLp.width = targetW;
            oldLp.height = targetH;
            playerView.setLayoutParams(oldLp);
        }

        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).setClipChildren(false);
            ((ViewGroup) parent).setClipToPadding(false);
        }

        playerView.post(() -> {
            try {
                playerView.setPivotX(playerView.getWidth() / 2f);
                playerView.setPivotY(playerView.getHeight() / 2f);
                playerView.setRotation(rotation);
                playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
            } catch (Exception ignored) {
            }
        });
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
