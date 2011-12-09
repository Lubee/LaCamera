package com.leao.lacamera.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class SearchGoogleUtil {
	/**
	 * 根据经纬度反向解析地址，有时需要多尝试几次
	 * 注意:(摘自：http://code.google.com/intl/zh-CN/apis/maps/faq.html
	 * 提交的地址解析请求次数是否有限制？) 如果在 24 小时时段内收到来自一个 IP 地址超过 2500 个地址解析请求， 或从一个 IP
	 * 地址提交的地址解析请求速率过快，Google 地图 API 编码器将用 620 状态代码开始响应。 如果地址解析器的使用仍然过多，则从该 IP
	 * 地址对 Google 地图 API 地址解析器的访问可能被永久阻止。
	 * 
	 * @param latitude
	 *            纬度
	 * @param longitude
	 *            经度
	 * @return
	 */
	public static String getAddr(double latitude, double longitude) {
		String addr = "";

		// 也可以是http://maps.google.cn/maps/geo?output=csv&key=abcdef&q=%s,%s，不过解析出来的是英文地址
		// 密钥可以随便写一个key=abc
		// output=csv,也可以是xml或json，不过使用csv返回的数据最简洁方便解析
		String url = String.format(
				"http://ditu.google.cn/maps/geo?output=csv&key=abcdef&q=%s,%s",
				latitude, longitude);
		URL myURL = null;
		HttpURLConnection httpsConn = null;
		try {
			myURL = new URL(url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
		try {
			httpsConn = (HttpURLConnection) myURL.openConnection();
			httpsConn.setRequestMethod("GET");
			httpsConn.setRequestProperty("Accept-Language", "zh-CN");
			httpsConn.setReadTimeout(10000);
			httpsConn.setConnectTimeout(10000);
			httpsConn.connect();
			int responseCode = httpsConn.getResponseCode();
			if (responseCode != HttpURLConnection.HTTP_OK) {
				return "网络设置问题";
			} 
			if (httpsConn != null) {
				InputStreamReader insr = new InputStreamReader(
						httpsConn.getInputStream(), "UTF-8");
				BufferedReader br = new BufferedReader(insr);
				String data = null;
				if ((data = br.readLine()) != null) {
					String[] retList = data.split(",");
					if (retList.length > 2 && ("200".equals(retList[0]))) {
						addr = retList[2];
						addr = addr.replace("\"", "");
					} else {
						addr = "";
					}
				}
				insr.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
			return "超时";
		}finally{
			httpsConn.disconnect();
		}
		return addr;
	}
}
