package com.example.football.data;

import android.net.Uri;

import com.example.football.database.MilestoneDbHelper;
import com.example.football.database.entity.MilestoneData;
import com.example.football.database.entity.TrainRecord;

import java.util.List;

public interface AppRepository {
    boolean isLoggedIn();

    void setLoggedIn(boolean loggedIn);

    String getCurrentAccount();

    void setCurrentAccount(String account);

    void logout();

    MilestoneData getMilestone(String account);

    MilestoneDbHelper.TrainingSummary getTrainingSummary(String account);

    void recordTrainingResult(String account, String mode, int avgScore, int actionCount);

    void resetTrainingProgress(String account);

    List<TrainRecord> getTrainRecordList(String account);

    String getTrainRecords(String account);

    void saveTrainRecords(String account, String records);

    void prependTrainRecord(String account, String record);

    // Unified write for one finished training session.
    void saveTrainingSession(String account, String mode, int avgScore, int actionCount, String videoPath, String feedbackJson);

    // Centralized media path allocator for training video output.
    String createTrainingVideoPath();

    String getAvatarPath(String account);

    void setAvatarPath(String account, String path);

    // Copy selected avatar content to app-managed storage and persist the path.
    String saveAvatarFromUri(String account, Uri uri);

    // Load user-uploaded local star photos used by Home banner.
    List<String> getLocalStarPhotoPaths();

    // Import picked images to app-managed star photo folder.
    int importStarPhotos(List<Uri> uris);

    // 获取当前账号的平均分数
    float getAvgScore(String account);

    // Replace full record list while preserving structured fields like video path and feedback JSON.
    void replaceTrainRecordList(String account, List<TrainRecord> records);
}
