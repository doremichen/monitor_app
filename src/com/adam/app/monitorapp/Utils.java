/**
 * ===========================================================================
 * Copyright Adam Sample code
 * All Rights Reserved
 * ===========================================================================
 * 
 * File Name: Utils.java
 * Brief: 
 * 
 * Author: AdamChen
 * Create Date: 2018/9/5
 */

package com.adam.app.monitorapp;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * <h1>Utils</h1>
 * 
 * @autor AdamChen
 * @since 2018/9/5
 */
public abstract class Utils {

    public static final String ACTION_UPDATE_VIEW = "com.adam.app.service.action.start_update_view";
    public static final String ACTION_START_RECORD = "com.adam.app.service.action.start_recording";
    public static final String ACTION_STOP_RECORD = "com.adam.app.service.action.stop_recording";

    private static final String TAG = "MonitorDemo";

    static final String PROC_MEMINFO_PATH = "/proc/meminfo";
    static final String PROC_STAT_PATH = "/proc/stat";

    static final String MEM_TOTAL_ITEM = "MemTotal:";
    static final String MEM_FREE_ITEM = "MemFree:";
    static final String BUFFERS_ITEM = "Buffers:";
    static final String CACHED_ITEM = "Cached:";
    static final String ACTIVE_ITEM = "Active:";
    static final String INACTIVE_ITEM = "Inactive:";
    static final String DIRTY_ITEM = "Dirty:";
    static final String VMALLOCTOTAL_ITEM = "VmallocTotal:";
    static final String VMALLOCUSED_ITEM = "VmallocUsed:";
    static final String VMALLOCCHUNK_ITEM = "VmallocChunk:";

    public static void inFo(Object obj, String str) {
        Log.i(TAG, obj.getClass().getSimpleName() + ": " + str);
    }

    public static void inFo(Class<?> clazz, String str) {
        Log.i(TAG, clazz.getSimpleName() + ": " + str);
    }

    public static void startMonitorServiceby(Context context, String act) {
        inFo(Utils.class.getSimpleName(), "startMonitorServiceby +++");
        Intent intent = new Intent(context, MonitorService.class);
        intent.setAction(act);
        context.startService(intent);
    }

    public static void stopService(Context context) {
        Intent intent = new Intent(context, MonitorService.class);
        context.stopService(intent);
    }

}
