package com.vmg265.fastdrawplus.fork.launcher.launcheritem;

import android.content.Context;

import androidx.annotation.NonNull;

import com.vmg265.fastdrawplus.fork.launcher.ShortcutItemManager;

public abstract class FiledShortcutItem implements ShortcutItem, Saveable {
    @NonNull abstract String getTypeKey();
    @NonNull abstract String getUUID();

    public String getFilename() {
        return getTypeKey() + "_" + getUUID();
    }

    @Override
    public void delete(final Context context) {
        ShortcutItemManager.deleteShortcut(context, this);
    }
}

