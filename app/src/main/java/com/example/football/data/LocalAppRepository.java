package com.example.football.data;

import android.content.Context;

/**
 * Backward-compatible alias. Use AppRepositoryImpl for new code.
 */
@Deprecated
public class LocalAppRepository extends AppRepositoryImpl {

    public LocalAppRepository(Context context) {
        super(context);
    }
}
