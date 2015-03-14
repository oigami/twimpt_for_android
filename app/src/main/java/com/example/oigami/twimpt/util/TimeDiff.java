package com.example.oigami.twimpt.util;

import java.util.Calendar;

/**
 * Created by oigami on 2014/11/04
 */
public class TimeDiff {

  //現在の日時と比較して、時間の差分を求めるメソッド
  public static String toDiffDate(long timeInMillis) {
    //比較対象日時
    //long型の差分（ミリ秒）
    long diffTime = System.currentTimeMillis() - timeInMillis;
    //秒
    long second = diffTime / 1000;
    if (second < 60) {
      return second + "秒";
    }
    //分
    long minute = second / 60;
    if (minute < 60) {
      return minute + "分";
    }
    //時
    long hour = minute / 60;
    if (hour < 24) {
      return hour + "時間";
    }
    //日
    long day = hour / 24;
    if (day <= 28) {
      return day + "日";
    }
    Calendar dateCal = Calendar.getInstance();
    dateCal.setTimeInMillis(timeInMillis);
    //現在の日時
    Calendar nowCal = Calendar.getInstance();
    //30日以上の場合
    //月＋1
    dateCal.add(Calendar.MONTH, 1);
    if (dateCal.after(nowCal)) {
      return day + "日";   //一ヶ月以内
    }
    dateCal.setTimeInMillis(timeInMillis);
    dateCal.add(Calendar.MONTH, 12);    //12ヶ月 増やす
    if (dateCal.after(nowCal)) {//一年（12ヶ月）以内
      for (int i = 11; i >= 1; i--) {
        //dateCal.setTimeInMillis(timeInMillis);
        dateCal.add(Calendar.MONTH, -1); //１ヶ月ずつ引いていく
        if (dateCal.before(nowCal)) {
          return i + "ヶ月";   //iヶ月　前
        }
      }
    }
    return "1年";    //1年前
  }
}
