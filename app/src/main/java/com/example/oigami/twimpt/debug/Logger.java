package com.example.oigami.twimpt.debug;

import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class Logger {
  public static void log(String msg) {
    StackTraceElement calledClass = Thread.currentThread().getStackTrace()[3];
    if (msg == null) {
      msg = "";
    }
    Log.d(calledClass.getFileName() + ":"
            + calledClass.getLineNumber(), msg);
  }

  public static String getStackTraceString(Exception e) {
    Writer writer = new StringWriter();
    PrintWriter printWriter = new PrintWriter(writer);
    e.printStackTrace(printWriter);
    return writer.toString();
  }
}