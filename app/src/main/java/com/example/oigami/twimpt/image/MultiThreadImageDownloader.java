package com.example.oigami.twimpt.image;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by oigami on 2014/10/02.
 */
public class MultiThreadImageDownloader {

  public static void execute(ImageCacheDB db,Context context, List<String> urls) {
    ArrayList<String> list1 = new ArrayList<String>();
    ArrayList<String> list2 = new ArrayList<String>();
    ArrayList<String> list3 = new ArrayList<String>();
    int i = 0;
    for (String url : urls) {
      switch (i % 3) {
        case 0: list1.add(url); break;
        case 1: list2.add(url); break;
        case 2: list3.add(url); break;
      }
      i++;
    }
    new ImageDownloadCacheTask(db,context).execute((String[])list1.toArray(new String[0]));
    new ImageDownloadCacheTask(db,context).execute((String[])list2.toArray(new String[0]));
    new ImageDownloadCacheTask(db,context).execute((String[])list3.toArray(new String[0]));
  }
}