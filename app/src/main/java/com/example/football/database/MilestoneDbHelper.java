package com.example.football.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.football.database.entity.MilestoneData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MilestoneDbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "football_milestone.db";
    private static final int DB_VERSION = 3;
    private static final String TABLE = "milestone";
    private static volatile MilestoneDbHelper instance;

    public static class TrainingSummary {
        public int totalCount;
        public int shootCount;
        public int dribbleCount;
        public int passCount;
        public int avgScore;
        public int successRate;
        public int level;
    }

    public static MilestoneDbHelper getInstance(Context context) {
        if (instance == null) {
            synchronized (MilestoneDbHelper.class) {
                if (instance == null) {
                    instance = new MilestoneDbHelper(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private MilestoneDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                + "account TEXT PRIMARY KEY,"
                + "level INTEGER NOT NULL,"
                + "technical_title TEXT NOT NULL,"
                + "experience INTEGER NOT NULL,"
                + "experience_to_next INTEGER NOT NULL,"
                + "xp_per_training INTEGER NOT NULL,"
                + "train_count INTEGER NOT NULL DEFAULT 0,"
                + "shoot_count INTEGER NOT NULL DEFAULT 0,"
                + "dribble_count INTEGER NOT NULL DEFAULT 0,"
                + "pass_count INTEGER NOT NULL DEFAULT 0,"
                + "badges_json TEXT,"
                + "technical_scores_json TEXT,"
                + "radar_scores_json TEXT,"
                + "selected_star_id INTEGER NOT NULL DEFAULT 0"
                + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE + " ADD COLUMN train_count INTEGER NOT NULL DEFAULT 0");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE + " ADD COLUMN shoot_count INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE + " ADD COLUMN dribble_count INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE + " ADD COLUMN pass_count INTEGER NOT NULL DEFAULT 0");
        }
    }

    public MilestoneData getOrCreate(String account) {
        MilestoneData data = find(account);
        if (data != null) {
            return data;
        }
        MilestoneData defaults = MilestoneData.createDefault(account);
        update(defaults);
        return defaults;
    }

    public void update(MilestoneData data) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("account", data.account);
        cv.put("level", data.level);
        cv.put("technical_title", data.technicalTitle);
        cv.put("experience", data.experience);
        cv.put("experience_to_next", data.experienceToNext);
        cv.put("xp_per_training", data.xpPerTraining);
        cv.put("train_count", data.trainCount);
        cv.put("shoot_count", data.shootCount);
        cv.put("dribble_count", data.dribbleCount);
        cv.put("pass_count", data.passCount);
        cv.put("badges_json", data.badgesJson);
        cv.put("technical_scores_json", data.technicalScoresJson);
        cv.put("radar_scores_json", data.radarScoresJson);
        cv.put("selected_star_id", data.selectedStarId);
        db.insertWithOnConflict(TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void recordTrainingResult(String account, String mode, int avgScore, int actionCount) {
        MilestoneData milestone = getOrCreate(account);
        milestone.trainCount += 1;
        if ("射门".equals(mode)) {
            milestone.shootCount += 1;
        } else if ("运球".equals(mode) || "盘带".equals(mode)) {
            milestone.dribbleCount += 1;
        } else if ("传球".equals(mode)) {
            milestone.passCount += 1;
        }

        int xpGain = Math.max(milestone.xpPerTraining, Math.max(5, avgScore / 3) + Math.max(0, actionCount / 2));
        milestone.experience += xpGain;

        while (milestone.experience >= milestone.experienceToNext) {
            milestone.experience -= milestone.experienceToNext;
            milestone.level += 1;
            milestone.experienceToNext += 50;
            milestone.xpPerTraining = Math.min(120, milestone.xpPerTraining + 2);
            milestone.technicalTitle = resolveTitleByLevel(milestone.level);
        }

        List<Float> trend = parseFloatList(milestone.technicalScoresJson);
        if (trend == null) {
            trend = new ArrayList<>();
        }
        trend.add((float) Math.max(0, Math.min(avgScore, 100)));
        if (trend.size() > 12) {
            trend = new ArrayList<>(trend.subList(trend.size() - 12, trend.size()));
        }
        milestone.technicalScoresJson = new Gson().toJson(trend);

        float base = avg(trend);
        float shoot = clamp(base + modeOffset(mode, "射门"), 50f, 99f);
        float pass = clamp(base + modeOffset(mode, "传球"), 50f, 99f);
        float dribble = clamp(base + Math.max(modeOffset(mode, "运球"), modeOffset(mode, "盘带")), 50f, 99f);
        List<Float> radar = new ArrayList<>();
        radar.add(shoot);
        radar.add(pass);
        radar.add(dribble);
        radar.add(clamp(base - 6f, 45f, 95f));
        radar.add(clamp(base - 2f, 45f, 98f));
        radar.add(clamp(base, 45f, 99f));
        milestone.radarScoresJson = new Gson().toJson(radar);

        update(milestone);
    }

    public TrainingSummary getTrainingSummary(String account) {
        MilestoneData data = getOrCreate(account);
        TrainingSummary summary = new TrainingSummary();
        summary.totalCount = data.trainCount;
        summary.shootCount = data.shootCount;
        summary.dribbleCount = data.dribbleCount;
        summary.passCount = data.passCount;
        summary.level = data.level;

        List<Float> trend = parseFloatList(data.technicalScoresJson);
        if (trend == null || trend.isEmpty()) {
            summary.avgScore = 0;
            summary.successRate = 0;
            return summary;
        }

        float sum = 0f;
        int success = 0;
        for (Float v : trend) {
            float score = (v == null ? 0f : v);
            sum += score;
            if (score >= 80f) {
                success++;
            }
        }
        summary.avgScore = Math.round(sum / trend.size());
        summary.successRate = Math.round(success * 100f / trend.size());
        return summary;
    }

    public void resetTrainingProgress(String account) {
        MilestoneData current = getOrCreate(account);
        MilestoneData reset = MilestoneData.createDefault(account);
        reset.selectedStarId = current.selectedStarId;
        update(reset);
    }

    private MilestoneData find(String account) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(TABLE, null, "account=?", new String[]{account}, null, null, null)) {
            if (!c.moveToFirst()) {
                return null;
            }
            MilestoneData d = new MilestoneData();
            d.account = c.getString(c.getColumnIndexOrThrow("account"));
            d.level = c.getInt(c.getColumnIndexOrThrow("level"));
            d.technicalTitle = c.getString(c.getColumnIndexOrThrow("technical_title"));
            d.experience = c.getInt(c.getColumnIndexOrThrow("experience"));
            d.experienceToNext = c.getInt(c.getColumnIndexOrThrow("experience_to_next"));
            d.xpPerTraining = c.getInt(c.getColumnIndexOrThrow("xp_per_training"));
            d.trainCount = getIntOrDefault(c, "train_count", 0);
            d.shootCount = getIntOrDefault(c, "shoot_count", 0);
            d.dribbleCount = getIntOrDefault(c, "dribble_count", 0);
            d.passCount = getIntOrDefault(c, "pass_count", 0);
            d.badgesJson = c.getString(c.getColumnIndexOrThrow("badges_json"));
            d.technicalScoresJson = c.getString(c.getColumnIndexOrThrow("technical_scores_json"));
            d.radarScoresJson = c.getString(c.getColumnIndexOrThrow("radar_scores_json"));
            d.selectedStarId = c.getInt(c.getColumnIndexOrThrow("selected_star_id"));
            return d;
        }
    }

    private int getIntOrDefault(Cursor c, String column, int defaultValue) {
        int idx = c.getColumnIndex(column);
        return idx >= 0 ? c.getInt(idx) : defaultValue;
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

    private String resolveTitleByLevel(int level) {
        if (level >= 10) {
            return "精英射手";
        }
        if (level >= 7) {
            return "高级球员";
        }
        if (level >= 4) {
            return "进阶球员";
        }
        return "初级球员";
    }

    private float modeOffset(String currentMode, String targetMode) {
        return targetMode.equals(currentMode) ? 4f : 0f;
    }


    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
