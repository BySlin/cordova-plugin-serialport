package com.byslin.serialport;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import tp.xmaihh.serialport.SerialHelper;
import tp.xmaihh.serialport.bean.ComBean;

public class SerialPortModel extends SerialHelper {

    private CallbackContext watcher;

    public SerialPortModel(String sPort, int iBaudRate) {
        super(sPort, iBaudRate);
    }

    @Override
    protected void onDataReceived(ComBean paramComBean) {
        if (watcher != null) {
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, paramComBean.bRec);
            pluginResult.setKeepCallback(true);
            watcher.sendPluginResult(pluginResult);
        }
    }


    public void setWatcher(CallbackContext watcher) {
        this.watcher = watcher;
    }
}
