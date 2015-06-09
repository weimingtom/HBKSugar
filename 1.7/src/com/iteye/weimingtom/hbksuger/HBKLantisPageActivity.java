package com.iteye.weimingtom.hbksuger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class HBKLantisPageActivity extends Activity {
	private final static boolean D = false;
	private final static String TAG = "HBKLantisPageActivity";
	
	public final static String EXTRA_TABNAME = "com.iteye.weimingtom.hbksuger.HBKLantisPageActivity.tabName";
	public final static String EXTRA_TITLENAME = "com.iteye.weimingtom.hbksuger.HBKLantisPageActivity.titleName";
	public final static int MESSAGE_UPDATE_THREAD = 111;
	
	private ListView viewBookList;
	private TextView textViewTitle;
	private MenuItemAdapter adapter;
	private ProgressBar pbLoading;
	private List<MenuItemModel> models;
	private List<MenuItemModel> tempmodels;
	private UpdateHandler updateHandler;
	private UpdateThread updateThread;
	private WebDowner webDowner;
	private String tabName;
	private String titleName;
	private List<WebDowner.LantisPageInfo> pages;
	private Button top_view_back;
	
	private volatile AtomicInteger isStop = new AtomicInteger(0);
	
	private class UpdateHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == MESSAGE_UPDATE_THREAD) {
				models.clear();
				if (tempmodels != null) {
					for (MenuItemModel model : tempmodels) {
						models.add(model);
					}
				}
				adapter.notifyDataSetChanged();
				viewBookList.setVisibility(ListView.VISIBLE);
				pbLoading.setVisibility(ProgressBar.INVISIBLE);
				new LoadThumbTask().execute();
			}
		}
	}
	
	private class UpdateThread extends Thread {
		private volatile boolean isStop = false;
		private Object isStopLock = new Object();
		
		public void setStop(boolean isStop) {
			synchronized (isStopLock) {
				this.isStop = isStop;
				if (webDowner != null) {
					webDowner.abort();
				}
			}
		}

		public boolean getStop() {
			synchronized (isStopLock) {
				return this.isStop;
			}
		}
		
		@Override
		public void run() {
			setStop(false);
			try {
				pages = webDowner.getLantisPages(tabName);
				tempmodels.clear();
				for (WebDowner.LantisPageInfo page : pages) {
					tempmodels.add(new MenuItemModel(page.title, 
						page.titleHref + "\n" +
						page.banner + "\n" +
						Html.fromHtml(page.comment).toString() + "\n" +
						page.asx32k + "\n" + 
						page.asx64k + "\n" +
						Html.fromHtml(page.time).toString(), 
						page.banner, null, null));
					if (D) {
						Log.e(TAG, "======banner=======" + page.banner);
					}
				}
			} catch (Throwable e) {
				tempmodels.clear();
				e.printStackTrace();
				e.printStackTrace();
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(HBKLantisPageActivity.this, 
							"加载失败，请检查网络连接", Toast.LENGTH_SHORT).show();
					}
				});
			} finally {
				updateHandler.sendMessage(updateHandler.obtainMessage(MESSAGE_UPDATE_THREAD));
			}
			setStop(true);
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.setContentView(R.layout.main_menu);
		viewBookList = (ListView) findViewById(R.id.viewBookList);
		textViewTitle = (TextView) findViewById(R.id.textViewTitle);
		pbLoading = (ProgressBar) findViewById(R.id.pbLoading);
		top_view_back = (Button) findViewById(R.id.top_view_back);
		top_view_back.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				finish();
			}
		});
		
//		textViewLoading.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View arg0) {
//				finish();
//			}
//		});
		
		textViewTitle.setText("Lantis节目表");
		models = new ArrayList<MenuItemModel>();
		tempmodels = new ArrayList<MenuItemModel>();
		adapter = new MenuItemAdapter(this, models);
		viewBookList.setAdapter(adapter);
		viewBookList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (position >= 0 && position < pages.size()) {
					startActivity(new Intent(HBKLantisPageActivity.this, HBKLantisDetailActivity.class)
						.putExtra(HBKLantisDetailActivity.EXTRA_TITLE_HREF, pages.get(position).titleHref)
						.putExtra(HBKLantisDetailActivity.EXTRA_TITLE, pages.get(position).title)
						.putExtra(HBKLantisDetailActivity.EXTRA_BANNER, pages.get(position).banner)
						.putExtra(HBKLantisDetailActivity.EXTRA_COMMENT, pages.get(position).comment)
						.putExtra(HBKLantisDetailActivity.EXTRA_ASX32K, pages.get(position).asx32k)
						.putExtra(HBKLantisDetailActivity.EXTRA_ASX64K, pages.get(position).asx64k)
						.putExtra(HBKLantisDetailActivity.EXTRA_TIME, pages.get(position).time)
					);
				}
			}
		});
		viewBookList.setVisibility(View.INVISIBLE);
		pbLoading.setVisibility(View.VISIBLE);
		webDowner = new WebDowner();
		updateHandler = new UpdateHandler();
		
		Intent intent = this.getIntent();
		if (intent != null) {
			tabName = intent.getStringExtra(EXTRA_TABNAME);
			titleName = intent.getStringExtra(EXTRA_TITLENAME);
			if (titleName != null && titleName.length() > 0) {
				textViewTitle.setText("Lantis - " + titleName);
			}
			if (tabName != null) {
				updateThread = new UpdateThread();
				updateThread.start();
			}
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		for (MenuItemModel model : models) {
			if (model != null) {
				model.recycle();
			}
		}
		if (updateThread != null && !updateThread.getStop()) {
			updateThread.setStop(true);
			try {
				updateThread.join(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		updateThread = null;
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (this.isFinishing()) {
			if (updateThread != null && !updateThread.getStop()) {
				updateThread.setStop(true);
				try {
					updateThread.join(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			updateThread = null;
		}
	}

	
	private class LoadThumbTask extends AsyncTask<Void, Integer, Boolean> {
		private final static float PERCENT = 0.8f;
		
		private boolean loadResult = false;
		private int count;
		private int imageThumbSizeW = 100;
		private int imageThumbSizeH = 100;
		private Bitmap[] oldThumbnails;
		private int firstLoad = -1;
		private boolean use16BitsThumb = false;
		private boolean[] loadSkip;
		private MenuItemModel[] items;
		private long totalsize = 0;
		
		private boolean isLoadSkip(int pos) {
			if (loadSkip == null) {
				return false;
			}
			if (pos < 0 || pos >= loadSkip.length) {
				return false;
			}
			return loadSkip[pos];
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			try {
				imageThumbSizeW = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size_width);
			} catch (Throwable e) {
				e.printStackTrace();
			}
			try {
				imageThumbSizeH = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size_height);
			} catch (Throwable e) {
				e.printStackTrace();
			}
			count = adapter.getCount();
			oldThumbnails = adapter.mThumbnails;
			adapter.mThumbnails = new Bitmap[count];
			loadSkip = new boolean[count];
			use16BitsThumb = true;
			
			isStop.set(0);
			
			items = new MenuItemModel[count];
			for (int i = 0; i < count; i++) {
				items[i] = (MenuItemModel)adapter.getItem(i);
			}
		}

		@Override
		protected Boolean doInBackground(Void... params) {
//			try {
//				Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
//			} catch (Throwable e) {
//				e.printStackTrace();
//			}
			try {
				if (oldThumbnails != null) {
					for (int i = 0; i < oldThumbnails.length; i++) {
						if (oldThumbnails[i] != null && !oldThumbnails[i].isRecycled()) {
							oldThumbnails[i].recycle();
							oldThumbnails[i] = null;
						}
					}
				}
				this.publishProgress(0, count, 0);
				totalsize = 0;
				int loadnum = 0;
				while (true) {
					if (isStop.get() == 1) {
						break;
					}
//					if (!listViewShelfIdle) {
//						continue;
//					}
					int position = -1;
					if (firstLoad >= 0 && adapter != null && 
						adapter.mThumbnails != null && 
						adapter.mThumbnails[firstLoad] == null && 
						!isLoadSkip(firstLoad)) {
						position = firstLoad;
					} else {
						for (int i = 0; i < count; i++) {
							if (adapter != null && 
								adapter.mThumbnails != null && 
								adapter.mThumbnails[i] == null && 
								!isLoadSkip(i)) {
								position = i;
								break;
							}
						}
					}
					if (D) {
						Log.e(TAG, "position == " + position);
					}
					if (position < 0) {
						break;
					}
					MenuItemModel item = null;
					//item = adapter.getItem(position);
					if (position >= 0 && position < items.length) {
						item = items[position];
					}
					if (item != null) {
						String filename = WebDowner.getUrlFileNameLantis(item.imageSrc);
						String pathname = WebDowner.getThumbDir();
						if (D) {
							Log.e(TAG, "===============filename: " + filename);
						}
						if (filename != null && pathname != null) {
							if (createSDCardDir(pathname)) {
								File file = new File(pathname, filename);
								download(file, item.imageSrc);
								if (file.exists() && file.canRead()) {
									adapter.mThumbnails[position] = decodeSampledBitmapFromFile(file.getAbsolutePath(), imageThumbSizeW, imageThumbSizeH, use16BitsThumb);
									if (adapter.mThumbnails[position] == null) {
										loadSkip[position] = true;
									}
									Bitmap bitmap = adapter.mThumbnails[position];
									if (bitmap != null) {
										totalsize += bitmap.getRowBytes() * bitmap.getHeight();
									}
									if (D) {
										Log.e(TAG, "position == " + position + ", totalsize == " + totalsize + 
											", max == " + Runtime.getRuntime().maxMemory() * PERCENT);
									}
								} else {
									loadSkip[position] = true;
								}
							}
						}
					}
					loadnum++;
					this.publishProgress(position + 1, count, loadnum);
					if (totalsize > Runtime.getRuntime().maxMemory() * PERCENT) {
						break;
					}
				}
				loadResult = true;				
			} catch (Throwable e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
			if (totalsize > Runtime.getRuntime().maxMemory() * PERCENT) {
				Toast.makeText(HBKLantisPageActivity.this, 
					"内存不足", 
					Toast.LENGTH_SHORT).show();
			}
			//actionBar.setTitle("" + values[2] + "/" + values[1]);
			if (D) {
				Log.e(TAG, "onProgressUpdate:" + values[2] + "/" + values[1]);
			}
			if (false) {
				adapter.notifyDataSetChanged();
			} else {
				final int position = values[0] - 1;
				final int total = values[1];
				final ImageView ivIcon = (ImageView) viewBookList.findViewWithTag(MenuItemAdapter.ICONTAG + position);
				if (ivIcon != null && 
					adapter != null && 
					adapter.mThumbnails != null &&
					position >= 0 &&
					adapter.mThumbnails[position] != null && 
					!adapter.mThumbnails[position].isRecycled()) {
					ivIcon.setImageBitmap(adapter.mThumbnails[position]);
				}
				int fp = viewBookList.getFirstVisiblePosition();
				int lp = viewBookList.getLastVisiblePosition();
				this.firstLoad = -1;
				if (adapter.mThumbnails != null) {
					for (int i = 0; i < adapter.mThumbnails.length; i++) {
						if (i >= fp && i <= lp && 
							adapter != null &&
							adapter.mThumbnails[i] == null && 
							!isLoadSkip(i)) {
							this.firstLoad = i;
							break;
						}
					}
				}
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result == true && !HBKLantisPageActivity.this.isFinishing()) {
				if (loadResult) {
					adapter.notifyDataSetChanged();
				} else {
					
				}
			} else if (result == false) {
				Toast.makeText(HBKLantisPageActivity.this, 
						"内存不足", Toast.LENGTH_SHORT).show();
			}
		}
		
		private Bitmap decodeSampledBitmapFromFile(String filename, int reqWidth, int reqHeight, boolean isUse16Bits) {
	    	final BitmapFactory.Options options = new BitmapFactory.Options();
	        options.inJustDecodeBounds = true;
	        BitmapFactory.decodeFile(filename, options);
	        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
	        if (D) {
	        	Log.e(TAG, "options.inSampleSize == " + options.inSampleSize + "," + reqWidth + "," + reqHeight);
	        }
	        options.inJustDecodeBounds = false;
			if (isUse16Bits) { 
				options.inPreferredConfig = Bitmap.Config.RGB_565;   
				options.inPurgeable = true;  
				options.inInputShareable = true;  
			}
	        return BitmapFactory.decodeFile(filename, options);
		}
	    
	    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
	        final int height = options.outHeight;
	        final int width = options.outWidth;
	        int inSampleSize = 1;
	        if (height > reqHeight || width > reqWidth) {
	            if (width > height) {
	                inSampleSize = Math.round((float) height / (float) reqHeight);
	            } else {
	                inSampleSize = Math.round((float) width / (float) reqWidth);
	            }
	            final float totalPixels = width * height;
	            final float totalReqPixelsCap = reqWidth * reqHeight * 2;
	            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
	                inSampleSize++;
	            }
	        }
	        return inSampleSize;
	    }
	    
	    private void download(File file, String imgUrl) {
	        if (file != null && 
	        	file.isFile() && file.exists() && file.length() > 0) {
	        	return;
	        }
	        boolean isNoTimeout = false;
	        InputStream bitmapIs = null;

	        boolean isSuccess = false;
	        File oldFile = file;
	        file = new File(file.getAbsolutePath() + ".temp"); 
	        {
		    	URL url;
		    	BufferedInputStream bis = null;
		    	URLConnection connection = null;
		    	FileOutputStream fos = null;
		    	BufferedOutputStream bos = null;
		    	try {
		           url = new URL(imgUrl);
		           connection = url.openConnection();
		           connection.setUseCaches(true);
		           if (!isNoTimeout) {
		        	   connection.setConnectTimeout(500);
		        	   connection.setReadTimeout(500);
		           } else {
		        	   connection.setConnectTimeout(5000);
		        	   connection.setReadTimeout(5000);        	   
		           }
		           bitmapIs = connection.getInputStream();
		           bis = new BufferedInputStream(bitmapIs);
		           byte[] data = new byte[1024 * 8]; //默认一次读8K
		           int count = 0;
		           fos = new FileOutputStream(file);
		           while ((count = bis.read(data)) != -1){
		            	fos.write(data, 0, count);
		           }
		           fos.flush();
		           isSuccess = true;
		        } catch (MalformedURLException e) {
		        	e.printStackTrace();
		        } catch (IOException e) {
		            e.printStackTrace();
		        } finally {
		        	if (bos != null) {
		        		try {
							bos.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
		        	}
		        	if (fos != null) {
		        		try {
							fos.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
		        	}
		        	if (bis != null) {
		        		try {
							bis.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
		        	}
		        	if (bitmapIs != null) {
		        		try {
		        			bitmapIs.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
		        	}
		        }
	        }
	        if (isSuccess) {
	        	file.renameTo(oldFile);
	        }
	    }
	    
	    public boolean createSDCardDir(String path){
	    	if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())){
	            File file = new File(path);
	            if (!file.exists()) {
	            	return file.mkdirs();
	            } else {
	            	return true;
	            }
	        }
	    	return false;
	    }
    }
}
