package com.example.oigami.twimpt;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.ImageView;

/**
 * Created by oigami on 2015/01/11
 */
public class ImageViewActivity extends ActionBarActivity {
  static final String INTENT_BITMAP_BYTE_ARRAY="BITMAP_BYTE_ARRAY";
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.image_view);
    Intent i = getIntent();
    byte bitmapByte[]=i.getByteArrayExtra(INTENT_BITMAP_BYTE_ARRAY);
    BitmapFactory.Options options = new BitmapFactory.Options();
    Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapByte, 0, bitmapByte.length, options);
    ImageView imageView=(ImageView)findViewById(R.id.image_view);
    imageView.setImageBitmap(bitmap);
    //mNowRoomHash = i.getParcelableExtra()Extra(INTENT_DRAWABLE);
  }
}