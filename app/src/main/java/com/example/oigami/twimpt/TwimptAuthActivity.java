package com.example.oigami.twimpt;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import com.example.oigami.twimpt.debug.Logger;
import com.example.oigami.twimpt.twimpt.TwimptNetwork;
import com.example.oigami.twimpt.twimpt.token.AccessTokenData;
import com.example.oigami.twimpt.twimpt.token.RequestTokenData;

import org.json.JSONObject;

/**
 * Created by oigami on 2014/10/05
 */
public class TwimptAuthActivity extends Activity {

  boolean mNowUpdate = false;
  Handler mHandler = new Handler();
  View button;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.auth_activity);
    RequestTokenData requestTokenData = TwimptToken.GetRequestToken(this);
    if (requestTokenData == null) {
      AuthRequest();
      button.setEnabled(false);
    }

    button = findViewById(R.id.auth_url_button);
    button.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        AuthRequest();
        v.setEnabled(false);
        // クリック時の処理
        //        RequestTokenData requestToken = TwimptToken.GetRequestToken(TwimptAuthActivity.this);
        //        Uri uri = Uri.parse(Twimpt.GetAuthURL(AUTH_ID, requestToken.token, requestToken.secret));
        //        Intent i = new Intent(Intent.ACTION_VIEW, uri);
        //        startActivity(i);

      }
    });
  }

  @Override
  public void onStart() {
    super.onStart();
    Intent intent = getIntent();
    if (Intent.ACTION_VIEW.equals(intent.getAction())) {//intentによって起動
      CheckAuth();
    }
  }

  public void AuthRequest() {
    if (mNowUpdate) return;
    mNowUpdate = true;
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          final JSONObject json = TwimptNetwork.GetRequestToken(TwimptDeveloperData.API_KEY, TwimptDeveloperData.API_KEY_SECRET);
          Logger.log(json.toString(2));
          final RequestTokenData requestTokenData = new RequestTokenData(json);
          mHandler.post(new Runnable() {
            @Override
            public void run() {
              button.setEnabled(true);
              TwimptToken.SetRequestToken(TwimptAuthActivity.this, requestTokenData);
              GoAuthWebPage(requestTokenData);
            }
          });
        } catch (final Exception e) {
          e.printStackTrace();
          mHandler.post(new Runnable() {
            @Override
            public void run() {
              Toast.makeText(TwimptAuthActivity.this, e.getMessage() + "\n\n" + Logger.getStackTraceString(e), Toast.LENGTH_LONG).show();
            }
          });
        }
        mNowUpdate = false;
      }
    }).start();
  }

  private void CheckAuth() {
    if (mNowUpdate) return;
    mNowUpdate = true;
    new Thread(new Runnable() {
      @Override
      public void run() {
        RequestTokenData requestToken = TwimptToken.GetRequestToken(TwimptAuthActivity.this);
        try {
          final JSONObject json = TwimptNetwork.GetAccessToken(TwimptDeveloperData.API_KEY, TwimptDeveloperData.API_KEY_SECRET, requestToken.token, requestToken.secret);
          final AccessTokenData accessTokenData = new AccessTokenData(json);
          mHandler.post(new Runnable() {
            @Override
            public void run() {
              TwimptToken.SetAccessToken(TwimptAuthActivity.this, accessTokenData);
              StartRoomActivity();
            }
          });
        } catch (final Exception e) {
          e.printStackTrace();
          mHandler.post(new Runnable() {
            @Override
            public void run() {
              Toast.makeText(TwimptAuthActivity.this, e.getMessage() + "\n\n" + e.getStackTrace(), Toast.LENGTH_LONG).show();
            }
          });
        }
      }
    }).start();
  }

  private void GoAuthWebPage(RequestTokenData requestTokenData) {
    Uri uri = Uri.parse(TwimptNetwork.GetAuthURL(TwimptDeveloperData.AUTH_ID, requestTokenData.token, requestTokenData.secret));
    Intent i = new Intent(Intent.ACTION_VIEW, uri);
    startActivity(i);
  }

  private void StartRoomActivity() {
    Intent intent = new Intent(this, RoomActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(intent);
    finish();
  }
}
