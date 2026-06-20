package com.vmg265.fastdrawplus.fork.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.vmg265.fastdrawplus.fork.activities.MainActivity;
import com.vmg265.fastdrawplus.fork.launcher.ShortcutItemManager;
import com.vmg265.fastdrawplus.fork.launcher.launcheritem.FiledShortcutItem;

public class InstallShortcutReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent data) {
        final FiledShortcutItem newShortcutItem = ShortcutItemManager.shortcutFromIntent(context, data);
        ShortcutItemManager.saveShortcut(context, newShortcutItem);

        final MainActivity activity = MainActivity.getInstance();
        if (activity != null) {
            activity.onShortcutReceived(newShortcutItem);
        }
    }
}
