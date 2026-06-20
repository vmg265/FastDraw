package com.vmg265.fastdrawplus.fork.launcher;

import static android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED;

import android.content.Context;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.stream.Collectors;

import com.vmg265.fastdrawplus.fork.R;

public class OreoShortcuts {
    private OreoShortcuts() {}

    public static CharSequence getLabel(@NonNull final ShortcutInfo shortcutInfo) {
        final CharSequence shortLabel = shortcutInfo.getShortLabel();
        if (!TextUtils.isEmpty(shortLabel)) {
            return shortLabel;
        }

        return shortcutInfo.getLongLabel();
    }

    public static Drawable getIcon(@NonNull final Context context, @NonNull ShortcutInfo shortcutInfo) {
        final LauncherApps launcherApps = (LauncherApps)context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        return launcherApps.getShortcutIconDrawable(shortcutInfo, 0);
    }

    @Nullable
    public static List<ShortcutInfo> getPinnedShortcuts(@NonNull final Context context) throws UserLockedException, HostPermissionException {
        final LauncherApps launcherApps = (LauncherApps)context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        final UserManager userManager = (UserManager)context.getSystemService(Context.USER_SERVICE);
        final UserHandle userHandle = getRunningUserHandle(launcherApps, userManager);
        return getPinnedShortcuts(launcherApps, userHandle);
    }

    public static void unpinShortcut(
        @NonNull final Context context,
        @NonNull final String shortcutPackage,
        @NonNull final String shortcutId
    ) throws UserLockedException, HostPermissionException {
        final LauncherApps launcherApps = (LauncherApps)context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        final UserManager userManager = (UserManager)context.getSystemService(Context.USER_SERVICE);
        final UserHandle userHandle = getRunningUserHandle(launcherApps, userManager);
        unpinShortcut(launcherApps, userHandle, shortcutPackage, shortcutId);
    }

    @Nullable
    private static List<ShortcutInfo> getPinnedShortcuts(
        @NonNull final LauncherApps launcherApps,
        @NonNull final UserHandle user
    ) throws UserLockedException, HostPermissionException {
        final LauncherApps.ShortcutQuery query = new LauncherApps.ShortcutQuery();
        query.setQueryFlags(FLAG_MATCH_PINNED);
        return getShortcuts(launcherApps, user, query);
    }

    @Nullable
    private static List<ShortcutInfo> getPinnedShortcuts(
        @NonNull final LauncherApps launcherApps,
        @NonNull final UserHandle user,
        @NonNull final String packageName
    ) throws UserLockedException, HostPermissionException {
        final LauncherApps.ShortcutQuery query = new LauncherApps.ShortcutQuery();
        query.setQueryFlags(FLAG_MATCH_PINNED);
        query.setPackage(packageName);
        return getShortcuts(launcherApps, user, query);
    }

    private static void unpinShortcut(
        @NonNull final LauncherApps launcherApps,
        @NonNull final UserHandle user,
        @NonNull final String shortcutPackage,
        @NonNull final String shortcutId
    ) throws UserLockedException, HostPermissionException {
        final List<ShortcutInfo> pinnedShortcuts = getPinnedShortcuts(launcherApps, user, shortcutPackage);
        if (pinnedShortcuts == null) {
            Log.w("OreoShortcuts", String.format("Shortcut %s of package %s is not pinned", shortcutId, shortcutPackage));
            return;
        }

        final List<String> newPinnedShortcutIds = pinnedShortcuts.stream().
            map(ShortcutInfo::getId).
            filter(id -> !id.equals(shortcutId)).
            collect(Collectors.toList());

        pinShortcuts(launcherApps, user, shortcutPackage, newPinnedShortcutIds);
    }

    @NonNull
    private static UserHandle getRunningUserHandle(
        @NonNull final LauncherApps launcherApps,
        @NonNull final UserManager userManager
    ) throws UserLockedException {
        for (final UserHandle user : launcherApps.getProfiles()) {
            if (userManager.isUserRunning(user) && userManager.isUserUnlocked(user)) {
                return user;
            }
        }
        throw new UserLockedException(null);
    }

    private static List<ShortcutInfo> getShortcuts(
        @NonNull final LauncherApps launcherApps,
        @NonNull final UserHandle user,
        @NonNull final LauncherApps.ShortcutQuery query
    ) throws UserLockedException, HostPermissionException {
        try {
            return launcherApps.getShortcuts(query, user);
        } catch (final IllegalStateException e) {
            throw new UserLockedException(e);
        } catch (final SecurityException e) {
            throw new HostPermissionException(e);
        }
    }

    private static void pinShortcuts(
        @NonNull final LauncherApps launcherApps,
        @NonNull final UserHandle user,
        @NonNull final String shortcutPackage,
        @NonNull List<String> newPinnedShortcutIds
    ) throws UserLockedException, HostPermissionException {
        try {
            launcherApps.pinShortcuts(shortcutPackage, newPinnedShortcutIds, user);
        } catch (final IllegalStateException e) {
            throw new UserLockedException(e);
        } catch (final SecurityException e) {
            throw new HostPermissionException(e);
        }
    }

    public static class UserLockedException extends Exception {
        public UserLockedException(@Nullable final IllegalStateException cause) {
            super("User is locked or not running", cause);
        }
    }

    public static class HostPermissionException extends Exception {
        public HostPermissionException(@Nullable final SecurityException cause) {
            super("Missing shortcut host permission", cause);
        }
    }
}
