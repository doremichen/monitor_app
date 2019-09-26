/**
 * ===========================================================================
 * Copyright Adam Sample code
 * All Rights Reserved
 * ===========================================================================
 * 
 * File Name: MonitorService.java
 * Brief: 
 * 
 * Author: AdamChen
 * Create Date: 2018/9/5
 */

package com.adam.app.monitorapp;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * <h1>MonitorService</h1>
 * 
 * @autor AdamChen
 * @since 2018/9/5
 */
public class MonitorService extends Service {

    private RemoteCallbackList<IMonitorClient> mCBLists = new RemoteCallbackList<IMonitorClient>();

    private ScheduledExecutorService mExecutorService;
    private ScheduledFuture<?> mFuture;

    // broadcast show info action
    static final String ACTION_SHOW_INFO = "show information";
    static final String KEY_INFO = "key.info";

    @Override
    public IBinder onBind(Intent intent) {
        return mNatievBinder.asBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Create thread pool
        mExecutorService = Executors.newSingleThreadScheduledExecutor();

    }

    private void scheduleReaderTask() {

        if ((mFuture != null) && (mFuture.isCancelled() == false)) {
            mFuture.cancel(true);
        }

        mReadTask = new ReaderTask(this);
        mFuture = mExecutorService.scheduleAtFixedRate(mReadTask, 1L, 1L,
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
        super.onDestroy();

        if (mReadTask != null) {
            mReadTask.stopRecord();
        }

        cancelAndFinishThreadPool();

    }

    private void cancelAndFinishThreadPool() {
        if ((mFuture != null) && (mFuture.isCancelled() == false)) {
            mFuture.cancel(true);
        }

        mExecutorService.shutdown();

        try {
            while (!mExecutorService.awaitTermination(3L, TimeUnit.SECONDS)) {
                ;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mReadTask = null;
            mExecutorService = null;
        }
    }

    public void setNotify(IMonitorClient callback) {

        mCBLists.register(callback);
    }

    public void releaseNotify(IMonitorClient callback) {

        mCBLists.unregister(callback);
    }

    public void readData(MonitorData data) {

        final int size = mCBLists.beginBroadcast();

        for (int i = 0; i < size; i++) {

            try {
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

        private MonitorService mService;

        private MonitorData mData;

        // record information
        private OutputStreamWriter mWriter;

        public ReaderTask(MonitorService svr) {
            this.mService = svr;

            mData = new MonitorData();

        }

        private boolean ReadOnlyOne;
        private boolean WriteOnlyOne;

        private BufferedReader mReader;

        private boolean mNeedRecord;

        private long mWorkBefoure;
        private long mTotalBefoure;

        @Override
        public void run() {

            try {
                mReader = new BufferedReader(new FileReader(
                        Utils.PROC_MEMINFO_PATH));
                String meminfo = mReader.readLine();

                while (meminfo != null) {
                    // read mem total
                    if (!ReadOnlyOne
                            && meminfo.startsWith(Utils.MEM_TOTAL_ITEM)) {
                        ReadOnlyOne = true;
                        String memtotal = meminfo.split("[ ]+", 3)[1];
                        this.mData.setMemTotal(memtotal);
                    }

                    if (meminfo.startsWith(Utils.MEM_FREE_ITEM)) {
                        String memfree = meminfo.split("[ ]+", 3)[1];
                        this.mData.setMemfree(memfree);
                    }

                    if (meminfo.startsWith(Utils.BUFFERS_ITEM)) {
                        String buffers = meminfo.split("[ ]+", 3)[1];
                        this.mData.setBuffers(buffers);
                    }

                    if (meminfo.startsWith(Utils.CACHED_ITEM)) {
                        String buffers = meminfo.split("[ ]+", 3)[1];
                        this.mData.setCached(buffers);
                    }

                    if (meminfo.startsWith(Utils.ACTIVE_ITEM)) {
                        String buffers = meminfo.split("[ ]+", 3)[1];
                        this.mData.setActive(buffers);
                    }

                    if (meminfo.startsWith(Utils.INACTIVE_ITEM)) {
                        String buffers = meminfo.split("[ ]+", 3)[1];
                        this.mData.setInactive(buffers);
                    }

                    if (meminfo.startsWith(Utils.DIRTY_ITEM)) {
                        String buffers = meminfo.split("[ ]+", 3)[1];
                        this.mData.setDirty(buffers);
                    }

                    if (meminfo.startsWith(Utils.VMALLOCTOTAL_ITEM)) {
                        String buffers = meminfo.split("[ ]+", 3)[1];
                        this.mData.setVmallocTotal(buffers);
                    }

                    if (meminfo.startsWith(Utils.VMALLOCUSED_ITEM)) {
                        String buffers = meminfo.split("[ ]+", 3)[1];
                        this.mData.setVmallocUsed(buffers);
                    }

                    if (meminfo.startsWith(Utils.VMALLOCCHUNK_ITEM)) {
                        String buffers = meminfo.split("[ ]+", 3)[1];
                        this.mData.setVmallocChunk(buffers);
                    }

                    meminfo = mReader.readLine();
                }

                mReader = new BufferedReader(new FileReader(
                        Utils.PROC_STAT_PATH));
                String[] cpuState = mReader.readLine().split("[ ]+", 9);
                long work = Long.parseLong(cpuState[1])
                        + Long.parseLong(cpuState[2])
                        + Long.parseLong(cpuState[3]);
                long total = work + Long.parseLong(cpuState[4])
                        + Long.parseLong(cpuState[5])
                        + Long.parseLong(cpuState[6])
                        + Long.parseLong(cpuState[7]);

                if (this.mTotalBefoure != 0) {
                    long workTemp = work - this.mWorkBefoure;
                    long totalTemp = total - this.mTotalBefoure;
                    float cpuWork = (workTemp * 100.0f) / totalTemp;
                    String cpuAverage = String.format("%.2f", cpuWork);

                    this.mData.setCpuWork(cpuAverage);
                } else {
                    this.mData.setCpuWork("0.00");
                }

                this.mWorkBefoure = work;
                this.mTotalBefoure = total;

                this.mService.readData(this.mData);
                if (mNeedRecord) {
                    startRecord();
                }
            } catch (FileNotFoundException e) {
                String info = e.getMessage();
                Utils.info(this, info);
                // broadcast
                this.mService.showInfoAction(info);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                closeIO(mReader);
            }

        }

        private void closeIO(Closeable ioObject) {

            try {
                ioObject.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void startRecord() {
            if (this.mWriter == null) {
                this.mWriter = getOutputWriter();
            }

            try {

                if (!WriteOnlyOne) {

                    try {
                        mWriter.write("Memory info Date: "
                                + Calendar.getInstance().getTime()
                                + "===========================\n");
                        WriteOnlyOne = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                mWriter.write(Calendar.getInstance().getTimeInMillis()
                        + "===========================\n");
                mWriter.write("MemTotal: " + mData.getMemTotal() + "  kB\n");
                mWriter.write("MemFree: " + mData.getMemfree() + "  kB\n");
                mWriter.write("Buffers: " + mData.getBuffers() + "  kB\n");
                mWriter.write("Cached: " + mData.getCached() + "  kB\n");
                mWriter.write("Active: " + mData.getActive() + "  kB\n");
                mWriter.write("Inactive: " + mData.getInactive() + "  kB\n");
                mWriter.write("VmallocTotal: " + mData.getVmallocTotal()
                        + "  kB\n");
                mWriter.write("VmallocUsed: " + mData.getVmallocUsed()
                        + "  kB\n");
                mWriter.write("VmallocChunk: " + mData.getVmallocChunk()
                        + "  kB\n");
                mWriter.write("CpuWork: " + mData.getCpuWork() + "  %\n");
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        public void enableRecord(boolean enable) {
            this.mNeedRecord = enable;
        }

        public void stopRecord() {

            if (!mNeedRecord) {
                return;
            }

            try {
                mWriter.flush();
                mWriter.close();
                mWriter = null;
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        private OutputStreamWriter getOutputWriter() {
            OutputStreamWriter writer = null;
            StringBuilder fileNameBuilder = new StringBuilder();
            fileNameBuilder.append("MemoryInfo");
            fileNameBuilder.append(Calendar.getInstance().getTime());
            fileNameBuilder.append(".Record");

            try {
                writer = new OutputStreamWriter(this.mService.openFileOutput(
                        fileNameBuilder.toString(), 0));
            } catch (FileNotFoundException e) {
                e.printStackTrace();

            }

            return writer;
        }

    }

    private MonitorServiceStub mNatievBinder = new MonitorServiceStub(this);

    private ReaderTask mReadTask;

    // The stub of the monitor service
    private static class MonitorServiceStub extends IMonitorService.Stub {

        private MonitorService mService;

        public MonitorServiceStub(MonitorService svr) {
            this.mService = svr;
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
