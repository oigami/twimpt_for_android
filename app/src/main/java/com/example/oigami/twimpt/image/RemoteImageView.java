package com.example.oigami.twimpt.image;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

/**
 * Created by oigami on 2014/10/02.
 */
abstract public class RemoteImageView  {
  public Context cxt;
  private static final String TAG = "RemoteImageView";

  public static final int IMG_DOWNLOADING = 1;

  public final Handler mHandler = new Handler(){

    @Override
    public void handleMessage(Message msg) {
      // 1秒毎にキャッシュからヒットするか検索する
      // ヒットしたら、検索をやめる
      // 画像表示に10回挑戦してダメだったらフラグを立てるとかしないとマズい
      if (msg.what == IMG_DOWNLOADING) {
        final String url = (String)msg.obj;
        int count = msg.arg1;
        Log.d(TAG, url);
        ImageCacheDB db = ImageCacheDB.getInstance(cxt);
        if (url != null && !url.equals("")) {
          final Cursor c = db.existsFile(url);
          if (c.moveToFirst()) {
            final String filename = c.getString(c.getColumnIndex(ImageCacheDB.CacheColumn.NAME));
            final String type = c.getString(c.getColumnIndex(ImageCacheDB.CacheColumn.TYPE));
            if (type.equals("image/jpg")
                    || type.equals("image/jpeg")
                    || type.equals("image/png")
                    || type.equals("image/gif")) {
              Drawable drawable = Drawable.createFromPath(cxt.getFileStreamPath(filename).getAbsolutePath());
              //setImageDrawable(drawable);
              //setVisibility(RemoteImageView.VISIBLE);
            } else {
              // 表示できる類いではない
              setImageNotFound();
            }
          } else {
            if (count <= 10) {
              setImageNowLoading();
              msg = obtainMessage(IMG_DOWNLOADING, ++count, 0, url);
              long current = SystemClock.uptimeMillis();
              long nextTime = current + 1000;
              sendMessageAtTime(msg, nextTime);
            } else {
              // チャレンジ10回して失敗したので失敗扱い
              setImageNotFound();
            }
          }
          c.close();
        } else {
          setImageNotFound();
        }
      }
    }
  };

  public RemoteImageView(Context context) {
    cxt=(context);
  }

  abstract public void setImageNotFound();

  abstract public void setImageNowLoading();
}
