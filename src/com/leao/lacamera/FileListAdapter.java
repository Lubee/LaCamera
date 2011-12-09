package com.leao.lacamera;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Date;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.leao.lacamera.util.FileUtil;
import com.leao.lacamera.util.Util;

/**
 * File item adapter for listview
 * */
public class FileListAdapter extends BaseAdapter implements FileAdapter {

	static String tag = "FileDialog";

	public final static int PHOTO = 6;

	public static Drawable dPhoto;

	protected static final float ICON_DIP = 32f;
	protected static final int PHO_DIP = 45;
	protected final int ICON_PIX;
	public final float PIX_SCALE;
	final int phSize; // (PHO_DIP * (int)(PIX_SCALE + 0.5f));
	protected final static int COLOR_NAME = Color.BLACK;
	protected final static int COLOR_SELECTED = 0xff009500;

	// private Context context;
	protected Resources res;
	protected static LaCameraActivity fileManager;
	protected FileData fData;
	protected LayoutInflater inflater;
	protected PackageManager packageManager;
	protected PackageInfo pkgInfo;
	protected AbsListView listView = null;
	// protected String currentPath = "/";

	public static final int STYLE_LIST = 1;
	public static final int STYLE_GRID = 2;
	protected int style;

	public FileListAdapter(LaCameraActivity context, FileData info) {
		fileManager = context;
		fData = info;
		if (fData == null)
			fData = new FileData(new ArrayList<FileListAdapter.FileInfo>(),
					null, LaCameraActivity.IMAGE_DIR);
		res = context.getResources();
		packageManager = context.getPackageManager();
		PIX_SCALE = res.getDisplayMetrics().density;
		ICON_PIX = (int) (ICON_DIP * PIX_SCALE + 0.5f);
		phSize = (PHO_DIP * (int) (PIX_SCALE + 0.5f));
		inflater = LayoutInflater.from(context);

	}

	private static final int HANDLER_SET_ICON_IMAGE = 0;
	private static final int HANDLER_RECYCLE_BITMAP = 1;
	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HANDLER_SET_ICON_IMAGE:
				if (msg.arg1 > getCount() || msg.arg1 < 0)
					return;
				FileInfo info = fData.fileInfos.get(msg.arg1);
				View v = info.getView();
				Viewholder holder;
				if (v == null) {
					v = inflater.inflate(getLayoutId(), null, false);
					holder = new Viewholder(v);
					info.setView(v);
				} else {
					info.setView(v);
					holder = (Viewholder) v.getTag();
				}
				Drawable d = info.getDrawable();
				ImageView iv = holder.getIcon(getIconId());
				if (iv == null) {
					return;
				}
				iv.setImageDrawable(d);
				break;
			case HANDLER_RECYCLE_BITMAP:
				int size = bitmaps.size();
				for (; size > 0; size--) {
					bitmaps.remove(0);
				}
				break;
			default:
				break;
			}
		}
	};

	/**
	 * 
	 * @param type
	 */
	public void initFileBitmap(int type) {
		if (res == null)
			return;
	}

	@Override
	public int getCount() {
		return fData.fileInfos.size();
	}

	@Override
	public Object getItem(int position) {
		return fData.fileInfos.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	protected void setViewExceptIcon(Viewholder holder, FileInfo fInfo) {
		holder.getDesc().setText(fInfo.desc);
		holder.getName().setText(fInfo.name);

	}

	protected int getStartSelfUpdateCount() {
		return 5;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		if (fData.fileInfos.size() == 0)
			return null;
		Viewholder holder;
		/**/
		FileInfo fInfo = fData.fileInfos.get(position);
		if (convertView == null) {
			convertView = inflater.inflate(getLayoutId(), null, false);
			holder = new Viewholder(convertView);
			fInfo.setView(convertView);
		} else {
			holder = (Viewholder) convertView.getTag();
			fInfo.setView(convertView);
		}

		setViewExceptIcon(holder, fInfo);

		Drawable icon = fInfo.getDrawable();
		ImageView iv = holder.getIcon(getIconId());
		if (icon == null) {
			updateInfos.add(fInfo);
			updateSem.release();
			// update(fInfo);
		} else {
			iv.setImageDrawable(icon);
		}

		return convertView;
	}

	public void notifyDataSetChanged() {
		stopUpdate();
		if (!updateThread.isAlive())
			updateThread.start();
		super.notifyDataSetChanged();
		handler.sendEmptyMessage(HANDLER_RECYCLE_BITMAP);
	}

	private void stopUpdate() {
		updateCheckCount = 0;
		acquireCount = 0;
		lastPosition = 0;
		handler.removeMessages(HANDLER_SET_ICON_IMAGE);
		while (updateSem.tryAcquire())
			;
		updateInfos.clear();
	}

	public static final Drawable getIconDrawbel(int type) {
		Drawable m = null;
		switch (type) {
		case PHOTO:
			m = dPhoto;
			break;
		default:
			break;
		}
		return m;
	}

	/**
	 * 
	 * */
	protected boolean stopUpdateb = false;
	protected boolean updatingb = false;

	public synchronized boolean updating() {
		return updatingb;
	}

	public interface AddOneIconListener {
		public void addOneIcon(int position);
	}

	AddOneIconListener addOneIconListener = null;

	public void setAddOneIcon(AddOneIconListener a) {
		addOneIconListener = a;
	}

	protected Semaphore updateSem = new Semaphore(0, true);
	protected CopyOnWriteArrayList<FileInfo> updateInfos = new CopyOnWriteArrayList<FileListAdapter.FileInfo>();
	protected Thread updateThread = new Thread(new UpdateThread());
	protected int lastPosition = 0;
	private int updateCheckCount = 0;
	private int acquireCount = 0;

	class UpdateThread implements Runnable {
		@Override
		public void run() {

			int position = 0;
			FileInfo fInfo = null;
			try {
				while (true) {
					updatingb = true;
					boolean hasInfo = true;
					if (updateSem.tryAcquire()) {

						fInfo = updateInfos.get(0);
						position = fData.fileInfos.indexOf(fInfo);
					} else {

						int count = getCount();
						if (acquireCount >= getStartSelfUpdateCount())
							for (int i = lastPosition; i < count; i++) {
								fInfo = fData.fileInfos.get(i);
								if (fInfo.getView() == null
										|| fInfo.getDrawable() == null) {
									hasInfo = false;
									position = i;
									lastPosition = i + 1;
									break;
								}
							}

						if (hasInfo) {

							if (lastPosition >= count) {
								lastPosition = 0;
								updateCheckCount++;
								continue;
							}
							updateSem.acquire();
							fInfo = updateInfos.get(0);
							position = fData.fileInfos.indexOf(fInfo);
							hasInfo = true;
						}
					}
					if (hasInfo)
						updateInfos.remove(0);
					doUpdate(fInfo, position);

				}
			} catch (InterruptedException e) {

				Log.e(tag, e.getLocalizedMessage());
			}
		}

		private void doUpdate(FileInfo fInfo, int position) {
			acquireCount++;
			View v = fInfo.getView();
			if (v == null) {
				v = inflater.inflate(getLayoutId(), null, false);
				new Viewholder(v);
				fInfo.setView(v);
			} else {
				fInfo.setView(v);
			}
			getAndInitlizeIconObject(fInfo);
			Message msg = handler.obtainMessage(HANDLER_SET_ICON_IMAGE,
					position, 0);
			// handler.hasMessages(HANDLER_SET_ICON_IMAGE);
			handler.sendMessage(msg);
			// handler.sendMessage(msg);
			if (addOneIconListener != null)
				addOneIconListener.addOneIcon(position);
		}

	}

	private void update(FileInfo fInfo) {

		View v = fInfo.getView();
		if (v == null) {
			v = inflater.inflate(getLayoutId(), null, false);
			new Viewholder(v);
			fInfo.setView(v);
		} else {
			fInfo.setView(v);
		}
		getAndInitlizeIconObject(fInfo);
	}

	public final void clearUpdateData() {
		acquireCount = 0;
		while (updateSem.tryAcquire())
			;
		updateInfos.clear();
	}

	public Drawable getAPKDrawable(String filePath) {
		Drawable dr = null;
		if (filePath != null) {
			String PATH_PackageParser = "android.content.pm.PackageParser";
			String PATH_AssetManager = "android.content.res.AssetManager";
			try {

				// PackageParser packageParser = new PackageParser(apkPath);
				Class pkgParserCls = Class.forName(PATH_PackageParser);
				Class[] typeArgs = new Class[1];
				typeArgs[0] = String.class;
				Constructor pkgParserCt = pkgParserCls.getConstructor(typeArgs);
				Object[] valueArgs = new Object[1];
				valueArgs[0] = filePath;
				Object pkgParser = pkgParserCt.newInstance(valueArgs);

				DisplayMetrics metrics = new DisplayMetrics();
				metrics.setToDefaults();

				typeArgs = new Class[4];
				typeArgs[0] = File.class;
				typeArgs[1] = String.class;
				typeArgs[2] = DisplayMetrics.class;
				typeArgs[3] = Integer.TYPE;
				Method pkgParser_parsePackageMtd = pkgParserCls
						.getDeclaredMethod("parsePackage", typeArgs);
				valueArgs = new Object[4];
				valueArgs[0] = new File(filePath);
				valueArgs[1] = filePath;
				valueArgs[2] = metrics;
				valueArgs[3] = 0;
				Object pkgParserPkg = pkgParser_parsePackageMtd.invoke(
						pkgParser, valueArgs);

				// ApplicationInfo info = mPkgInfo.applicationInfo;
				Field appInfoFld = pkgParserPkg.getClass().getDeclaredField(
						"applicationInfo");
				ApplicationInfo info = (ApplicationInfo) appInfoFld
						.get(pkgParserPkg);

				Class assetMagCls = Class.forName(PATH_AssetManager);
				Constructor assetMagCt = assetMagCls
						.getConstructor((Class[]) null);
				Object assetMag = assetMagCt.newInstance((Object[]) null);
				typeArgs = new Class[1];
				typeArgs[0] = String.class;
				Method assetMag_addAssetPathMtd = assetMagCls
						.getDeclaredMethod("addAssetPath", typeArgs);
				valueArgs = new Object[1];
				valueArgs[0] = filePath;
				assetMag_addAssetPathMtd.invoke(assetMag, valueArgs);
				Resources res = fileManager.getResources();
				typeArgs = new Class[3];
				typeArgs[0] = assetMag.getClass();
				typeArgs[1] = res.getDisplayMetrics().getClass();
				typeArgs[2] = res.getConfiguration().getClass();
				Constructor resCt = Resources.class.getConstructor(typeArgs);
				valueArgs = new Object[3];
				valueArgs[0] = assetMag;
				valueArgs[1] = res.getDisplayMetrics();
				valueArgs[2] = res.getConfiguration();
				res = (Resources) resCt.newInstance(valueArgs);
				CharSequence label = null;
				if (info.labelRes != 0) {
					label = res.getText(info.labelRes);
				}

				if (info.icon != 0) {
					dr = res.getDrawable(info.icon);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return dr;
	}

	ArrayList<Bitmap> bitmaps = new ArrayList<Bitmap>();

	/**
	 * */
	public Drawable getAndInitlizeIconObject(FileInfo fInfo) {
		Drawable d = fInfo.getDrawable();
		if (d != null)
			return d;
		switch (fInfo.type) {
		case PHOTO:
			BitmapFactory.Options opt = new BitmapFactory.Options();

			opt.inPreferredConfig = Bitmap.Config.RGB_565;
			opt.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(fInfo.path(), opt);
			opt.inJustDecodeBounds = false;
			// Log.d(tag, "phSize: " + phSize + " pix_scale: " + PIX_SCALE);
			if (opt.outWidth > opt.outHeight) {
				opt.inSampleSize = opt.outWidth / phSize;
				;
			} else {
				opt.inSampleSize = opt.outHeight / phSize;
			}
			Bitmap b = BitmapFactory.decodeFile(fInfo.path(), opt);
			d = new BitmapDrawable(res, b);
			bitmaps.add(b);
			break;
		default:
			d = FileListAdapter.getIconDrawbel(fInfo.type);
			break;
		}
		fInfo.setDrawble(d);
		return d;
	}

	/**
	 * File infomation, about name ,icon, size, folder or not.
	 * */
	public static class FileInfo implements Comparable<FileInfo> {

		int type;

		String name = null;
		String size = null;
		String desc;

		String path = null;
		String date = null;
		Long time ;
		boolean directory = false;
		boolean selected = false;
		Drawable dr = null;
		private View view = null;

		// String date,
		public FileInfo(String name, String path, int type, String size,
				boolean directory, String desc,Long time) {
			this.name = name;
			this.size = size;
			this.type = type;
			this.path = path;
			this.desc = desc;
			 this.time = time;
			this.directory = directory;
		}

		public FileInfo(String path) {
			File file = new File(path);
			this.name = file.getName();
			this.desc = FileUtil.getDesc(file);
			this.size = String.valueOf(Util.fileSize(file.length()));

			this.type = FileUtil.switchIcon(file);
			this.path = path;
			// this.date = date;
			this.directory = file.isDirectory();
		}

		public final String name() {
			return this.name;
		}

		public final String path() {
			return this.path;
		}

		public final void invertSelected() {
			selected = !selected;
		}

		public final boolean selectted() {
			return selected;
		}

		public final void setSelected(boolean s) {
			selected = s;
		}

		public final String size() {
			if (size == null) {
				File file = new File(path);
				this.size = String.valueOf(Util.fileSize(file.length()));
				this.date = new Date(file.lastModified()).toLocaleString();
			}
			return this.size;
		}

		public final String date() {
			if (date == null) {
				File file = new File(path);
				this.size = String.valueOf(Util.fileSize(file.length()));
				this.date = new Date(file.lastModified()).toLocaleString();
			}
			return this.date;
		}

		public final void setView(View v) {
			view = v;
		}

		public final View getView() {
			return view;
		}

		public final boolean isPhoto() {
			return type == PHOTO;
		}

		public final synchronized void setDrawble(Drawable d) {
			dr = d;
		}

		public final synchronized Drawable getDrawable() {
			return dr;
		}

		public final boolean directory() {
			return this.directory;
		}

		public final int type() {
			return type;
		}

		@Override
		public int compareTo(FileInfo another) {
			return another.time.compareTo(this.time);
		}

	}

	protected class Viewholder {
		private View base;
		private ImageView icon = null;
		private TextView name = null;
		// private TextView size = null;
		// private TextView date = null;
		private TextView desc = null;

		boolean changed;

		/**
		 * */
		Viewholder(View view) {
			base = view;
			base.setTag(this);
		}

		public final boolean isChanged() {
			return changed;
		}

		public final void changed() {
			changed = true;
		}

		public final void clearChanged() {
			changed = false;
		}

		public final ImageView getIcon(int id) {
			if (icon == null) {
				icon = (ImageView) base.findViewById(id);
				// icon.setOnLongClickListener(longClickListener);
			}
			return icon;
		}

		public final TextView getName() {
			if (name == null) {
				name = (TextView) base.findViewById(getFileNameTextId());
			}
			return name;
		}

		public final TextView getDesc() {
			if (desc == null) {
				desc = (TextView) base.findViewById(R.id.desc);
			}
			return desc;
		}

	}

	@Override
	public int getIconId() {

		return R.id.img;
	}

	@Override
	public int getFileNameTextId() {
		return R.id.name;
	}

	@Override
	public int getLayoutId() {
		return R.layout.file_listitem;
	}

}
