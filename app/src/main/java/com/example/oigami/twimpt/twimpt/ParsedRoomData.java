package com.example.oigami.twimpt.twimpt;

import com.example.oigami.twimpt.twimpt.room.TwimptRoom;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ConcurrentHashMap;

public  class ParsedRoomData extends ParsedData {
  public TwimptRoom nowTwimptRoom;

  public ParsedRoomData(JSONObject json, final ConcurrentHashMap<String, TwimptRoom> twimptRooms) throws JSONException {
    super(json, twimptRooms);
    putRoomDataIfAbsent(json, twimptRooms);
  }

  private void putRoomDataIfAbsent(JSONObject json, final ConcurrentHashMap<String, TwimptRoom> twimptRooms) throws JSONException {
    JSONObject room_data = json.getJSONObject("room_data");
    String hash = room_data.getString("hash");
    if (twimptRooms.containsKey(hash)) {
      nowTwimptRoom = twimptRooms.get(hash);
    } else if (mTwimptRooms.containsKey(hash)) {
      nowTwimptRoom = mTwimptRooms.get(hash);
    } else {
      TwimptRoom twimptRoom = new TwimptRoom(hash, room_data);
      nowTwimptRoom = twimptRoom;
      mTwimptRooms.put(hash, twimptRoom);
    }
  }

  @Override
  protected void Parse(JSONObject json, final ConcurrentHashMap<String, TwimptRoom> twimptRooms) throws JSONException {
    super.Parse(json, twimptRooms);
    putRoomDataIfAbsent(json, twimptRooms);
  }
}
