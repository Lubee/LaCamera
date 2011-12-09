package com.leao.lacamera.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtil {
	private static  SimpleDateFormat imageSdf = new SimpleDateFormat("yyyyMMddHHmmss");  
	private static  SimpleDateFormat waterSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  
	
	
	public static String getImageDate(){
		return imageSdf.format(new Date());
	}
	
	public static String getWaterDate(){
		return waterSdf.format(new Date());
	}
}
