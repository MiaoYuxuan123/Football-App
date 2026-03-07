package com.example.football.database.entity;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

/**
 * Plain data model for milestone dashboard.
 */
public class MilestoneData {

    public String account;
    public int level;
    public String technicalTitle;
    public int experience;
    public int experienceToNext;
    public int xpPerTraining;
    public int trainCount;
    public int shootCount;
    public int dribbleCount;
    public int passCount;
    public String badgesJson;
    public String technicalScoresJson;
    public String radarScoresJson;
    public int selectedStarId;

    public static MilestoneData createDefault(String account) {
        MilestoneData d = new MilestoneData();
        d.account = account;
        d.level = 1;
        d.technicalTitle = "初级球员";
        d.experience = 30;
        d.experienceToNext = 100;
        d.xpPerTraining = 10;
        d.trainCount = 0;
        d.shootCount = 0;
        d.dribbleCount = 0;
        d.passCount = 0;
        d.badgesJson = defaultBadgesJson();
        d.technicalScoresJson = "[68,72,74,76,79]";
        d.radarScoresJson = "[72,70,74,66,71,69]";
        d.selectedStarId = 0;
        return d;
    }

    private static String defaultBadgesJson() {
        List<BadgeItem> badges = new ArrayList<>();
        badges.add(new BadgeItem("b1", "初级射手", true));
        badges.add(new BadgeItem("b2", "连训3天", true));
        badges.add(new BadgeItem("b3", "百次射门", false));
        return new Gson().toJson(badges);
    }

    private static class BadgeItem {
        String id;
        String name;
        boolean unlocked;

        BadgeItem(String id, String name, boolean unlocked) {
            this.id = id;
            this.name = name;
            this.unlocked = unlocked;
        }
    }
}
