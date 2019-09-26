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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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

    public static void info(Object obj, String str) {
        Log.i(TAG, obj.getClass().getSimpleName() + ": " + str);
    }

    public static void info(Class<?> clazz, String str) {
        Log.i(TAG, clazz.getSimpleName() + ": " + str);
    }

    public static void startMonitorServiceby(Context context, String act) {
        info(Utils.class.getSimpleName(), "startMonitorServiceby +++");
        Intent intent = new Intent(context, MonitorService.class);
        intent.setAction(act);
        context.startService(intent);
    }

    public static void stopService(Context context) {
        Intent intent = new Intent(context, MonitorService.class);
        context.stopService(intent);
    }

    public static void showAlertDialog(Context context, String msg, DialogInterface.OnClickListener listener) {
        info(Utils.class, "showAlertDialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Info:");
        builder.setMessage(msg);
        builder.setPositiveButton(context.getResources().getString(R.string.label_ok_btn),
                listener);
        AlertDialog dialog = builder.create();

        dialog.show();
    }

}
