package com.leao.lacamera;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class MyLocationService extends Service implements LocationListener {

	protected final static long MINTIME = 1000; // ms
	protected final static float MINDISTANCE = 1; // m
	private Handler locationHandler;

	public MyLocationService() {
		if (null == locationHandler) {
			locationHandler = LaCameraActivity.mHandler;
		}

//		new Thread() {
//
//			@Override
//			public void run() {
//				while (true) {
//					Message m = locationHandler.obtainMessage(
//							LaCameraActivity.LOCATION_STATUS, 0, 0,
//							Math.random()*12 + "");
//					locationHandler.sendMessage(m);
//				}
//			}
//
//		}.start();
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
//			String networkProvider = LocationManager.NETWORK_PROVIDER;
//			LaCameraActivity.locationManager.requestLocationUpdates(
//					networkProvider, MINTIME, MINDISTANCE,
//					LaCameraActivity.networkLocationListener);
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
//			LaCameraActivity.locationManager.requestLocationUpdates(provider,
//					MINTIME, MINDISTANCE,
//					LaCameraActivity.networkLocationListener);
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
			Message m = locationHandler.obtainMessage(
					LaCameraActivity.LOCATION_STATUS, 0, 0, numSatellites + "0");
			locationHandler.sendMessage(m);
		} else if (status == 1) {
			// Log.i("Location", provider + " is TEMPORARILY UNAVAILABLE");
			// invoke network's requestLocationUpdates() if not tracking
			Message m = locationHandler.obtainMessage(
					LaCameraActivity.LOCATION_STATUS, 0, 0, numSatellites + "1");
			locationHandler.sendMessage(m);
			if (provider.equals("gps")) {
//				String networkProvider = LocationManager.NETWORK_PROVIDER;
//				LaCameraActivity.locationManager.requestLocationUpdates(
//						networkProvider, MINTIME, MINDISTANCE,
//						LaCameraActivity.networkLocationListener);
//				Log.i("Location", networkProvider
//						+ " requestLocationUpdates() " + MINTIME + " "
//						+ MINDISTANCE);
			}
		} else {
			// Log.i("Location", provider + " is AVAILABLE");
			// gpsLocationListener has higher priority than
			// networkLocationListener
			Message m = locationHandler.obtainMessage(
					LaCameraActivity.LOCATION_STATUS, 0, 0, numSatellites + "2");
			locationHandler.sendMessage(m);
			if (provider.equals("gps")) {
//				LaCameraActivity.locationManager
//						.removeUpdates(LaCameraActivity.networkLocationListener);
			}
		}
	}

}