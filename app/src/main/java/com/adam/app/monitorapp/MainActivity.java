package com.adam.app.monitorapp;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.adam.app.monitorapp.IMonitorClient;
import com.adam.app.monitorapp.IMonitorService;

public class MainActivity extends AppCompatActivity {

    private static final String KEY_MONITOR_DATA = "Monitor_Data";

    private IMonitorService mSvrProxy;

    private TextView mMemNumView, mMemFreeNumView, mMemBuffers, mMemCached;
    private TextView mMemActive, mMemInactive, mMemDirty;
    private TextView mMemVmallocTotal, mMemVmallocUsed, mMemVmallocChunk;
    private TextView mCpuWork;

    private Button mStartRecord, mStopRecord, mSettings;

    private AlertDialog mDialog;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (MonitorService.ACTION_SHOW_INFO.equals(intent.getAction())) {
                String info = intent.getStringExtra(MonitorService.KEY_INFO);
                showAlert(info);
            }
        }
    };

    private final Handler mUiH = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            MonitorData data = msg.getData().getParcelable(KEY_MONITOR_DATA);
            if (data != null) {
                updateUI(data);
            }
        }
    };

    private final IMonitorClient mCallBack = new IMonitorClient.Stub() {
        @Override
        public void update(MonitorData data) throws RemoteException {
            // log monitor data
            Utils.info(MainActivity.this, "Receive data:\n" + data.toString());

            if (data != null) {
                Message message = Message.obtain(mUiH);
                Bundle bundle = new Bundle();
                bundle.putParcelable(KEY_MONITOR_DATA, data);
                message.setData(bundle);
                mUiH.sendMessage(message);
            }
        }
    };

    private final ServiceConnection mConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mSvrProxy = IMonitorService.Stub.asInterface(service);
            try {
                mSvrProxy.registerCB(mCallBack);
                Utils.startMonitorServiceby(MainActivity.this, Utils.ACTION_UPDATE_VIEW);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            try {
                if (mSvrProxy != null) {
                    mSvrProxy.unregisterCB(mCallBack);
                }
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

        initViews();
        BtnState.registerBtn(mStartRecord, mStopRecord, mSettings);
        BtnState.INSTANCE.configBtnFuntion(false);

        IntentFilter filter = new IntentFilter(MonitorService.ACTION_SHOW_INFO);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(mReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(mReceiver, filter);
        }

        Intent intent = new Intent(this, MonitorService.class);
        bindService(intent, mConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (mSvrProxy != null) {
                mSvrProxy.unregisterCB(mCallBack);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        unbindService(mConn);
        Utils.stopService(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initViews() {
        mMemNumView = findViewById(R.id.MemTotalNum);
        mMemFreeNumView = findViewById(R.id.MemFreeNum);
        mMemBuffers = findViewById(R.id.MemBufferNum);
        mMemCached = findViewById(R.id.MemCachedNum);
        mMemActive = findViewById(R.id.MemActiveNum);
        mMemInactive = findViewById(R.id.MemInactiveNum);
        mMemDirty = findViewById(R.id.MemDirtyNum);
        mMemVmallocTotal = findViewById(R.id.MemVmallocTotalNum);
        mMemVmallocUsed = findViewById(R.id.MemVmallocUsedNum);
        mMemVmallocChunk = findViewById(R.id.MemVmallocChunkNum);
        mCpuWork = findViewById(R.id.CpuWorkNum);

        mStartRecord = findViewById(R.id.btn_record);
        mStopRecord = findViewById(R.id.btn_stop);
        mSettings = findViewById(R.id.btn_start_gc);

        mDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title)
                .setMessage(R.string.dialog_message)
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }

    private void updateUI(MonitorData data) {
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

    public void onStartRecord(View v) {
        showInfo("Start recording...");
        BtnState.INSTANCE.configBtnFuntion(true);
        Utils.startMonitorServiceby(this, Utils.ACTION_START_RECORD);
    }

    public void onStopRecord(View v) {
        showInfo("Stop recording...");
        BtnState.INSTANCE.configBtnFuntion(false);
        Utils.startMonitorServiceby(this, Utils.ACTION_STOP_RECORD);
    }

    public void onGC(View v) {
        showInfo("Start GC...");
        Runtime.getRuntime().gc();
    }

    public void onExit(View v) {
        finish();
    }

    public void onAbout(View v) {
        mDialog.show();
    }

    public void showInfo(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void showAlert(String msg) {
        new AlertDialog.Builder(this)
                .setTitle("Info")
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private enum BtnState {
        INSTANCE;

        private static Button mStartBtn, mStopBtn, mSettingBtn;

        public static void registerBtn(Button start, Button stop, Button setting) {
            mStartBtn = start;
            mStopBtn = stop;
            mSettingBtn = setting;
        }

        void configBtnFuntion(boolean isRecording) {
            mStartBtn.setEnabled(!isRecording);
            mStopBtn.setEnabled(isRecording);
            mSettingBtn.setEnabled(!isRecording);
        }
    }
}
