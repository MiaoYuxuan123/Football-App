package com.example.football.data;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public final class TrainingRefreshNotifier {

    public static final String SCOPE_RECORDS = "records";
    public static final String SCOPE_OVERVIEW = "overview";
    public static final String SCOPE_ALL = "all";
    public static final String SCOPE_MEDIA = "media";

    private static final MutableLiveData<TrainingRefreshEvent> EVENTS = new MutableLiveData<>();

    private TrainingRefreshNotifier() {
    }

    public static LiveData<TrainingRefreshEvent> events() {
        return EVENTS;
    }

    public static void notifyRecordsChanged(String account) {
        post(account, SCOPE_RECORDS);
    }

    public static void notifyOverviewChanged(String account) {
        post(account, SCOPE_OVERVIEW);
    }

    public static void notifyAllChanged(String account) {
        post(account, SCOPE_ALL);
    }

    public static void notifyMediaChanged(String account) {
        post(account, SCOPE_MEDIA);
    }

    private static void post(String account, String scope) {
        EVENTS.postValue(new TrainingRefreshEvent(normalizeAccount(account), scope, System.currentTimeMillis()));
    }

    private static String normalizeAccount(String account) {
        if (account == null) {
            return "default";
        }
        String trimmed = account.trim();
        return trimmed.isEmpty() ? "default" : trimmed;
    }

    public static final class TrainingRefreshEvent {
        public final String account;
        public final String scope;
        public final long version;

        public TrainingRefreshEvent(@NonNull String account, @NonNull String scope, long version) {
            this.account = account;
            this.scope = scope;
            this.version = version;
        }

        public boolean matchesAccount(String targetAccount) {
            return account.equals(normalizeAccount(targetAccount));
        }

        public boolean affectsOverview() {
            return SCOPE_OVERVIEW.equals(scope) || SCOPE_ALL.equals(scope);
        }

        public boolean affectsRecords() {
            return SCOPE_RECORDS.equals(scope) || SCOPE_ALL.equals(scope);
        }

        public boolean affectsMedia() {
            return SCOPE_MEDIA.equals(scope) || SCOPE_ALL.equals(scope);
        }
    }
}
