/**
 * ===========================================================================
 * Copyright Adam Sample code
 * All Rights Reserved
 * ===========================================================================
 * 
 * File Name: MainActivity.java
 * Brief: 
 * 
 * Author: AdamChen
 * Create Date: 2018/9/5
 */

package com.adam.app.monitorapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final String KEY_MONITOR_DATA = "Monitor_Data";

    // Service proxy
    private IMonitorService mSvrProxy;

    // UI view
    private TextView mMemNumView;
    private TextView mMemFreeNumView;
    private TextView mMemBuffers;
    private TextView mMemCached;
    private TextView mMemActive;
    private TextView mMemInactive;
    private TextView mMemDirty;
    private TextView mMemVmallocTotal;
    private TextView mMemVmallocUsed;
    private TextView mMemVmallocChunk;
    private TextView mCpuWork;

    private Button mStartRecord;
    private Button mStopRecord;
    private Button mSettings;

    private Dialog mDialog;

    /**
     * Receive action from service
     */
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Utils.info(this, "onReceive...");
            String action = intent.getAction();

            if (MonitorService.ACTION_SHOW_INFO.equals(action)) {
                // get data
                Bundle data = intent.getExtras();
                // get information
                String info = data.getString(MonitorService.KEY_INFO);
                // show dialog
                Utils.showAlertDialog(MainActivity.this, info, null);
            }
        }
    };


    // UI Hanlder
    private Handler mUiH = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            Bundle bundle = msg.getData();
            MonitorData data = bundle.getParcelable(KEY_MONITOR_DATA);

            // show mem total
            mMemNumView.setText(data.getMemTotal() + " ");
            mMemFreeNumView.setText(data.getMemfree() + " ");
            mMemBuffers.setText(data.getBuffers() + " ");
            mMemCached.setText(data.getCached() + " ");
            mMemActive.setText(data.getActive() + " ");
            mMemInactive.setText(data.getInactive() + " ");
            mMemDirty.setText(data.getDirty() + " ");
            mMemVmallocTotal.setText(data.getVmallocTotal() + " ");
            mMemVmallocUsed.setText(data.getVmallocUsed() + " ");
            mMemVmallocChunk.setText(data.getVmallocChunk() + " ");
            mCpuWork.setText(data.getCpuWork() + " ");
        }

    };

    // Service callBack service
    private IMonitorClient mCallBack = new IMonitorClient.Stub() {

        @Override
        public void update(MonitorData data) throws RemoteException {

            if (data != null) {
                Message message = Message.obtain(mUiH);
                Bundle bundle = new Bundle();
                bundle.putParcelable(KEY_MONITOR_DATA, data);
                message.setData(bundle);

                // send message to ui hanlder
                mUiH.sendMessage(message);

            }

        }
    };

    private ServiceConnection mConn = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            mSvrProxy = IMonitorService.Stub.asInterface(service);

            // rehister notufy
            try {
                mSvrProxy.registerCB(mCallBack);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            // Start schedule to update view
            Utils.startMonitorServiceby(MainActivity.this,
                    Utils.ACTION_UPDATE_VIEW);

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

            // unregister notidy
            try {
                mSvrProxy.unregisterCB(mCallBack);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            mSvrProxy = null;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mMemNumView = (TextView) this.findViewById(R.id.MemTotalNum);
        mMemFreeNumView = (TextView) this.findViewById(R.id.MemFreeNum);
        mMemBuffers = (TextView) this.findViewById(R.id.MemBufferNum);
        mMemCached = (TextView) this.findViewById(R.id.MemCachedNum);
        mMemActive = (TextView) this.findViewById(R.id.MemActiveNum);
        mMemInactive = (TextView) this.findViewById(R.id.MemInactiveNum);
        mMemDirty = (TextView) this.findViewById(R.id.MemDirtyNum);
        mMemVmallocTotal = (TextView) this
                .findViewById(R.id.MemVmallocTotalNum);
        mMemVmallocUsed = (TextView) this.findViewById(R.id.MemVmallocUsedNum);
        mMemVmallocChunk = (TextView) this
                .findViewById(R.id.MemVmallocChunkNum);
        mCpuWork = (TextView) this.findViewById(R.id.CpuWorkNum);

        mStartRecord = (Button) this.findViewById(R.id.btn_record);
        mStopRecord = (Button) this.findViewById(R.id.btn_stop);
        mSettings = (Button) this.findViewById(R.id.btn_start_gc);

        mDialog = this.buildDialog();

        BtnState.registerBtn(mStartRecord, mStopRecord, mSettings);

        // config button function
        BtnState.INSTANCE.configBtnFuntion(false);

        // register broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(MonitorService.ACTION_SHOW_INFO);
        this.registerReceiver(mReceiver, filter);


        // Start service
        Intent intent = new Intent(this, MonitorService.class);
        this.bindService(intent, mConn, Context.BIND_AUTO_CREATE);



    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // unregister notidy
        try {
            mSvrProxy.unregisterCB(mCallBack);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        // Stop service
        this.unbindService(mConn);
        Utils.stopService(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Button handler
     */
    public void onStartRecord(View v) {
        showInfo("start recording...");
        // config button function
        BtnState.INSTANCE.configBtnFuntion(true);

        Utils.startMonitorServiceby(this, Utils.ACTION_START_RECORD);

    }

    public void onStopRecord(View v) {
        showInfo("stop recording...");
        // config button function
        BtnState.INSTANCE.configBtnFuntion(false);

        Utils.startMonitorServiceby(this, Utils.ACTION_STOP_RECORD);
    }

    public void onGC(View v) {

        showInfo("Start gc...");

        // start gc
        Runtime.getRuntime().gc();
    }

    public void onExit(View v) {

        // finish ui
        this.finish();
    }

    public void onAbout(View v) {

        mDialog.show();
    }

    /**
     * Show info
     */
    public void showInfo(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    public Dialog buildDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_title);
        builder.setMessage(R.string.dialog_message);

        return builder.create();
    }

    private enum BtnState {

        INSTANCE;

        private static Button mStartBtn;
        private static Button mStopBtn;
        private static Button mSettingBtn;

        public static void registerBtn(Button start, Button stop, Button setting) {
            mStartBtn = start;
            mStopBtn = stop;
            mSettingBtn = setting;
        }

        void configBtnFuntion(boolean enable) {
            mStartBtn.setEnabled(!enable);
            mStopBtn.setEnabled(enable);
            mSettingBtn.setEnabled(!enable);
        }
    }

}
