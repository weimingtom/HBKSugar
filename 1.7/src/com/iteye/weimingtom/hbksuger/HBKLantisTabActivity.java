package com.iteye.weimingtom.hbksuger;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class HBKLantisTabActivity extends Activity {
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
	private Button top_view_back;
	
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
				List<WebDowner.LantisTabInfo> tabs = webDowner.getLantisTabs();
				tempmodels.clear();
				for (WebDowner.LantisTabInfo tab : tabs) {
					tempmodels.add(new MenuItemModel(tab.title, tab.href, null, null, null));
				}
			} catch (Throwable e) {
				e.printStackTrace();
				tempmodels.clear();
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(HBKLantisTabActivity.this, 
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
		
		textViewTitle.setText("Lantis节目分组");
		models = new ArrayList<MenuItemModel>();
		tempmodels = new ArrayList<MenuItemModel>();
		adapter = new MenuItemAdapter(this, models);
		viewBookList.setAdapter(adapter);
		viewBookList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (position >= 0 && position < models.size()) {
					startActivity(new Intent(HBKLantisTabActivity.this, HBKLantisPageActivity.class)
						.putExtra(HBKLantisPageActivity.EXTRA_TABNAME, models.get(position).detail)
						.putExtra(HBKLantisPageActivity.EXTRA_TITLENAME, models.get(position).title));
				}
			}
		});
		viewBookList.setVisibility(View.INVISIBLE);
		pbLoading.setVisibility(View.VISIBLE);
		webDowner = new WebDowner();
		updateHandler = new UpdateHandler();
		updateThread = new UpdateThread();
		updateThread.start();
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
	
	
}
