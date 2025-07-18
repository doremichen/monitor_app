/**
 * ===========================================================================
 * Copyright Adam Sample code
 * All Rights Reserved
 * ===========================================================================
 * 
 * File Name: MonitorData.java
 * Brief: This class is the data class of monitor system application
 * 
 * Author: AdamChen
 * Create Date: 2018/9/7
 */
package com.adam.app.monitorapp;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

/**
 * <h1>MonitorData</h1>
 * 
 * @autor AdamChen
 * @since 2018/9/7
 */
public class MonitorData implements Parcelable {

    private String mMemToatal;
    private String mMemFree;
    private String mBuffers;
    private String mCached;
    private String mActive;
    private String mInactive;
    private String mDirty;
    private String mVmallocTotal;
    private String mVmallocUsed;
    private String mVmallocChunk;

    private String mCpuWork;

    // for aidl
    public static Parcelable.Creator<MonitorData> CREATOR = new Parcelable.Creator<MonitorData>() {

        @Override
        public MonitorData createFromParcel(Parcel source) {
            return new MonitorData(source);
        }

        @Override
        public MonitorData[] newArray(int size) {
            return new MonitorData[size];
        }

    };

    public MonitorData() {
    }

    private MonitorData(Parcel source) {
        readFromParcel(source);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mMemToatal);
        dest.writeString(this.mMemFree);
        dest.writeString(this.mBuffers);
        dest.writeString(this.mCached);
        dest.writeString(this.mActive);
        dest.writeString(this.mInactive);
        dest.writeString(this.mDirty);
        dest.writeString(this.mVmallocTotal);
        dest.writeString(this.mVmallocUsed);
        dest.writeString(this.mVmallocChunk);
        dest.writeString(this.mCpuWork);
    }

    private void readFromParcel(Parcel source) {
        this.mMemToatal = source.readString();
        this.mMemFree = source.readString();
        this.mBuffers = source.readString();
        this.mCached = source.readString();
        this.mActive = source.readString();
        this.mInactive = source.readString();
        this.mDirty = source.readString();
        this.mVmallocTotal = source.readString();
        this.mVmallocUsed = source.readString();
        this.mVmallocChunk = source.readString();
        this.mCpuWork = source.readString();
    }

    // public interface
    public void setMemTotal(String str) {
        this.mMemToatal = str;
    }

    public String getMemTotal() {
        return this.mMemToatal;
    }

    public void setMemfree(String str) {
        this.mMemFree = str;
    }

    public String getMemfree() {
        return this.mMemFree;
    }

    public void setBuffers(String str) {
        this.mBuffers = str;
    }

    public String getBuffers() {
        return this.mBuffers;
    }

    public String getCached() {
        return mCached;
    }

    public void setCached(String cached) {
        mCached = cached;
    }

    public String getActive() {
        return mActive;
    }

    public void setActive(String active) {
        mActive = active;
    }

    public String getInactive() {
        return mInactive;
    }

    public void setInactive(String inactive) {
        mInactive = inactive;
    }

    public String getDirty() {
        return mDirty;
    }

    public void setDirty(String dirty) {
        mDirty = dirty;
    }

    public String getVmallocTotal() {
        return mVmallocTotal;
    }

    public void setVmallocTotal(String vmallocTotal) {
        mVmallocTotal = vmallocTotal;
    }

    public String getVmallocUsed() {
        return mVmallocUsed;
    }

    public void setVmallocUsed(String vmallocUsed) {
        mVmallocUsed = vmallocUsed;
    }

    public String getVmallocChunk() {
        return mVmallocChunk;
    }

    public void setVmallocChunk(String vmallocChunk) {
        mVmallocChunk = vmallocChunk;
    }

    public String getCpuWork() {
        return mCpuWork;
    }

    public void setCpuWork(String cpuWork) {
        mCpuWork = cpuWork;
    }


    /**
     * toString
     */
    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MemTotal: ").append(this.mMemToatal).append("\n");
        sb.append("MemFree: ").append(this.mMemFree).append("\n");
        sb.append("Buffers: ").append(this.mBuffers).append("\n");
        sb.append("Cached: ").append(this.mCached).append("\n");
        sb.append("Active: ").append(this.mActive).append("\n");
        sb.append("Inactive: ").append(this.mInactive).append("\n");
        sb.append("VmallocTotal: ").append(this.mVmallocTotal).append("\n");
        sb.append("VmallocUsed: ").append(this.mVmallocUsed).append("\n");
        sb.append("VmallocChunk: ").append(this.mVmallocChunk).append("\n");
        sb.append("CpuWork: ").append(this.mCpuWork).append("\n");

        return sb.toString();
    }
}
