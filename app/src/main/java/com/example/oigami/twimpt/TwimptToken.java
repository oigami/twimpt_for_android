package com.example.oigami.twimpt;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.oigami.twimpt.twimpt.token.AccessTokenData;
import com.example.oigami.twimpt.twimpt.token.RequestTokenData;

/**
 * Created by oigami on 2015/01/11
 */
public class TwimptToken {
  /**
   * @param context activity
   * @return null or トークン
   */
  public static RequestTokenData GetRequestToken(Context context) {
    SharedPreferences sharedPref = context.getSharedPreferences("token", Context.MODE_PRIVATE);
    RequestTokenData reqToken = new RequestTokenData();
    reqToken.token = sharedPref.getString("request_token", null);
    reqToken.secret = sharedPref.getString("request_token_secret", null);
    if (reqToken.token == null || reqToken.secret == null)
      return null;
    return reqToken;
  }

  public static void SetRequestToken(Context context, RequestTokenData reqToken) {
    final SharedPreferences sharedPref = context.getSharedPreferences("token", Context.MODE_PRIVATE);
    SharedPreferences.Editor e = sharedPref.edit();
    e.putString("request_token", reqToken.token);
    e.putString("request_token_secret", reqToken.secret);
    e.commit();
  }

  public static void ClearRequestToken(Context context) {
    final SharedPreferences sharedPref = context.getSharedPreferences("token", Context.MODE_PRIVATE);
    SharedPreferences.Editor e = sharedPref.edit();
    e.remove("request_token");
    e.remove("request_token_secret");
    e.commit();
  }

  /**
   * @param context activity
   * @return null or トークン
   */
  public static AccessTokenData GetAccessToken(Context context) {
    SharedPreferences sharedPref = context.getSharedPreferences("token", Context.MODE_PRIVATE);
    AccessTokenData accessTokenData = new AccessTokenData();
    accessTokenData.token = sharedPref.getString("access_token", null);
    accessTokenData.secret = sharedPref.getString("access_token_secret", null);
    if (accessTokenData.token == null || accessTokenData.secret == null)
      return null;
    return accessTokenData;
  }

  public static void SetAccessToken(Context context, AccessTokenData accessToken) {
    final SharedPreferences sharedPref = context.getSharedPreferences("token", Context.MODE_PRIVATE);
    SharedPreferences.Editor e = sharedPref.edit();
    e.putString("access_token", accessToken.token);
    e.putString("access_token_secret", accessToken.secret);
    e.commit();
  }

  public static void ClearAccessToken(Context context) {
    final SharedPreferences sharedPref = context.getSharedPreferences("token", Context.MODE_PRIVATE);
    SharedPreferences.Editor e = sharedPref.edit();
    e.remove("access_token");
    e.remove("access_token_secret");
    e.commit();
  }
}

