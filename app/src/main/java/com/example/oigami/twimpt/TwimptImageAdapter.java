package com.example.oigami.twimpt;

/**
 * Created by oigami on 2015/02/10
 */

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.oigami.twimpt.file.FileDownloadThread;
import com.example.oigami.twimpt.file.FileDownloader;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * グリッドビューのアダプタ
 * アダプターは、画像ダウンロード中の文字列表示と画像の表示を行う
 */
public class TwimptImageAdapter extends BaseAdapter {
  HashMap<String, String> mDownloadingTextImageMap = new HashMap<>();
  /** ダウンロード中のurl */
  private ArrayList<String> isDownloading = new ArrayList<>();
  private Drawable[] mDrawableMap;
  String[] mPostedImageUrl;
  private Context mContext;
  DrawableListener mListener;
  Handler mHandler = new Handler(Looper.getMainLooper()) {
    @Override
    public void handleMessage(Message message) {
      notifyDataSetChanged();
    }
  };

  public interface DrawableListener {
    /**
     * 画像を取得
     * @param url 取得するurl
     * @return drawable or null（無い時）
     */
    Drawable getDrawable(String url);

    /**
     * 画像をダウンロード
     * @param url ダウンロードするurl
     * @return FileDownloadNonThread or null(urlが不正な時など)
     */
    FileDownloadThread downloadDrawable(String url);
  }

  class ViewHolder {
    TextView mText;
    ImageView mImage;
  }

  TwimptImageAdapter(Context context, String[] posdedImageUrl, DrawableListener listener) {
    mContext = context;
    mListener = listener;
    mPostedImageUrl = posdedImageUrl;
    if (mPostedImageUrl == null) return;
    for (String url : mPostedImageUrl) {
      mDownloadingTextImageMap.put(url, "ダウンロード待ち");
    }
    mDrawableMap = new Drawable[mPostedImageUrl.length];
  }

  @Override
  public final int getCount() {
    if (mPostedImageUrl == null) return 0;
    return mPostedImageUrl.length;
  }

  public final String getItemUrl(int position) {
    if (getCount() < position) return null;
    return mPostedImageUrl[position];

  }

  @Override
  public final Drawable getItem(int position) {
    Drawable drawable = null;
    //通常の場所から画像を取得
    //    drawable = mTwimptLogData.mPostedImageUrl.get(position).second;
    //    if (drawable != null)
    //      return drawable;
    drawable = mDrawableMap[position];
    if (drawable != null)
      return drawable;
    String url = mPostedImageUrl[position];
    //上位から画像取得
    drawable = mListener.getDrawable(url);
    if (drawable != null)
      mDrawableMap[position] = drawable;
    return drawable;
  }

  @Override
  public final long getItemId(int position) {
    return position;
  }

  private ViewHolder CreateViewHolder(View v) {
    ViewHolder holder = new ViewHolder();
    holder.mText = (TextView) v.findViewById(R.id.image_text_text);
    holder.mImage = (ImageView) v.findViewById(R.id.image_text_image);
    return holder;
  }

  @Override
  public View getView(final int position, View convertView, ViewGroup parent) {
    // 画像表示用のImageView
    View v = convertView;
    ViewHolder holder;
    // convertViewがnullならImageViewを新規に作成する
    if (v == null) {
      LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      v = inflater.inflate(R.layout.text_image, null);
      holder = CreateViewHolder(v);
      v.setTag(holder);
    } else {
      // convertViewがnullでない場合は再利用
      holder = (ViewHolder) v.getTag();
    }
    //holder.mGridView.setMinimumHeight(50);
    Drawable drawable = getItem(position);
    holder.mImage.setImageDrawable(drawable);
    if (drawable != null) {
      holder.mText.setText(null);
      return v;
    }
    final String url = mPostedImageUrl[position];
    holder.mText.setText(mDownloadingTextImageMap.get(url));

    //        以下画像ダウンロード処理
    //すでにダウンロード中の場合再ダウンロードしない
    //TODO ダウンロード失敗時の処理をそのうち考える
    if (isDownloading.contains(url)) return v;
    //ダウンロード中の印を付ける
    isDownloading.add(url);

    FileDownloadThread downloader = mListener.downloadDrawable(url);
    if (downloader == null) return v;
    downloader.SetOnDownloadingListener(new FileDownloader.OnDownloadingListener() {
      @Override
      public void OnDownloading(int nowLoadedByte) {
        mDownloadingTextImageMap.put(url, String.format("%d", nowLoadedByte));
        mHandler.sendEmptyMessage(0);
      }
    });
    downloader.SetOnDownloadedListener(new FileDownloader.OnDownloadedListener() {
      @Override
      public void OnDownloaded(String url, boolean isSuccess) {
        Drawable drawable = mListener.getDrawable(url);
        mDrawableMap[position] = drawable;
        mHandler.sendEmptyMessage(0);
      }
    });
    downloader.Download();
    return v;
  }
}
