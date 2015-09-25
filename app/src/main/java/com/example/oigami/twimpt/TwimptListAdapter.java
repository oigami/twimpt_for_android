package com.example.oigami.twimpt;

/**
 * Created by oigami on 2015/02/10
 */

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Spannable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.oigami.twimpt.file.FileDownloadThread;
import com.example.oigami.twimpt.file.FileDownloader;
import com.example.oigami.twimpt.twimpt.TwimptLogData;
import com.example.oigami.twimpt.twimpt.TwimptTextParser;
import com.example.oigami.twimpt.twimpt.room.TwimptRoom;
import com.example.oigami.twimpt.util.TimeDiff;

import java.util.HashMap;

/** リストアダプタ */
public class TwimptListAdapter extends BaseAdapter {
  /** 画像urlと画像 */
  private HashMap<String, Drawable> mUrlDrawableMap;
  /** ログのhashと 投稿ごとのImageAdapter */
  private HashMap<String, TwimptImageAdapter> mImageAdapters;
  private Context mContext;
  /** 現在表示しているルーム */
  private TwimptRoom mNowRoom;
  private Handler mHandler;
  private int mReadHere;

  public interface DrawableListener {
    /**
     * 画像を取得
     * @param hash 取得するhash
     * @return drawable or null（無い時）
     */
    public Drawable getDrawable(String hash);

    /**
     * 画像をダウンロード
     * @param hash ダウンロードするhash
     * @return FileDownloadNonThread or null(urlが不正な時など)
     */
    public FileDownloadThread downloadDrawable(String hash);
  }

  DrawableListener mIconListener;
  TwimptImageAdapter.DrawableListener mImageListener;
  AdapterView.OnItemClickListener mImageClickListener;
  private TwimptTextParser mLogParser;

  public void setOnImageItemClickListener(AdapterView.OnItemClickListener listener) {
    mImageClickListener = listener;
  }

  public void reset(Context context,
                    TwimptRoom nowRoom,
                    DrawableListener iconListener,
                    TwimptImageAdapter.DrawableListener imageListener) {
    mContext = context;
    mNowRoom = nowRoom;
    mIconListener = iconListener;
    mImageListener = imageListener;
    mImageAdapters.clear();
    mUrlDrawableMap.clear();
  }

  public TwimptListAdapter(Context context,
                           TwimptRoom nowRoom,
                           DrawableListener iconListener,
                           TwimptImageAdapter.DrawableListener imageListener) {
    mImageAdapters = new HashMap<String, TwimptImageAdapter>();
    mUrlDrawableMap = new HashMap<String, Drawable>();
    mLogParser = new TwimptTextParser();
    mHandler = new Handler(Looper.getMainLooper()) {
      @Override
      public void handleMessage(Message message) {
        notifyDataSetChanged();
      }
    };
    reset(context, nowRoom, iconListener, imageListener);

  }

  @Override
  public boolean isEnabled(int position) {
    return true;
  }

  @Override
  public int getCount() {
    int size = mNowRoom.dataList.size();
    if (size == 0) return 0;
    return size + 1;
  }

  public void SetReadHerePosition(int position) {
    mReadHere = position;
  }

  public int GetReadHerePosition() {
    return mReadHere;
  }

  @Override
  public TwimptLogData getItem(int position) {
    if (mReadHere == position)//「ここまで読んだ」タグの場合
      return null;
    if (mReadHere < position)//「ここまで読んだ」タグより大きい場合
      return mNowRoom.dataList.get(position - 1);
    return mNowRoom.dataList.get(position);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  private class ViewHolder {
    ViewHolder(View v) {
      linkListener = new TextLinkListener();

      text = (TextView) v.findViewById(R.id.text);
      name = (TextView) v.findViewById(R.id.name);
      roomName = (TextView) v.findViewById(R.id.room_name);
      icon = (ImageView) v.findViewById(R.id.user_image);
      time = (TextView) v.findViewById(R.id.time);
      mGridView = (GridView) v.findViewById(R.id.gridImageView);
      mGridView.setOnItemClickListener(mImageClickListener);
    }

    private void SetVisibility(int visibility) {
      name.setVisibility(visibility);
      roomName.setVisibility(visibility);
      icon.setVisibility(visibility);
      time.setVisibility(visibility);
      mGridView.setVisibility(visibility);
    }

    void SetReadHereMode() {
      SetVisibility(View.GONE);
      text.setText(R.string.read_here);
      text.setGravity(Gravity.CENTER);
    }

    void SetDataMode() {
      SetVisibility(View.VISIBLE);
      text.setGravity(Gravity.LEFT);
    }

    TextLinkListener linkListener;
    TextView text, name, roomName, time;
    ImageView icon;
    GridView mGridView;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    final TwimptLogData twimptLogData = getItem(position);
    //ログの場合
    View v = convertView;
    // ビューホルダー
    ViewHolder holder;
    // 登録されているモノを使う
    if (v != null) {
      holder = (ViewHolder) v.getTag();
      assert holder != null;
    } else {
      LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      v = inflater.inflate(R.layout.list_view, null);
      holder = new ViewHolder(v);
      // ビューにホルダーを登録する
      v.setTag(holder);
    }
    if (twimptLogData != null) {
      holder.SetDataMode();
      Spannable text = twimptLogData.decodedText;
      if (text == null) {
        // logData.rawText += "\n<a href=\"http://twimpt.com/upload/original/20150110/CGMIadXM.png\" data-lightbox=\"uploaded-image\">test</a>";
        twimptLogData.TextParse(mLogParser);
        text = twimptLogData.decodedText;
        holder.linkListener.createURLSpanEx(text);
      }
      holder.linkListener.SetTextOnTouchListener(holder.text, text);
      //holder.text.setText(text);

      if (twimptLogData.postedImageUrl != null) {
        holder.mGridView.setVisibility(View.VISIBLE);
        TwimptImageAdapter imageAdapter = mImageAdapters.get(twimptLogData.hash);
        if (imageAdapter == null) {
          imageAdapter = new TwimptImageAdapter(mContext, twimptLogData, mImageListener);
          mImageAdapters.put(twimptLogData.hash, imageAdapter);
        }
        holder.mGridView.setAdapter(imageAdapter);
      } else {
        holder.mGridView.setVisibility(View.GONE);
      }

      //holder.text.setText(twimptLogData.decodedText);
      holder.name.setText(twimptLogData.user.name);

      String timeString = TimeDiff.toDiffDate(twimptLogData.time) + "前";
      holder.time.setText(timeString);
      holder.roomName.setText(twimptLogData.roomData.name);

      final String iconStr = twimptLogData.icon;
      Drawable icon = mUrlDrawableMap.get(iconStr);
      if (icon != null) {
        holder.icon.setImageDrawable(icon);
        return v;
      }
      icon = mIconListener.getDrawable(iconStr);
      if (icon != null) {
        mUrlDrawableMap.put(iconStr, icon);
        holder.icon.setImageDrawable(icon);
        return v;//通常のログデータのviewを返す
      }

      holder.icon.setImageDrawable(null);
      //2回目のダウンロードを禁止
      mUrlDrawableMap.put(iconStr, null);
      FileDownloadThread downloader = mIconListener.downloadDrawable(iconStr);
      if (downloader == null) return v;
      downloader.SetOnDownloadedListener(new FileDownloader.OnDownloadedListener() {
        @Override
        public void OnDownloaded(String url, boolean isSuccess) {
          mHandler.sendEmptyMessage(0);
        }
      });
      downloader.Download();
      return v;//通常のログデータのviewを返す
    }
    holder.SetReadHereMode();
    return v;
  }

  /**
   * これしないとリリースしまくってエラーになる
   * Adapterには必要らしいandroid4.0.3のバグだとか
   */
  @Override
  public void unregisterDataSetObserver(DataSetObserver observer) {
    if (observer != null)
      super.unregisterDataSetObserver(observer);
  }
}