package gl.live.danceshow.media;


import gl.live.danceshow.fragment.SubtitleItem;

import java.io.File;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.app.showdance.impl.ContentValue;
import com.android.app.showdance.logic.DownloadMediaService;
import com.android.app.showdance.logic.VolleyManager;
import com.android.app.showdance.widget.CustomAlertDialog;
import com.android.app.wumeiniang.R;
import com.android.app.wumeiniang.app.InitApplication;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

/**
 * 
 * @ClassName: RecordedVideoAdapter
 * @Description: 已录制视频
 * @author maminghua
 * @date 2015-5-14 下午03:51:19
 * 
 */
public class SubtitleAdapter extends BaseAdapter implements OnItemLongClickListener{

	protected Context context;
	protected List<SubtitleItem> mList;
	protected static int mSelected=-1;
	
	public SubtitleAdapter(Context context){
		init();
	}
	
	public SubtitleAdapter(Context context,List<SubtitleItem> list) {
		this.context = context;
		mList = list;
		init();
	}
	
	protected void  init() {
		
	}
	 
	public void setSubTitleList(List<SubtitleItem> list) {
		mList = list;//new ArrayList<SubtitleItem>();
	}
	
	@Override
	public int getCount() {
		if(mList.size() == 0) return 0;
		return getCurrentList().size();
	}

	@Override
	public Object getItem(int position) {
		return getCurrentList().get(position);
	}

	public List<SubtitleItem> getCurrentList() {
		return mList;
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public int getItemViewType(int position) {
		return position;
	}


	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = ((Activity)context).getLayoutInflater().inflate(R.layout.subtitle_list, null); 
		}
		SubtitleItem item = getCurrentList().get(position);
//		String stringid = item.getName();
		TextView text = (TextView)convertView.findViewById(R.id.list_item_name);
		switch (item.getDownload()) {
		case ContentValue.DOWNLOAD_STATE_SUCCESS:
			text.setText("使用");
			break;
		case ContentValue.DOWNLOAD_STATE_DOWNLOADING:
			text.setText(String.valueOf(item.getPercent())+"%");
			break;
		case ContentValue.DOWNLOAD_STATE_WATTING:
		case ContentValue.DOWNLOAD_STATE_EXCLOUDDOWNLOAD:
			text.setText("下载中");
			break;
		case ContentValue.DOWNLOAD_STATE_NONE:
			text.setText("未下载");
			break;
		default:
			text.setText("未下载");
			break;
		}
//		text.setText(stringid);
		if(item.getImg() == null)
			return convertView;
		
		DisplayImageOptions opt = new DisplayImageOptions.Builder()
				.imageScaleType(ImageScaleType.EXACTLY).cacheOnDisk(true)
				.build();
		ImageView img =  (ImageView)convertView.findViewById(R.id.fg_img);
		ImageLoader.getInstance().displayImage(VolleyManager.SERVER_URL+item.getImg(), img,opt);
//		img.setOnClickListener(new FrameOnClick(item));
//		
//		CheckBox box = (CheckBox)convertView.findViewById(R.id.list_item_checkbox);
//		box.setChecked(mSelected == getCurrentList().get(position).getRawId());
		return convertView;
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		final SubtitleItem fileName = mList.get(position);
		if(fileName.getDownload() != ContentValue.DOWNLOAD_STATE_SUCCESS) return false;
		final int index = position;
		CustomAlertDialog mCustomDialog = new CustomAlertDialog(context).builder(R.style.DialogTVAnimWindowAnim);
		mCustomDialog.setTitle("删除提示");
		mCustomDialog.setMsg("确认删除 " + fileName.getName() + " 吗?");
		mCustomDialog.setPositiveButton(context.getResources().getString(R.string.dialog_ok), new OnClickListener() {
			@Override
			public void onClick(View v) {
				File delFilePath = new File(fileName.getPath());
				Log.d("guolei","deleteDir "+deleteFile(delFilePath));
				mList.remove(index);
				notifyDataSetChanged();
			}
		}).setNegativeButton(context.getResources().getString(R.string.dialog_cancel), new OnClickListener() {
			@Override
			public void onClick(View v) {

			}
		}).show();
		
		return true;
	
	}

	private boolean deleteFile(File file) {
//		if (dir.isDirectory()) {
//			String[] children = dir.list();
//			for (int i = 0; i < children.length; i++) {
//				boolean success = deleteDir(new File(dir, children[i]));
//				if (!success) {
//					return false;
//				}
//			}
//		}
		// The directory is now empty so now it can be smoked
		return file.delete();
	} 
	public interface OnFgItemClickListener {
		void onItemClick(AnimItem item);
	}




	public void updateList() {
		
	}
}
