package com.example.oigami.twimpt.twimpt.room;

import org.json.JSONException;
import org.json.JSONObject;

public class UserRoom {
  private UserRoom() {
  }

  public static TwimptRoom create(JSONObject user_data) throws JSONException {
    TwimptRoom twimptRoom = new TwimptRoom();
    twimptRoom.name = user_data.getString("name");
    twimptRoom.hash = user_data.getString("hash");
    twimptRoom.type = "user";
    twimptRoom.id = user_data.getString("id");
    return twimptRoom;
  }
}
