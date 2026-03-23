package com.example.football.data;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.example.football.database.MilestoneDbHelper;
import com.example.football.database.entity.MilestoneData;
import com.example.football.database.entity.TrainRecord;
import com.example.football.utils.SPUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
        String normalized = normalizeAccount(account);
        MilestoneDbHelper.getInstance(appContext).resetTrainingProgress(normalized);
        TrainingRefreshNotifier.notifyOverviewChanged(normalized);
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
        TrainingRefreshNotifier.notifyRecordsChanged(normalized);
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
        TrainingRefreshNotifier.notifyRecordsChanged(normalized);
    }

    @Override
    public void saveTrainingSession(String account, String mode, int avgScore, int actionCount, String videoPath) {
        String normalized = normalizeAccount(account);
        ensureTrainRecordsMigrated(normalized);

        TrainRecord record = new TrainRecord();
        record.account = normalized;
        record.createdAt = formatNow();
        record.mode = mode == null ? "" : mode.trim();
        record.actionCount = Math.max(0, actionCount);
        record.avgScore = Math.max(0, avgScore);
        record.videoPath = videoPath == null ? "" : videoPath.trim();
        record.rawText = buildTrainRecordText(record);

        MilestoneDbHelper.getInstance(appContext)
                .saveTrainingSessionAtomic(normalized, record.mode, record.avgScore, record.actionCount, record);
        markTrainRecordsMigrated(normalized);
        clearLegacyTrainRecordKeys(normalized);
        TrainingRefreshNotifier.notifyAllChanged(normalized);
    }

    @Override
    public String createTrainingVideoPath() {
        File dir = new File(appContext.getFilesDir(), "train_videos");
        if (!dir.exists() && !dir.mkdirs()) {
            return "";
        }
        String fileName = "train_" + System.currentTimeMillis() + ".mp4";
        return new File(dir, fileName).getAbsolutePath();
    }

    @Override
    public String getAvatarPath(String account) {
        return SPUtils.getString(appContext, buildAvatarPathKey(account), "");
    }

    @Override
    public void setAvatarPath(String account, String path) {
        String normalized = normalizeAccount(account);
        SPUtils.putString(appContext, buildAvatarPathKey(normalized), path == null ? "" : path);
        TrainingRefreshNotifier.notifyMediaChanged(normalized);
    }

    @Override
    public String saveAvatarFromUri(String account, Uri uri) {
        String normalized = normalizeAccount(account);
        if (uri == null) {
            return "";
        }
        File dir = new File(appContext.getFilesDir(), "avatars");
        if (!dir.exists() && !dir.mkdirs()) {
            return "";
        }
        File outFile = new File(dir, normalized + "_" + System.currentTimeMillis() + ".jpg");
        if (!copyUriToFile(uri, outFile)) {
            return "";
        }
        String path = outFile.getAbsolutePath();
        setAvatarPath(normalized, path);
        return path;
    }

    @Override
    public List<String> getLocalStarPhotoPaths() {
        List<String> result = new ArrayList<>();
        File dir = new File(appContext.getFilesDir(), "star_photos");
        if (!dir.exists() || !dir.isDirectory()) {
            return result;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return result;
        }
        for (File file : files) {
            if (file != null && file.isFile()) {
                result.add(file.getAbsolutePath());
            }
        }
        return result;
    }

    @Override
    public int importStarPhotos(List<Uri> uris) {
        if (uris == null || uris.isEmpty()) {
            return 0;
        }
        File dir = new File(appContext.getFilesDir(), "star_photos");
        if (!dir.exists() && !dir.mkdirs()) {
            return 0;
        }
        int imported = 0;
        for (Uri uri : uris) {
            if (uri == null) {
                continue;
            }
            File outFile = new File(dir, "star_" + System.currentTimeMillis() + "_" + imported + ".jpg");
            if (copyUriToFile(uri, outFile)) {
                imported++;
            }
        }
        if (imported > 0) {
            TrainingRefreshNotifier.notifyMediaChanged(getCurrentAccount());
        }
        return imported;
    }

    @Override
    public float getAvgScore(String account) {
        List<TrainRecord> records = getTrainRecordList(account);
        if (records == null || records.isEmpty()) {
            return 0f;
        }
        float sum = 0f;
        int count = 0;
        for (TrainRecord record : records) {
            sum += record.avgScore;
            count++;
        }
        return count == 0 ? 0f : sum / count;
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

    private String formatNow() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
    }

    private String buildTrainRecordText(TrainRecord record) {
        return record.createdAt
                + " | " + record.mode
                + " | 次数:" + record.actionCount
                + " | 均分:" + record.avgScore
                + (record.videoPath.isEmpty() ? "" : " | 视频:" + record.videoPath);
    }

    private boolean copyUriToFile(Uri uri, File outFile) {
        try (InputStream in = appContext.getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(outFile)) {
            if (in == null) {
                return false;
            }
            byte[] buffer = new byte[8 * 1024];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            out.flush();
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private String normalizeAccount(String account) {
        if (TextUtils.isEmpty(account)) {
            return "default";
        }
        String trimmed = account.trim();
        return trimmed.isEmpty() ? "default" : trimmed;
    }
}
