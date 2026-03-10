package com.example.jalkster;

import android.content.Context;
import android.content.Intent;

public final class SessionIntentFactory {

    private SessionIntentFactory() {
    }

    public static Intent buildLiveSessionIntent(Context ctx,
                                                long jogMs,
                                                long walkMs,
                                                long restMs,
                                                String mode) {
        Intent intent = new Intent(ctx, JalkLiveSessionActivity.class);
        intent.putExtra(JalkSessionConfigActivity.EXTRA_JOG_DURATION_MILLIS, jogMs);
        intent.putExtra(JalkSessionConfigActivity.EXTRA_WALK_DURATION_MILLIS, walkMs);
        intent.putExtra(JalkSessionConfigActivity.EXTRA_REST_DURATION_MILLIS, restMs);
        if (mode != null) {
            intent.putExtra(JalkSessionConfigActivity.EXTRA_SESSION_MODE, mode);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }
}
