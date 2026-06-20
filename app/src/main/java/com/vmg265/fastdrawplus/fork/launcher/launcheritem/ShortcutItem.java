package com.vmg265.fastdrawplus.fork.launcher.launcheritem;

import android.content.Context;

import com.vmg265.fastdrawplus.fork.launcher.OreoShortcuts;

public interface ShortcutItem extends LauncherItem {
    void delete(Context context) throws OreoShortcuts.UserLockedException, OreoShortcuts.HostPermissionException;
}
