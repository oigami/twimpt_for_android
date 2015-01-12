package com.example.oigami.twimpt;

import android.util.Log;

import com.example.oigami.twimpt.debug.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by oigami on 2014/09/30
 */
public class Twimpt {

  private static final String TWIMPT_API_URL = "http://api.twimpt.com/";

  private static JSONObject Request(final List<NameValuePair> postParam, final String url) throws IOException, JSONException {
    HttpPost request = new HttpPost(url);
    DefaultHttpClient client = new DefaultHttpClient();
    // 送信パラメータのエンコードを指定
    request.setEntity(new UrlEncodedFormEntity(postParam, "UTF-8"));
    final HttpResponse response = client.execute(request);
    // レスポンスヘッダーの取得(ファイルが無かった場合などは404)
    Log.d("Twimpt.java", "StatusCode=" + response.getStatusLine().getStatusCode());

    HttpEntity entity = response.getEntity();
    JSONObject out_json;
    if (entity == null)
      return null;
    String str = EntityUtils.toString(entity);
    Logger.log(url);
    out_json = new JSONObject(str);
    Logger.log(out_json.toString(2));
    return out_json;
  }

  /**
   * twimptに投稿
   * @param access_token        アクセストークン
   * @param access_token_secret アクセストークンシークレット
   * @param post_type           "room",    ルームに書き込むとき<br>
   *                            "user",      ユーザーに対して書き込むとき<br>
   *                            "monologue", ひとりごとに書き込むとき<br>
   * @param post_hash           ハッシュ値<br>
   *                            ユーザーのハッシュやルームのハッシュを指定
   * @param message             投稿したい文字列
   * @param update_type         投稿とは関係ない<br>
   *                            投稿後にupdateを行うので取得したいtypeを指定する
   * @param update_hash         投稿とは関係ない<br>
   *                            投稿後にupdateを行うので取得したいhashを指定する
   * @param latest_log_hash     取得したログの中で最も新しいログのハッシュ<br>
   *                            ここの値で取得するデータの範囲が変わる
   * @param latest_modify_hash  UpdateRequestで取得したlatest_modify_hash
   * @return jsonデータ UpdateRequestと同じデータ
   * @throws JSONException
   * @throws IOException
   */
  public static JSONObject PostRequest(final String access_token, final String access_token_secret,
                                       final String post_type, final String post_hash, final String message,
                                       final String update_type, final String update_hash, final String latest_log_hash,
                                       final String latest_modify_hash) throws JSONException, IOException {
    // POSTパラメータ付きでPOSTリクエストを構築
    List<NameValuePair> post_params = new ArrayList<NameValuePair>();
    post_params.add(new BasicNameValuePair("access_token", access_token));
    post_params.add(new BasicNameValuePair("access_token_secret", access_token_secret));
    post_params.add(new BasicNameValuePair("post_type", post_type));
    post_params.add(new BasicNameValuePair("post_hash", post_hash));
    post_params.add(new BasicNameValuePair("post_message", message));
    post_params.add(new BasicNameValuePair("update_type", update_type));
    post_params.add(new BasicNameValuePair("update_hashes", update_hash));
    post_params.add(new BasicNameValuePair("latest_log_hash", latest_log_hash));
    post_params.add(new BasicNameValuePair("latest_modify_hash", latest_modify_hash));
    return Request(post_params, TWIMPT_API_URL + "logs/post");
  }

  /**
   * twimptの最新データ取得
   * @param update_type        "room", ルームに書き込むとき<br>
   *                           "user", ユーザーに対して書き込むとき<br>
   *                           "monologue", ひとりごとに書き込むとき<br>
   * @param update_hash        ハッシュ値<br>
   *                           ユーザーのハッシュやルームのハッシュを指定
   * @param latest_log_hash    取得したログの中で最も新しいログのハッシュ ここの値で取得するデータの範囲が変わる
   * @param latest_modify_hash UpdateRequestで取得したhash
   * @return jsonデータ
   * @throws JSONException
   * @throws IOException
   */
  public static JSONObject UpdateRequest(final String update_type, final String update_hash,
                                         final String latest_log_hash, final String latest_modify_hash) throws JSONException, IOException {
    List<NameValuePair> post_params = new ArrayList<NameValuePair>();
    post_params.add(new BasicNameValuePair("update_type", update_type));
    post_params.add(new BasicNameValuePair("update_hashes", update_hash));
    if (latest_log_hash != null)
      post_params.add(new BasicNameValuePair("latest_log_hash", latest_log_hash));
    if (latest_modify_hash != null)
      post_params.add(new BasicNameValuePair("latest_modify_hash", latest_modify_hash));
    return Request(post_params, TWIMPT_API_URL + "logs/update");

  }

  /**
   * twimptの過去ログを取得
   * @param update_type     "room", "user", "monologue", etc...
   * @param update_hash     user_hash, room_hash, etc...
   * @param oldest_log_hash 取得したログの中で最も古いログのハッシュ ここの値で取得するデータの範囲が変わる
   * @return jsonデータ
   * @throws JSONException
   * @throws IOException
   */
  public static JSONObject LogRequest(final String update_type, final String update_hash,
                                      final String oldest_log_hash) throws JSONException, IOException {
    ArrayList<NameValuePair> post_params = new ArrayList<NameValuePair>();
    post_params.add(new BasicNameValuePair("update_type", update_type));
    post_params.add(new BasicNameValuePair("update_hashes", update_hash));
    post_params.add(new BasicNameValuePair("oldest_log_hash", oldest_log_hash));
    return Request(post_params, TWIMPT_API_URL + "logs/log");
  }

  public static JSONObject GetRecentRoomDataList(final String url, final int page_num) throws IOException, JSONException {
    DefaultHttpClient client = new DefaultHttpClient();
    HttpGet request = new HttpGet(TWIMPT_API_URL + "rooms/recent/" + url + "/" + page_num);
    final HttpResponse response = client.execute(request);
    // レスポンスヘッダーの取得(ファイルが無かった場合などは404)
    Logger.log("StatusCode=" + response.getStatusLine().getStatusCode());
    HttpEntity entity = response.getEntity();
    if (entity == null)
      return null;
    String str = EntityUtils.toString(entity);
    Logger.log(TWIMPT_API_URL + "rooms/recent/" + url + "/" + page_num);
    JSONObject out_json;
    out_json = new JSONObject(str);
    Logger.log(out_json.toString(2));
    return out_json;
  }

  public static JSONObject GetRecentCreatedRoomDataList(final int page_num) throws JSONException, IOException {
    return GetRecentRoomDataList("created/", page_num);
  }

  public static JSONObject GetRecentOpenedRoomDataList(final int page_num) throws JSONException, IOException {
    return GetRecentRoomDataList("opened/", page_num);
  }

  public static JSONObject GetRecentPostedRoomDataList(final int page_num) throws JSONException, IOException {
    return GetRecentRoomDataList("posted/", page_num);
  }

  /*
      ここから認証関係
   */

  public static JSONObject GetRequestToken(String apiKey, String apiKeySecret) throws IOException, JSONException {
    ArrayList<NameValuePair> post_params = new ArrayList<NameValuePair>();
    post_params.add(new BasicNameValuePair("api_key", apiKey));
    post_params.add(new BasicNameValuePair("api_key_secret", apiKeySecret));
    return Request(post_params, TWIMPT_API_URL + "request_token");
  }

  /**
   * @param apiID              api固有id 開発者ページで確認できる
   * @param requestToken       GetRequestTokenで取得したトークン
   * @param requestTokenSecret GetRequestTokenで取得したトークン
   * @return 認証するためのurl文字列
   */
  public static String GetAuthURL(String apiID, String requestToken, String requestTokenSecret) {
    return "http://twist.twimpt.com/authorize/" + apiID +
            "/?request_token=" + requestToken + "&request_token_secret=" + requestTokenSecret;
  }

  public static JSONObject GetAccessToken(String apiKey, String apiKeySecret,
                                          String requestToken, String requestTokenSecret) throws IOException, JSONException {
    ArrayList<NameValuePair> post_params = new ArrayList<NameValuePair>();
    post_params.add(new BasicNameValuePair("api_key", apiKey));
    post_params.add(new BasicNameValuePair("api_key_secret", apiKeySecret));
    post_params.add(new BasicNameValuePair("request_token", requestToken));
    post_params.add(new BasicNameValuePair("request_token_secret", requestTokenSecret));
    return Request(post_params, TWIMPT_API_URL + "access_token");
  }

  //webページ関連

  /**
   * webページのurlを取得
   * @param type ページのタイプ
   * @param hash ハッシュ値（typeが"public" or "monologue"の場合は省略可）
   * @param id   ルームのid（typeが"public" or "monologue"の場合は省略可）
   * @return webページのurl
   */
  public static String GetWebPage(String type, String hash, String id) {
    if (type.equals("public") || type.equals("monologue")) {
      return ("http://twist.twimpt.com/" + type);
    } else {
      return ("http://twist.twimpt.com/" + type + "/" + id);
    }
  }
}
