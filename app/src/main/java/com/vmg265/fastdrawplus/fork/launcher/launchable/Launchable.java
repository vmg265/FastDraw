package com.vmg265.fastdrawplus.fork.launcher.launchable;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;

import com.vmg265.fastdrawplus.fork.launcher.LaunchManager;

public interface Launchable {
    void launch(Context context, LaunchManager launchManager, Bundle opts, Rect clipBounds);
}
