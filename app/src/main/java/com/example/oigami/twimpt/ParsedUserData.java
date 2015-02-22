package com.example.oigami.twimpt;

import com.example.oigami.twimpt.twimpt.room.TwimptRoom;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ConcurrentHashMap;

public  class ParsedUserData extends ParsedData {
  public TwimptRoom nowUserRoomData;
  public ParsedUserData(JSONObject json, final ConcurrentHashMap<String, TwimptRoom> twimptRooms) throws JSONException {
    super(json, twimptRooms);
    UserDataParse(json, twimptRooms);
  }
  private void UserDataParse(JSONObject json, final ConcurrentHashMap<String, TwimptRoom> twimptRooms) throws JSONException {
    JSONObject room_data = json.getJSONObject("user_data");
    String hash = room_data.getString("hash");
    if (twimptRooms.containsKey(hash)) {
      nowUserRoomData = twimptRooms.get(hash);
    } else if (mTwimptRooms.containsKey(hash)) {
      nowUserRoomData = mTwimptRooms.get(hash);
    } else {
      TwimptRoom userRoom = new TwimptRoom(hash, room_data);
      userRoom.type="user";
      nowUserRoomData = userRoom;
      mTwimptRooms.put(hash, userRoom);
    }
  }

  @Override
  protected void Parse(JSONObject json, final ConcurrentHashMap<String, TwimptRoom> twimptRooms) throws JSONException {
    super.Parse(json, twimptRooms);
    UserDataParse(json, twimptRooms);
  }
}
