package com.leao.lacamera.util;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

import com.leao.lacamera.FileListAdapter;

public class FileUtil {

	public static final int switchIcon(File file) {

		String suffix = getSuffix(file);
		suffix = suffix.toLowerCase();
		if (suffix.equals("jpeg") || suffix.equals("jpg")
				|| suffix.equals("bmp") || suffix.equals("gif")
				|| suffix.equals("png")) {
			return FileListAdapter.PHOTO;
		}
		return 0;

	}

	private static String getSuffix(File file) {
		String suffix;
		String name = file.getName();
		int la = name.lastIndexOf('.');
		if (la == -1)
			suffix = null;
		else
			suffix = name.substring(la + 1).toLowerCase();
		return suffix;
	}


	public static String getDesc(File file) {
		StringBuffer str = new StringBuffer("|");
		/**
		 * 文件夹就计算修改时间，如果是文件就计算大小
		 */
		if (file.isFile()) {
			String[] size = Util.fileSize(file.length());
			str.append(size[0]).append(size[1]).append(" |");
		}
		long time = file.lastModified();
		str.append(getLong2Date(time));
		str.append(" |").append(file.canRead() ? 'r' : '-')
				.append(file.canWrite() ? 'w' : '-');
		return str.toString();
	}

	public static String getLong2Date(long time) {
		if (time <= 0) {
			time = System.currentTimeMillis();
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yy/MM/dd HH:mm:ss");
		Date date = new Date(time);
		return sdf.format(date);
	}
	
	public static boolean deleteFile(String filePath){
		File file = new File(filePath);
		if(file.exists()){
			file.delete();
			return true;
		}
		return false;
	}
}