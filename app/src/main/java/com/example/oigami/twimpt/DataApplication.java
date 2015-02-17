package com.example.oigami.twimpt;

import android.app.Application;

import com.example.oigami.twimpt.twimpt.TwimptRoom;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by oigami on 2014/10/02
 */
public class DataApplication extends Application {
  Map<String, TwimptRoom> twimptRooms = new HashMap<String, TwimptRoom>();
  //String now_room_hash;
}

