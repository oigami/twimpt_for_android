package com.example.oigami.twimpt;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by oigami on 2014/10/03.
 */

public class TwimptJson {
  static class ParsedData {
    public String latestModifyHash;
    public TwimptLogData[] mTwimptLogData;
    public Map<String, TwimptRoom> mTwimptRooms = new HashMap<String, TwimptRoom>();
    //public TwimptRoom[] mTwimptRooms;
  }

  public static void TwimptLogDataParse(TwimptLogData twimptLogData, Map<String, TwimptRoom> twimptRooms, JSONObject logData)
          throws JSONException {
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
      String hash = room_data.getString("hash");
      twimptLogData.roomHash = hash;
      TwimptRoom twimptRoom = twimptRooms.get(hash);
      if (twimptRoom == null) {
        twimptRoom = new TwimptRoom();
        twimptRoom.hash = hash;
        twimptRoom.name = room_data.getString("name");
        twimptRoom.id = room_data.getString("id");
        twimptRoom.type = "room";
        twimptRooms.put(hash, twimptRoom);
      }
    } else {
      final String hash = "monologue";
      twimptLogData.roomHash = hash;
      TwimptRoom twimptRoom = twimptRooms.get(hash);
      if (twimptRoom == null) {
        twimptRoom = new TwimptRoom();
        twimptRoom.hash = null;
        twimptRoom.name = "ひとりごと";
        twimptRoom.type = "monologue";
        twimptRooms.put(hash, twimptRoom);
      }
    }
  }

  public static ParsedData UpdateLogParse(JSONObject json) throws JSONException {
    ParsedData parsedData = new ParsedData();
    if (!json.isNull("latest_modify_hash")) {
      parsedData.latestModifyHash = json.getString("latest_modify_hash");
    }
    JSONArray log_data_list = json.getJSONArray("log_data_list");
    JSONObject log_data;
    parsedData.mTwimptLogData = new TwimptLogData[log_data_list.length()];
    for (int i =0;i< log_data_list.length(); i++) {
      log_data = log_data_list.getJSONObject(i);
      TwimptLogData twimpt_log_data = parsedData.mTwimptLogData[i] = new TwimptLogData();
      TwimptLogDataParse(twimpt_log_data, parsedData.mTwimptRooms, log_data);
      //twimpt_room.dataList.add(0, twimpt_log_data);
    }
    log_data_list = null;
    log_data = null;
    return parsedData;
  }
 public static ParsedData UpdateParse(JSONObject json) throws JSONException {
  return UpdateLogParse(json);
 }
  public static ParsedData LogParse(JSONObject json) throws JSONException {
    return UpdateLogParse(json);
  }

  public static void RecentRoomListParse(Map<String, TwimptRoom> twimptRoomMap, JSONObject json, ArrayList<String> list)
          throws JSONException {
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

  /*
  ここから認証関係
   */
  public static RequestTokenData RequestTokenParse(JSONObject json) throws JSONException {
    RequestTokenData outData = new RequestTokenData();
    outData.token = json.getString("request_token");
    outData.secret = json.getString("request_token_secret");
    return outData;
  }

  public static AccessTokenData AccessTokenParse(JSONObject json) throws JSONException {
    AccessTokenData outData = new AccessTokenData();
    outData.token = json.getString("access_token");
    outData.secret = json.getString("access_token_secret");
    return outData;
  }

}
