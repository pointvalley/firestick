package com.pointvalley.firestick;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Opens the home screen when the Firestick finishes starting up. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            Intent open = new Intent(context, MainActivity.class);
            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                context.startActivity(open);
            } catch (Exception ignored) {
                // some Fire OS versions block this; Projectivy "Launch on Boot" is the fallback
            }
        }
    }
}
