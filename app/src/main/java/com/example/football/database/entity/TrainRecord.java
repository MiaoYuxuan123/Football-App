package com.example.football.database.entity;

public class TrainRecord {
    public long id;
    public String account;
    public String rawText;
    public String createdAt;
    public String mode;
    public int actionCount;
    public int avgScore;
    public String videoPath;
    public String feedbackJson;

    public static TrainRecord fromRawText(String rawText) {
        TrainRecord record = new TrainRecord();
        record.rawText = rawText == null ? "" : rawText.trim();
        record.createdAt = "";
        record.mode = "";
        record.videoPath = "";
        record.feedbackJson = "";
        if (record.rawText.isEmpty()) {
            return record;
        }

        String[] parts = record.rawText.split("\\|");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i] == null ? "" : parts[i].trim();
            if (part.isEmpty()) {
                continue;
            }
            if (i == 0) {
                record.createdAt = part;
            }
            if (part.startsWith("次数:") || part.startsWith("次数：")) {
                record.actionCount = safeParseInt(part.substring(3));
                continue;
            }
            if (part.startsWith("均分:") || part.startsWith("均分：")) {
                record.avgScore = safeParseInt(part.substring(3));
                continue;
            }
            if (part.startsWith("视频:") || part.startsWith("视频：")) {
                record.videoPath = part.substring(3).trim();
                continue;
            }
            if (part.contains("射门") || part.contains("运球") || part.contains("盘带") || part.contains("传球")) {
                record.mode = part;
            }
        }
        return record;
    }

    private static int safeParseInt(String raw) {
        if (raw == null) {
            return 0;
        }
        String digits = raw.replaceAll("[^0-9-]", "").trim();
        if (digits.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
