package com.adam.app.monitorapp;

import com.adam.app.monitorapp.MonitorData;

interface IMonitorClient {

    void update(in MonitorData data);


}