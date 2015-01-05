package com.example.oigami.twimpt;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by oigami on 2014/10/01.
 */

enum TwimptAsyncType {
  UPDATE,
  POST,
  LOG,
  RECENT_CREATED_ROOM,
}

class TwimptAsyncTaskParams {
  TwimptRoom mTwimptRoom;
  JSONObject mJSONObjecton;
}

public class TwimptAsyncTask extends AsyncTask<Void, Void, TwimptAsyncTaskParams> {
  private TwimptRoom mTwimptRoom;
  private TwimptAsyncType mTwimptUpdateType;
  private Handler mHandler;
  private String mPostMessage;
  private DataApplication mGlobals;
  private String mAccessToken = "", mAccessTokenSecret = "";


  TwimptAsyncTask(TwimptAsyncType twimptUpdateType, Handler handler) {
    mTwimptUpdateType = twimptUpdateType;
    mHandler = handler;
  }

  void setDataApplication(DataApplication dataApplication) {
    mGlobals = dataApplication;
  }

  void setPostMessage(String message) {
    mPostMessage = message;
  }

  void setAccessToken(final String access_token, final String access_token_secret) {
    mAccessToken = access_token;
    mAccessTokenSecret = access_token_secret;
  }

  public static boolean isConnected(Context context) {
    ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo ni = cm.getActiveNetworkInfo();
    if (ni != null) {
      return cm.getActiveNetworkInfo().isConnected();
    }
    return false;
  }

  @Override
  protected void onPreExecute() {
    //if (mGlobals != null)
     // mTwimptRoom = mGlobals.twimptRooms.get(now_room_hash);
    // ここに前処理を記述します
    // 例） プログレスダイアログ表示
  }

  @Override
  protected TwimptAsyncTaskParams doInBackground(Void... a) {
    // バックグランド処理をここに記述します
    TwimptAsyncTaskParams twimptAsyncTaskParams = new TwimptAsyncTaskParams();
    twimptAsyncTaskParams.mTwimptRoom = mTwimptRoom;

    Log.d("RoomActivity.java", "doInBackground");
    try {
      twimptAsyncTaskParams.mJSONObjecton = Update();
    } catch (JSONException e) {
      Log.e("TwimptAsyncTask.java", e.toString());
      e.printStackTrace();
    } catch (IOException e) {
      Log.e("TwimptAsyncTask.java", e.toString());
      e.printStackTrace();
    }
    return twimptAsyncTaskParams;
  }


  private void RoomDataParse() {

  }

  @Override
  protected void onPostExecute(TwimptAsyncTaskParams result) {
    // バックグランド処理終了後の処理をここに記述します
    // 例） プログレスダイアログ終了
    //    UIコンポーネントへの処理
    Log.d("RoomActivity.java", "onPostExecute");
    Message msg = Message.obtain();
    msg.obj = result;
    mHandler.sendMessage(msg);
      /*try {
        textView.setText(result.toString(2));
      } catch (JSONException e) {
        e.printStackTrace();
      }*/
  }

  JSONObject Update() throws IOException, JSONException {
    JSONObject json = null;
    int size = mTwimptRoom.dataList.size();
    String latest_log_hash = null;//= new String();
    switch (mTwimptUpdateType) {
      case UPDATE:
        if (size > 0) latest_log_hash = mTwimptRoom.dataList.get(0).hash;
        return Twimpt.UpdateRequest(mTwimptRoom.type, mTwimptRoom.hash, latest_log_hash, mTwimptRoom.LatestModifyHash);

      case LOG:
        String oldest_log_hash = null;//= new String();
        if (size > 0) oldest_log_hash = mTwimptRoom.dataList.get(size - 1).hash;
        return Twimpt.LogRequest(mTwimptRoom.type, mTwimptRoom.hash, oldest_log_hash);

      case POST:
        if (size > 0) latest_log_hash = mTwimptRoom.dataList.get(0).hash;
        return Twimpt.PostRequest(mAccessToken, mAccessTokenSecret, mTwimptRoom.type, mTwimptRoom.hash,
                mPostMessage, mTwimptRoom.type, mTwimptRoom.hash, latest_log_hash, mTwimptRoom.LatestModifyHash);

      case RECENT_CREATED_ROOM:
        return Twimpt.GetRecentCreatedRoomDataList(1);
    }
    return null;
  }
}

class TwimptRecentRoomAsyncTask extends AsyncTask<Void, Void, JSONObject> {

  @Override
  protected JSONObject doInBackground(Void... voids) {
    return null;
  }
}