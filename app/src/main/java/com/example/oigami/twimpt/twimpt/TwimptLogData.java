package com.example.oigami.twimpt.twimpt;

import android.graphics.drawable.Drawable;
import android.util.Pair;

import java.util.LinkedHashMap;
import java.util.List;

public class TwimptLogData {
  /** 投稿したユーザーの名前 */
  public String name;
  /** 書き込まれたテキスト */
  public String text;
  /** 投稿情報のハッシュ値 */
  public String hash;
  /** アイコンのurl */
  public String icon;
  /** 投稿時間（unix時間） */
  public long time;

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
  public Drawable iconDrawable;
}
