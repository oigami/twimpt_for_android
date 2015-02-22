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
import android.text.Layout;
import android.text.Spannable;
import android.text.style.ClickableSpan;
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
import com.example.oigami.twimpt.twimpt.TwimptLogData;
import com.example.oigami.twimpt.twimpt.room.TwimptRoom;

import java.util.ArrayList;
import java.util.HashMap;
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
  private Handler mHandler;
  private int mReadHere;
  TwimptImageAdapter.DrawableListener mIconListener;
  TwimptImageAdapter.DrawableListener mImageListener;
  AdapterView.OnItemClickListener mImageClickListener;

  public void setOnImageItemClickListener(AdapterView.OnItemClickListener listener) {
    mImageClickListener = listener;
  }

  public void reset(Context context,
                    TwimptRoom nowRoom,
                    TwimptImageAdapter.DrawableListener iconListener,
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
        holder.text.setOnTouchListener(new View.OnTouchListener() {
          @Override
          public boolean onTouch(View v, MotionEvent event) {
            CharSequence text = ((TextView) v).getText();
            Spannable spannable = Spannable.Factory.getInstance().newSpannable(text);
            TextView widget = (TextView) v;
            int action = event.getAction();

            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
              int x = (int) event.getX();
              int y = (int) event.getY();

              x -= widget.getTotalPaddingLeft();
              y -= widget.getTotalPaddingTop();

              x += widget.getScrollX();
              y += widget.getScrollY();

              Layout layout = widget.getLayout();
              int line = layout.getLineForVertical(y);
              int off = layout.getOffsetForHorizontal(line, x);

              if (x < layout.getLineMax(line)) {
                ClickableSpan[] link = spannable.getSpans(off, off, ClickableSpan.class);
                if (link.length != 0) {
                  if (action == MotionEvent.ACTION_UP) {
                    link[0].onClick(widget);
                  }
                  return true;
                }
              }
            }
            return false;
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

      holder.name.setText(twimptLogData.user.name);

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