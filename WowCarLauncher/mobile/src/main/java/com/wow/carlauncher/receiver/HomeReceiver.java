package com.wow.carlauncher.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.wow.carlauncher.activity.LauncherActivity;
import com.wow.carlauncher.service.MainService;

/**
 * Created by 10124 on 2017/10/31.
 */

public class HomeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        Intent startIntent = new Intent(context, LauncherActivity.class);
        startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(startIntent);
    }
}
