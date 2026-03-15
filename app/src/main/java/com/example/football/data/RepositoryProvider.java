package com.example.football.data;

import android.content.Context;

public final class RepositoryProvider {

    private static volatile AppRepository instance;

    private RepositoryProvider() {
    }

    public static AppRepository get(Context context) {
        if (instance == null) {
            synchronized (RepositoryProvider.class) {
                if (instance == null) {
                    instance = new AppRepositoryImpl(context.getApplicationContext());
                }
            }
        }
        return instance;
    }
}
