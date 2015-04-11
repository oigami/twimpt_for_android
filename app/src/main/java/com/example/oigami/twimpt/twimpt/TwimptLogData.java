package com.example.oigami.twimpt.twimpt;

import android.text.Spannable;

import com.example.oigami.twimpt.twimpt.room.GuestRoom;
import com.example.oigami.twimpt.twimpt.room.TwimptRoom;
import com.example.oigami.twimpt.twimpt.room.UserRoom;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class TwimptLogData implements Serializable {
  /** ユーザーデータ */
  public TwimptRoom user;
  /** 書き込まれたテキスト(たぐhtmlに変換されている) */
  public String text;
  /** 書き込まれたテキスト(タグがそのままの状態) */
  public String rawText;
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
  public Spannable decodedText;
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
    /**
     * @param room_data nullの場合はmonologueのデータ
     * @return twimptのルームデータ
     * @throws JSONException
     */
    public TwimptRoom TwimptRoomParse(JSONObject room_data) throws JSONException;
  }

  public void TextParse(TwimptTextParser parser) {
    TwimptTextParser.ParsedTextData parsedText = parser.Parse(rawText);
    decodedText = parsedText.textSpan;
    postedImageUrl = parsedText.postedImageUrl;
  }

  public void Parse(JSONObject logData, TwimptRoomParseListener roomParseListener) throws JSONException {
    text = logData.getString("text");
    rawText = logData.getString("raw_text");
    time = logData.getInt("time");
    //ret.text.replaceAll("<br />", "\n");
    hash = logData.getString("hash");
    host = GuestRoom.create(logData.getString("host"));
    {
      JSONObject user_data = logData.getJSONObject("user_data");
      user = UserRoom.create(user_data);
      name = user.name;
      icon = user_data.getString("icon");
    }
    JSONObject room_data = null;
    if (!logData.isNull("room_data")) {
      room_data = logData.getJSONObject("room_data");
    }
    roomData = roomParseListener.TwimptRoomParse(room_data);
  }
}
