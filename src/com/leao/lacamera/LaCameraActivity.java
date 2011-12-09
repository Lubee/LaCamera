package com.leao.lacamera;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.leao.lacamera.FileListAdapter.FileInfo;
import com.leao.lacamera.util.DateUtil;
import com.leao.lacamera.util.FileUtil;
import com.leao.lacamera.util.SearchGoogleUtil;

public class LaCameraActivity extends Activity implements OnItemClickListener ,OnClickListener {

	public static final String TAG = "LaCamera";

	private ImageView mImageView;
	FileListAdapter fileAdapterList;
	FileData currentData;
	ListView itemlist = null;
	ArrayList<FileInfo> fInfos = new ArrayList<FileListAdapter.FileInfo>();

	public static final int PHOTOHRAPH = 1;
	// private static final String TEMP_FILE_NAME = "temp.jpg";
	// private static final String IMAGE_TEMP_DIR =
	// Environment.getExternalStorageDirectory()+"/LaCamera/temp/";
	public static final String IMAGE_DIR = Environment
			.getExternalStorageDirectory() + "/LaCamera/image/";

	protected static final int UPATE_LOCATION = 1001;
	protected static final int REFRESH = 1002;

	public static double douLatitude = 23.098022285398542;
	public static double douLongitude = 113.2801204919815;

	protected static LocationManager locationManager;
	protected static Handler locationHandler;
	public static Location currentLocation;
	protected static MyLocationService gpsLocationListener;
	protected static MyLocationService networkLocationListener;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// 获取定位服务的Manager
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		initComponents();

		initList();
		handleMessage();
		startGPSLocationListener();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Intent intentLocation = new Intent(this, MyLocationService.class);
		this.startService(intentLocation);
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		finishGPSLocationListener(); // release the GPS resources
		Intent intent = new Intent(this, MyLocationService.class);
		this.stopService(intent);
		System.exit(-1);
	}

	private void handleMessage() {
		// 在MyLocationService.java中，字面可了解大概意思
		locationHandler = new Handler() {
			public void handleMessage(Message msg) {
				Location location = (Location) msg.obj;
				switch (msg.what) {
				case UPATE_LOCATION:
					// 开启GPS定位，在下面的onResume事件（用户可以交互时触发）中调用
					// 如果当前无定位信息，则给出默认坐标
					currentLocation = location;
					mImageView.setEnabled(true);
					break;
				case REFRESH:
					refreshList();
				default:
					break;
				}
			}
		};
	}
//	LayoutInflater inflater;
	RelativeLayout relativeLayoutPre;
	LinearLayout lineLayoutCont;
	Button postBtn ;
	Button cancelBtn ;
	ImageView previewView;
	private void initComponents() {
		mImageView = (ImageView) findViewById(R.id.camera);
//		inflater = getLayoutInflater();

//		LayoutInflater inflater =(LayoutInflater)this.getSystemService(LAYOUT_INFLATER_SERVICE);
//		inflater = LayoutInflater.from(this);
	    relativeLayoutPre = (RelativeLayout)findViewById(R.id.preview_layout);
	    lineLayoutCont = (LinearLayout)findViewById(R.id.content_layout);
		mImageView.setOnClickListener(listener);
		itemlist = (ListView) findViewById(R.id.listView);

		itemlist.setOnItemClickListener(this);
		itemlist.setOnItemLongClickListener(itemLongClickListener);
		
		postBtn = (Button) findViewById(R.id.post_btn);
		cancelBtn = (Button) findViewById(R.id.cancel_btn);
		previewView = (ImageView) findViewById(R.id.preview_view);
		postBtn.setOnClickListener(this);
		cancelBtn.setOnClickListener(this);
		
	}


	private OnItemLongClickListener itemLongClickListener = new OnItemLongClickListener() {
		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int position, long id) {
			doOpenFile(currentData.fileInfos.get(position).path);
			return true;
		}
	};
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		return super.onTouchEvent(event);
	}

	  @Override
	  public boolean onKeyDown(int keyCode, KeyEvent event) {
	    switch (keyCode) {
	    case KeyEvent.KEYCODE_BACK:
	      if(relativeLayoutPre.isShown()){
				mImageView.setEnabled(true);
				itemlist.setEnabled(true);
				relativeLayoutPre.setVisibility(View.INVISIBLE);
				return true;
	      }
	      finish();
	    default:
	      return super.onKeyDown(keyCode, event);
	    }
	  }
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.post_btn:
			
			break;
		case R.id.cancel_btn:
			mImageView.setEnabled(true);
			itemlist.setEnabled(true);
			relativeLayoutPre.setVisibility(View.INVISIBLE);
			break;
		default:
			break;
		}
		
	}
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
	
		FileInfo fileinfo = currentData.fileInfos.get(position);
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inSampleSize = 1;
		Bitmap bmp = BitmapFactory.decodeFile(fileinfo.path, opts);
//		ImageView img = new ImageView(this);
//		img.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
//				LayoutParams.FILL_PARENT));
//		BitmapDrawable d = new BitmapDrawable(this.getResources(), bmp);
//		img.setImageBitmap(bmp);
//		img.setBackgroundDrawable(d);
		previewView.setImageBitmap(bmp);
		mImageView.setEnabled(false);
		itemlist.setEnabled(false);
		relativeLayoutPre.setVisibility(View.VISIBLE);
		
//		  AlertDialog.Builder builder = new AlertDialog.Builder(this);  
//	        builder.setTitle("发送图像")  
//	        .setView(img).setPositiveButton("发送", new DialogInterface.OnClickListener() {  
//	            @Override  
//	            public void onClick(DialogInterface dialog, int which) {  
//	               
//	            }  
//	        })  
//	        .setNegativeButton("取消",null);
//	        
//			img.setOnTouchListener(new OnTouchListener(){
//
//				@Override
//				public boolean onTouch(View v, MotionEvent event) {
//					Toast.makeText(LaCameraActivity.this, R.string.msg_unable_to_get_current_location,
//							Toast.LENGTH_SHORT).show();
//					return false;
//				}
//				
//				
//			});
//	        builder.show();
	        
	}
	private void doOpenFile(String filePath) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		Uri uri = Uri.parse("file://" + filePath);
		String type = null;
		type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
				MimeTypeMap.getFileExtensionFromUrl(filePath));
		if (type != null) {
			intent.setDataAndType(uri, type);
			try {
				startActivity(intent);
			} catch (ActivityNotFoundException e) {
				Toast.makeText(this, getString(R.string.can_not_open_file),
						Toast.LENGTH_SHORT).show();
			}
		} else {
			Toast.makeText(
					this,
					getString(R.string.can_not_find_a_suitable_program_to_open_this_file),
					Toast.LENGTH_SHORT).show();
		}

	}

	private void initList() {
		findFileInfo(IMAGE_DIR, fInfos);
		currentData = new FileData(fInfos, null, IMAGE_DIR);
		fileAdapterList = new FileListAdapter(this, currentData);
		itemlist.setAdapter(fileAdapterList);

		fileAdapterList.notifyDataSetChanged();
	}

	private void refreshList() {
		initList();
	}

	/**
	 * 
	 * @param path
	 * @param list
	 */
	private void findFileInfo(String path, List<FileInfo> list) {

		synchronized (list) {
			list.clear();

			File base = new File(path);
			File[] files = base.listFiles();
			if (files == null || files.length == 0)
				return;
			String name;
			int length = files.length;
			for (int i = 0; i < length; i++) {
				File file = files[i];
				name = file.getName();
				// if (files[i].isHidden()) {
				// continue;
				// }
				long time = file.lastModified();
				list.add(new FileInfo(name, file.getAbsolutePath(), FileUtil
						.switchIcon(file), null, // fileSize(files[i].length()),
						file.isDirectory(), FileUtil.getDesc(file), time)); // //date.toLocaleString(),

			}
			Collections.sort(list);
		}

	}

	private OnClickListener listener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.camera:
				Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				// File file = new File(IMAGE_TEMP_DIR);
				// if(!file.exists()){
				// file.mkdirs();
				// }

				// intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new
				// File(file,TEMP_FILE_NAME)));
				// //intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
				// android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
				startActivityForResult(intent, PHOTOHRAPH);
				break;
			default:
				break;
			}

		}
	};

	@Override
	protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
		   // 创建ProgressDialog对象  
		final ProgressDialog xh_pDialog = new ProgressDialog(this);  

        // 设置进度条风格，风格为圆形，旋转的  
        xh_pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);  

        // 设置ProgressDialog 标题  
        xh_pDialog.setTitle("提示");  

        // 设置ProgressDialog提示信息  
        xh_pDialog.setMessage("这是一个圆形进度条对话框");  
 

        // 设置ProgressDialog 的进度条是否不明确 false 就是不设置为不明确  
        xh_pDialog.setIndeterminate(false);  

        // 设置ProgressDialog 是否可以按退回键取消  
        xh_pDialog.setCancelable(true);  

        // 让ProgressDialog显示  
        xh_pDialog.show();  
		if (resultCode == 0 || data == null)
			return;
		// 拍照
		if (requestCode == PHOTOHRAPH) {

			new Thread(){

				@Override
				public void run() {
					String imageFilepath = "";
					Uri u = data.getData();
					Cursor cursor = getContentResolver().query(u, null, null, null,
							null);
					cursor.moveToFirst();
					int index = cursor
							.getColumnIndex(android.provider.MediaStore.Images.Media.DATA);
					if (-1 != index) {
						imageFilepath = cursor.getString(index);// 获取文件的绝对路径
					}
					cursor.close();

					double latitude = currentLocation.getLatitude();
					double longitude = currentLocation.getLongitude();
					// 设置文件保存路径这里放在跟目录下
					// File picture = new File(IMAGE_TEMP_DIR+TEMP_FILE_NAME);
					String addr = SearchGoogleUtil.getAddr(latitude, longitude);
					pressText(DateUtil.getWaterDate(),
							String.format(getString(R.string.latitude_longitude),
									latitude, longitude), addr, imageFilepath, "宋体",
							36, Color.YELLOW, 25, 20, 0, 0x88);

					File tempFile = new File(imageFilepath);
					tempFile.delete();
					xh_pDialog.cancel();
					locationHandler.obtainMessage(REFRESH).sendToTarget();
				}
				
			}.start();
		}
	

		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * 文字水印
	 * 
	 * @param pressText
	 *            水印文字
	 * @param targetImg
	 *            目标图片
	 * @param fontName
	 *            字体名称
	 * @param fontStyle
	 *            字体样式
	 * @param white
	 *            字体颜色
	 * @param fontSize
	 *            字体大小
	 * @param x
	 *            修正值
	 * @param y
	 *            修正值
	 * @param alpha
	 *            透明度
	 */
	private void pressText(String pressText, String locationText, String addr,
			String targetImg, String fontName, int fontStyle, int color,
			int fontSize, int x, int y, int alpha) {
		if (null == targetImg || targetImg.length() < 0) {
			return;
		}
		try {
			BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inSampleSize = 2;

			Bitmap bmp = BitmapFactory.decodeFile(targetImg, opts);
			int width = bmp.getWidth();
			int height = bmp.getHeight();
			Bitmap mbmpTest = Bitmap
					.createBitmap(width, height, Config.RGB_565);
			Canvas canvasTemp = new Canvas(mbmpTest);
			Typeface font = Typeface.create(fontName, fontStyle);

			Paint p = new Paint();
			canvasTemp.drawBitmap(bmp, 0, 0, p );

			p.setColor(color);
			p.setTypeface(font);
			p.setTextSize(fontSize);
			// p.setAlpha(alpha);
			canvasTemp.drawText(pressText, x, (height - fontSize * 3) + y, p);
			canvasTemp
					.drawText(locationText, x, (height - fontSize * 2) + y, p);
			canvasTemp.drawText(addr, x, (height - fontSize) + y, p);
			File imageFileDir = new File(IMAGE_DIR);
			if (!imageFileDir.exists()) {
				imageFileDir.mkdirs();
			}
			OutputStream bos = new FileOutputStream(new File(imageFileDir,
					DateUtil.getImageDate() + ".jpg"));
			bmp.recycle();
			mbmpTest.compress(CompressFormat.JPEG, 80, bos);
			mbmpTest.recycle();
			bos.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void startGPSLocationListener() {
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setAltitudeRequired(false);
		criteria.setSpeedRequired(false);
		criteria.setBearingRequired(false);
		criteria.setCostAllowed(true);
		criteria.setPowerRequirement(Criteria.POWER_LOW);
		String provider = locationManager.getBestProvider(criteria, true); // gps
		if (provider != null) {
			Location location = locationManager.getLastKnownLocation(provider);
			if (null == location) {
				location = new Location("");
				location.setLatitude(douLatitude);
				location.setLongitude(douLongitude);
			}
			currentLocation = location;
			mImageView.setEnabled(true);
		} else { // gps and network are both disabled
			Toast.makeText(this, R.string.msg_unable_to_get_current_location,
					Toast.LENGTH_SHORT).show();
			mImageView.setEnabled(false);
		}

		/* GPS_PROVIDER */
		if (gpsLocationListener == null) {
			gpsLocationListener = new MyLocationService();
			// LocationManager.GPS_PROVIDER = "gps"
			provider = LocationManager.GPS_PROVIDER;
			locationManager.requestLocationUpdates(provider,
					MyLocationService.MINTIME, MyLocationService.MINDISTANCE,
					gpsLocationListener);
			// Log.i("Location", provider + " requestLocationUpdates() " +
			// minTime + " " + minDistance);
		}

		/* NETWORK_PROVIDER */
		if (networkLocationListener == null) {
			networkLocationListener = new MyLocationService();

			// LocationManager.NETWORK_PROVIDER = "network"
			provider = LocationManager.NETWORK_PROVIDER;
			locationManager.requestLocationUpdates(provider,
					MyLocationService.MINTIME, MyLocationService.MINDISTANCE,
					networkLocationListener);
			// Log.i("Location", provider + " requestLocationUpdates() " +
			// minTime + " " + minDistance);
		}
	}

	protected static void finishGPSLocationListener() {
		if (locationManager != null) {
			if (networkLocationListener != null) {
				locationManager.removeUpdates(networkLocationListener);
			}
			if (gpsLocationListener != null) {
				locationManager.removeUpdates(gpsLocationListener);
			}
			networkLocationListener = null;
			gpsLocationListener = null;
		}
	}




}

class FileData {
	public ArrayList<FileInfo> fileInfos;
	public ArrayList<Integer> selectedId;
	public String path;
	public boolean searchingTag = false;

	public FileData(ArrayList<FileInfo> fileInfos,
			ArrayList<Integer> selectedId, String path) {
		if (fileInfos == null)
			this.fileInfos = new ArrayList<FileListAdapter.FileInfo>();
		else
			this.fileInfos = fileInfos;
		if (selectedId == null)
			this.selectedId = new ArrayList<Integer>();
		else
			this.selectedId = selectedId;
		if (path == null)
			this.path = "/sdcard";
		else
			this.path = path;
	}
}