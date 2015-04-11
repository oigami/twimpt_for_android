package com.example.oigami.twimpt.twimpt;


import com.example.oigami.twimpt.twimpt.room.TwimptRoom;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by oigami on 2014/10/03
 */

public class TwimptJson {

  public static void RecentRoomListParse(Map<String, TwimptRoom> twimptRoomMap, JSONObject json, ArrayList<String> list) throws JSONException {
    JSONArray room_data_list = json.getJSONArray("room_data_list");
    for (int i = 0; i < room_data_list.length(); i++) {
      JSONObject roomData = room_data_list.getJSONObject(i);
      String hash = roomData.getString("hash");
      list.add(hash);
      if (twimptRoomMap.get(hash) == null) {
        TwimptRoom tempRoom = new TwimptRoom();
        tempRoom.name = roomData.getString("name");
        tempRoom.id = roomData.getString("id");
        tempRoom.hash = hash;
        tempRoom.type = "room";
        twimptRoomMap.put(hash, tempRoom);
      }
    }
  }
}

