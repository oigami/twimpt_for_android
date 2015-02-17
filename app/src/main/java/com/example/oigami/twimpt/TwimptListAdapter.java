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
import android.text.Html;
import android.text.Spannable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.oigami.twimpt.image.FileDownloadThread;
import com.example.oigami.twimpt.image.FileDownloader;
import com.example.oigami.twimpt.textLink.MutableLinkMovementMethod;
import com.example.oigami.twimpt.twimpt.TwimptLogData;
import com.example.oigami.twimpt.twimpt.TwimptRoom;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** リストアダプタ */
public class TwimptListAdapter extends BaseAdapter {
  /** 画像urlと画像 */
  private Map<String, Drawable> mUrlDrawableMap;
  /** 投稿ごとのImageAdapter */
  private Map<Integer, TwimptImageAdapter> mImageAdapters;
  private Context mContext;
  /** 現在表示しているルーム */
  private TwimptRoom mNowRoom;
  /** ルーム一覧 */
  private Map<String, TwimptRoom> mTwimptRooms;
  private Handler mHandler;
  private int mReadHere;
  TwimptImageAdapter.DrawableListener mIconListener;
  TwimptImageAdapter.DrawableListener mImageListener;
  AdapterView.OnItemClickListener mImageClickListener;

  public void setOnImageItemClickListener(AdapterView.OnItemClickListener listener) {
    mImageClickListener = listener;
  }

  public void reset(Context context,
                    Map<String, TwimptRoom> twimptRooms, TwimptRoom nowRoom,
                    TwimptImageAdapter.DrawableListener iconListener,
                    TwimptImageAdapter.DrawableListener imageListener) {
    mContext = context;
    mTwimptRooms = twimptRooms;
    mNowRoom = nowRoom;
    mIconListener = iconListener;
    mImageListener = imageListener;
    mImageAdapters.clear();
    mUrlDrawableMap.clear();
  }

  public TwimptListAdapter(Context context,
                           Map<String, TwimptRoom> twimptRooms, TwimptRoom nowRoom,
                           TwimptImageAdapter.DrawableListener iconListener,
                           TwimptImageAdapter.DrawableListener imageListener) {
    mImageAdapters = new HashMap<Integer, TwimptImageAdapter>();
    mUrlDrawableMap = new HashMap<String, Drawable>();
    mHandler = new Handler(Looper.getMainLooper()) {
      @Override
      public void handleMessage(Message message) {
        notifyDataSetChanged();
      }
    };
    reset(context, twimptRooms, nowRoom, iconListener, imageListener);

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
  public Object getItem(int position) {
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
    TextView text, name, roomName, time;
    ImageView icon;
    GridView mGridView;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    TwimptLogData twimptLogData = (TwimptLogData) getItem(position);
    if (twimptLogData != null) {
      //ログの場合
      View v = convertView;
      // ビューホルダー
      final ViewHolder holder;
      // 無い場合だけ作る
      int id = v == null ? 0 : v.getId();
      if (v == null || id != R.layout.list_view) {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        v = inflater.inflate(R.layout.list_view, null);
        holder = new ViewHolder();
        holder.text = (TextView) v.findViewById(R.id.text);
        //holder.text.setCompoundDrawablesRelative();
        // TextView に LinkMovementMethod を登録します
        //holder.text.setMovementMethod(movement_method);
        //http://www.globefish.jp/mt/2011/09/listview-textview-setmovementmethod.html
        //setMovementMethodの後にフォーカスをfalseにしないとlistviewのクリックに持ってかれる
        holder.text.setFocusable(false);
        holder.text.setOnTouchListener(new ViewGroup.OnTouchListener() {
          @Override
          public boolean onTouch(View view, MotionEvent event) {
            TextView textView = (TextView) view;
            //継承したLinkMovementMethod
            MutableLinkMovementMethod m = new MutableLinkMovementMethod();
            //MovementMethod m=LinkMovementMethod.getInstance();
            //リンクのチェックを行うため一時的にsetする
            textView.setMovementMethod(m);
            boolean mt = m.onTouchEvent(textView, (Spannable) textView.getText(), event);
            //チェックが終わったので解除する しないと親view(listview)に行けない
            textView.setMovementMethod(null);
            //setMovementMethodを呼ぶとフォーカスがtrueになるのでfalseにする
            textView.setFocusable(false);
            //戻り値がtrueの場合は今のviewで処理、falseの場合は親viewで処理
            return mt;
          }
        });
        holder.name = (TextView) v.findViewById(R.id.name);
        holder.roomName = (TextView) v.findViewById(R.id.room_name);
        holder.icon = (ImageView) v.findViewById(R.id.user_image);
        holder.time = (TextView) v.findViewById(R.id.time);
        holder.mGridView = (GridView) v.findViewById(R.id.gridImageView);
        holder.mGridView.setOnItemClickListener(mImageClickListener);

        // ビューにホルダーを登録する
        v.setTag(holder);
      } else {
        // 登録されているモノを使う
        holder = (ViewHolder) v.getTag();
      }
      TextDecode(twimptLogData);
      TwimptImageAdapter imageAdapter = null;
      if (twimptLogData.postedImageUrl != null) {
        holder.mGridView.setVisibility(View.VISIBLE);
        imageAdapter = mImageAdapters.get(position);
        if (imageAdapter == null) {
          imageAdapter = new TwimptImageAdapter(mContext, twimptLogData, mImageListener);
          mImageAdapters.put(position, imageAdapter);
        }
      } else {
        holder.mGridView.setVisibility(View.GONE);
      }
      holder.mGridView.setAdapter(imageAdapter);

      holder.text.setText(twimptLogData.decodedText);
      holder.text.setFocusable(false);
      holder.text.setClickable(false);
      holder.name.setText(twimptLogData.name);
      // ミリ秒で比較するので1000倍する
      String timeString = TimeDiff.toDiffDate(twimptLogData.time * 1000) + "前";
      holder.time.setText(timeString);

      Drawable icon = null;
      final String iconStr = twimptLogData.icon;
      final String url = "http://twimpt.com/icon/" + iconStr;
      if (mUrlDrawableMap.containsKey(iconStr)) {
        icon = mUrlDrawableMap.get(iconStr);
      } else {
        icon = mIconListener.getDrawable(url);
        if (icon != null) {
          mUrlDrawableMap.put(iconStr, icon);
        } else {
          mUrlDrawableMap.put(iconStr, null);
          FileDownloadThread downloader = mIconListener.downloadDrawable(url);
          downloader.SetOnDownloadedListener(new FileDownloader.OnDownloadedListener() {
            @Override
            public void OnDownloaded(String url, boolean isSuccess) {
              Drawable drawable = mIconListener.getDrawable(url);
              if (drawable == null) return;
              mUrlDrawableMap.put(iconStr, drawable);
              mHandler.sendEmptyMessage(0);
            }
          });
          downloader.Download();
        }
      }
      holder.icon.setImageDrawable(icon);

      holder.roomName.setText(twimptLogData.roomData.name);
      return v;//通常のログデータのviewを返す
    } else {
      //「ここまで読んだ」タグの場合
      int id = convertView == null ? 0 : convertView.getId();
      if (convertView == null || id != R.layout.read_here) {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = inflater.inflate(R.layout.read_here, null);
      }
      return convertView;
    }
  }

  /**
   * TwimptLogDataのtextをデコードする
   * @param logData デコードするtwimptのデータ
   */
  public void TextDecode(TwimptLogData logData) {
    if (logData.decodedText != null) return;

    //      logData.text="<a href=\"http://twimpt.com/upload/original/20150110/CGMIadXM.png\" data-lightbox=\"uploaded-image\"><img src=\"http://twimpt.com/upload/thumbnail/20150110/256/CGMIadXM.png\" class=\"imageThumbnail\" /></a>";
    //    logData.text += "\n<a href=\"http://twimpt.com/upload/original/20150110/CGMIadXM.png\" data-lightbox=\"uploaded-image\"><img src=\"http://twimpt.com/upload/thumbnail/20150110/256/CGMIadXM.png\" class=\"imageThumbnail\" /></a>";
    //TODO デコードにraw_textを使った方がいいが全てのタグを変換するのが面倒なので今はtextの方を使う
    //imgタグのデコード
    String regex = "<a.+?href\\s?=\\s?\"(.*?)\".*?><img.+?src\\s?=\\s?\"(.*?)\".*?>";
    Pattern p = Pattern.compile(regex);
    Matcher m = p.matcher(logData.text);
    String text;
    if (m.find()) {
      ArrayList<String> url = new ArrayList<String>();
      do {
        url.add(m.group(1));
        //Logger.log(m.group(1));
      } while (m.find());
      logData.postedImageUrl = url.toArray(new String[url.size()]);
    }
    text = m.replaceAll("<a href=\"$1\">$1");
    logData.decodedText = Html.fromHtml(text);
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