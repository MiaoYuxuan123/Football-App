package com.example.football.data;

import android.content.Context;
import android.text.TextUtils;

import com.example.football.database.MilestoneDbHelper;
import com.example.football.database.entity.MilestoneData;
import com.example.football.database.entity.TrainRecord;
import com.example.football.utils.SPUtils;

import java.util.ArrayList;
import java.util.List;

public class AppRepositoryImpl implements AppRepository {

    private static final String KEY_IS_LOGIN = "isLogin";
    private static final String KEY_ACCOUNT = "account";
    private static final String KEY_TRAIN_RECORDS_LEGACY = "train_records";
    private static final String KEY_TRAIN_RECORDS_PREFIX = "train_records_";
    private static final String KEY_TRAIN_RECORDS_MIGRATED_PREFIX = "train_records_migrated_";
    private static final String KEY_AVATAR_PATH_PREFIX = "avatar_path_";

    private final Context appContext;

    public AppRepositoryImpl(Context context) {
        this.appContext = context.getApplicationContext();
    }

    @Override
    public boolean isLoggedIn() {
        return SPUtils.getBoolean(appContext, KEY_IS_LOGIN, false);
    }

    @Override
    public void setLoggedIn(boolean loggedIn) {
        SPUtils.putBoolean(appContext, KEY_IS_LOGIN, loggedIn);
    }

    @Override
    public String getCurrentAccount() {
        return normalizeAccount(SPUtils.getString(appContext, KEY_ACCOUNT, "default"));
    }

    @Override
    public void setCurrentAccount(String account) {
        SPUtils.putString(appContext, KEY_ACCOUNT, normalizeAccount(account));
    }

    @Override
    public void logout() {
        setLoggedIn(false);
    }

    @Override
    public MilestoneData getMilestone(String account) {
        return MilestoneDbHelper.getInstance(appContext).getOrCreate(normalizeAccount(account));
    }

    @Override
    public MilestoneDbHelper.TrainingSummary getTrainingSummary(String account) {
        return MilestoneDbHelper.getInstance(appContext).getTrainingSummary(normalizeAccount(account));
    }

    @Override
    public void recordTrainingResult(String account, String mode, int avgScore, int actionCount) {
        MilestoneDbHelper.getInstance(appContext)
                .recordTrainingResult(normalizeAccount(account), mode, avgScore, actionCount);
    }

    @Override
    public void resetTrainingProgress(String account) {
        MilestoneDbHelper.getInstance(appContext).resetTrainingProgress(normalizeAccount(account));
    }

    @Override
    public List<TrainRecord> getTrainRecordList(String account) {
        String normalized = normalizeAccount(account);
        ensureTrainRecordsMigrated(normalized);
        return MilestoneDbHelper.getInstance(appContext).getTrainRecords(normalized);
    }

    @Override
    public String getTrainRecords(String account) {
        String normalized = normalizeAccount(account);
        ensureTrainRecordsMigrated(normalized);
        return joinLines(MilestoneDbHelper.getInstance(appContext).getTrainRecordTexts(normalized));
    }

    @Override
    public void saveTrainRecords(String account, String records) {
        String normalized = normalizeAccount(account);
        MilestoneDbHelper.getInstance(appContext)
                .replaceTrainRecords(normalized, parseTrainRecords(records));
        markTrainRecordsMigrated(normalized);
        clearLegacyTrainRecordKeys(normalized);
    }

    @Override
    public void prependTrainRecord(String account, String record) {
        String normalized = normalizeAccount(account);
        ensureTrainRecordsMigrated(normalized);
        TrainRecord parsed = TrainRecord.fromRawText(record);
        if (parsed.rawText == null || parsed.rawText.trim().isEmpty()) {
            return;
        }
        MilestoneDbHelper.getInstance(appContext).prependTrainRecord(normalized, parsed);
        markTrainRecordsMigrated(normalized);
        clearLegacyTrainRecordKeys(normalized);
    }

    @Override
    public String getAvatarPath(String account) {
        return SPUtils.getString(appContext, buildAvatarPathKey(account), "");
    }

    @Override
    public void setAvatarPath(String account, String path) {
        SPUtils.putString(appContext, buildAvatarPathKey(account), path == null ? "" : path);
    }

    private String buildTrainRecordsKey(String account) {
        return KEY_TRAIN_RECORDS_PREFIX + normalizeAccount(account);
    }

    private String buildAvatarPathKey(String account) {
        return KEY_AVATAR_PATH_PREFIX + normalizeAccount(account);
    }

    private String buildTrainRecordMigratedKey(String account) {
        return KEY_TRAIN_RECORDS_MIGRATED_PREFIX + normalizeAccount(account);
    }

    private void ensureTrainRecordsMigrated(String account) {
        String normalized = normalizeAccount(account);
        if (SPUtils.getBoolean(appContext, buildTrainRecordMigratedKey(normalized), false)) {
            return;
        }

        MilestoneDbHelper dbHelper = MilestoneDbHelper.getInstance(appContext);
        if (dbHelper.hasTrainRecords(normalized)) {
            markTrainRecordsMigrated(normalized);
            clearLegacyTrainRecordKeys(normalized);
            return;
        }

        String legacyRecords = SPUtils.getString(appContext, buildTrainRecordsKey(normalized), "");
        if (TextUtils.isEmpty(legacyRecords) && normalized.equals(getCurrentAccount())) {
            legacyRecords = SPUtils.getString(appContext, KEY_TRAIN_RECORDS_LEGACY, "");
        }
        if (!TextUtils.isEmpty(legacyRecords)) {
            dbHelper.replaceTrainRecords(normalized, parseTrainRecords(legacyRecords));
        }
        markTrainRecordsMigrated(normalized);
        clearLegacyTrainRecordKeys(normalized);
    }

    private void markTrainRecordsMigrated(String account) {
        SPUtils.putBoolean(appContext, buildTrainRecordMigratedKey(account), true);
    }

    private void clearLegacyTrainRecordKeys(String account) {
        SPUtils.putString(appContext, buildTrainRecordsKey(account), "");
        if (normalizeAccount(account).equals(getCurrentAccount())) {
            SPUtils.putString(appContext, KEY_TRAIN_RECORDS_LEGACY, "");
        }
    }

    private List<TrainRecord> parseTrainRecords(String records) {
        List<TrainRecord> result = new ArrayList<>();
        if (records == null || records.trim().isEmpty()) {
            return result;
        }
        String[] lines = records.split("\\n");
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            result.add(TrainRecord.fromRawText(trimmed));
        }
        return result;
    }

    private String joinLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(lines.get(i));
        }
        return sb.toString();
    }

    private String normalizeAccount(String account) {
        if (TextUtils.isEmpty(account)) {
            return "default";
        }
        String trimmed = account.trim();
        return trimmed.isEmpty() ? "default" : trimmed;
    }
}
