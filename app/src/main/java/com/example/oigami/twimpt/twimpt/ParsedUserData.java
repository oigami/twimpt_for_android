package com.example.oigami.twimpt.twimpt;

import com.example.oigami.twimpt.twimpt.room.TwimptRoom;
import com.example.oigami.twimpt.twimpt.room.UserRoom;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ConcurrentHashMap;

public class ParsedUserData extends ParsedData {
  public TwimptRoom nowUserRoomData;

  public ParsedUserData(JSONObject json, final ConcurrentHashMap<String, TwimptRoom> twimptRooms) throws JSONException {
    super(json, twimptRooms);
    putUserDataIfAbsent(json, twimptRooms);
  }

  private void putUserDataIfAbsent(JSONObject json, final ConcurrentHashMap<String, TwimptRoom> twimptRooms) throws JSONException {
    JSONObject user_data = json.getJSONObject("user_data");
    String hash = user_data.getString("hash");
    if (twimptRooms.containsKey(hash)) {
      nowUserRoomData = twimptRooms.get(hash);
    } else if (mTwimptRooms.containsKey(hash)) {
      nowUserRoomData = mTwimptRooms.get(hash);
    } else {
      TwimptRoom userRoom = UserRoom.create(user_data);
      nowUserRoomData = userRoom;
      mTwimptRooms.put(hash, userRoom);
    }
  }

  @Override
  protected void Parse(JSONObject json, final ConcurrentHashMap<String, TwimptRoom> twimptRooms) throws JSONException {
    super.Parse(json, twimptRooms);
    putUserDataIfAbsent(json, twimptRooms);
  }
}
