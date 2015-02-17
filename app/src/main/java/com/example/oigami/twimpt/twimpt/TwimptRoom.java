package com.example.oigami.twimpt.twimpt;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class TwimptRoom {
  public TwimptRoom(){}
  public TwimptRoom(JSONObject roomData) throws JSONException {
    hash = roomData.getString("hash");
    name = roomData.getString("name");
    id = roomData.getString("id");
    type = "room";
  }
  /** ルームの名前 */
  public String name;
  /** ルームのタイプ */
  public String type;
  /** ルームのハッシュ値 */
  public String hash;
  /** ルームのurlの個別部分 */
  public String id;
  /** 投稿内容に変更があった時のためのハッシュ値 */
  public String LatestModifyHash;
  public ArrayList<TwimptLogData> dataList = new ArrayList<TwimptLogData>();

  /**
   * 最も最新のハッシュ値を返す
   * ログデータがない場合はnull
   * @return ハッシュ値 or null
   */
  public String getLatestLogHash() {
    int size = dataList.size();
    return size > 0 ? dataList.get(0).hash : null;
  }

  /**
   * 最も古いハッシュ値を返す
   * ログデータがない場合はnull
   * @return ハッシュ値 or null
   */
  public String getOldestLogHash() {
    int size = dataList.size();
    return size > 0 ? dataList.get(size - 1).hash : null;
  }
}
