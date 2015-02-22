package com.example.oigami.twimpt.twimpt;

import com.example.oigami.twimpt.twimpt.room.GuestRoom;
import com.example.oigami.twimpt.twimpt.room.TwimptRoom;
import com.example.oigami.twimpt.twimpt.room.UserRoom;

import org.json.JSONException;
import org.json.JSONObject;

public class TwimptLogData {
  /** ユーザーデータ */
  public TwimptRoom user;
  /** 書き込まれたテキスト */
  public String text;
  /** 投稿情報のハッシュ値 */
  public String hash;
  /** 一時的なidのデータ */
  public TwimptRoom host;
  /** 投稿時間（unix時間） */
  public long time;

  /** ユーザの名前 */
  public String name;
  /** アイコンのurl */
  public String icon;
  /** ルームのハッシュ値 */
  //  public String roomHash;
  /** 投稿先のルームデータ */
  public TwimptRoom roomData;

  /** textをデコードした後の、実際に表示するテキストデータ */
  public CharSequence decodedText;
  /**
   * 投稿された画像urlを保持
   * nullの場合は画像なし
   */
  public String[] postedImageUrl;
  //  public Drawable iconDrawable;

  public TwimptLogData(JSONObject logData, TwimptRoomParseListener roomParseListener) throws JSONException {
    Parse(logData, roomParseListener);
  }

  public interface TwimptRoomParseListener {
    /** room_dataがnullのばあいはmonologue */
    public TwimptRoom OnTwimptRoomParse(JSONObject room_data) throws JSONException;
  }

  public void Parse(JSONObject logData, TwimptRoomParseListener roomParseListener) throws JSONException {
    text = logData.getString("text");
    time = logData.getInt("time");
    //ret.text.replaceAll("<br />", "\n");
    hash = logData.getString("hash");
    host = GuestRoom.create(logData.getString("host"));
    {
      JSONObject user_data = logData.getJSONObject("user_data");
      //ユーザーデータもルームと同じ扱いにする
      //      TwimptRoom userData = roomParseListener.OnTwimptRoomParse(user_data);
      //      //ユーザデータなので書き換える
      //      userData.type = "user";
      //      if (userData.hash.length() == 0)
      //        userData.hash = null;

      user = UserRoom.create(user_data);
      name = user.name;
      icon = user_data.getString("icon");
    }
    JSONObject room_data = null;
    if (!logData.isNull("room_data")) {
      room_data = logData.getJSONObject("room_data");
    }
    roomData = roomParseListener.OnTwimptRoomParse(room_data);

  }
}
