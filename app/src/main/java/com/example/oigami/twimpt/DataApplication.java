package com.example.oigami.twimpt;

import android.app.Application;
import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by oigami on 2014/10/02.
 */
public class DataApplication extends Application {
  Map<String, TwimptRoom> twimptRooms = new HashMap<String, TwimptRoom>();
  //String now_room_hash;
}


class TwimptLogData {
  public String name;
  public String text;
  public CharSequence decodedText;
  public String hash;
  public String roomHash;
  public String icon;
  public long time;
  public Bitmap iconBmp;
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

class RequestTokenData {
  String token;
  String secret;
}

class AccessTokenData {
  String token;
  String secret;
}