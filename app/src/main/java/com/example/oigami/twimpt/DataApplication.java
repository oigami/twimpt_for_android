package com.example.oigami.twimpt;

import android.app.Application;

import com.example.oigami.twimpt.twimpt.room.TwimptRoom;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by oigami on 2014/10/02
 */
public class DataApplication extends Application {
  ConcurrentHashMap<String, TwimptRoom> twimptRooms = new ConcurrentHashMap<String, TwimptRoom>();
  //String now_room_hash;
}

