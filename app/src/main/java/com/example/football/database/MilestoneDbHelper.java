package com.example.football.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.football.database.entity.MilestoneData;

public class MilestoneDbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "football_milestone.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE = "milestone";
    private static volatile MilestoneDbHelper instance;

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
                + "badges_json TEXT,"
                + "technical_scores_json TEXT,"
                + "radar_scores_json TEXT,"
                + "selected_star_id INTEGER NOT NULL DEFAULT 0"
                + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
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
        cv.put("badges_json", data.badgesJson);
        cv.put("technical_scores_json", data.technicalScoresJson);
        cv.put("radar_scores_json", data.radarScoresJson);
        cv.put("selected_star_id", data.selectedStarId);
        db.insertWithOnConflict(TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
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
            d.badgesJson = c.getString(c.getColumnIndexOrThrow("badges_json"));
            d.technicalScoresJson = c.getString(c.getColumnIndexOrThrow("technical_scores_json"));
            d.radarScoresJson = c.getString(c.getColumnIndexOrThrow("radar_scores_json"));
            d.selectedStarId = c.getInt(c.getColumnIndexOrThrow("selected_star_id"));
            return d;
        }
    }
}

