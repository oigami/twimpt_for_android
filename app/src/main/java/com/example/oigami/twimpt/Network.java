package com.example.oigami.twimpt;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by oigami on 2015/02/12
 */
public class Network {
  public static boolean isConnected(Context context) {
    ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo ni = cm.getActiveNetworkInfo();
    if (ni != null) {
      return cm.getActiveNetworkInfo().isConnected();
    }
    return false;
  }
}
