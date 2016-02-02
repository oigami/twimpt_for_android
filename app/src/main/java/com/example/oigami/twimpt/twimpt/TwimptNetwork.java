package com.example.oigami.twimpt.twimpt;


import android.content.ContentValues;
import android.util.Base64;

import com.example.oigami.twimpt.debug.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by oigami on 2014/09/30
 */
public class TwimptNetwork {

  private static final String TWIMPT_API_URL = "http://api.twimpt.com/";
  private static final String TWIMPT_TWIST_URL = "http://twist.twimpt.com/";

  private static String ConvertInputStreamToString(InputStream is) throws IOException {
    InputStreamReader reader = new InputStreamReader(is);
    StringBuilder builder = new StringBuilder();
    char[] buf = new char[1024];
    int numRead;
    while (0 <= (numRead = reader.read(buf))) {
      builder.append(buf, 0, numRead);
    }
    return builder.toString();
  }

  private static JSONObject Request(HttpURLConnection con) throws IOException, JSONException {
    String str = ConvertInputStreamToString(con.getInputStream());
    Logger.log(str);
    JSONObject out_json = new JSONObject(str);
    Logger.log(out_json.toString(2));
    return out_json;
  }

  private static String CreatePostParameter(ContentValues postParam) throws UnsupportedEncodingException {
    Set<Map.Entry<String, Object>> s = postParam.valueSet();
    Iterator itr = s.iterator();
    Map.Entry me = (Map.Entry) itr.next();
    StringBuilder postStr = new StringBuilder();
    postStr.append(me.getKey()).append('=');
    if (me.getValue() != null)
      postStr.append(URLEncoder.encode(me.getValue().toString(), "utf-8"));
    while (itr.hasNext()) {
      me = (Map.Entry) itr.next();
      postStr.append('&').append(me.getKey()).append('=');
      if (me.getValue() != null)
        postStr.append(URLEncoder.encode(me.getValue().toString(), "utf-8"));
    }
    Logger.log(postStr.toString());
    return postStr.toString();
  }

  private static JSONObject HttpPostRequest(final ContentValues postParam, final String urlStr)
          throws IOException, JSONException {
    Logger.log(urlStr);
    URL url = new URL(urlStr);
    HttpURLConnection con = (HttpURLConnection) url.openConnection();

    con.setRequestMethod("POST");
    // 送信パラメータのエンコードを指定
    if (postParam != null) {
      con.setDoOutput(true);
      String postStr = CreatePostParameter(postParam);
      OutputStream os = con.getOutputStream();
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
      writer.write(postStr);
      writer.close();
    }

    return Request(con);
  }

  /**
   * twimptに投稿
   * @param accessToken       アクセストークン
   * @param accessTokenSecret アクセストークンシークレット
   * @param postType          "room",    ルームに書き込むとき<br>
   *                          "user",      ユーザーに対して書き込むとき<br>
   *                          "monologue", ひとりごとに書き込むとき<br>
   * @param postHash          ハッシュ値<br>
   *                          ユーザーのハッシュやルームのハッシュを指定
   * @param message           投稿したい文字列
   * @param updateType        投稿とは関係ない<br>
   *                          投稿後にupdateを行うので取得したいtypeを指定する
   * @param updateHash        投稿とは関係ない<br>
   *                          投稿後にupdateを行うので取得したいhashを指定する
   * @param latestLogHash     取得したログの中で最も新しいログのハッシュ<br>
   *                          ここの値で取得するデータの範囲が変わる
   * @param latestModifyHash  UpdateRequestで取得したlatest_modify_hash
   * @return jsonデータ UpdateRequestと同じデータ
   * @throws JSONException
   * @throws IOException
   */
  public static JSONObject PostRequest(final String accessToken, final String accessTokenSecret,
                                       final String postType, final String postHash, final String message,
                                       final String updateType, final String updateHash,
                                       final String latestLogHash, final String latestModifyHash)
          throws JSONException, IOException {
    // POSTパラメータ付きでPOSTリクエストを構築
    ContentValues post_params = new ContentValues();
    post_params.put("access_token", accessToken);
    post_params.put("access_token_secret", accessTokenSecret);
    post_params.put("post_type", postType);
    post_params.put("post_hash", postHash);
    post_params.put("post_message", message);
    post_params.put("update_type", updateType);
    post_params.put("update_hashes", updateHash);
    post_params.put("latest_log_hash", latestLogHash);
    post_params.put("latest_modify_hash", latestModifyHash);
    return HttpPostRequest(post_params, TWIMPT_API_URL + "logs/post");
  }

  /**
   * twimptの最新データ取得
   * @param updateType       "room", ルームに書き込むとき<br>
   *                         "user", ユーザーに対して書き込むとき<br>
   *                         "monologue", ひとりごとに書き込むとき<br>
   * @param updateHash       ハッシュ値<br>
   *                         ユーザーのハッシュやルームのハッシュを指定
   * @param latestLogHash    取得したログの中で最も新しいログのハッシュ
   *                         ここの値で取得できるデータの範囲が変わる
   * @param latestModifyHash UpdateRequestで取得したhash
   * @return jsonデータ
   * @throws JSONException
   * @throws IOException
   */
  public static JSONObject UpdateRequest(final String updateType, final String updateHash,
                                         final String latestLogHash, final String latestModifyHash)
          throws JSONException, IOException {
    ContentValues post_params = new ContentValues();
    post_params.put("update_type", updateType);
    post_params.put("update_hashes", updateHash);
    if (latestLogHash != null)
      post_params.put("latest_log_hash", latestLogHash);
    if (latestModifyHash != null)
      post_params.put("latest_modify_hash", latestModifyHash);
    return HttpPostRequest(post_params, TWIMPT_API_URL + "logs/update");
  }

  /**
   * twimptの過去ログを取得
   * @param updateType    "room", "user", "monologue", etc...
   * @param updateHash    user_hash, room_hash, etc...
   * @param oldestLogHash 取得したログの中で最も古いログのハッシュ
   *                      ここの値で取得できるデータの範囲が変わる
   * @return jsonデータ
   * @throws JSONException
   * @throws IOException
   */
  public static JSONObject LogRequest(final String updateType, final String updateHash,
                                      final String oldestLogHash) throws JSONException, IOException {
    ContentValues post_params = new ContentValues();
    post_params.put("update_type", updateType);
    post_params.put("update_hashes", updateHash);
    post_params.put("oldest_log_hash", oldestLogHash);
    return HttpPostRequest(post_params, TWIMPT_API_URL + "logs/log");
  }

  public static JSONObject ImageUploadRequest(byte[] imageBuf) throws IOException, JSONException {
    ContentValues params = new ContentValues();
    String encodedImageData = Base64.encodeToString(imageBuf, Base64.NO_WRAP);
    params.put("file_content", encodedImageData);
    return HttpPostRequest(params, TWIMPT_API_URL + "files/upload");
  }
  /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
                                      部屋情報関連
   - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

  public static JSONObject GetRecentRoom(final String urlStr, final int pageNum) throws IOException, JSONException {
    URL url = new URL(TWIMPT_API_URL + "rooms/recent/" + urlStr + "/" + pageNum);
    HttpURLConnection con = (HttpURLConnection) url.openConnection();
    Logger.log(TWIMPT_API_URL + "rooms/recent/" + url + "/" + pageNum);
    return Request(con);
  }

  public static JSONObject GetRecentCreatedRoom(final int pageNum) throws JSONException, IOException {
    return GetRecentRoom("created/", pageNum);
  }

  public static JSONObject GetRecentOpenedRoom(final int pageNum) throws JSONException, IOException {
    return GetRecentRoom("opened/", pageNum);
  }

  public static JSONObject GetRecentPostedRoom(final int pageNum) throws JSONException, IOException {
    return GetRecentRoom("posted/", pageNum);
  }

  /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
                                          認証関連
   - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

  public static JSONObject GetRequestToken(String apiKey, String apiKeySecret)
          throws IOException, JSONException {
    ContentValues post_params = new ContentValues();
    post_params.put("api_key", apiKey);
    post_params.put("api_key_secret", apiKeySecret);
    return HttpPostRequest(post_params, TWIMPT_API_URL + "request_token");
  }

  /**
   * @param apiID              api固有id 開発者ページで確認できる
   * @param requestToken       GetRequestTokenで取得したトークン
   * @param requestTokenSecret GetRequestTokenで取得したトークン
   * @return 認証するためのurl文字列
   */
  public static String GetAuthURL(String apiID, String requestToken, String requestTokenSecret) {
    return TWIMPT_TWIST_URL + "authorize/" + apiID +
           "/?request_token=" + requestToken + "&request_token_secret=" + requestTokenSecret;
  }

  public static JSONObject GetAccessToken(String apiKey, String apiKeySecret, String requestToken, String requestTokenSecret) throws IOException, JSONException {
    ContentValues post_params = new ContentValues();
    post_params.put("api_key", apiKey);
    post_params.put("api_key_secret", apiKeySecret);
    post_params.put("request_token", requestToken);
    post_params.put("request_token_secret", requestTokenSecret);
    return HttpPostRequest(post_params, TWIMPT_API_URL + "access_token");
  }

  /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
                                      webページ関連
   - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

  /**
   * idを使って最新ログを取得するときのみ使う
   * 一度呼んだ後は戻ってきたjsonの中のhashを使って最新ログを取得する
   * @param type "room" or "user" or "log"
   * @param id   webのurlに使われているid
   * @return jsonデータ
   * @throws IOException
   * @throws JSONException
   */
  public static JSONObject UpdateRequest(String type, String id) throws IOException, JSONException {
    String url = TWIMPT_API_URL + type + '/' + id;
    return Request((HttpURLConnection) new URL(url).openConnection());
  }

  /**
   * wevのurlパスを使って最新ログを取得するときのみ使う
   * 一度呼んだ後は戻ってきたjsonの中のhashを使って最新ログを取得する
   * @param path "room/vip3" or "user/nelie" or "public" or "monologue" etc...
   * @return jsonデータ
   * @throws IOException
   * @throws JSONException
   */
  public static JSONObject UpdateRequest(String path) throws IOException, JSONException {
    String url = TWIMPT_API_URL + path;
    return Request((HttpURLConnection) new URL(url).openConnection());
  }

  /**
   * webページのurlを取得
   * @param type ページのタイプ ("public" or"monologue" or "room" , )
   * @param id   ルームやユーザーのid（typeが"public" or "monologue"の場合は省略可）
   * @return webページのurl
   */
  public static String GetWebPage(String type, String id) {
    if (type.equals("public") || type.equals("monologue")) {
      return (TWIMPT_TWIST_URL + type);
    } else {
      return (TWIMPT_TWIST_URL + type + "/" + id);
    }
  }
}
