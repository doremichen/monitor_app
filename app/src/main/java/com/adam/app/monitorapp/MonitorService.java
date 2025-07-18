/**
 * ===========================================================================
 * Copyright Adam Sample code
 * All Rights Reserved
 * ===========================================================================
 * 
 * File Name: MonitorService.java
 * Brief: This class is the service of monitor app.
 * 
 * Author: AdamChen
 * Create Date: 2018/9/5
 */
package com.adam.app.monitorapp;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * <h1>MonitorService</h1>
 * 
 * @autor AdamChen
 * @since 2018/9/5
 */
public class MonitorService extends Service {

    private final RemoteCallbackList<IMonitorClient> mCBLists = new RemoteCallbackList<>();

    private ScheduledExecutorService mExecutorService;
    private ScheduledFuture<?> mFuture;

    // broadcast show info action
    static final String ACTION_SHOW_INFO = "show information";
    static final String KEY_INFO = "key.info";

    @Override
    public IBinder onBind(Intent intent) {
        return mNativeBinder.asBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Create thread pool
        mExecutorService = Executors.newSingleThreadScheduledExecutor();

    }

    private void scheduleReaderTask() {

        if (mExecutorService == null) {
            throw new RuntimeException("No thread pool");
        }

        if ((mFuture != null) && (!mFuture.isCancelled())) {
            mFuture.cancel(true);
        }

        mReadTask = new ReaderTask(this);
        mFuture = mExecutorService.scheduleWithFixedDelay(mReadTask, 1L, 1L,
                TimeUnit.SECONDS);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String action = intent.getAction();

        if (Utils.ACTION_UPDATE_VIEW.equals(action)) {
            scheduleReaderTask();
        } else if (Utils.ACTION_START_RECORD.equals(action)) {

            if (mReadTask == null) {
                throw new RuntimeException("No reader task");
            }
            mReadTask.enableRecord(true);

        } else if (Utils.ACTION_STOP_RECORD.equals(action)) {

            if (mReadTask == null) {
                throw new RuntimeException("No reader task");
            }
            mReadTask.enableRecord(false);

        }

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        Utils.info(this, "onDestroy");
        super.onDestroy();

        if (mReadTask != null) {
            mReadTask.stopRecord();
        }

        cancelAndFinishThreadPool();

    }

    private void cancelAndFinishThreadPool() {
        Utils.info(this, "cancelAndFinishThreadPool");
        if ((mFuture != null) && (!mFuture.isCancelled())) {
            Utils.info(this, "cancel task!!!");
            mFuture.cancel(true);
        }

        Utils.info(this, "shutdown thread pool");
        mExecutorService.shutdown();

        try {
            while (!mExecutorService.awaitTermination(3L, TimeUnit.SECONDS)) {
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mReadTask = null;
            mExecutorService = null;
            mFuture = null;
            Utils.info(this, "thread pool finished");
        }
    }

    public void setNotify(IMonitorClient callback) {

        mCBLists.register(callback);
    }

    public void releaseNotify(IMonitorClient callback) {

        mCBLists.unregister(callback);
    }

    public void readData(MonitorData data) {
        Utils.info(this, "readData");
        final int size = mCBLists.beginBroadcast();

        for (int i = 0; i < size; i++) {

            try {
                Utils.info(this, "start to update!!!");
                mCBLists.getBroadcastItem(i).update(data);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }

        mCBLists.finishBroadcast();

    }

    /**
     * Send broadcast action
     * @param str
     */
    void showInfoAction(String str) {
        Utils.info(this, "showInfoAction");
        Bundle data = new Bundle();
        data.putString(KEY_INFO, str);
        // broadcast
        Intent intent = new Intent();
        intent.putExtras(data);
        intent.setAction(ACTION_SHOW_INFO);
        this.sendBroadcast(intent);
    }

    // work Task
    private static class ReaderTask implements Runnable {

        public static final String LABEL_MEMORY_INFO_DATE = "Memory info Date: ";
        public static final String SEPARATOR = " ===========================\n";
        public static final String UNIT_KB = "  kB\n";
        public static final String UNIT_PERCENT = "  %\n";
        public static final String PREFIX_MEMORY_INFO = "MemoryInfo";
        public static final String SUFFIX_RECORD = ".Record";

        private final MonitorService mService;

        private final MonitorData mData;

        // record information
        private OutputStreamWriter mWriter;

        /**
         * Constructor of ReaderTask
         * @param svr MonitorService
         */
        public ReaderTask(MonitorService svr) {
            WeakReference<MonitorService> ref = new WeakReference<MonitorService>(svr);
            this.mService = ref.get();

            mData = new MonitorData();
        }

        private boolean mReadOnly;
        private boolean mWriteOnly;

        private BufferedReader mReader;

        private boolean mNeedRecord;

        @Override
        public void run() {
            Map<String, Consumer<String>> keyActionMap = new HashMap<String, Consumer<String>>() {{
                put(Utils.MEM_TOTAL_ITEM, value -> {
                    if (!mReadOnly) {
                        mReadOnly = true;
                        mData.setMemTotal(value);
                    }
                });
                put(Utils.MEM_FREE_ITEM, mData::setMemfree);
                put(Utils.BUFFERS_ITEM, mData::setBuffers);
                put(Utils.CACHED_ITEM, mData::setCached);
                put(Utils.ACTIVE_ITEM, mData::setActive);
                put(Utils.INACTIVE_ITEM, mData::setInactive);
                put(Utils.DIRTY_ITEM, mData::setDirty);
                put(Utils.VM_ALLOC_TOTAL_ITEM, mData::setVmallocTotal);
                put(Utils.VM_ALLOC_USED_ITEM, mData::setVmallocUsed);
                put(Utils.VM_ALLOC_CHUNK_ITEM, mData::setVmallocChunk);
            }};

            try (BufferedReader reader = new BufferedReader(new FileReader(Utils.PROC_MEM_INFO_PATH))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty()) continue;

                    String[] parts = line.trim().split("\\s+", 3);
                    if (parts.length < 2) continue;

                    String key = parts[0];
                    String value = parts[1];

                    Consumer<String> action = keyActionMap.get(key);
                    if (action != null) {
                        action.accept(value);
                    }
                }

                // Log CPU usage
                String cpuInfo = Utils.readCpuUsageFromTop();
                Utils.info(this, "cpuInfo: " + cpuInfo);

                double cpuWork = Utils.calculateCpuUsage(cpuInfo);
                String cpuAverage = String.format(Locale.getDefault(), "%.2f", cpuWork);
                mData.setCpuWork(cpuAverage);

                // Send data
                mService.readData(mData);
                if (mNeedRecord) {
                    startRecord();
                }

            } catch (FileNotFoundException e) {
                String info = e.getMessage();
                Utils.info(this, info);
                mService.showInfoAction(info);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        /**
         * Start to record data
         */
        private void startRecord() {
            // Create file
            if (this.mWriter == null) {
                this.mWriter = getOutputWriter();
            }

            try {
                long currentTimeMillis = System.currentTimeMillis();
                String currentTimeStr = Calendar.getInstance().getTime().toString();

                StringBuilder sb = new StringBuilder();

                if (!mWriteOnly) {
                    sb.append(LABEL_MEMORY_INFO_DATE)
                            .append(currentTimeStr)
                            .append(SEPARATOR);
                    mWriteOnly = true;
                }

                sb.append(currentTimeMillis)
                        .append(SEPARATOR)
                        .append(Utils.MEM_TOTAL_ITEM).append(" ").append(mData.getMemTotal()).append(UNIT_KB)
                        .append(Utils.MEM_FREE_ITEM).append(" ").append(mData.getMemfree()).append(UNIT_KB)
                        .append(Utils.BUFFERS_ITEM).append(" ").append(mData.getBuffers()).append(UNIT_KB)
                        .append(Utils.CACHED_ITEM).append(" ").append(mData.getCached()).append(UNIT_KB)
                        .append(Utils.ACTIVE_ITEM).append(" ").append(mData.getActive()).append(UNIT_KB)
                        .append(Utils.INACTIVE_ITEM).append(" ").append(mData.getInactive()).append(UNIT_KB)
                        .append(Utils.VM_ALLOC_TOTAL_ITEM).append(" ").append(mData.getVmallocTotal()).append(UNIT_KB)
                        .append(Utils.VM_ALLOC_USED_ITEM).append(" ").append(mData.getVmallocUsed()).append(UNIT_KB)
                        .append(Utils.VM_ALLOC_CHUNK_ITEM).append(" ").append(mData.getVmallocChunk()).append(UNIT_KB)
                        .append(Utils.CPU_WORK_ITEM).append(" ").append(mData.getCpuWork()).append(UNIT_PERCENT);

                mWriter.write(sb.toString());

            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        public void enableRecord(boolean enable) {
            this.mNeedRecord = enable;
        }

        /**
         * Stop to record data
         */
        public void stopRecord() {
            if (!mNeedRecord || mWriter == null) {
                return;
            }

            try {
                mWriter.flush();
            } catch (IOException e) {
                System.err.println("Flush failed in stopRecord(): " + e.getMessage());
                e.printStackTrace();
            }

            try {
                mWriter.close();
            } catch (IOException e) {
                System.err.println("Close failed in stopRecord(): " + e.getMessage());
                e.printStackTrace();
            } finally {
                mWriter = null;
            }
        }


        /**
         * Get output writer
         * @return OutputWriter of file
         */
        private OutputStreamWriter getOutputWriter() {
            OutputStreamWriter writer = null;
            StringBuilder fileNameBuilder = new StringBuilder();
            fileNameBuilder.append(PREFIX_MEMORY_INFO);
            fileNameBuilder.append(Calendar.getInstance().getTime());
            fileNameBuilder.append(SUFFIX_RECORD);

            try {
                writer = new OutputStreamWriter(this.mService.openFileOutput(
                        fileNameBuilder.toString(), 0));
            } catch (FileNotFoundException e) {
                e.printStackTrace();

            }

            return writer;
        }

    }

    // monitor service stub
    private final MonitorServiceStub mNativeBinder = new MonitorServiceStub(this);

    // reader task
    private ReaderTask mReadTask;

    // The stub of the monitor service
    private static class MonitorServiceStub extends IMonitorService.Stub {

        private final MonitorService mService;

        public MonitorServiceStub(MonitorService svr) {
            WeakReference<MonitorService> ref = new WeakReference<MonitorService>(svr);
            this.mService = ref.get();
        }

        @Override
        public void registerCB(IMonitorClient client) throws RemoteException {
            if (client != null) {
                this.mService.setNotify(client);
            }
        }

        @Override
        public void unregisterCB(IMonitorClient client) throws RemoteException {
            if (client != null) {
                this.mService.releaseNotify(client);
            }
        }

    }

}
