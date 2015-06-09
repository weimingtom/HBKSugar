package com.iteye.weimingtom.hbksuger;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class MenuItemAdapter extends BaseAdapter{
	public final static String ICONTAG = "ICONTAG";
	
	private LayoutInflater inflater;
	private List<MenuItemModel> models;
	public volatile Bitmap[] mThumbnails;

	public MenuItemAdapter(Context context, List<MenuItemModel> models){
		this.inflater = LayoutInflater.from(context);
		this.models = models;
	}

	@Override
	public int getCount() {
		if (models == null) {
			return 0;
		}
		return models.size();
	}

	@Override
	public Object getItem(int position) {
		if (position >= getCount()){
			return null;
		}
		return models.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null){
			convertView = inflater.inflate(R.layout.menu_item_adapter, null);
			holder = new ViewHolder();
			holder.sItemTitle = (TextView)convertView.findViewById(R.id.sItemTitle);
			holder.sItemInfo = (TextView)convertView.findViewById(R.id.sItemInfo);
			holder.sItemImage = (ImageView)convertView.findViewById(R.id.sItemImage);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		holder.sItemImage.setTag(ICONTAG + position);
		MenuItemModel model = models.get(position);
		if (model != null) {
			holder.sItemTitle.setText(model.title);
			if (model.progress != null) {
				holder.sItemTitle.append(model.progress);
			}
			holder.sItemInfo.setText(model.detail);
			if (mThumbnails != null && model.imageSrc != null) {
				holder.sItemImage.setVisibility(View.VISIBLE);
				if (position >= 0 && position < mThumbnails.length) {
					Bitmap bitmap = mThumbnails[position];
					if (bitmap != null && !bitmap.isRecycled()) {
						holder.sItemImage.setImageBitmap(bitmap);
					} else {
						holder.sItemImage.setImageBitmap(null);
					}
				} else {
					holder.sItemImage.setImageBitmap(null);
				}
			} else {
				holder.sItemImage.setVisibility(View.GONE);
			}
		}
		return convertView;
	}
	
    private static final class ViewHolder {
    	TextView sItemTitle;
    	TextView sItemInfo;
    	ImageView sItemImage;
    }
}
