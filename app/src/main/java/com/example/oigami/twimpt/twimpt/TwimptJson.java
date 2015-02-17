package com.example.oigami.twimpt.twimpt;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by oigami on 2014/10/03
 */

public class TwimptJson {
  public static class ParsedData {
    public String latestModifyHash;
    public TwimptLogData[] mTwimptLogData;
    public Map<String, TwimptRoom> mTwimptRooms = new HashMap<String, TwimptRoom>();
    //public TwimptRoom[] mTwimptRooms;
  }

  public static TwimptLogData TwimptLogDataParse(Map<String, TwimptRoom> twimptRooms, JSONObject logData) throws JSONException {
    TwimptLogData twimptLogData = new TwimptLogData();
    twimptLogData.text = logData.getString("text");
    twimptLogData.time = logData.getInt("time");
    //ret.text.replaceAll("<br />", "\n");
    twimptLogData.hash = logData.getString("hash");
    {
      JSONObject user_data = logData.getJSONObject("user_data");
      twimptLogData.name = user_data.getString("name");
      twimptLogData.icon = user_data.getString("icon");
    }
    if (!logData.isNull("room_data")) {
      JSONObject room_data = logData.getJSONObject("room_data");
      final String roomDataHash = room_data.getString("hash");
      if (twimptRooms.containsKey(roomDataHash)) {
        twimptLogData.roomData = twimptRooms.get(roomDataHash);
        return twimptLogData;
      }

      TwimptRoom twimptRoom = new TwimptRoom();
      twimptRoom.hash = roomDataHash;
      twimptRoom.name = room_data.getString("name");
      twimptRoom.id = room_data.getString("id");
      twimptRoom.type = "room";
      twimptRooms.put(roomDataHash, twimptRoom);
      twimptLogData.roomData = twimptRoom;
    } else {
      final String roomDataHash = "monologue";
      if (twimptRooms.containsKey(roomDataHash)) {
        twimptLogData.roomData = twimptRooms.get(roomDataHash);
        return twimptLogData;
      }
      TwimptRoom twimptRoom = new TwimptRoom();
      twimptRoom.name = "ひとりごと";
      twimptRoom.type = "monologue";
      twimptRooms.put(roomDataHash, twimptRoom);
      twimptLogData.roomData = twimptRoom;
    }
    return twimptLogData;
  }

  public static ParsedData UpdateLogParse(JSONObject json) throws JSONException {
    ParsedData parsedData = new ParsedData();
    if (!json.isNull("latest_modify_hash")) {
      parsedData.latestModifyHash = json.getString("latest_modify_hash");
    }
    JSONArray log_data_list = json.getJSONArray("log_data_list");
    parsedData.mTwimptLogData = new TwimptLogData[log_data_list.length()];
    for (int i = 0; i < log_data_list.length(); i++) {
      JSONObject log_data = log_data_list.getJSONObject(i);
      parsedData.mTwimptLogData[i] = TwimptLogDataParse(parsedData.mTwimptRooms, log_data);
      //twimpt_room.dataList.add(0, twimpt_log_data);
    }
    return parsedData;
  }

  public static ParsedData UpdateParse(JSONObject json) throws JSONException {
    return UpdateLogParse(json);
  }

  public static ParsedData LogParse(JSONObject json) throws JSONException {
    return UpdateLogParse(json);
  }

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

