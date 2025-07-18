/**
 * ===========================================================================
 * Copyright Adam Sample code
 * All Rights Reserved
 * ===========================================================================
 * 
 * File Name: Utils.java
 * Brief: This class defines the common methods
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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

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

    static final String PROC_MEM_INFO_PATH = "/proc/meminfo";

    static final String MEM_TOTAL_ITEM = "MemTotal:";
    static final String MEM_FREE_ITEM = "MemFree:";
    static final String BUFFERS_ITEM = "Buffers:";
    static final String CACHED_ITEM = "Cached:";
    static final String ACTIVE_ITEM = "Active:";
    static final String INACTIVE_ITEM = "Inactive:";
    static final String DIRTY_ITEM = "Dirty:";
    static final String VM_ALLOC_TOTAL_ITEM = "VmAllocTotal:";
    static final String VM_ALLOC_USED_ITEM = "VmAllocUsed:";
    static final String VM_ALLOC_CHUNK_ITEM = "VmAllocChunk:";
    public static final String CPU_WORK_ITEM = "CpuWork:";

    public static void info(Object obj, String str) {
        Log.i(TAG, obj.getClass().getSimpleName() + ": " + str);
    }

    public static void info(Class<?> clazz, String str) {
        Log.i(TAG, clazz.getSimpleName() + ": " + str);
    }

    public static void startMonitorServiceBy(Context context, String act) {
        info(Utils.class.getSimpleName(), "startMonitorServiceBy +++");
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
        builder.setTitle(R.string.alertdlg_title_info);
        builder.setMessage(msg);
        builder.setPositiveButton(context.getResources().getString(R.string.label_ok_btn),
                listener);
        AlertDialog dialog = builder.create();

        dialog.show();
    }

    /**
     * Read CPU usage from top command
     * @return CPU usage
     */
    public static String readCpuUsageFromTop() {
        StringBuilder output = new StringBuilder();
        try {
            Process process = new ProcessBuilder("top", "-n", "2", "-d", "0.3")
                    .redirectErrorStream(true)
                    .start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.toLowerCase().contains("cpu")) {
                    return line;
                }
            }

            return "Can not get CPU usage!!!";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Calculate CPU usage from command line output
     * @param cpuLine command line output
     * @return CPU usage
     */
    public static double calculateCpuUsage(String cpuLine) {
        // 假設輸入字串如: "400%cpu   0%user   0%nice   0%sys   400%idle   0%iow   0%irq   0%sirq   0%host"
        try {
            String[] parts = cpuLine.split("\\s+");

            int user = 0, nice = 0, sys = 0, idle = 0;

            for (String part : parts) {
                if (part.endsWith("%user")) {
                    user = Integer.parseInt(part.replace("%user", ""));
                } else if (part.endsWith("%nice")) {
                    nice = Integer.parseInt(part.replace("%nice", ""));
                } else if (part.endsWith("%sys")) {
                    sys = Integer.parseInt(part.replace("%sys", ""));
                } else if (part.endsWith("%idle")) {
                    idle = Integer.parseInt(part.replace("%idle", ""));
                }
            }

            int used = user + nice + sys;
            int total = used + idle;

            if (total == 0) return 0;

            return (used * 100.0) / total;

        } catch (Exception e) {
            return -1; // 表示解析錯誤
        }
    }

    /**
     * Get CPU information from /proc/cpuinfo
     * @return CPU information
     */
    public static  String getCpuInfo() {
        StringBuilder cpuInfo = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(
                    new FileReader("/proc/cpuinfo")
            );
            String line;
            while ((line = reader.readLine()) != null) {
                cpuInfo.append(line).append("\n");
            }
            reader.close();
        } catch (IOException e) {
            cpuInfo.append("Error reading CPU info: ").append(e.getMessage());
        }
        return cpuInfo.toString();
    }


}
