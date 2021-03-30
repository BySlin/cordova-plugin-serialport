package com.byslin.serialport;

import java.io.IOException;
import java.io.InputStream;

import tp.xmaihh.serialport.stick.AbsStickPackageHelper;

public class InterByteTimeoutStickPackageHelper implements AbsStickPackageHelper {

  /**
   * 发出数据的静默时间（以毫秒为单位）
   */
  private int interval;
  /**
   * 将发出数据的最大字节数。默认值为 65536
   */
  private int maxBufferSize = 65536;

  private InterByteTimeoutStickPackageHelper() {
  }

  public InterByteTimeoutStickPackageHelper(int interval) {
    this.interval = interval;
  }

  public InterByteTimeoutStickPackageHelper(int interval, int maxBufferSize) {
    this.interval = interval;
    this.maxBufferSize = maxBufferSize;
  }

  @Override
  public byte[] execute(InputStream is) {
    //方法开始时间
    long startTime = System.currentTimeMillis();
    int len = 0;
    byte[] tempResult = new byte[maxBufferSize];
    try {
      while (true) {
        int available = is.available();
        if (available > 0) {
          byte[] temp = new byte[available];
          int read = is.read(temp);
          if (read > 0) {
            if ((len + read) < maxBufferSize) {
              System.arraycopy(temp, 0, tempResult, len, read);
              len += read;
            } else {
              //超出maxBufferSize直接停止
              System.arraycopy(temp, 0, tempResult, len, maxBufferSize - len);
              len = maxBufferSize;
              break;
            }
          }
        }
        //判断是否到时间了
        boolean isBreak = (System.currentTimeMillis() - startTime) > interval;
        if (isBreak) {
          break;
        }
      }
      if (len == 0) {
        return null;
      }
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
    //当len与maxBufferSize不等的，重新copy一个和len等长的数组
    if (len != maxBufferSize) {
      byte[] result = new byte[len];
      System.arraycopy(tempResult, 0, result, 0, len);
      return result;
    }
    return tempResult;
  }
}
