package com.example.oigami.twimpt.twimpt.room;

public class GuestRoom {
  /** 投稿ユーザの一時的なID(1日毎に変わる） */
  //  public String host;
  private GuestRoom() {
  }

  public static TwimptRoom create(String host) {
    TwimptRoom twimptRoom = new TwimptRoom();
    twimptRoom.name = "ID:" + host;
    twimptRoom.id = host;
    twimptRoom.hash = host;
    twimptRoom.type = "guest";
    return twimptRoom;
  }
}
