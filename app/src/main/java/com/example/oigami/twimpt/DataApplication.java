package com.example.oigami.twimpt;

import android.app.Application;
import android.graphics.drawable.Drawable;
import android.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by oigami on 2014/10/02
 */
public class DataApplication extends Application {
  Map<String, TwimptRoom> twimptRooms = new HashMap<String, TwimptRoom>();
  //String now_room_hash;
}

class TwimptLogData {
  public String name;
  public String text;
  /**
   * textをデコードした後の、実際に表示するテキストデータ
   */
  public CharSequence decodedText;
  public String hash;
  public String roomHash;
  public String icon;
  public long time;

  /**
   * 投稿された画像urlを保持
   * nullの場合は画像なし
   */
  List<Pair<String, Drawable>> postedImage;
  public Drawable iconDrawable;
}

 class TwimptRoom {
  public String name;
  public String type;
  public String hash;
  public String id;
  public String LatestModifyHash;
  public ArrayList<TwimptLogData> dataList = new ArrayList<TwimptLogData>();

  public String getLatestLogHash() {
    int size = dataList.size();
    return size > 0 ? dataList.get(0).hash : null;
  }

  public String getOldestLogHash() {
    int size = dataList.size();
    return size > 0 ? dataList.get(size - 1).hash : null;
  }
}

