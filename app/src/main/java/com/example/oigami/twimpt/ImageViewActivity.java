package com.example.oigami.twimpt;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;

/**
 * Created by oigami on 2015/01/11
 */
public class ImageViewActivity extends ActionBarActivity {
  //static final String INTENT_BITMAP_BYTE_ARRAY = "BITMAP_BYTE_ARRAY";
  static final String INTENT_DRAWABLE_FILENAME = "DRAWABLE_FILENAME";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.image_view);
    Intent i = getIntent();
    String filename = i.getStringExtra(INTENT_DRAWABLE_FILENAME);
    filename = this.getFileStreamPath(filename).getAbsolutePath();
    ImageView imageView = (ImageView) findViewById(R.id.image_view);
    imageView.setImageDrawable(Drawable.createFromPath(filename));
  }

  private Bitmap LoadToJustDisplayBitmap(byte[] bitmapByte) {
    Point size = new Point();
    DisplayMetrics display = this.getResources().getDisplayMetrics();
    size.x = display.widthPixels;
    size.y = display.heightPixels;
    Bitmap bitmap = loadSavedSizeBitmap(Math.max(size.x, size.y), bitmapByte);
    bitmapByte = null;
    return bitmap;
  }

  public static Bitmap loadSavedSizeBitmap(int maxSize, byte[] data) {
    BitmapFactory.Options option = new BitmapFactory.Options();
    //サイズだけロードするように設定して
    option.inJustDecodeBounds = true;
    //サイズ情報だけロード
    BitmapFactory.decodeByteArray(data, 0, data.length, option);
    int w = option.outWidth;
    int h = option.outHeight;
    //大きい辺に合わせて読み込みスケールを調整
    if (maxSize < Math.max(w, h)) {
      option.inSampleSize = Math.max(w, h) / maxSize + 1;
    }
    //今度こそ読み込む
    option.inJustDecodeBounds = false;
    return BitmapFactory.decodeByteArray(data, 0, data.length, option);
  }

  public static byte[] ConvertDrawableToByteArray(Drawable drawable) {
    if (drawable == null) throw new NullPointerException("drawable is null");
    Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
    //bitmapをbyte[]にする
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos)) {
      return null;
    }
    return bos.toByteArray();
  }
}