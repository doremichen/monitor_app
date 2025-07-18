package com.adam.app.monitorapp;

import com.adam.app.monitorapp.IMonitorClient;


interface IMonitorService {
    // register
    oneway void registerCB(IMonitorClient client);
    // unregister
    oneway void unregisterCB(IMonitorClient client);
}