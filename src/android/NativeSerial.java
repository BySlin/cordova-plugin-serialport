package com.byslin.serialport;

import android.util.Base64;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import android_serialport_api.SerialPortFinder;
import tp.xmaihh.serialport.stick.AbsStickPackageHelper;

/**
 * This class echoes a string called from JavaScript.
 */
public class NativeSerial extends CordovaPlugin {
  private static final String LOG_TAG = "NativeSerial";

  private final Map<String, SerialPortModel> portMap = new ConcurrentHashMap<>();
  private final SerialPortFinder mSerialPortFinder = new SerialPortFinder();
  private final String[] allDevicesPath = mSerialPortFinder.getAllDevicesPath();
  private final JSONArray resArr = new JSONArray();

  {
    for (int i = 0; i < allDevicesPath.length; i++) {
      try {
        resArr.put(i, allDevicesPath[i]);
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public void onReset() {
    super.onReset();
    Set<String> keySet = portMap.keySet();
    for (String device : keySet) {
      closePort(device);
    }
  }

  @Override
  public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
    if (action.equals("list")) {
      callbackContext.success(resArr);
      return true;
    } else if (action.equals("open")) {
      Log.d(LOG_TAG, "execute open");
      final String device = args.getString(0);
      final JSONObject options = args.getJSONObject(1);

      final int baudRate = options.getInt("baudRate");
      //设置粘包超时时间
      //reference https://serialport.io/docs/api-parser-inter-byte-timeout
      final int interval = options.getInt("interval");
      final int maxBufferSize = options.getInt("maxBufferSize");


      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
          if (interval > 0) {
            final InterByteTimeoutStickPackageHelper interByteTimeoutStickPackageHelper = new InterByteTimeoutStickPackageHelper(interval, maxBufferSize);
            NativeSerial.this.openPort(device, baudRate, interByteTimeoutStickPackageHelper, callbackContext);
          } else {
            NativeSerial.this.openPort(device, baudRate, null, callbackContext);
          }
        }
      });
      return true;
    } else if (action.equals("close")) {
      final String device = args.getString(0);
      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
          NativeSerial.this.closePort(device, callbackContext);
        }
      });
      return true;
    } else if (action.equals("write")) {
      final String device = args.getString(0);
      final String data = args.getString(1);
      Log.d(LOG_TAG, "execute write:" + data);
      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
          byte[] decode = Base64.decode(data, Base64.NO_WRAP);
          Log.d(LOG_TAG, "write bytes:" + decode);
          NativeSerial.this.writeBytes(device, decode, callbackContext);
        }
      });
      return true;
    } else if (action.equals("writeText")) {
      final String device = args.getString(0);
      final String data = args.getString(1);
      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
          NativeSerial.this.writeText(device, data, callbackContext);
        }
      });
      return true;
    } else if (action.equals("register")) {
      final String device = args.getString(0);

      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
          SerialPortModel serialPortModel = portMap.get(device);
          if (serialPortModel != null) {
            serialPortModel.setWatcher(callbackContext);
          }
        }
      });
      return true;
    }
    Log.d(LOG_TAG, "unknown action:" + action);
    return false;
  }

  private void openPort(String device, int baudRate, AbsStickPackageHelper stickPackageHelper, final CallbackContext callbackContext) {
    if (Arrays.binarySearch(allDevicesPath, device) < 0) {
      callbackContext.error("not serial port");
      return;
    }

    try {
      if (!portMap.containsKey(device)) {
        SerialPortModel serialPortModel = new SerialPortModel(device, baudRate);
        if (stickPackageHelper != null) {
          serialPortModel.setStickPackageHelper(stickPackageHelper);
        }
        serialPortModel.open();
        portMap.put(device, serialPortModel);
      }
      callbackContext.success();
      Log.d(LOG_TAG, "open " + device + " success");
    } catch (Exception e) {
      String message = e.getMessage();
      callbackContext.error(message);
      Log.d(LOG_TAG, "open " + device + " error");
      Log.e(LOG_TAG, message);
    }
  }

  private void closePort(String device, final CallbackContext callbackContext) {
    closePort(device);
    callbackContext.success();
  }

  private void closePort(String device) {
    SerialPortModel serialPortModel = portMap.get(device);
    if (serialPortModel != null) {
      serialPortModel.close();
      portMap.remove(device);
      Log.d(LOG_TAG, "close " + device + " success");
    }
  }

  private void writeBytes(String device, final byte[] bytes, final CallbackContext callbackContext) {
    SerialPortModel serialPortModel = portMap.get(device);
    if (serialPortModel != null) {
      serialPortModel.send(bytes);
      callbackContext.success();
    }
  }

  private void writeText(String device, final String data, final CallbackContext callbackContext) {
    writeBytes(device, data.getBytes(), callbackContext);
  }
}
