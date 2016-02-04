package com.example.oigami.twimpt.twimpt;

import com.example.oigami.twimpt.twimpt.room.TwimptRoom;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ParsedData {
  public String latestModifyHash;
  public TwimptLogData[] mTwimptLogData;
  public Map<String, TwimptRoom> mTwimptRooms;

  //public TwimptRoom[] mTwimptRooms;
  public ParsedData() {
    mTwimptRooms = new HashMap<>();
  }

  public ParsedData(JSONObject json, final ConcurrentHashMap<String, TwimptRoom> twimptRooms) throws JSONException {
    mTwimptRooms = new HashMap<>();
    Parse(json, twimptRooms);
  }

  protected void Parse(JSONObject json, final ConcurrentHashMap<String, TwimptRoom> twimptRooms) throws JSONException {
    if (!json.isNull("latest_modify_hash")) {
      latestModifyHash = json.getString("latest_modify_hash");
    }
    JSONArray log_data_list = json.getJSONArray("log_data_list");
    mTwimptLogData = new TwimptLogData[log_data_list.length()];
    TwimptLogData.TwimptRoomParseListener listener = new TwimptLogData.TwimptRoomParseListener() {
      @Override
      public TwimptRoom TwimptRoomParse(JSONObject room_data) throws JSONException {
        TwimptRoom twimptRoom = null;
        if (room_data == null) {
          final String roomDataHash = "monologue";
          if (twimptRooms.containsKey(roomDataHash))
            return twimptRooms.get(roomDataHash);
          if (mTwimptRooms.containsKey(roomDataHash))
            return mTwimptRooms.get(roomDataHash);
          twimptRoom = new TwimptRoom();
          twimptRoom.name = "ひとりごと";
          twimptRoom.type = "monologue";
          mTwimptRooms.put(roomDataHash, twimptRoom);
          return twimptRoom;
        }

        final String roomDataHash = room_data.getString("hash");
        if (roomDataHash.length() != 0) {
          if (twimptRooms.containsKey(roomDataHash))
            return twimptRooms.get(roomDataHash);
          if (mTwimptRooms.containsKey(roomDataHash))
            return mTwimptRooms.get(roomDataHash);
        }
        twimptRoom = new TwimptRoom(roomDataHash, room_data);
        mTwimptRooms.put(roomDataHash, twimptRoom);
        return twimptRoom;
      }
    };
    for (int i = 0; i < log_data_list.length(); i++) {
      JSONObject log_data = log_data_list.getJSONObject(i);
      TwimptLogData data = new TwimptLogData(log_data, listener);
      mTwimptLogData[i] = data;
      String[] userHash = {data.user.hash, data.host.hash};
      TwimptRoom[] twimptRoom = {data.user, data.host};
      for (int j = 0; j < userHash.length; j++) {
        if (twimptRooms.containsKey(userHash[j]))
          continue;
        if (mTwimptRooms.containsKey(userHash[j]))
          continue;
        mTwimptRooms.put(userHash[j], twimptRoom[j]);
      }

      //twimpt_room.dataList.add(0, twimpt_log_data);
    }
  }
}
