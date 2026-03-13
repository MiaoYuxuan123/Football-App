package com.example.football.ui.main.model;

public class BadgeDisplayItem {
    public final String id;
    public final String title;
    public final String subtitle;
    public final boolean unlocked;

    public BadgeDisplayItem(String id, String title, String subtitle, boolean unlocked) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.unlocked = unlocked;
    }
}

