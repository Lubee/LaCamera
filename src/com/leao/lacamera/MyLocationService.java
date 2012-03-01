package com.leao.lacamera;

import java.util.Iterator;

import com.leao.lacamera.util.LogUtil;

import android.app.Service;
import android.content.Intent;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class MyLocationService extends Service implements LocationListener, GpsStatus.Listener{

	protected final static long MINTIME = 1000; // ms
	protected final static float MINDISTANCE = 1; // m
	private Handler locationHandler;

	public MyLocationService() {
		if (null == locationHandler) {
			locationHandler = LaCameraActivity.mHandler;
		}

		// new Thread() {
		//
		// @Override
		// public void run() {
		// while (true) {
		// Message m = locationHandler.obtainMessage(
		// LaCameraActivity.LOCATION_STATUS, 0, 0,
		// Math.random()*12 + "");
		// locationHandler.sendMessage(m);
		// }
		// }
		//
		// }.start();
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d("MyLocationService", "Service: onCreate()");
		if (locationHandler == null) {
			// stop service to avoid error when recording a track and being
			// killed
			// manually
			Intent intent = new Intent(this, MyLocationService.class);
			this.stopService(intent);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d("MyLocationService", "Service: onDestroy()");
	}

	@Override
	public IBinder onBind(Intent i) {
		return null;
	}

	// 当定位变化时
	@Override
	public void onLocationChanged(Location location) {
		Message m = locationHandler.obtainMessage(
				LaCameraActivity.UPATE_LOCATION, 0, 0, location);
		locationHandler.sendMessage(m);
	}

	@Override
	public void onProviderDisabled(String provider) {
		// Log.i("Location", provider + " is disabled.");
		if (provider.equals("gps")) {
			// String networkProvider = LocationManager.NETWORK_PROVIDER;
			// LaCameraActivity.locationManager.requestLocationUpdates(
			// networkProvider, MINTIME, MINDISTANCE,
			// LaCameraActivity.networkLocationListener);
			// Log.i("Location", networkProvider + " requestLocationUpdates() "
			// + minTime + " " + minDistance);
		}
	}

	@Override
	public void onProviderEnabled(String provider) {
		// Log.i("Location", provider + " is enabled.");
		if (provider.equals("gps")) {
			LaCameraActivity.locationManager.requestLocationUpdates(provider,
					MINTIME, MINDISTANCE, LaCameraActivity.gpsLocationListener);
		} else {
			// LaCameraActivity.locationManager.requestLocationUpdates(provider,
			// MINTIME, MINDISTANCE,
			// LaCameraActivity.networkLocationListener);
		}
		// Log.i("Location", provider + " requestLocationUpdates() " + minTime +
		// " " + minDistance);
	}

	// 状态变化时修改状态显示表
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		int numSatellites = extras.getInt("satellites", 11);
		if (status == 0) {
			// Log.i("Location", provider + " is OUT OF SERVICE");
			Message m = locationHandler
					.obtainMessage(LaCameraActivity.LOCATION_STATUS, 0, 0,
							numSatellites + "0");
			locationHandler.sendMessage(m);
		} else if (status == 1) {
			// Log.i("Location", provider + " is TEMPORARILY UNAVAILABLE");
			// invoke network's requestLocationUpdates() if not tracking
			Message m = locationHandler
					.obtainMessage(LaCameraActivity.LOCATION_STATUS, 0, 0,
							numSatellites + "1");
			locationHandler.sendMessage(m);
			if (provider.equals("gps")) {
				// String networkProvider = LocationManager.NETWORK_PROVIDER;
				// LaCameraActivity.locationManager.requestLocationUpdates(
				// networkProvider, MINTIME, MINDISTANCE,
				// LaCameraActivity.networkLocationListener);
				// Log.i("Location", networkProvider
				// + " requestLocationUpdates() " + MINTIME + " "
				// + MINDISTANCE);
			}
		} else {
			// Log.i("Location", provider + " is AVAILABLE");
			// gpsLocationListener has higher priority than
			// networkLocationListener
			Message m = locationHandler
					.obtainMessage(LaCameraActivity.LOCATION_STATUS, 0, 0,
							numSatellites + "2");
			locationHandler.sendMessage(m);
			if (provider.equals("gps")) {
				// LaCameraActivity.locationManager
				// .removeUpdates(LaCameraActivity.networkLocationListener);
			}
		}
	}

	private static int oldCount = -1;

	@Override
	public void onGpsStatusChanged(int event) {

		// 获取当前状态
		GpsStatus gpsstatus = LaCameraActivity.locationManager
				.getGpsStatus(null);
		LogUtil.writeFileToSD(gpsstatus.getSatellites().toString());
		switch (event) {
		// 第一次定位时的事件
		case GpsStatus.GPS_EVENT_FIRST_FIX:
			break;
		// 开始定位的事件
		case GpsStatus.GPS_EVENT_STARTED:
			break;
		// 发送GPS卫星状态事件
		case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
			Iterable<GpsSatellite> allSatellites = gpsstatus.getSatellites();
			Iterator<GpsSatellite> it = allSatellites.iterator();
			int count = 0;
			while (it.hasNext()) {
				it.next();
				count++;
			}
			if (count == oldCount || count < 0) {
				break;
			}
			oldCount = count;
			Message m = locationHandler.obtainMessage(
					LaCameraActivity.GPS_STATUS, 0, 0, count + "");
			locationHandler.sendMessage(m);
			break;
		// 停止定位事件
		case GpsStatus.GPS_EVENT_STOPPED:
			Log.d("Location", "GPS_EVENT_STOPPED");
			break;
		default:
		}

	}

}