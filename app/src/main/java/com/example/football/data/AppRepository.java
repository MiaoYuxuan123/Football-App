package com.example.football.data;

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

    String getAvatarPath(String account);

    void setAvatarPath(String account, String path);
}
