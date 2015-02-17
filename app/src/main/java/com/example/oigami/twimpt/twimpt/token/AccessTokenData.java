package com.example.oigami.twimpt.twimpt.token;

import com.example.oigami.twimpt.twimpt.TwimptNetException;

import org.json.JSONException;
import org.json.JSONObject;

public class AccessTokenData {
  public String token;
  public String secret;

  public AccessTokenData() {}

  public AccessTokenData(JSONObject json) throws JSONException, TwimptNetException {
    if (!json.isNull("error_code")) {
      throw new TwimptNetException("error_code:" + json.getInt("error_code") + "\n" + json.getString("error_params"));
    }
    token = json.getString("access_token");
    secret = json.getString("access_token_secret");
  }
}
