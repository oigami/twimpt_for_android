package com.example.oigami.twimpt.twimpt.token;

import com.example.oigami.twimpt.twimpt.TwimptNetException;

import org.json.JSONException;
import org.json.JSONObject;

public class RequestTokenData {
  public String token;
  public String secret;

  public RequestTokenData() {}

  public RequestTokenData(JSONObject json) throws JSONException, TwimptNetException {
    if (!json.isNull("error_code")) {
      throw new TwimptNetException("error_code:" + json.getInt("error_code") + "\n" + "error_type:" + json.getString("error_type"));
    }
    token = json.getString("request_token");
    secret = json.getString("request_token_secret");
  }
}
