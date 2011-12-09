package com.leao.lacamera.util;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

public class Util {
  /**
   * 获取sd卡的大小和剩余大小
   * @return
   */
  public static String showSdCardInfo()
  {
    String text = null;
    if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
    {
      File path = Environment.getExternalStorageDirectory();

      StatFs statFs = new StatFs(path.getPath());
      /* Blocksize */
      long blockSize = statFs.getBlockSize();
      long totalBlocks = statFs.getBlockCount();
      long availableBlocks = statFs.getAvailableBlocks();

      String[] total = fileSize(totalBlocks * blockSize);
      String[] available = fileSize(availableBlocks * blockSize);

      text = " T:" + total[0] + total[1];
      text += " A:" + available[0] + available[1];

    } else if (Environment.getExternalStorageState().equals(
        Environment.MEDIA_REMOVED))
    {
       text = "SD卡不存在";
    }
    return text;
  }

  /* 格式化大小 */
  public static String[] fileSize(long size)
  {
    String str = "";
    if (size >= 1024)
    {
      str = "K";
      size /= 1024;
      if (size >= 1024)
      {
        str = "M";
        size /= 1024;
      }
    }else{
      str ="B";
    }

    DecimalFormat formatter = new DecimalFormat("0");

    formatter.setGroupingSize(3);
    String result[] = new String[2];
    result[0] = formatter.format(size);
    result[1] = str;

    return result;
  }

}
