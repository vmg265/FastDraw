package com.vmg265.fastdrawplus.fork;

public class Utils {
    public static float clamp(final float value, final float min, final float max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
