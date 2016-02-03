package com.example.oigami.twimpt;

import android.app.Application;
import android.graphics.drawable.Drawable;

import com.example.oigami.twimpt.twimpt.room.TwimptRoom;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by oigami on 2014/10/02
 */
public class DataApplication extends Application {
  ConcurrentHashMap<String, TwimptRoom> twimptRooms = new ConcurrentHashMap<String, TwimptRoom>();
  //String now_room_hash;
  public HashMap<String, Drawable> ImageCacheDrawable = new HashMap<String, Drawable>();

}

